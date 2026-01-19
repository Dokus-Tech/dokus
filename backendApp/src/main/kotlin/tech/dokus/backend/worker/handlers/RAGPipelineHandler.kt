package tech.dokus.backend.worker.handlers

import org.slf4j.Logger
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.repository.ChunkRepository
import tech.dokus.domain.repository.ChunkWithEmbedding
import tech.dokus.domain.utils.json
import tech.dokus.features.ai.services.ChunkingService
import tech.dokus.features.ai.services.EmbeddingException
import tech.dokus.features.ai.services.EmbeddingService
import java.security.MessageDigest

/**
 * Handler for RAG (Retrieval-Augmented Generation) pipeline.
 * Chunks document text and generates embeddings for vector similarity search.
 */
internal class RAGPipelineHandler(
    private val chunkingService: ChunkingService?,
    private val embeddingService: EmbeddingService?,
    private val chunkRepository: ChunkRepository?,
    private val logger: Logger
) {
    /**
     * Check if RAG (chunking/embedding) is enabled.
     */
    val isEnabled: Boolean
        get() = chunkingService != null && embeddingService != null && chunkRepository != null

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
    suspend fun chunkAndEmbed(
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
}
