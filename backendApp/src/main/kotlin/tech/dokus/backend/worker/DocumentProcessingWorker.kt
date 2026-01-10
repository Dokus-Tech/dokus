package tech.dokus.backend.worker

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import tech.dokus.backend.services.contacts.ContactMatchingService
import tech.dokus.database.entity.IngestionItemEntity
import tech.dokus.database.repository.cashflow.DocumentDraftRepository
import tech.dokus.database.repository.processor.ProcessorIngestionRepository
import tech.dokus.domain.enums.CounterpartyIntent
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.IndexingStatus
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.repository.ChunkRepository
import tech.dokus.domain.repository.ChunkWithEmbedding
import tech.dokus.domain.utils.json
import tech.dokus.features.ai.models.meetsMinimalThreshold
import tech.dokus.features.ai.models.toDomainType
import tech.dokus.features.ai.models.toExtractedDocumentData
import tech.dokus.features.ai.service.AIService
import tech.dokus.features.ai.services.ChunkingService
import tech.dokus.features.ai.services.DocumentImageService
import tech.dokus.features.ai.services.EmbeddingException
import tech.dokus.features.ai.services.EmbeddingService
import tech.dokus.features.ai.services.UnsupportedDocumentTypeException
import tech.dokus.foundation.backend.config.ProcessorConfig
import tech.dokus.foundation.backend.storage.DocumentStorageService
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Background worker that polls for pending documents and processes them with AI extraction.
 *
 * VISION-FIRST Architecture:
 * 1. Download document bytes from storage
 * 2. Convert to images using DocumentImageService
 * 3. Send images to AIService for classification and extraction (vision models)
 * 4. Persist results
 * 5. RAG: Use extractedText from vision model for chunking/embedding
 *
 * Features:
 * - Polling-based processing (configurable interval)
 * - Automatic retry with backoff
 * - Graceful shutdown support
 * - RAG preparation: chunking and embedding using vision-extracted text
 */
