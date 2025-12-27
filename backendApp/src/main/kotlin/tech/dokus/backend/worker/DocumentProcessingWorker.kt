package tech.dokus.backend.worker

import tech.dokus.foundation.ktor.config.ProcessorConfig
import ai.dokus.ai.services.ChunkingService
import ai.dokus.ai.services.EmbeddingException
import ai.dokus.ai.services.EmbeddingService
import ai.dokus.foundation.database.repository.processor.ProcessingItem
import ai.dokus.foundation.database.repository.processor.ProcessorDocumentProcessingRepository
import ai.dokus.foundation.domain.ids.DocumentProcessingId
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.repository.ChunkRepository
import ai.dokus.foundation.domain.repository.ChunkWithEmbedding
import tech.dokus.foundation.ktor.storage.DocumentStorageService
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
import tech.dokus.backend.processor.ExtractionException
import tech.dokus.backend.processor.ExtractionProviderFactory
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Background worker that polls for pending documents and processes them with AI extraction.
 *
 * Features:
 * - Polling-based processing (configurable interval)
 * - Automatic retry with backoff
 * - Graceful shutdown support
 * - Provider fallback if primary fails
 * - RAG preparation: chunking and embedding generation after extraction
 */
class DocumentProcessingWorker(
    private val processingRepository: ProcessorDocumentProcessingRepository,
    private val documentStorage: DocumentStorageService,
    private val providerFactory: ExtractionProviderFactory,
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
     * Process a batch of pending documents.
     */
    private suspend fun processBatch() {
        // Find pending documents
        val pending = processingRepository.findPendingForProcessing(
            limit = config.batchSize,
            maxAttempts = config.maxAttempts
        )

        if (pending.isEmpty()) {
            logger.debug("No pending documents to process")
            return
        }

        logger.info("Found ${pending.size} pending documents to process")

        for (processing in pending) {
            if (!isRunning.get()) break

            try {
                processDocument(processing)
            } catch (e: Exception) {
                logger.error("Failed to process document ${processing.documentId}", e)
            }
        }
    }

    /**
     * Process a single document.
     */
    private suspend fun processDocument(processing: ProcessingItem) {
        val documentId = processing.documentId
        val processingId = processing.processingId

        logger.info("Processing document: $documentId (attempt ${processing.attempts + 1})")

        // Get AI provider
        val provider = providerFactory.getFirstAvailableProvider()
        if (provider == null) {
            logger.error("No AI provider available for processing")
            processingRepository.markAsFailed(processingId, "No AI provider available")
            return
        }

        // Mark as processing
        processingRepository.markAsProcessing(processingId, provider.name)

        try {
            // Download document from storage
            val documentBytes = downloadDocument(processing.storageKey)

            // Run AI extraction
            val result = provider.extract(
                documentBytes = documentBytes,
                contentType = processing.contentType,
                filename = processing.filename
            )

            // Update with results
            processingRepository.markAsProcessed(
                processingId = processingId,
                documentType = result.documentType,
                extractedData = result.extractedData,
                confidence = result.confidence,
                rawText = result.rawText
            )

            logger.info("Successfully processed document $documentId: type=${result.documentType}, confidence=${result.confidence}")

            // RAG preparation: chunk and embed the extracted text
            if (isRagEnabled && result.rawText != null) {
                try {
                    chunkAndEmbedDocument(
                        tenantId = processing.tenantId,
                        processingId = processingId,
                        rawText = result.rawText
                    )
                } catch (e: Exception) {
                    // Log but don't fail the extraction - RAG is enhancement, not critical
                    logger.error("Failed to chunk/embed document $documentId: ${e.message}", e)
                }
            }

        } catch (e: ExtractionException) {
            logger.error("Extraction failed for document $documentId: ${e.message}")

            if (e.isRetryable && processing.attempts < config.maxAttempts - 1) {
                // Will be retried on next poll
                processingRepository.markAsFailed(processingId, "Extraction failed: ${e.message}")
            } else {
                // Final failure
                processingRepository.markAsFailed(processingId, "Extraction failed (final): ${e.message}")
            }

            // Try fallback provider if available
            tryFallbackProvider(processing, provider.name, e)

        } catch (e: Exception) {
            logger.error("Unexpected error processing document $documentId", e)
            processingRepository.markAsFailed(processingId, "Processing error: ${e.message}")
        }
    }

    /**
     * Chunk the document text and generate embeddings for RAG.
     *
     * This step prepares the document for vector similarity search in the chat feature.
     * Chunks are stored in the database with their embeddings for later retrieval.
     *
     * @param tenantId The tenant owning this document (for isolation)
     * @param processingId The document processing ID
     * @param rawText The extracted text to chunk and embed
     */
    private suspend fun chunkAndEmbedDocument(
        tenantId: String,
        processingId: String,
        rawText: String
    ) {
        val chunkingSvc = chunkingService ?: return
        val embeddingSvc = embeddingService ?: return
        val chunkRepo = chunkRepository ?: return

        logger.info("Starting RAG preparation for document $processingId")

        // Step 1: Chunk the text
        val chunkingResult = chunkingSvc.chunk(rawText)

        if (chunkingResult.chunks.isEmpty()) {
            logger.warn("No chunks generated for document $processingId (empty text?)")
            return
        }

        logger.info("Generated ${chunkingResult.totalChunks} chunks for document $processingId")

        // Step 2: Generate embeddings for each chunk
        val chunkTexts = chunkingResult.chunks.map { it.content }
        val embeddings = try {
            embeddingSvc.generateEmbeddings(chunkTexts)
        } catch (e: EmbeddingException) {
            logger.error("Failed to generate embeddings for document $processingId: ${e.message}")
            if (e.isRetryable) {
                throw e // Let it be retried
            }
            return
        }

        if (embeddings.size != chunkingResult.chunks.size) {
            logger.error("Embedding count mismatch: expected ${chunkingResult.chunks.size}, got ${embeddings.size}")
            return
        }

        val embeddingModel = embeddings.firstOrNull()?.model ?: "unknown"
        logger.info("Generated embeddings for ${embeddings.size} chunks (model=$embeddingModel)")

        // Step 3: Store chunks with embeddings
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
                tenantId = TenantId.parse(tenantId),
                documentId = DocumentProcessingId.parse(processingId),
                chunks = chunksWithEmbeddings
            )
            logger.info("Stored ${chunksWithEmbeddings.size} chunks with embeddings for document $processingId")
        } catch (e: Exception) {
            logger.error("Failed to store chunks for document $processingId: ${e.message}", e)
            throw e
        }
    }

    /**
     * Try a fallback provider after primary failure.
     */
    private suspend fun tryFallbackProvider(
        processing: ProcessingItem,
        failedProvider: String,
        originalError: ExtractionException
    ) {
        if (!originalError.isRetryable) return

        val providers = providerFactory.getAvailableProviders()
            .filter { it.name != failedProvider }

        for (provider in providers) {
            try {
                logger.info("Trying fallback provider: ${provider.name}")

                processingRepository.markAsProcessing(processing.processingId, provider.name)

                val documentBytes = downloadDocument(processing.storageKey)

                val result = provider.extract(
                    documentBytes = documentBytes,
                    contentType = processing.contentType,
                    filename = processing.filename
                )

                processingRepository.markAsProcessed(
                    processingId = processing.processingId,
                    documentType = result.documentType,
                    extractedData = result.extractedData,
                    confidence = result.confidence,
                    rawText = result.rawText
                )

                logger.info("Fallback provider ${provider.name} succeeded for document ${processing.documentId}")

                // RAG preparation for fallback success
                if (isRagEnabled && result.rawText != null) {
                    try {
                        chunkAndEmbedDocument(
                            tenantId = processing.tenantId,
                            processingId = processing.processingId,
                            rawText = result.rawText
                        )
                    } catch (e: Exception) {
                        logger.error("Failed to chunk/embed document ${processing.documentId} after fallback: ${e.message}", e)
                    }
                }

                return

            } catch (e: Exception) {
                logger.warn("Fallback provider ${provider.name} also failed: ${e.message}")
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
            throw ExtractionException(
                "Failed to download document from storage: ${e.message}",
                "storage",
                isRetryable = true,
                cause = e
            )
        }
    }
}
