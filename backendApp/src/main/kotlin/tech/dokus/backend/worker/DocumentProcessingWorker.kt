package tech.dokus.backend.worker

import ai.koog.http.client.KoogHttpClientException
import ai.koog.prompt.executor.clients.LLMClientException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.slf4j.MDC
import tech.dokus.backend.services.documents.AutoConfirmPolicy
import tech.dokus.backend.services.documents.ContactResolutionService
import tech.dokus.backend.services.documents.DocumentPurposeService
import tech.dokus.backend.services.documents.DocumentTruthService
import tech.dokus.backend.services.documents.confirmation.DocumentConfirmationDispatcher
import tech.dokus.database.entity.IngestionItemEntity
import tech.dokus.database.repository.auth.TenantRepository
import tech.dokus.database.repository.auth.UserRepository
import tech.dokus.database.repository.cashflow.DocumentDraftRepository
import tech.dokus.database.repository.processor.ProcessorIngestionRepository
import tech.dokus.domain.enums.ContactLinkSource
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.ids.IngestionRunId
import tech.dokus.domain.model.contact.ContactResolution
import tech.dokus.domain.processing.DocumentProcessingConstants
import tech.dokus.domain.utils.json
import tech.dokus.features.ai.agents.DocumentProcessingAgent
import tech.dokus.features.ai.graph.AcceptDocumentInput
import tech.dokus.features.ai.models.DirectionResolutionSource
import tech.dokus.features.ai.models.confidenceScore
import tech.dokus.features.ai.models.toAuthoritativeCounterpartySnapshot
import tech.dokus.features.ai.models.toDraftData
import tech.dokus.features.ai.models.toProcessingOutcome
import tech.dokus.features.ai.validation.CheckType
import tech.dokus.foundation.backend.config.ProcessorConfig
import tech.dokus.foundation.backend.utils.loggerFor
import tech.dokus.foundation.backend.utils.runSuspendCatching
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration

/**
 * Background worker that polls for pending documents and processes them with AI extraction.
 *
 * KOOG GRAPH Architecture:
 * 1. Graph injects tenant context + document images
 * 2. Classify → Extract → Validate inside graph
 * 3. Backend persists draft + audit output
 *
 * Features:
 * - Polling-based processing (configurable interval)
 * - Automatic retry with backoff
 * - Graceful shutdown support
 * - Example-based few-shot learning for repeat vendors
 */
