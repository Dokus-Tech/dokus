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
import org.slf4j.MDC
import tech.dokus.database.entity.IngestionItemEntity
import tech.dokus.database.repository.auth.TenantRepository
import tech.dokus.database.repository.cashflow.DocumentDraftRepository
import tech.dokus.database.repository.processor.ProcessorIngestionRepository
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.enums.ContactLinkSource
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.utils.json
import tech.dokus.features.ai.agents.DocumentProcessingAgent
import tech.dokus.features.ai.graph.AcceptDocumentInput
import tech.dokus.features.ai.models.confidenceScore
import tech.dokus.features.ai.models.toDraftData
import tech.dokus.features.ai.models.toProcessingOutcome
import tech.dokus.foundation.backend.config.IntelligenceMode
import tech.dokus.foundation.backend.config.ProcessorConfig
import tech.dokus.foundation.backend.utils.loggerFor
import tech.dokus.backend.services.documents.ContactResolutionService
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
    private val config: ProcessorConfig,
    private val mode: IntelligenceMode,
    private val tenantRepository: TenantRepository,
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
        val semaphore = Semaphore(config.batchSize.coerceAtLeast(1))

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

        MDC.put("runId", runId.toString())
        MDC.put("documentId", documentId.toString())
        MDC.put("tenantId", tenantId.toString())

        logger.info("Processing ingestion run: $runId for document: $documentId")

        // Mark as processing
        ingestionRepository.markAsProcessing(runId.toString(), "koog-graph")

        try {
            // Fetch tenant context for improved INVOICE vs BILL classification
            val parsedTenantId = tenantId
            val tenant = tenantRepository.findById(parsedTenantId)
                ?: error("Tenant not found: $tenantId")

            val result = processingAgent.process(
                AcceptDocumentInput(
                    documentId = documentId,
                    tenant = tenant
                )
            )

            val processingOutcome = result.toProcessingOutcome()
            val documentType = result.classification.documentType
            val confidence = minOf(
                result.classification.confidence,
                result.extraction.confidenceScore()
            )
            val draftData = result.extraction.toDraftData()

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
                val resolution = contactResolutionService.resolve(parsedTenantId, draftData)
                when (val decision = resolution.resolution) {
                    is tech.dokus.domain.model.contact.ContactResolution.Matched -> {
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
                    is tech.dokus.domain.model.contact.ContactResolution.AutoCreate -> {
                        val contactId = contactResolutionService.createContactFromResolution(
                            tenantId = parsedTenantId,
                            resolution = decision
                        )
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
                    is tech.dokus.domain.model.contact.ContactResolution.Suggested -> {
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
                    is tech.dokus.domain.model.contact.ContactResolution.PendingReview -> {
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
        } catch (e: Exception) {
            logger.error("Unexpected error processing document $documentId", e)
            ingestionRepository.markAsFailed(runId.toString(), "Processing error: ${e.message}")
        } finally {
            MDC.remove("runId")
            MDC.remove("documentId")
            MDC.remove("tenantId")
        }
    }

    // Orchestrator-based processing removed; Koog graph is now the production path.
}
