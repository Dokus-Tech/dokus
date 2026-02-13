package tech.dokus.backend.worker

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
import tech.dokus.backend.services.documents.confirmation.DocumentConfirmationDispatcher
import tech.dokus.backend.util.runSuspendCatching
import tech.dokus.database.entity.IngestionItemEntity
import tech.dokus.database.repository.auth.TenantRepository
import tech.dokus.database.repository.auth.UserRepository
import tech.dokus.database.repository.cashflow.DocumentDraftRepository
import tech.dokus.database.repository.cashflow.DocumentRepository
import tech.dokus.database.repository.processor.ProcessorIngestionRepository
import tech.dokus.domain.enums.ContactLinkSource
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
import java.util.concurrent.atomic.AtomicBoolean

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
    private val draftRepository: DocumentDraftRepository,
    private val documentRepository: DocumentRepository,
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

    /**
     * Start the processing worker.
     */
    fun start() {
        if (!isRunning.compareAndSet(false, true)) {
            logger.warn("Worker already running")
            return
        }

        val concurrency = config.batchSize.coerceAtLeast(1)
        logger.info(
            "Starting worker (KOOG-GRAPH): interval=${config.pollingInterval}ms, " +
                    "batch=${config.batchSize}, concurrency=$concurrency"
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
     * Concurrency is limited by mode.maxConcurrentRequests.
     */
    private suspend fun processBatch(
        timeoutMillis: Long = DocumentProcessingConstants.INGESTION_RUN_TIMEOUT_MS
    ) {
        // Recover any runs stuck in Processing from a previous crash
        runSuspendCatching {
            val recovered = ingestionRepository.recoverStaleRuns()
            if (recovered > 0) {
                logger.warn("Recovered $recovered stale ingestion run(s) (marked as Failed)")
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

        logger.info("Found ${pending.size} pending ingestion runs to process")

        // Concurrent processing limited by mode's maxConcurrentRequests
        val semaphore = Semaphore(config.batchSize.coerceAtLeast(1))

        supervisorScope {
            pending.map { ingestion ->
                async {
                    if (!isRunning.get()) return@async
                    semaphore.withPermit {
                        try {
                            processIngestionRunWithTimeout(ingestion, timeoutMillis)
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
                        } catch (e: Exception) {
                            logger.error(
                                "Failed to process ingestion run ${ingestion.runId} " +
                                        "for document ${ingestion.documentId}",
                                e
                            )
                            markRunFailedSafely(
                                ingestion.runId,
                                "Processing error: ${e.message ?: "Unknown error"}"
                            )
                        }
                    }
                }
            }.awaitAll()
        }
    }

    internal suspend fun processBatchForTest(timeoutMillis: Long) {
        isRunning.set(true)
        try {
            processBatch(timeoutMillis = timeoutMillis)
        } finally {
            isRunning.set(false)
        }
    }

    internal suspend fun processIngestionRunWithTimeout(
        ingestion: IngestionItemEntity,
        timeoutMillis: Long = DocumentProcessingConstants.INGESTION_RUN_TIMEOUT_MS
    ) {
        try {
            withTimeout(timeoutMillis) {
                processIngestionRun(ingestion)
            }
        } catch (e: TimeoutCancellationException) {
            val timeoutMessage = DocumentProcessingConstants.ingestionTimeoutErrorMessage()
            logger.error(
                "Ingestion run {} for document {} exceeded timeout {}ms; marking as failed",
                ingestion.runId,
                ingestion.documentId,
                timeoutMillis,
                e
            )
            markRunFailedSafely(ingestion.runId, timeoutMessage)
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

    /**
     * Process a single ingestion run.
     *
     * Each ingestion run is a single processing attempt. If it fails, it stays failed.
     * Retries are handled via the /reprocess endpoint which creates new runs.
     */
    private suspend fun processIngestionRun(ingestion: IngestionItemEntity) {
        val runId = ingestion.runId
        val documentId = ingestion.documentId
        val tenantId = ingestion.tenantId

        MDC.put("runId", runId.toString())
        MDC.put("documentId", documentId.toString())
        MDC.put("tenantId", tenantId.toString())

        logger.info("Processing ingestion run: $runId for document: $documentId")

        // Mark as processing
        ingestionRepository.markAsProcessing(runId.toString(), "koog-graph")

        try {
            // Fetch tenant context for improved invoice classification and direction resolution
            val parsedTenantId = tenantId
            val tenant = tenantRepository.findById(parsedTenantId)
                ?: error("Tenant not found: $tenantId")

            val members = userRepository.listByTenant(parsedTenantId, activeOnly = true)
            val personNames = members.mapNotNull { m ->
                listOfNotNull(m.user.firstName?.value, m.user.lastName?.value)
                    .joinToString(" ").ifBlank { null }
            }

            val result = processingAgent.process(
                AcceptDocumentInput(
                    documentId = documentId,
                    tenant = tenant,
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
                return
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
                description = null,
                keywords = emptyList(),
                force = false
            )

            if (draftData != null) {
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

                val document = documentRepository.getById(parsedTenantId, documentId)
                if (document != null) {
                    val canAutoConfirm = autoConfirmPolicy.canAutoConfirm(
                        tenantId = parsedTenantId,
                        documentId = documentId,
                        source = document.source,
                        documentType = documentType,
                        draftData = draftData,
                        auditPassed = result.auditReport.isValid,
                        confidence = confidence,
                        linkedContactId = linkedContactId,
                        directionResolvedFromAiHintOnly = result.directionResolution.source == DirectionResolutionSource.AiHint
                    )

                    if (canAutoConfirm) {
                        try {
                            confirmationDispatcher.confirm(parsedTenantId, documentId, draftData, linkedContactId).getOrThrow()
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
            }
        } catch (e: CancellationException) {
            if (!isRunning.get()) throw e
            logger.error("Unexpected cancellation while processing document $documentId", e)
            markRunFailedSafely(runId, "Processing cancelled: ${e.message ?: "Unknown cancellation"}")
        } catch (e: Exception) {
            logger.error("Unexpected error processing document $documentId", e)
            markRunFailedSafely(runId, "Processing error: ${e.message ?: "Unknown error"}")
        } finally {
            MDC.remove("runId")
            MDC.remove("documentId")
            MDC.remove("tenantId")
        }
    }
}