@Suppress("LongParameterList")
class DocumentProcessingWorker(
    private val ingestionRepository: ProcessorIngestionRepository,
    private val processingAgent: DocumentProcessingAgent,
    private val contactResolutionService: ContactResolutionService,
    private val purposeService: DocumentPurposeService,
    private val documentTruthService: DocumentTruthService,
    private val draftRepository: DocumentDraftRepository,
    private val autoConfirmPolicy: AutoConfirmPolicy,
    private val confirmationDispatcher: DocumentConfirmationDispatcher,
    private val config: ProcessorConfig,
    private val tenantRepository: TenantRepository,
    private val userRepository: UserRepository,
) {
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val logger = loggerFor()
    private var pollingJob: Job? = null
    private val isRunning = AtomicBoolean(false)

    private enum class AttemptResult(val label: String) {
        Succeeded("succeeded"),
        FailedTimeout("failed_timeout"),
        FailedProvider("failed_provider"),
        FailedValidation("failed_validation"),
        FailedCancelled("failed_cancelled"),
        FailedUnexpected("failed_unexpected"),
        SkippedAlreadyClaimed("skipped_already_claimed"),
    }

    /**
     * Start the processing worker.
     */
    fun start() {
        if (!isRunning.compareAndSet(false, true)) {
            logger.warn("Worker already running")
            return
        }

        logger.info(
            "Starting worker (KOOG-GRAPH): interval=${config.pollingInterval}ms, " +
                "batchSize=${config.batchSize}, maxConcurrentRuns=${config.maxConcurrentRuns}"
        )

        pollingJob = scope.launch {
            while (isActive && isRunning.get()) {
                try {
                    processBatch()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.error("Error in processing loop", e)
                }

                delay(config.pollingInterval)
            }
        }
    }

    /**
     * Stop the processing worker gracefully.
     */
    fun stop() {
        if (!isRunning.compareAndSet(true, false)) {
            logger.warn("Worker not running")
            return
        }

        logger.info("Stopping document processing worker...")
        pollingJob?.cancel()
        pollingJob = null
        logger.info("Document processing worker stopped")
    }

    /**
     * Process a batch of pending ingestion runs concurrently.
     * Queue fetch size is controlled by batchSize and execution parallelism by maxConcurrentRuns.
     */
    private suspend fun processBatch(
        timeout: Duration = DocumentProcessingConstants.INGESTION_RUN_TIMEOUT
    ) {
        // Recover any runs stuck in Processing from a previous crash
        runSuspendCatching {
            val recovered = ingestionRepository.recoverStaleRuns()
            if (recovered > 0) {
                logger.info("Recovered $recovered stale ingestion run(s) (marked as Failed)")
            }
        }.onFailure { e ->
            logger.error("Failed to recover stale runs", e)
        }

        // Find queued ingestion runs
        val pending = ingestionRepository.findPendingForProcessing(
            limit = config.batchSize
        )

        if (pending.isEmpty()) {
            logger.debug("No pending ingestion runs to process")
            return
        }

        val queueDepthAtClaim = pending.size
        logger.info("Found ${pending.size} pending ingestion runs to process (queueDepthAtClaim=$queueDepthAtClaim)")

        val semaphore = Semaphore(config.maxConcurrentRuns)

        supervisorScope {
            pending.map { ingestion ->
                async {
                    if (!isRunning.get()) return@async
                    semaphore.withPermit {
                        val startedAtNanos = System.nanoTime()
                        var attemptResult = AttemptResult.FailedUnexpected
                        try {
                            val claimed = ingestionRepository.markAsProcessing(ingestion.runId.toString(), "koog-graph")
                            if (!claimed) {
                                logger.info(
                                    "Skipping ingestion run {} for document {} because it was already claimed",
                                    ingestion.runId,
                                    ingestion.documentId
                                )
                                attemptResult = AttemptResult.SkippedAlreadyClaimed
                                return@withPermit
                            }
                            attemptResult = processIngestionRunWithTimeout(ingestion, timeout)
                        } catch (e: CancellationException) {
                            if (!isRunning.get()) throw e
                            logger.error(
                                "Unexpected cancellation while processing ingestion run ${ingestion.runId} " +
                                    "for document ${ingestion.documentId}",
                                e
                            )
                            markRunFailedSafely(
                                ingestion.runId,
                                "Processing cancelled: ${e.message ?: "Unknown cancellation"}"
                            )
                            attemptResult = AttemptResult.FailedCancelled
                        } catch (e: Exception) {
                            logger.error(
                                "Failed to process ingestion run ${ingestion.runId} " +
                                    "for document ${ingestion.documentId}",
                                e
                            )
                            val providerFailure = isProviderFailure(e)
                            markRunFailedSafely(
                                ingestion.runId,
                                if (providerFailure) {
                                    "Processing provider error: ${e.message ?: "Unknown provider error"}"
                                } else {
                                    "Processing error: ${e.message ?: "Unknown error"}"
                                }
                            )
                            attemptResult = if (providerFailure) {
                                AttemptResult.FailedProvider
                            } else {
                                AttemptResult.FailedUnexpected
                            }
                        } finally {
                            logIngestionAttemptComplete(
                                ingestion = ingestion,
                                queueDepthAtClaim = queueDepthAtClaim,
                                startedAtNanos = startedAtNanos,
                                attemptResult = attemptResult
                            )
                        }
                    }
                }
            }.awaitAll()
        }
    }

    /** Test-only entry point. Must not be called concurrently with [start]/[stop]. */
    internal suspend fun processBatchForTest(timeout: Duration) {
        isRunning.set(true)
        try {
            processBatch(timeout = timeout)
        } finally {
            isRunning.set(false)
        }
    }

    private suspend fun processIngestionRunWithTimeout(
        ingestion: IngestionItemEntity,
        timeout: Duration = DocumentProcessingConstants.INGESTION_RUN_TIMEOUT
    ): AttemptResult {
        return try {
            withTimeout(timeout) {
                processIngestionRun(ingestion)
            }
        } catch (e: TimeoutCancellationException) {
            val timeoutMessage = DocumentProcessingConstants.INGESTION_TIMEOUT_ERROR_MESSAGE
            logger.error(
                "Ingestion run {} for document {} exceeded timeout {}; marking as failed",
                ingestion.runId,
                ingestion.documentId,
                timeout,
                e
            )
            markRunFailedSafely(ingestion.runId, timeoutMessage)
            AttemptResult.FailedTimeout
        } finally {
            // Defensive cleanup: processIngestionRun sets MDC keys and clears them in its own
            // finally block. If a timeout + parent cancellation prevents that finally from running,
            // stale MDC keys would leak onto the shared thread pool.
            MDC.remove("runId")
            MDC.remove("documentId")
            MDC.remove("tenantId")
        }
    }

    private suspend fun markRunFailedSafely(runId: IngestionRunId, message: String) {
        withContext(NonCancellable) {
            try {
                ingestionRepository.markAsFailed(runId.toString(), message)
            } catch (markError: Exception) {
                logger.error("Failed to mark ingestion run {} as failed", runId, markError)
            }
        }
    }

    private fun logIngestionAttemptComplete(
        ingestion: IngestionItemEntity,
        queueDepthAtClaim: Int,
        startedAtNanos: Long,
        attemptResult: AttemptResult
    ) {
        val processingTimeMs = (System.nanoTime() - startedAtNanos) / 1_000_000
        logger.info(
            "Ingestion attempt completed: runId={}, documentId={}, queueDepthAtClaim={}, processingTimeMs={}, " +
                "maxConcurrentRuns={}, attemptResult={}",
            ingestion.runId,
            ingestion.documentId,
            queueDepthAtClaim,
            processingTimeMs,
            config.maxConcurrentRuns,
            attemptResult.label
        )
    }

    private fun isProviderFailure(error: Throwable): Boolean {
        var current: Throwable? = error
        while (current != null) {
            if (current is LLMClientException || current is KoogHttpClientException) return true
            current = current.cause
        }
        return false
    }

    /**
     * Process a single ingestion run.
     *
     * Each ingestion run is a single processing attempt. If it fails, it stays failed.
     * Retries are handled via the /reprocess endpoint which creates new runs.
     */
    private suspend fun processIngestionRun(ingestion: IngestionItemEntity): AttemptResult {
        val runId = ingestion.runId
        val documentId = ingestion.documentId
        val tenantId = ingestion.tenantId

        MDC.put("runId", runId.toString())
        MDC.put("documentId", documentId.toString())
        MDC.put("tenantId", tenantId.toString())

        logger.info("Processing ingestion run: $runId for document: $documentId")

        try {
            // Fetch tenant context for improved invoice classification and direction resolution
            val parsedTenantId = tenantId
            val tenant = tenantRepository.findById(parsedTenantId)
                ?: error("Tenant not found: $tenantId")
            val sourceChannel = ingestion.sourceChannel ?: ingestion.documentSource
            if (ingestion.sourceChannel == null) {
                logger.warn(
                    "Missing source channel for run {} document {}; using document source {}",
                    runId,
                    documentId,
                    sourceChannel
                )
            }

            val members = userRepository.listByTenant(parsedTenantId, activeOnly = true)
            val personNames = members.mapNotNull { m ->
                listOfNotNull(m.user.firstName?.value, m.user.lastName?.value)
                    .joinToString(" ").ifBlank { null }
            }

            if (sourceChannel == DocumentSource.Peppol &&
                ingestion.peppolStructuredSnapshotJson.isNullOrBlank()
            ) {
                logger.warn(
                    "PEPPOL run {} has no structured snapshot; falling back to vision extraction",
                    runId
                )
            }

            val result = processingAgent.process(
                AcceptDocumentInput(
                    documentId = documentId,
                    tenant = tenant,
                    sourceChannel = sourceChannel,
                    peppolStructuredSnapshotJson = ingestion.peppolStructuredSnapshotJson,
                    peppolSnapshotVersion = ingestion.peppolSnapshotVersion,
                    associatedPersonNames = personNames,
                    userFeedback = ingestion.userFeedback,
                    maxPagesOverride = ingestion.overrideMaxPages,
                    dpiOverride = ingestion.overrideDpi
                )
            )

            val counterpartyInvariantFailure = result.auditReport.criticalFailures.firstOrNull {
                it.type == CheckType.COUNTERPARTY_INTEGRITY
            }
            if (counterpartyInvariantFailure != null) {
                val errorMessage = "Counterparty invariant violation: ${counterpartyInvariantFailure.message}"
                logger.warn(
                    "Failing ingestion run {} for document {}: {}",
                    runId,
                    documentId,
                    errorMessage
                )
                markRunFailedSafely(runId, errorMessage)
                return AttemptResult.FailedValidation
            }

            val processingOutcome = result.toProcessingOutcome()
            val documentType = result.classification.documentType
            val confidence = minOf(
                result.classification.confidence,
                result.extraction.confidenceScore()
            )
            val draftData = result.toDraftData()

            val rawExtractionJson = json.encodeToString(result)

            ingestionRepository.markAsSucceeded(
                runId = runId.toString(),
                tenantId = tenantId.toString(),
                documentId = documentId.toString(),
                documentType = documentType,
                draftData = draftData,
                rawExtractionJson = rawExtractionJson,
                confidence = confidence,
                processingOutcome = processingOutcome,
                rawText = null,
                keywords = emptyList(),
                force = false
            )

            if (draftData != null) {
                val matchOutcome = documentTruthService.applyPostExtractionMatching(
                    tenantId = parsedTenantId,
                    documentId = documentId,
                    sourceId = ingestion.sourceId,
                    draftData = draftData,
                    extractedSnapshotJson = json.encodeToString(draftData)
                )
                if (matchOutcome.documentId != documentId ||
                    matchOutcome.outcome == tech.dokus.domain.enums.DocumentIntakeOutcome.PendingMatchReview
                ) {
                    logger.info(
                        "Document {} source {} resolved by truth matcher: outcome={}, target={}",
                        documentId,
                        ingestion.sourceId,
                        matchOutcome.outcome,
                        matchOutcome.documentId
                    )
                    return AttemptResult.Succeeded
                }

                // Ensure drafts start in NeedsReview; auto-confirm will set Confirmed explicitly.
                draftRepository.updateDocumentStatus(
                    documentId = documentId,
                    tenantId = parsedTenantId,
                    status = DocumentStatus.NeedsReview
                )

                var linkedContactId: tech.dokus.domain.ids.ContactId? = null
                val authoritativeSnapshot = result.extraction.toAuthoritativeCounterpartySnapshot()
                if (authoritativeSnapshot == null) {
                    logger.warn(
                        "Missing authoritative counterparty snapshot for document {} in run {}; forcing PendingReview",
                        documentId,
                        runId
                    )
                    draftRepository.updateContactResolution(
                        documentId = documentId,
                        tenantId = parsedTenantId,
                        contactSuggestions = emptyList(),
                        counterpartySnapshot = null,
                        matchEvidence = null,
                        linkedContactId = null,
                        linkedContactSource = null
                    )
                } else {
                    val resolution = contactResolutionService.resolve(
                        tenantId = parsedTenantId,
                        draftData = draftData,
                        authoritativeSnapshot = authoritativeSnapshot,
                        tenantVat = tenant.vatNumber
                    )
                    when (val decision = resolution.resolution) {
                        is ContactResolution.Matched -> {
                            linkedContactId = decision.contactId
                            draftRepository.updateContactResolution(
                                documentId = documentId,
                                tenantId = parsedTenantId,
                                contactSuggestions = emptyList(),
                                counterpartySnapshot = resolution.snapshot,
                                matchEvidence = decision.evidence,
                                linkedContactId = decision.contactId,
                                linkedContactSource = ContactLinkSource.AI
                            )
                        }

                        is ContactResolution.AutoCreate -> {
                            val contactId = contactResolutionService.createContactFromResolution(
                                tenantId = parsedTenantId,
                                resolution = decision
                            )
                            linkedContactId = contactId
                            draftRepository.updateContactResolution(
                                documentId = documentId,
                                tenantId = parsedTenantId,
                                contactSuggestions = emptyList(),
                                counterpartySnapshot = resolution.snapshot,
                                matchEvidence = decision.evidence,
                                linkedContactId = contactId,
                                linkedContactSource = if (contactId != null) ContactLinkSource.AI else null
                            )
                        }

                        is ContactResolution.Suggested -> {
                            draftRepository.updateContactResolution(
                                documentId = documentId,
                                tenantId = parsedTenantId,
                                contactSuggestions = decision.candidates,
                                counterpartySnapshot = resolution.snapshot,
                                matchEvidence = null,
                                linkedContactId = null,
                                linkedContactSource = null
                            )
                        }

                        is ContactResolution.PendingReview -> {
                            draftRepository.updateContactResolution(
                                documentId = documentId,
                                tenantId = parsedTenantId,
                                contactSuggestions = emptyList(),
                                counterpartySnapshot = resolution.snapshot,
                                matchEvidence = null,
                                linkedContactId = null,
                                linkedContactSource = null
                            )
                        }
                    }
                }

                val currentDraft = draftRepository.getByDocumentId(documentId, parsedTenantId)
                if (currentDraft != null) {
                    runSuspendCatching {
                        purposeService.enrichAfterContactResolution(
                            tenantId = parsedTenantId,
                            documentId = documentId,
                            documentType = documentType,
                            draftData = draftData,
                            linkedContactId = linkedContactId,
                            currentDraft = currentDraft
                        )
                    }.onFailure { e ->
                        logger.warn("Purpose enrichment failed for document {}, skipping", documentId, e)
                    }
                }

                val canAutoConfirm = autoConfirmPolicy.canAutoConfirm(
                    tenantId = parsedTenantId,
                    documentId = documentId,
                    source = sourceChannel,
                    documentType = documentType,
                    draftData = draftData,
                    auditPassed = result.auditReport.isValid,
                    confidence = confidence,
                    linkedContactId = linkedContactId,
                    directionResolvedFromAiHintOnly = result.directionResolution.source == DirectionResolutionSource.AiHint
                )

                if (canAutoConfirm) {
                    try {
                        confirmationDispatcher.confirm(
                            parsedTenantId,
                            documentId,
                            draftData,
                            linkedContactId
                        ).getOrThrow()
                    } catch (e: Exception) {
                        logger.error("Auto-confirm failed for document $documentId", e)
                        draftRepository.updateDocumentStatus(
                            documentId = documentId,
                            tenantId = parsedTenantId,
                            status = DocumentStatus.NeedsReview
                        )
                    }
                }
            }
            return AttemptResult.Succeeded
        } catch (e: CancellationException) {
            if (!isRunning.get()) throw e
            logger.error("Unexpected cancellation while processing document $documentId", e)
            markRunFailedSafely(runId, "Processing cancelled: ${e.message ?: "Unknown cancellation"}")
            return AttemptResult.FailedCancelled
        } catch (e: Exception) {
            val providerFailure = isProviderFailure(e)
            logger.error("Unexpected error processing document $documentId", e)
            markRunFailedSafely(
                runId,
                if (providerFailure) {
                    "Processing provider error: ${e.message ?: "Unknown provider error"}"
                } else {
                    "Processing error: ${e.message ?: "Unknown error"}"
                }
            )
            return if (providerFailure) AttemptResult.FailedProvider else AttemptResult.FailedUnexpected
        } finally {
            MDC.remove("runId")
            MDC.remove("documentId")
            MDC.remove("tenantId")
        }
    }
}
