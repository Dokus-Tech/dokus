package tech.dokus.backend.worker

import ai.dokus.ai.models.meetsMinimalThreshold
import ai.dokus.ai.models.toDomainType
import ai.dokus.ai.models.toExtractedDocumentData
import ai.dokus.ai.service.AIService
import tech.dokus.domain.enums.IndexingStatus
import ai.dokus.ai.services.ChunkingService
import ai.dokus.ai.services.EmbeddingException
import ai.dokus.ai.services.EmbeddingService
import ai.dokus.foundation.database.entity.IngestionItemEntity
import ai.dokus.foundation.database.repository.processor.ProcessorIngestionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.repository.ChunkRepository
import tech.dokus.domain.repository.ChunkWithEmbedding
import tech.dokus.foundation.ktor.config.ProcessorConfig
import tech.dokus.foundation.ktor.storage.DocumentStorageService
import tech.dokus.ocr.OcrEngine
import tech.dokus.ocr.OcrInput
import tech.dokus.ocr.OcrResult
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.seconds

/**
 * Background worker that polls for pending documents and processes them with AI extraction.
 *
 * TEXT-FIRST Architecture:
 * 1. Download document bytes from storage
 * 2. Save to temp file
 * 3. Extract text via OCR engine (handles PDF and images)
 * 4. Send text to AIService for classification and extraction
 * 5. Persist results
 *
 * Features:
 * - Polling-based processing (configurable interval)
 * - Automatic retry with backoff
 * - Graceful shutdown support
 * - RAG preparation: chunking and embedding generation after extraction
 */
class DocumentProcessingWorker(
    private val ingestionRepository: ProcessorIngestionRepository,
    private val documentStorage: DocumentStorageService,
    private val aiService: AIService,
    private val ocrEngine: OcrEngine,
    private val config: ProcessorConfig,
    // Optional RAG dependencies - if provided, chunking and embedding will be performed
    private val chunkingService: ChunkingService? = null,
    private val embeddingService: EmbeddingService? = null,
    private val chunkRepository: ChunkRepository? = null
) {
    private val logger = LoggerFactory.getLogger(DocumentProcessingWorker::class.java)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var pollingJob: Job? = null
    private val isRunning = AtomicBoolean(false)

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

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

        logger.info("Starting document processing worker (interval=${config.pollingInterval}ms, batch=${config.batchSize}, RAG=${isRagEnabled})")

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

        var tempFile: Path? = null
        try {
            // Step 1: Download document from storage
            val documentBytes = downloadDocument(ingestion.storageKey)

            // Step 2: Save to temp file for OCR
            tempFile = Files.createTempFile("dokus_ocr_", "_${ingestion.filename}")
            Files.write(tempFile, documentBytes)
            logger.debug("Saved document to temp file: {}", tempFile)

            // Step 3: Extract text via OCR (use overrides if provided)
            val ocrResult = ocrEngine.extractText(
                OcrInput(
                    filePath = tempFile,
                    mimeType = ingestion.contentType,
                    maxPages = ingestion.overrideMaxPages ?: 10,
                    dpi = ingestion.overrideDpi ?: 300,
                    timeout = (ingestion.overrideTimeoutSeconds?.toLong() ?: 60).seconds
                )
            )

            val rawText = when (ocrResult) {
                is OcrResult.Success -> ocrResult.text
                is OcrResult.Failure -> {
                    val errorMsg = ocrResult.toErrorString()
                    logger.error("OCR failed for document $documentId: $errorMsg")

                    // Log additional timeout details for debugging
                    ocrResult.timeoutDetails?.let { details ->
                        logger.error(
                            "Timeout details: stage=${details.stage}, " +
                            "timeoutMs=${details.timeoutMs}, " +
                            "pagesProcessed=${details.pagesProcessed}/${details.totalPages ?: "?"}"
                        )
                    }

                    ingestionRepository.markAsFailed(runId, errorMsg)
                    return
                }
            }

            if (rawText.isBlank()) {
                logger.warn("OCR returned empty text for document $documentId")
                ingestionRepository.markAsFailed(runId, "OCR returned empty text")
                return
            }

            logger.debug("OCR extracted ${rawText.length} characters from document $documentId")

            // Step 4: Send text to AIService for classification and extraction
            val aiResult = aiService.processDocument(rawText)

            val result = aiResult.getOrElse { e ->
                logger.error("AI processing failed for document $documentId: ${e.message}", e)
                ingestionRepository.markAsFailed(runId, "AI processing failed: ${e.message}")
                return
            }

            // Step 5: Convert to domain model and persist
            val extractedData = result.toExtractedDocumentData()
            val documentType = result.toDomainType()

            // Check threshold using AI-layer check (SINGLE source of truth)
            val meetsThreshold = result.meetsMinimalThreshold()

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

            logger.info("Successfully processed document $documentId: type=$documentType, confidence=${result.confidence}, meetsThreshold=$meetsThreshold")

            // RAG preparation: chunk and embed the extracted text
            // This is independent of draft creation - chunks are created for all docs with rawText
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
        } finally {
            // Clean up temp file
            tempFile?.let { path ->
                try {
                    Files.deleteIfExists(path)
                    logger.debug("Deleted temp file: $path")
                } catch (e: Exception) {
                    logger.warn("Failed to delete temp file: $path", e)
                }
            }
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
     * @param rawText The extracted text to chunk and embed
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
            logger.info("Deleted $deletedCount old chunks for document $documentId (old hash=$existingHash, new hash=$contentHash)")
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

        if (embeddings.size != chunkingResult.chunks.size) {
            logger.error("Embedding count mismatch: expected ${chunkingResult.chunks.size}, got ${embeddings.size}")
            throw IllegalStateException("Embedding count mismatch: expected ${chunkingResult.chunks.size}, got ${embeddings.size}")
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
            logger.info("Stored ${chunksWithEmbeddings.size} chunks with embeddings for document $documentId (hash=$contentHash)")
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
}

/**
 * Exception thrown when document processing fails.
 */
class DocumentProcessingException(
    message: String,
    val isRetryable: Boolean = false,
    cause: Throwable? = null
) : RuntimeException(message, cause)
