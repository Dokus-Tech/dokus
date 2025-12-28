package ai.dokus.ai.services

import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.ChunkRetrievalRequest
import tech.dokus.domain.model.ChunkRetrievalResponse
import tech.dokus.domain.model.DocumentChunkId
import tech.dokus.domain.model.DocumentChunkSummary
import tech.dokus.domain.repository.ChunkRepository
import tech.dokus.domain.repository.DraftStatusChecker
import tech.dokus.domain.repository.IngestionStatusChecker
import tech.dokus.domain.repository.RetrievedChunk
import org.slf4j.LoggerFactory
import kotlin.uuid.ExperimentalUuidApi

/**
 * RAG (Retrieval Augmented Generation) Service for vector similarity search.
 *
 * This service handles:
 * - Query embedding generation
 * - Vector similarity search via repository abstraction
 * - Context assembly for LLM prompting
 *
 * CRITICAL: All queries MUST filter by tenantId for multi-tenant security.
 * The ChunkRepository implementation is responsible for enforcing tenant isolation.
 *
 * Architecture:
 * - RAGService handles AI/embedding logic (lives in ai-backend)
 * - ChunkRepository handles database access (implemented in foundation:database)
 *
 * Usage:
 * ```kotlin
 * val ragService = RAGService(embeddingService, chunkRepository)
 *
 * // Single-document search
 * val result = ragService.retrieveRelevantChunks(
 *     tenantId = tenantId,
 *     query = "What is the total VAT?",
 *     documentId = documentId,
 *     topK = 5
 * )
 *
 * // Cross-document search
 * val result = ragService.retrieveRelevantChunks(
 *     tenantId = tenantId,
 *     query = "Spending at VendorX",
 *     documentId = null,
 *     topK = 10
 * )
 *
 * // Assemble context for LLM
 * val context = ragService.assembleContext(result.chunks)
 * ```
 */
