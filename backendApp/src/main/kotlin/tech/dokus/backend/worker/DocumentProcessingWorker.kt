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
import org.slf4j.LoggerFactory
import tech.dokus.backend.services.contacts.ContactMatchingService
import tech.dokus.backend.worker.handlers.RAGPipelineHandler
import tech.dokus.backend.worker.models.JudgmentInfo
import tech.dokus.backend.worker.models.ProcessingResult
import tech.dokus.database.entity.IngestionItemEntity
import tech.dokus.database.repository.auth.TenantRepository
import tech.dokus.database.repository.cashflow.DocumentDraftRepository
import tech.dokus.database.repository.processor.ProcessorIngestionRepository
import tech.dokus.domain.enums.CounterpartyIntent
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.IndexingStatus
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.repository.ChunkRepository
import tech.dokus.features.ai.models.toDomainType
import tech.dokus.features.ai.models.toExtractedDocumentData
import tech.dokus.features.ai.orchestrator.DocumentOrchestrator
import tech.dokus.features.ai.orchestrator.OrchestratorResult
import tech.dokus.features.ai.prompts.AgentPrompt
import tech.dokus.features.ai.services.ChunkingService
import tech.dokus.features.ai.services.DocumentImageService
import tech.dokus.features.ai.services.EmbeddingService
import tech.dokus.features.ai.services.UnsupportedDocumentTypeException
import tech.dokus.foundation.backend.config.IntelligenceMode
import tech.dokus.foundation.backend.config.ProcessorConfig
import tech.dokus.foundation.backend.storage.DocumentStorageService
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Background worker that polls for pending documents and processes them with AI extraction.
 *
 * ORCHESTRATOR Architecture:
 * 1. Download document bytes from storage
 * 2. Convert to images using DocumentImageService
 * 3. Process images through DocumentOrchestrator (single orchestrator with tool calling)
 * 4. Persist results (orchestrator handles all business logic including contact creation)
 * 5. RAG: Orchestrator handles chunking/embedding
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
    private val documentStorage: DocumentStorageService,
    private val orchestrator: DocumentOrchestrator,
    private val documentImageService: DocumentImageService,
    private val config: ProcessorConfig,
    private val mode: IntelligenceMode,
    private val draftRepository: DocumentDraftRepository,
    private val contactMatchingService: ContactMatchingService,
    private val tenantRepository: TenantRepository,
    // RAG dependencies
    private val chunkingService: ChunkingService,
    private val embeddingService: EmbeddingService,
    private val chunkRepository: ChunkRepository
) {
    private val logger = LoggerFactory.getLogger(DocumentProcessingWorker::class.java)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var pollingJob: Job? = null
    private val isRunning = AtomicBoolean(false)

    private val ragHandler = RAGPipelineHandler(
        chunkingService = chunkingService,
        embeddingService = embeddingService,
        chunkRepository = chunkRepository,
        logger = logger
    )

    /**
     * Start the processing worker.
     */
    fun start() {
        if (!isRunning.compareAndSet(false, true)) {
            logger.warn("Worker already running")
            return
        }

        logger.info(
            "Starting worker (VISION-FIRST): interval=${config.pollingInterval}ms, " +
                    "batch=${config.batchSize}, concurrency=${mode.maxConcurrentRequests}, RAG=${ragHandler.isEnabled}"
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
                                "Failed to process ingestion run ${ingestion.runId} for document ${ingestion.documentId}",
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

        logger.info("Processing ingestion run: $runId for document: $documentId")

        // Mark as processing
        ingestionRepository.markAsProcessing(runId, "5-Layer Autonomous Pipeline")

        try {
            // Step 1: Download document from storage
            val documentBytes = downloadDocument(ingestion.storageKey)

            // Step 2: Convert document to images for vision processing
            val documentImages = try {
                documentImageService.getDocumentImages(
                    documentBytes = documentBytes,
                    mimeType = ingestion.contentType,
                    maxPages = ingestion.overrideMaxPages ?: 10,
                    dpi = ingestion.overrideDpi ?: 150
                )
            } catch (e: UnsupportedDocumentTypeException) {
                logger.error("Unsupported document type for $documentId: ${e.message}")
                ingestionRepository.markAsFailed(
                    runId,
                    "Unsupported document type: ${ingestion.contentType}"
                )
                return
            }

            if (documentImages.images.isEmpty()) {
                logger.warn("No images rendered for document $documentId")
                ingestionRepository.markAsFailed(runId, "No pages could be rendered from document")
                return
            }

            logger.debug(
                "Rendered ${documentImages.processedPages}/${documentImages.totalPages} pages for document $documentId"
            )

            // Fetch tenant context for improved INVOICE vs BILL classification
            val tenant = tenantRepository.findById(TenantId.parse(tenantId))
            val tenantContext = AgentPrompt.TenantContext(
                vatNumber = tenant?.vatNumber?.value,
                companyName = tenant?.displayName?.value ?: tenant?.legalName?.value
            )

            // Step 3: Process document through DocumentOrchestrator
            val processingResult =
                processDocument(documentImages.images, tenantContext, runId, documentId, tenantId)
                    ?: return // Already marked as failed inside processDocument

            val (extractedData, documentType, meetsThreshold, rawText, confidence, judgmentInfo, contactId, contactCreated) =
                processingResult

            // Handle null extraction as needs-review
            if (extractedData == null) {
                logger.warn("Document $documentId has no extracted data - marking as needs review")
                ingestionRepository.markAsFailed(runId, "No data could be extracted")
                return
            }

            // Create draft (always for classifiable types, with appropriate status)
            ingestionRepository.markAsSucceeded(
                runId = runId,
                tenantId = tenantId,
                documentId = documentId,
                documentType = documentType,
                extractedData = extractedData,
                confidence = confidence,
                rawText = rawText,
                meetsThreshold = meetsThreshold,
                force = false // Don't overwrite user edits
            )

            // Log judgment info if using coordinator
            if (judgmentInfo != null) {
                logger.info(
                    "5-Layer Pipeline: judgment=${judgmentInfo.outcome}, " +
                            "corrected=${judgmentInfo.wasCorrected}, issues=${judgmentInfo.issues.size}"
                )
            }

            // Best-effort: suggest a contact match for the counterparty (if not user-edited/linked).
            runCatching {
                updateContactSuggestionIfApplicable(
                    tenantId = TenantId.parse(tenantId),
                    documentId = DocumentId.parse(documentId),
                    documentType = documentType,
                    extractedData = extractedData
                )
            }.onFailure { e ->
                logger.warn(
                    "Contact suggestion failed for document $documentId (tenant=$tenantId): ${e.message}",
                    e
                )
            }

            logger.info(
                "Processed doc $documentId: type=$documentType, " +
                        "conf=$confidence, threshold=$meetsThreshold"
            )

            // RAG preparation: chunk and embed the extracted text
            // Vision model provides extractedText for RAG indexing
            if (ragHandler.isEnabled && rawText.isNotBlank()) {
                try {
                    val chunksCount = ragHandler.chunkAndEmbed(
                        tenantId = tenantId,
                        documentId = documentId,
                        rawText = rawText
                    )
                    // Track successful indexing
                    ingestionRepository.updateIndexingStatus(
                        runId = runId,
                        status = IndexingStatus.Succeeded,
                        chunksCount = chunksCount
                    )
                } catch (e: Exception) {
                    // Log and track indexing failure (don't fail the extraction)
                    logger.error("Failed to chunk/embed document $documentId: ${e.message}", e)
                    ingestionRepository.updateIndexingStatus(
                        runId = runId,
                        status = IndexingStatus.Failed,
                        errorMessage = e.message
                    )
                }
            }
        } catch (e: Exception) {
            logger.error("Unexpected error processing document $documentId", e)
            ingestionRepository.markAsFailed(runId, "Processing error: ${e.message}")
        }
    }

    // =========================================================================
    // Processing Methods
    // =========================================================================

    /**
     * Process document using the DocumentOrchestrator.
     */
    private suspend fun processDocument(
        images: List<DocumentImageService.DocumentImage>,
        tenantContext: AgentPrompt.TenantContext,
        runId: String,
        documentId: String,
        tenantId: String
    ): ProcessingResult? {
        logger.info("Processing document $documentId with DocumentOrchestrator")

        return when (
            val result = orchestrator.process(
                documentId = DocumentId.parse(documentId),
                tenantId = TenantId.parse(tenantId),
                images = images,
                tenantContext = tenantContext
            )
        ) {
            is OrchestratorResult.Success -> {
                val documentType = result.documentType.toDomainType()
                val meetsThreshold = result.confidence >= 0.85

                // Log audit trail
                result.auditTrail.forEach { step ->
                    logger.debug("Step ${step.step}: ${step.action} (${step.durationMs}ms)")
                }

                ProcessingResult(
                    extractedData = result.extraction.toExtractedDocumentData(documentType),
                    documentType = documentType,
                    meetsThreshold = meetsThreshold,
                    rawText = result.rawText,
                    confidence = result.confidence,
                    judgmentInfo = JudgmentInfo(
                        outcome = if (meetsThreshold) "AUTO_APPROVE" else "NEEDS_REVIEW",
                        wasCorrected = result.correctionsApplied > 0,
                        issues = if (!result.validationPassed) listOf("Validation failed") else emptyList()
                    ),
                    contactId = result.contactId?.toString(),
                    contactCreated = result.contactCreated
                )
            }

            is OrchestratorResult.NeedsReview -> {
                logger.warn("Document $documentId needs review: ${result.reason}")
                val documentType = result.documentType?.toDomainType() ?: DocumentType.Unknown

                ProcessingResult(
                    extractedData = result.partialExtraction?.toExtractedDocumentData(documentType),
                    documentType = documentType,
                    meetsThreshold = false,
                    rawText = "",
                    confidence = 0.0,
                    judgmentInfo = JudgmentInfo(
                        outcome = "NEEDS_REVIEW",
                        wasCorrected = false,
                        issues = result.issues
                    ),
                    contactId = null,
                    contactCreated = false
                )
            }

            is OrchestratorResult.Failed -> {
                logger.error("Document $documentId failed: ${result.reason} at ${result.stage}")
                ingestionRepository.markAsFailed(runId, "Failed: ${result.reason}")
                null
            }
        }
    }

    /**
     * Download document from object storage.
     */
    private suspend fun downloadDocument(storageKey: String): ByteArray {
        return try {
            documentStorage.downloadDocument(storageKey)
        } catch (e: Exception) {
            throw DocumentProcessingException(
                "Failed to download document from storage: ${e.message}",
                isRetryable = true,
                cause = e
            )
        }
    }

    @Suppress("CyclomaticComplexMethod")
    private suspend fun updateContactSuggestionIfApplicable(
        tenantId: TenantId,
        documentId: DocumentId,
        documentType: DocumentType,
        extractedData: tech.dokus.domain.model.ExtractedDocumentData,
    ) {
        val draft = draftRepository.getByDocumentId(documentId, tenantId) ?: return
        if (draft.linkedContactId != null) return
        if (draft.counterpartyIntent == CounterpartyIntent.Pending) return
        if (draft.draftVersion > 0) return // user-edited: don't override suggestion

        val extractedCounterparty = when (documentType) {
            DocumentType.Invoice -> extractedData.invoice?.let { inv ->
                ContactMatchingService.ExtractedCounterparty(
                    name = inv.clientName,
                    vatNumber = inv.clientVatNumber,
                    email = inv.clientEmail,
                    address = inv.clientAddress
                )
            }

            DocumentType.Bill -> extractedData.bill?.let { bill ->
                ContactMatchingService.ExtractedCounterparty(
                    name = bill.supplierName,
                    vatNumber = bill.supplierVatNumber,
                    address = bill.supplierAddress
                )
            }

            DocumentType.Expense -> extractedData.expense?.let { exp ->
                ContactMatchingService.ExtractedCounterparty(
                    name = exp.merchant,
                    vatNumber = exp.merchantVatNumber,
                    address = exp.merchantAddress
                )
            }

            DocumentType.CreditNote,
            DocumentType.Receipt,
            DocumentType.ProForma,
            DocumentType.Unknown -> null
        } ?: return

        if (
            extractedCounterparty.name.isNullOrBlank() &&
            extractedCounterparty.vatNumber.isNullOrBlank() &&
            extractedCounterparty.peppolId.isNullOrBlank()
        ) {
            return
        }

        val suggestion =
            contactMatchingService.findMatch(tenantId, extractedCounterparty).getOrThrow()
        if (suggestion.contactId == null) return

        val reason = listOfNotNull(
            suggestion.matchReason.name,
            suggestion.matchDetails
        ).joinToString(": ")

        draftRepository.updateContactSuggestion(
            documentId = documentId,
            tenantId = tenantId,
            contactId = suggestion.contactId,
            confidence = suggestion.confidence,
            reason = reason
        )
    }
}

/**
 * Exception thrown when document processing fails.
 */
class DocumentProcessingException(
    message: String,
    val isRetryable: Boolean = false,
    cause: Throwable? = null
) : RuntimeException(message, cause)
