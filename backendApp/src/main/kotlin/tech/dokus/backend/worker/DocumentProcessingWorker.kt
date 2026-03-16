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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.slf4j.MDC
import tech.dokus.backend.mappers.from
import tech.dokus.backend.services.documents.DocumentIntakeServiceResult
import tech.dokus.backend.services.documents.postextraction.PostExtractionContext
import tech.dokus.backend.services.documents.postextraction.PostExtractionOrchestrator
import tech.dokus.backend.services.documents.postextraction.PostExtractionOutcome
import tech.dokus.backend.services.documents.sse.DocumentSsePublisher
import tech.dokus.database.entity.IngestionItemEntity
import tech.dokus.database.entity.isPeppol
import tech.dokus.database.repository.auth.TenantRepository
import tech.dokus.database.repository.auth.UserRepository
import tech.dokus.database.repository.processor.ProcessorIngestionRepository
import tech.dokus.domain.Name
import tech.dokus.domain.enums.SourceTrust
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.IngestionRunId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.buildProvenance
import tech.dokus.domain.processing.DocumentProcessingConstants
import tech.dokus.domain.utils.json
import tech.dokus.features.ai.agents.DocumentProcessingAgent
import tech.dokus.features.ai.graph.AcceptDocumentInput
import tech.dokus.features.ai.models.FinancialExtractionResult
import tech.dokus.features.ai.models.confidenceScore
import tech.dokus.features.ai.models.toDraftData
import tech.dokus.features.ai.models.toProcessingOutcome
import tech.dokus.features.ai.queue.LlmQueue
import tech.dokus.features.ai.queue.documentProcessing
import tech.dokus.features.ai.validation.CheckType
import tech.dokus.foundation.backend.config.ProcessorConfig
import tech.dokus.foundation.backend.utils.loggerFor
import tech.dokus.foundation.backend.utils.runSuspendCatching
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

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
 * - Automatic retry with backoff for transient failures
 * - Graceful shutdown support
 * - Example-based few-shot learning for repeat vendors
 */