class RAGService(
    private val embeddingService: EmbeddingService,
    private val chunkRepository: ChunkRepository,
    private val ingestionStatusChecker: IngestionStatusChecker? = null,
    private val draftStatusChecker: DraftStatusChecker? = null
) {
    private val logger = LoggerFactory.getLogger(RAGService::class.java)

    companion object {
        /** Default number of chunks to retrieve */
        const val DEFAULT_TOP_K = 5

        /** Default minimum similarity threshold (0.0 - 1.0) */
        const val DEFAULT_MIN_SIMILARITY = 0.3f

        /** Maximum number of chunks to retrieve in a single query */
        const val MAX_TOP_K = 50

        /** Default maximum tokens for context assembly */
        const val DEFAULT_MAX_CONTEXT_TOKENS = 2000
    }


    /**
     * Result of a RAG retrieval operation.
     */
    data class RetrievalResult(
        /** Retrieved chunks ordered by similarity (highest first) */
        val chunks: List<RetrievedChunk>,
        /** Total number of chunks searched */
        val totalSearched: Long,
        /** Embedding model used for the query */
        val embeddingModel: String,
        /** Time taken for retrieval in milliseconds */
        val retrievalTimeMs: Long,
        /** Query embedding dimensions */
        val embeddingDimensions: Int
    )

    /**
     * Retrieve relevant chunks for a query using vector similarity search.
     *
     * CRITICAL: Always filters by tenantId for multi-tenant security.
     *
     * @param tenantId The tenant to filter by (REQUIRED)
     * @param query The search query text
     * @param documentId Optional document ID to filter to a single document
     * @param topK Maximum number of chunks to return (default: 5)
     * @param minSimilarity Minimum cosine similarity threshold (default: 0.3)
     * @return RetrievalResult containing the relevant chunks with similarity scores
     */
    suspend fun retrieveRelevantChunks(
        tenantId: TenantId,
        query: String,
        documentId: DocumentId? = null,
        topK: Int = DEFAULT_TOP_K,
        minSimilarity: Float = DEFAULT_MIN_SIMILARITY
    ): RetrievalResult {
        val startTime = System.currentTimeMillis()
        val effectiveTopK = minOf(topK, MAX_TOP_K)

        logger.debug("RAG retrieval: tenantId=$tenantId, documentId=$documentId, topK=$effectiveTopK")

        // Step 1: Generate embedding for the query
        val queryEmbedding = try {
            embeddingService.generateEmbedding(query)
        } catch (e: EmbeddingException) {
            logger.error("Failed to generate query embedding", e)
            throw RAGException(
                message = "Failed to generate query embedding: ${e.message}",
                isRetryable = e.isRetryable,
                cause = e
            )
        }

        logger.debug("Query embedding generated: dimensions=${queryEmbedding.dimensions}, model=${queryEmbedding.model}")

        // Step 2: Perform vector similarity search via repository
        // confirmedOnly=true ensures only chunks from Confirmed documents are returned
        val searchResult = try {
            chunkRepository.searchSimilarChunks(
                tenantId = tenantId,
                queryEmbedding = queryEmbedding.embedding,
                documentId = documentId,
                topK = effectiveTopK,
                minSimilarity = minSimilarity,
                confirmedOnly = true
            )
        } catch (e: Exception) {
            logger.error("Failed to search similar chunks", e)
            throw RAGException(
                message = "Failed to search chunks: ${e.message}",
                isRetryable = true,
                cause = e
            )
        }

        val retrievalTimeMs = System.currentTimeMillis() - startTime
        logger.debug("RAG retrieval completed: ${searchResult.chunks.size} chunks in ${retrievalTimeMs}ms")

        return RetrievalResult(
            chunks = searchResult.chunks,
            totalSearched = searchResult.totalSearched,
            embeddingModel = queryEmbedding.model,
            retrievalTimeMs = retrievalTimeMs,
            embeddingDimensions = queryEmbedding.dimensions
        )
    }

    /**
     * Retrieve relevant chunks using the domain ChunkRetrievalRequest model.
     */
    @OptIn(ExperimentalUuidApi::class)
    suspend fun retrieve(
        tenantId: TenantId,
        request: ChunkRetrievalRequest
    ): ChunkRetrievalResponse {
        val result = retrieveRelevantChunks(
            tenantId = tenantId,
            query = request.query,
            documentId = request.documentId,
            topK = request.topK,
            minSimilarity = request.minSimilarity
        )

        return ChunkRetrievalResponse(
            chunks = result.chunks.map { chunk ->
                DocumentChunkSummary(
                    id = DocumentChunkId.parse(chunk.id),
                    documentId = DocumentId.parse(chunk.documentId),
                    chunkIndex = chunk.chunkIndex,
                    content = chunk.content,
                    pageNumber = chunk.pageNumber,
                    similarityScore = chunk.similarityScore,
                    documentName = chunk.documentName
                )
            },
            totalSearched = result.totalSearched,
            embeddingModel = result.embeddingModel,
            retrievalTimeMs = result.retrievalTimeMs.toInt()
        )
    }

    /**
     * Assemble context from retrieved chunks for LLM prompting.
     *
     * Creates a formatted context string suitable for inclusion in a RAG prompt.
     * Chunks are included in order of relevance (highest similarity first).
     *
     * @param chunks The retrieved chunks
     * @param maxTokens Maximum approximate tokens to include (rough estimate: chars / 4)
     * @param includeMetadata Whether to include source metadata (page numbers, etc.)
     * @return Formatted context string
     */
    fun assembleContext(
        chunks: List<RetrievedChunk>,
        maxTokens: Int = DEFAULT_MAX_CONTEXT_TOKENS,
        includeMetadata: Boolean = true
    ): String {
        if (chunks.isEmpty()) {
            return "No relevant context found."
        }

        val contextBuilder = StringBuilder()
        var estimatedTokens = 0
        val tokenEstimator = { text: String -> text.length / 4 } // Rough estimate

        for ((index, chunk) in chunks.withIndex()) {
            val chunkTokens = tokenEstimator(chunk.content)

            // Check if we would exceed the limit
            if (estimatedTokens + chunkTokens > maxTokens && contextBuilder.isNotEmpty()) {
                logger.debug("Context truncated at chunk $index due to token limit")
                break
            }

            if (includeMetadata) {
                contextBuilder.append("[Source ${index + 1}")
                chunk.documentName?.let { contextBuilder.append(": $it") }
                chunk.pageNumber?.let { contextBuilder.append(", page $it") }
                contextBuilder.append(", relevance: ${String.format("%.0f%%", chunk.similarityScore * 100)}]\n")
            }

            contextBuilder.append(chunk.content)
            contextBuilder.append("\n\n")

            estimatedTokens += chunkTokens
        }

        return contextBuilder.toString().trim()
    }

    /**
     * Format a system prompt with RAG context for LLM usage.
     *
     * @param basePrompt The base system prompt
     * @param context The assembled context from chunks
     * @return Combined prompt with context
     */
    fun formatRAGPrompt(
        basePrompt: String,
        context: String
    ): String {
        return """
            |$basePrompt
            |
            |Use the following context to answer the user's question.
            |If the answer is not in the context, say so clearly.
            |Always cite sources when using information from the context.
            |
            |--- CONTEXT ---
            |$context
            |--- END CONTEXT ---
        """.trimMargin()
    }

    /**
     * Check if RAG retrieval is available (embeddings service is configured).
     */
    suspend fun isAvailable(): Boolean {
        return try {
            embeddingService.isAvailable()
        } catch (e: Exception) {
            logger.warn("RAG availability check failed", e)
            false
        }
    }

    /**
     * Get the current embedding dimensions based on the configured provider.
     */
    fun getEmbeddingDimensions(): Int {
        return embeddingService.getEmbeddingDimensions()
    }

    /**
     * Check if a document has indexed chunks.
     *
     * Used by ChatAgent to determine document state for chat availability.
     *
     * @param tenantId The tenant ID (REQUIRED for security)
     * @param documentId The document ID to check
     * @return true if the document has indexed chunks, false otherwise
     */
    suspend fun hasChunksForDocument(tenantId: TenantId, documentId: DocumentId): Boolean {
        return try {
            chunkRepository.hasChunks(tenantId, documentId)
        } catch (e: Exception) {
            logger.warn("Failed to check chunks for document: $documentId", e)
            false
        }
    }

    /**
     * Check if a document is currently being processed (ingestion run active).
     *
     * Used by ChatAgent to determine if a document is PROCESSING vs NOT_INDEXED.
     * Returns false if no IngestionStatusChecker is configured.
     *
     * @param tenantId The tenant ID (REQUIRED for security)
     * @param documentId The document ID to check
     * @return true if document has an active ingestion run, false otherwise
     */
    suspend fun isDocumentProcessing(tenantId: TenantId, documentId: DocumentId): Boolean {
        val checker = ingestionStatusChecker
        if (checker == null) {
            logger.debug("No IngestionStatusChecker configured, assuming document is not processing")
            return false
        }
        return try {
            checker.isProcessing(tenantId, documentId)
        } catch (e: Exception) {
            logger.warn("Failed to check processing status for document: $documentId", e)
            false
        }
    }

    /**
     * Check if a document has been confirmed by the user.
     *
     * Chat is only allowed for Confirmed documents. This method checks the
     * draft status to enforce this policy.
     *
     * Returns false if no DraftStatusChecker is configured (fail-safe).
     *
     * @param tenantId The tenant ID (REQUIRED for security)
     * @param documentId The document ID to check
     * @return true if document has status=Confirmed, false otherwise
     */
    suspend fun isDocumentConfirmed(tenantId: TenantId, documentId: DocumentId): Boolean {
        val checker = draftStatusChecker
        if (checker == null) {
            logger.debug("No DraftStatusChecker configured, assuming document is not confirmed")
            return false
        }
        return try {
            checker.isConfirmed(tenantId, documentId)
        } catch (e: Exception) {
            logger.warn("Failed to check confirmation status for document: $documentId", e)
            false
        }
    }
}

/**
 * Exception thrown when RAG operations fail.
 */
class RAGException(
    message: String,
    val isRetryable: Boolean = false,
    cause: Throwable? = null
) : RuntimeException(message, cause)