@Suppress("LongParameterList")
class DocumentProcessingWorker(
    private val ingestionRepository: ProcessorIngestionRepository,
    private val documentStorage: DocumentStorageService,
    private val aiService: AIService,
    private val documentImageService: DocumentImageService,
    private val config: ProcessorConfig,
    private val draftRepository: DocumentDraftRepository,
    private val contactMatchingService: ContactMatchingService,
    // Optional RAG dependencies - if provided, chunking and embedding will be performed
    private val chunkingService: ChunkingService? = null,
    private val embeddingService: EmbeddingService? = null,
    private val chunkRepository: ChunkRepository? = null
) {
    private val logger = LoggerFactory.getLogger(DocumentProcessingWorker::class.java)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var pollingJob: Job? = null
    private val isRunning = AtomicBoolean(false)

    /**
     * Check if RAG (chunking/embedding) is enabled.
     */
    private val isRagEnabled: Boolean
        get() = chunkingService != null && embeddingService != null && chunkRepository != null

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
                "batch=${config.batchSize}, RAG=$isRagEnabled"
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
     * Process a batch of pending ingestion runs.
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

        for (ingestion in pending) {
            if (!isRunning.get()) break

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

        // Mark as processing with AI provider name
        val providerName = aiService.getConfigSummary().substringBefore(",")
        ingestionRepository.markAsProcessing(runId, providerName)

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
                ingestionRepository.markAsFailed(runId, "Unsupported document type: ${ingestion.contentType}")
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

            // Step 3: Send images to AIService for vision-based classification and extraction
            val aiResult = aiService.processDocument(documentImages.images)

            val result = aiResult.getOrElse { e ->
                logger.error("AI processing failed for document $documentId: ${e.message}", e)
                ingestionRepository.markAsFailed(runId, "AI processing failed: ${e.message}")
                return
            }

            // Step 4: Convert to domain model and persist
            val extractedData = result.toExtractedDocumentData()
            val documentType = result.toDomainType()

            // Check threshold using AI-layer check (SINGLE source of truth)
            val meetsThreshold = result.meetsMinimalThreshold()

            // Get the extracted text for RAG (from vision model's transcription)
            val rawText = result.rawText

            // Create draft (always for classifiable types, with appropriate status)
            ingestionRepository.markAsSucceeded(
                runId = runId,
                tenantId = tenantId,
                documentId = documentId,
                documentType = documentType,
                extractedData = extractedData,
                confidence = result.confidence,
                rawText = rawText,
                meetsThreshold = meetsThreshold,
                force = false // Don't overwrite user edits
            )

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
                    "conf=${result.confidence}, threshold=$meetsThreshold"
            )

            // RAG preparation: chunk and embed the extracted text
            // Vision model provides extractedText for RAG indexing
            if (isRagEnabled && rawText.isNotBlank()) {
                try {
                    val chunksCount = chunkAndEmbedDocument(
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

    /**
     * Chunk the document text and generate embeddings for RAG.
     *
     * This step prepares the document for vector similarity search in the chat feature.
     * Chunks are stored in the database with their embeddings for later retrieval.
     *
     * Uses contentHash-based deduplication:
     * - If content hash matches existing chunks, skip indexing
     * - If content hash differs, delete old chunks and insert new ones
     *
     * @param tenantId The tenant owning this document (for isolation)
     * @param documentId The document ID
     * @param rawText The extracted text to chunk and embed (from vision model)
     * @return Number of chunks created (or existing if unchanged)
     */
    private suspend fun chunkAndEmbedDocument(
        tenantId: String,
        documentId: String,
        rawText: String
    ): Int {
        val chunkingSvc = chunkingService ?: return 0
        val embeddingSvc = embeddingService ?: return 0
        val chunkRepo = chunkRepository ?: return 0

        val tenantIdParsed = TenantId.parse(tenantId)
        val documentIdParsed = DocumentId.parse(documentId)

        logger.info("Starting RAG preparation for document $documentId")

        // Step 1: Compute content hash for deduplication
        val contentHash = rawText.sha256()

        // Step 2: Check if content has changed since last indexing
        val existingHash = chunkRepo.getContentHashForDocument(tenantIdParsed, documentIdParsed)
        if (existingHash == contentHash) {
            logger.info("Content unchanged for document $documentId (hash=$contentHash), skipping chunk indexing")
            // Return existing chunk count (assume it's already indexed)
            return chunkRepo.countChunksForDocument(tenantIdParsed, documentIdParsed).toInt()
        }

        // Step 3: Delete old chunks if they exist (content has changed)
        if (existingHash != null) {
            val deletedCount = chunkRepo.deleteChunksForDocument(tenantIdParsed, documentIdParsed)
            logger.info("Deleted $deletedCount chunks for doc $documentId (hash changed)")
        }

        // Step 4: Chunk the text
        val chunkingResult = chunkingSvc.chunk(rawText)

        if (chunkingResult.chunks.isEmpty()) {
            logger.warn("No chunks generated for document $documentId (empty text?)")
            return 0
        }

        logger.info("Generated ${chunkingResult.totalChunks} chunks for document $documentId")

        // Step 5: Generate embeddings for each chunk
        val chunkTexts = chunkingResult.chunks.map { it.content }
        val embeddings = try {
            embeddingSvc.generateEmbeddings(chunkTexts)
        } catch (e: EmbeddingException) {
            logger.error("Failed to generate embeddings for document $documentId: ${e.message}")
            if (e.isRetryable) {
                throw e // Let it be retried
            }
            throw e // Re-throw to track as failure
        }

        check(embeddings.size == chunkingResult.chunks.size) {
            "Embedding count mismatch: expected ${chunkingResult.chunks.size}, got ${embeddings.size}"
        }

        val embeddingModel = embeddings.firstOrNull()?.model ?: "unknown"
        logger.info("Generated embeddings for ${embeddings.size} chunks (model=$embeddingModel)")

        // Step 6: Store chunks with embeddings and contentHash
        val chunksWithEmbeddings = chunkingResult.chunks.mapIndexed { index, chunk ->
            ChunkWithEmbedding(
                content = chunk.content,
                chunkIndex = chunk.index,
                totalChunks = chunkingResult.totalChunks,
                embedding = embeddings[index].embedding,
                embeddingModel = embeddings[index].model,
                startOffset = chunk.provenance.offsets?.start,
                endOffset = chunk.provenance.offsets?.end,
                pageNumber = chunk.provenance.pageNumber,
                metadata = chunk.metadata?.let { json.encodeToString(it) },
                tokenCount = chunk.estimatedTokens
            )
        }

        try {
            chunkRepo.storeChunks(
                tenantId = tenantIdParsed,
                documentId = documentIdParsed,
                contentHash = contentHash,
                chunks = chunksWithEmbeddings
            )
            logger.info("Stored ${chunksWithEmbeddings.size} chunks for doc $documentId")
            return chunksWithEmbeddings.size
        } catch (e: Exception) {
            logger.error("Failed to store chunks for document $documentId: ${e.message}", e)
            throw e
        }
    }

    /**
     * Compute SHA-256 hash of a string.
     */
    private fun String.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(this.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
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

    private suspend fun updateContactSuggestionIfApplicable(
        tenantId: TenantId,
        documentId: DocumentId,
        documentType: DocumentType,
        extractedData: tech.dokus.domain.model.ExtractedDocumentData
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
            DocumentType.Unknown -> null
        } ?: return

        if (
            extractedCounterparty.name.isNullOrBlank() &&
            extractedCounterparty.vatNumber.isNullOrBlank() &&
            extractedCounterparty.peppolId.isNullOrBlank()
        ) {
            return
        }

        val suggestion = contactMatchingService.findMatch(tenantId, extractedCounterparty).getOrThrow()
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