internal class DocumentProcessingWorker(
    private val ingestionRepository: ProcessorIngestionRepository,
    private val processingAgent: DocumentProcessingAgent,
    private val documentSsePublisher: DocumentSsePublisher,
    private val config: ProcessorConfig,
    private val tenantRepository: TenantRepository,
    private val userRepository: UserRepository,
    private val llmQueue: LlmQueue,
    private val postExtractionOrchestrator: PostExtractionOrchestrator,
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
                "batchSize=${config.batchSize}, maxAttempts=${config.maxAttempts}"
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
     * Queue fetch size is controlled by batchSize. LLM concurrency is managed by [LlmQueue].
     */
    private suspend fun processBatch(
        timeout: Duration = DocumentProcessingConstants.INGESTION_RUN_TIMEOUT
    ) {
        // Recover any runs stuck in Processing from a previous crash
        runSuspendCatching {
            val recoveredRuns = ingestionRepository.recoverStaleRunsDetailed(config.maxAttempts)
            if (recoveredRuns.isNotEmpty()) {
                val requeued = recoveredRuns.count { it.requeued }
                val failed = recoveredRuns.count { !it.requeued }
                // Only publish SSE for terminal failures (not retries)
                recoveredRuns.filter { !it.requeued }.forEach { recoveredRun ->
                    documentSsePublisher.publishDocumentChanged(
                        tenantId = recoveredRun.tenantId,
                        documentId = recoveredRun.documentId
                    )
                }
                logger.info(
                    "Recovered {} stale ingestion run(s): {} re-queued for retry, {} marked as Failed",
                    recoveredRuns.size, requeued, failed
                )
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

        supervisorScope {
            pending.map { ingestion ->
                async {
                    if (!isRunning.get()) return@async
                    val startedAtNanos = System.nanoTime()
                    var attemptResult = AttemptResult.FailedUnexpected
                    try {
                        val claimed = ingestionRepository.markAsProcessing(
                            ingestion.runId.toString(),
                            "koog-graph"
                        )
                        if (!claimed) {
                            logger.info(
                                "Skipping ingestion run {} for document {} because it was already claimed",
                                ingestion.runId,
                                ingestion.documentId
                            )
                            attemptResult = AttemptResult.SkippedAlreadyClaimed
                            return@async
                        }
                        documentSsePublisher.publishDocumentChanged(
                            ingestion.tenantId,
                            ingestion.documentId
                        )
                        attemptResult = processIngestionRunWithTimeout(ingestion, timeout)
                    } catch (e: CancellationException) {
                        if (!isRunning.get()) throw e
                        logger.error(
                            "Unexpected cancellation while processing ingestion run ${ingestion.runId} " +
                                    "for document ${ingestion.documentId}",
                            e
                        )
                        handleFailure(
                            runId = ingestion.runId,
                            tenantId = ingestion.tenantId,
                            documentId = ingestion.documentId,
                            attemptCount = ingestion.attemptCount,
                            attemptResult = AttemptResult.FailedCancelled,
                            message = "Processing cancelled: ${e.message ?: "Unknown cancellation"}"
                        )
                        attemptResult = AttemptResult.FailedCancelled
                    } catch (e: Exception) {
                        logger.error(
                            "Failed to process ingestion run ${ingestion.runId} " +
                                    "for document ${ingestion.documentId}",
                            e
                        )
                        val providerFailure = isProviderFailure(e)
                        val result = if (providerFailure) {
                            AttemptResult.FailedProvider
                        } else {
                            AttemptResult.FailedUnexpected
                        }
                        handleFailure(
                            runId = ingestion.runId,
                            tenantId = ingestion.tenantId,
                            documentId = ingestion.documentId,
                            attemptCount = ingestion.attemptCount,
                            attemptResult = result,
                            message = if (providerFailure) {
                                "Processing provider error: ${e.message ?: "Unknown provider error"}"
                            } else {
                                "Processing error: ${e.message ?: "Unknown error"}"
                            }
                        )
                        attemptResult = result
                    } finally {
                        logIngestionAttemptComplete(
                            ingestion = ingestion,
                            queueDepthAtClaim = queueDepthAtClaim,
                            startedAtNanos = startedAtNanos,
                            attemptResult = attemptResult
                        )
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
            handleFailure(
                runId = ingestion.runId,
                tenantId = ingestion.tenantId,
                documentId = ingestion.documentId,
                attemptCount = ingestion.attemptCount,
                attemptResult = AttemptResult.FailedTimeout,
                message = timeoutMessage
            )
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

    /**
     * Handle a processing failure: schedule retry if transient and under maxAttempts,
     * otherwise mark as terminal failure.
     */
    private suspend fun handleFailure(
        runId: IngestionRunId,
        tenantId: TenantId,
        documentId: DocumentId,
        attemptCount: Int,
        attemptResult: AttemptResult,
        message: String,
    ) {
        withContext(NonCancellable) {
            try {
                val nextAttemptCount = attemptCount + 1
                if (attemptResult.isRetryable && nextAttemptCount < config.maxAttempts) {
                    val backoff = retryDelay(nextAttemptCount)
                    val nextAttemptAt = (Clock.System.now() + backoff).toLocalDateTime(TimeZone.UTC)
                    val scheduled = ingestionRepository.scheduleRetry(
                        runId = runId,
                        attemptCount = nextAttemptCount,
                        nextAttemptAt = nextAttemptAt,
                        error = message,
                    )
                    if (scheduled) {
                        logger.info(
                            "Scheduled retry for run {} (attempt {}/{}, backoff={}, next at {})",
                            runId, nextAttemptCount, config.maxAttempts, backoff, nextAttemptAt
                        )
                        // No SSE — document stays "in-flight" from the user's perspective
                    } else {
                        logger.warn(
                            "Failed to schedule retry for run {}, falling back to terminal failure",
                            runId
                        )
                        ingestionRepository.markAsFailed(runId.toString(), message)
                        documentSsePublisher.publishDocumentChanged(tenantId, documentId)
                    }
                } else {
                    if (attemptResult.isRetryable) {
                        logger.warn(
                            "Run {} exhausted retries ({}/{}), marking as terminal failure",
                            runId, nextAttemptCount, config.maxAttempts
                        )
                    }
                    ingestionRepository.markAsFailed(runId.toString(), message)
                    documentSsePublisher.publishDocumentChanged(tenantId, documentId)
                }
            } catch (markError: Exception) {
                logger.error("Failed to handle failure for ingestion run {}", runId, markError)
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
            "Ingestion attempt completed: runId={}, documentId={}, attempt={}/{}, " +
                "queueDepthAtClaim={}, processingTimeMs={}, attemptResult={}",
            ingestion.runId,
            ingestion.documentId,
            ingestion.attemptCount + 1,
            config.maxAttempts,
            queueDepthAtClaim,
            processingTimeMs,
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

    private val AttemptResult.isRetryable: Boolean
        get() = when (this) {
            AttemptResult.FailedTimeout,
            AttemptResult.FailedProvider,
            AttemptResult.FailedUnexpected,
            AttemptResult.FailedCancelled -> true
            AttemptResult.FailedValidation,
            AttemptResult.Succeeded,
            AttemptResult.SkippedAlreadyClaimed -> false
        }

    private fun retryDelay(attemptCount: Int): Duration = when (attemptCount) {
        1 -> 1.minutes
        2 -> 5.minutes
        else -> 15.minutes
    }

    /**
     * Process a single ingestion run.
     */
    private suspend fun processIngestionRun(ingestion: IngestionItemEntity): AttemptResult {
        val runId = ingestion.runId
        val documentId = ingestion.documentId
        val tenantId = ingestion.tenantId

        MDC.put("runId", runId.toString())
        MDC.put("documentId", documentId.toString())
        MDC.put("tenantId", tenantId.toString())

        logger.info(
            "Processing ingestion run: {} for document: {} (attempt {}/{})",
            runId, documentId, ingestion.attemptCount + 1, config.maxAttempts
        )

        try {
            // Fetch tenant context for improved invoice classification and direction resolution
            val parsedTenantId = tenantId
            val tenant = tenantRepository.findById(parsedTenantId)
                ?: error("Tenant not found: $tenantId")
            val sourceChannel = ingestion.effectiveOrigin

            val members = userRepository.listByTenant(parsedTenantId, activeOnly = true)
            val personNames = members.mapNotNull { m ->
                listOfNotNull(m.user.firstName?.value, m.user.lastName?.value)
                    .joinToString(" ")
                    .ifBlank { null }
            }.map { Name(it) }

            if (ingestion.isPeppol()) {
                logger.warn(
                    "PEPPOL run {} has no structured snapshot; falling back to vision extraction",
                    runId
                )
            }

            val result = llmQueue.documentProcessing("doc-extract:$documentId") {
                processingAgent.process(AcceptDocumentInput.from(ingestion, tenant, personNames))
            }

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
                handleFailure(
                    runId = runId,
                    tenantId = parsedTenantId,
                    documentId = documentId,
                    attemptCount = ingestion.attemptCount,
                    attemptResult = AttemptResult.FailedValidation,
                    message = errorMessage
                )
                return AttemptResult.FailedValidation
            }

            val processingOutcome = result.toProcessingOutcome()
            val documentType = result.classification.documentType
            val confidence = if (result.extraction is FinancialExtractionResult.Unsupported) {
                result.classification.confidence
            } else {
                minOf(result.classification.confidence, result.extraction.confidenceScore())
            }
            val draftData = result.toDraftData()

            val rawExtractionJson = json.encodeToString(result)

            val provenance = draftData?.buildProvenance(
                sourceTrust = SourceTrust.UploadScan,
                sourceRunId = IngestionRunId.parse(runId.toString()),
                extractionConfidence = confidence,
            )

            // Override direction field confidence with direction resolution confidence
            // when it's higher — direction resolution via VAT_MATCH (1.0) should beat
            // a stale UNKNOWN direction from a previous run with equal extraction confidence.
            val directionConfidence = result.directionResolution.confidence
            val finalProvenance = if (provenance != null && directionConfidence > confidence) {
                val directionProv = provenance.direction
                if (directionProv != null) {
                    provenance.withDirectionConfidence(directionConfidence)
                } else {
                    provenance
                }
            } else {
                provenance
            }

            ingestionRepository.markAsSucceeded(
                runId = runId,
                tenantId = tenantId,
                documentId = documentId,
                documentType = documentType,
                draftData = draftData,
                rawExtractionJson = rawExtractionJson,
                confidence = confidence,
                processingOutcome = processingOutcome,
                rawText = null,
                keywords = emptyList(),
                force = false,
                fieldProvenance = finalProvenance
            )

            val postExtractionContext = PostExtractionContext(
                tenantId = parsedTenantId,
                documentId = documentId,
                sourceId = ingestion.sourceId,
                sourceChannel = sourceChannel,
                documentType = documentType,
                draftData = draftData,
                confidence = confidence,
                auditPassed = result.auditReport.isAutoConfirmSafe,
                directionSource = result.directionResolution.source,
                extraction = result.extraction,
                tenantVat = tenant.vatNumber,
            )
            when (val outcome = postExtractionOrchestrator.orchestrate(postExtractionContext)) {
                is PostExtractionOutcome.TruthResolved -> {
                    publishResolvedDocumentState(
                        tenantId = parsedTenantId,
                        originalDocumentId = documentId,
                        matchOutcome = outcome.matchOutcome,
                    )
                }

                is PostExtractionOutcome.UnsupportedConfirmed,
                is PostExtractionOutcome.BankStatementProcessed,
                is PostExtractionOutcome.StandardProcessed,
                is PostExtractionOutcome.Skipped -> {
                    documentSsePublisher.publishDocumentChanged(parsedTenantId, documentId)
                }
            }
            return AttemptResult.Succeeded
        } catch (e: CancellationException) {
            if (!isRunning.get()) throw e
            logger.error("Unexpected cancellation while processing document $documentId", e)
            handleFailure(
                runId = runId,
                tenantId = tenantId,
                documentId = documentId,
                attemptCount = ingestion.attemptCount,
                attemptResult = AttemptResult.FailedCancelled,
                message = "Processing cancelled: ${e.message ?: "Unknown cancellation"}"
            )
            return AttemptResult.FailedCancelled
        } catch (e: Exception) {
            val providerFailure = isProviderFailure(e)
            logger.error("Unexpected error processing document $documentId", e)
            val result = if (providerFailure) AttemptResult.FailedProvider else AttemptResult.FailedUnexpected
            handleFailure(
                runId = runId,
                tenantId = tenantId,
                documentId = documentId,
                attemptCount = ingestion.attemptCount,
                attemptResult = result,
                message = if (providerFailure) {
                    "Processing provider error: ${e.message ?: "Unknown provider error"}"
                } else {
                    "Processing error: ${e.message ?: "Unknown error"}"
                }
            )
            return result
        } finally {
            MDC.remove("runId")
            MDC.remove("documentId")
            MDC.remove("tenantId")
        }
    }

    private fun publishResolvedDocumentState(
        tenantId: TenantId,
        originalDocumentId: DocumentId,
        matchOutcome: DocumentIntakeServiceResult,
    ) {
        if (matchOutcome.orphanedDocumentId == originalDocumentId) {
            documentSsePublisher.publishDocumentDeleted(tenantId, originalDocumentId)
        } else if (matchOutcome.documentId != originalDocumentId) {
            documentSsePublisher.publishDocumentChanged(tenantId, originalDocumentId)
        }

        if (matchOutcome.orphanedDocumentId == matchOutcome.documentId) {
            documentSsePublisher.publishDocumentDeleted(tenantId, matchOutcome.documentId)
        } else if (matchOutcome.documentId != originalDocumentId) {
            documentSsePublisher.publishDocumentChanged(tenantId, matchOutcome.documentId)
        }
    }
}
