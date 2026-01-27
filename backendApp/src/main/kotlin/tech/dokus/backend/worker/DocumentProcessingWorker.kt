package tech.dokus.backend.worker

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.builtins.ListSerializer
import org.slf4j.MDC
import tech.dokus.database.entity.IngestionItemEntity
import tech.dokus.database.repository.auth.AddressRepository
import tech.dokus.database.repository.auth.TenantRepository
import tech.dokus.database.repository.processor.ProcessorIngestionRepository
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.utils.json
import tech.dokus.features.ai.orchestrator.DocumentOrchestrator
import tech.dokus.features.ai.orchestrator.OrchestratorResult
import tech.dokus.features.ai.orchestrator.ProcessingStep
import tech.dokus.features.ai.prompts.AgentPrompt
import tech.dokus.foundation.backend.config.IntelligenceMode
import tech.dokus.foundation.backend.config.ProcessorConfig
import tech.dokus.foundation.backend.utils.loggerFor
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Background worker that polls for pending documents and processes them with AI extraction.
 *
 * ORCHESTRATOR Architecture:
 * 1. Orchestrator fetches document images via tools
 * 2. Orchestrator classifies, extracts, validates, and enriches
 * 3. Orchestrator persists extraction + RAG indexing via tools
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
    private val orchestrator: DocumentOrchestrator,
    private val config: ProcessorConfig,
    private val mode: IntelligenceMode,
    private val tenantRepository: TenantRepository,
    private val addressRepository: AddressRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
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

        logger.info(
            "Starting worker (ORCHESTRATOR-FIRST): interval=${config.pollingInterval}ms, " +
                    "batch=${config.batchSize}, concurrency=${mode.maxConcurrentRequests}"
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
    private suspend fun processBatch() {
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
        val semaphore = Semaphore(mode.maxConcurrentRequests)

        supervisorScope {
            pending.map { ingestion ->
                async {
                    if (!isRunning.get()) return@async
                    semaphore.withPermit {
                        try {
                            processIngestionRun(ingestion)
                        } catch (e: Exception) {
                            logger.error(
                                "Failed to process ingestion run ${ingestion.runId} " +
                                        "for document ${ingestion.documentId}",
                                e
                            )
                        }
                    }
                }
            }.awaitAll()
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

        MDC.put("runId", runId)
        MDC.put("documentId", documentId)
        MDC.put("tenantId", tenantId)

        logger.info("Processing ingestion run: $runId for document: $documentId")

        // Mark as processing
        ingestionRepository.markAsProcessing(runId, "5-Layer Autonomous Pipeline")

        try {
            // Fetch tenant context for improved INVOICE vs BILL classification
            val parsedTenantId = TenantId.parse(tenantId)
            val tenant = tenantRepository.findById(parsedTenantId)
                ?: error("Tenant not found: $tenantId")
            val address = addressRepository.getCompanyAddress(parsedTenantId)
                ?: error("Address not found for tenant: $tenantId")
            val tenantContext = AgentPrompt.TenantContext(
                vatNumber = tenant.vatNumber,
                companyName = tenant.legalName,
                address = address
            )

            // Process document through DocumentOrchestrator (tool-calling orchestrator)
            val processingResult = processDocument(
                tenantContext = tenantContext,
                runId = runId,
                documentId = documentId,
                tenantId = tenantId,
                maxPages = ingestion.overrideMaxPages,
                dpi = ingestion.overrideDpi
            )

            when (processingResult) {
                is OrchestratorResult.Success -> {
                    logger.info(
                        "Processed doc $documentId: type=${processingResult.documentType}, " +
                                "conf=${processingResult.confidence}, validated=${processingResult.validationPassed}"
                    )
                }

                is OrchestratorResult.NeedsReview -> {
                    logger.warn(
                        "Document $documentId needs review: ${processingResult.reason} " +
                                "(${processingResult.issues.size} issues)"
                    )
                }

                is OrchestratorResult.Failed -> {
                    logger.error("Document $documentId failed: ${processingResult.reason} at ${processingResult.stage}")
                    val status = ingestionRepository.getRunStatus(runId)
                    if (status == IngestionStatus.Processing || status == IngestionStatus.Queued) {
                        ingestionRepository.markAsFailed(runId, "Failed: ${processingResult.reason}")
                    }
                    return
                }
            }

            // Guard against runs left in Processing if the orchestrator skipped persistence.
            val runStatus = ingestionRepository.getRunStatus(runId)
            if (runStatus == IngestionStatus.Processing) {
                ingestionRepository.markAsFailed(
                    runId,
                    "Orchestrator completed without persisting results"
                )
            }
        } catch (e: Exception) {
            logger.error("Unexpected error processing document $documentId", e)
            ingestionRepository.markAsFailed(runId, "Processing error: ${e.message}")
        } finally {
            MDC.remove("runId")
            MDC.remove("documentId")
            MDC.remove("tenantId")
        }
    }

    // =========================================================================
    // Processing Methods
    // =========================================================================

    /**
     * Process a document using the DocumentOrchestrator.
     */
    private suspend fun processDocument(
        tenantContext: AgentPrompt.TenantContext,
        runId: String,
        documentId: DocumentId,
        tenantId: TenantId,
    ): OrchestratorResult {
        logger.info("Processing document $documentId with DocumentOrchestrator")

        val result = orchestrator.process(
            documentId = documentId,
            tenantId = tenantId,
            tenantContext = tenantContext,
            runId = runId,
            maxPages = maxPages,
            dpi = dpi
        )

        // Log audit trail
        val auditTrail = when (result) {
            is OrchestratorResult.Success -> result.auditTrail
            is OrchestratorResult.NeedsReview -> result.auditTrail
            is OrchestratorResult.Failed -> result.auditTrail
        }
        auditTrail.forEach { step ->
            logger.debug("Step ${step.step}: ${step.action} (${step.durationMs}ms)")
        }

        // Persist processing trace for observability
        val trace = when (result) {
            is OrchestratorResult.Success -> result.auditTrail
            is OrchestratorResult.NeedsReview -> result.auditTrail
            is OrchestratorResult.Failed -> result.auditTrail
        }
        runCatching {
            val traceJson = json.encodeToString(
                ListSerializer(ProcessingStep.serializer()),
                trace
            )
            ingestionRepository.updateProcessingTrace(runId, traceJson)
        }.onFailure { e ->
            logger.warn("Failed to persist processing trace for runId=$runId", e)
        }

        return result
    }
}