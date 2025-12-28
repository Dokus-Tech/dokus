package tech.dokus.domain.repository

import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.ai.ChatMessageDto
import tech.dokus.domain.model.ai.ChatMessageId
import tech.dokus.domain.model.ai.ChatScope
import tech.dokus.domain.model.ai.ChatSessionId
import tech.dokus.domain.model.ai.ChatSessionSummary
import tech.dokus.domain.model.DocumentChunkDto
import tech.dokus.domain.model.DocumentChunkId

// =============================================================================
// Chunk Repository Interface
// =============================================================================

/**
 * Repository interface for document chunk storage and vector similarity search.
 *
 * This interface abstracts the database layer for RAG (Retrieval Augmented Generation)
 * operations. Implementations should use pgvector or similar for vector similarity search.
 *
 * CRITICAL: All implementations MUST filter by tenantId for multi-tenant security.
 */
interface ChunkRepository {
    /**
     * Search for similar chunks using vector similarity.
     *
     * @param tenantId The tenant to filter by (REQUIRED for security)
     * @param queryEmbedding The query embedding vector
     * @param documentId Optional document ID to filter to a single document
     * @param topK Maximum number of chunks to return
     * @param minSimilarity Minimum cosine similarity threshold (0.0 - 1.0)
     * @return ChunkSearchResult containing matched chunks
     */
    suspend fun searchSimilarChunks(
        tenantId: TenantId,
        queryEmbedding: List<Float>,
        documentId: DocumentId?,
        topK: Int,
        minSimilarity: Float
    ): ChunkSearchResult

    /**
     * Store chunks with embeddings for a document.
     *
     * @param tenantId The tenant owning the document
     * @param documentId The document ID
     * @param chunks The chunks to store with their embeddings
     */
    suspend fun storeChunks(
        tenantId: TenantId,
        documentId: DocumentId,
        chunks: List<ChunkWithEmbedding>
    )

    /**
     * Delete all chunks for a document.
     *
     * @param tenantId The tenant owning the document
     * @param documentId The document ID
     * @return Number of chunks deleted
     */
    suspend fun deleteChunksForDocument(
        tenantId: TenantId,
        documentId: DocumentId
    ): Int

    /**
     * Get all chunks for a document, ordered by chunk index.
     */
    suspend fun getChunksForDocument(
        tenantId: TenantId,
        documentId: DocumentId
    ): List<DocumentChunkDto>

    /**
     * Get a single chunk by ID.
     */
    suspend fun getChunkById(
        tenantId: TenantId,
        chunkId: DocumentChunkId
    ): DocumentChunkDto?

    /**
     * Count chunks for a document.
     */
    suspend fun countChunksForDocument(
        tenantId: TenantId,
        documentId: DocumentId
    ): Long

    /**
     * Check if a document has chunks.
     */
    suspend fun hasChunks(
        tenantId: TenantId,
        documentId: DocumentId
    ): Boolean

    /**
     * Get the total chunk count for a tenant (for usage monitoring).
     */
    suspend fun countTotalChunksForTenant(tenantId: TenantId): Long
}

/**
 * Result of a chunk similarity search.
 */
data class ChunkSearchResult(
    /** Retrieved chunks ordered by similarity (highest first) */
    val chunks: List<RetrievedChunk>,
    /** Total number of chunks that were searched */
    val totalSearched: Long
)

/**
 * A chunk retrieved from similarity search with its score.
 */
data class RetrievedChunk(
    /** Chunk ID */
    val id: String,
    /** Document ID */
    val documentId: String,
    /** Text content of the chunk */
    val content: String,
    /** Position within the document (0-indexed) */
    val chunkIndex: Int,
    /** Page number in source document (1-indexed, if available) */
    val pageNumber: Int?,
    /** Cosine similarity score (0.0 - 1.0, higher is more similar) */
    val similarityScore: Float,
    /** Source document filename (if available) */
    val documentName: String?
)

/**
 * A chunk with its embedding, ready to be stored.
 */
data class ChunkWithEmbedding(
    /** Text content of the chunk */
    val content: String,
    /** Position within the document (0-indexed) */
    val chunkIndex: Int,
    /** Total number of chunks in the document */
    val totalChunks: Int,
    /** The embedding vector */
    val embedding: List<Float>,
    /** Embedding model used */
    val embeddingModel: String,
    /** Character offset where this chunk starts in the source text */
    val startOffset: Int?,
    /** Character offset where this chunk ends in the source text */
    val endOffset: Int?,
    /** Page number in source document (1-indexed, if available) */
    val pageNumber: Int?,
    /** Metadata as JSON string */
    val metadata: String?,
    /** Estimated token count */
    val tokenCount: Int?
)

// =============================================================================
// Chat Repository Interface
// =============================================================================

/**
 * Repository interface for chat message persistence and conversation management.
 *
 * Provides CRUD operations for chat messages and session management for
 * document Q&A conversations. Supports both single-document and cross-document
 * chat scopes.
 *
 * CRITICAL: All implementations MUST filter by tenantId for multi-tenant isolation.
 */
interface ChatRepository {

    // =========================================================================
    // Message Operations
    // =========================================================================

    /**
     * Save a new chat message.
     */
    suspend fun saveMessage(message: ChatMessageDto): ChatMessageDto

    /**
     * Get a message by ID.
     */
    suspend fun getMessageById(
        tenantId: TenantId,
        messageId: ChatMessageId
    ): ChatMessageDto?

    /**
     * Get all messages for a session, ordered by sequence number.
     */
    suspend fun getSessionMessages(
        tenantId: TenantId,
        sessionId: ChatSessionId,
        limit: Int = 100,
        offset: Int = 0,
        descending: Boolean = false
    ): Pair<List<ChatMessageDto>, Long>

    /**
     * Get messages for a specific document (across all sessions).
     */
    suspend fun getMessagesForDocument(
        tenantId: TenantId,
        documentId: DocumentId,
        limit: Int = 50,
        offset: Int = 0
    ): Pair<List<ChatMessageDto>, Long>

    /**
     * Get the next sequence number for a session.
     */
    suspend fun getNextSequenceNumber(
        tenantId: TenantId,
        sessionId: ChatSessionId
    ): Int

    // =========================================================================
    // Session Operations
    // =========================================================================

    /**
     * List chat sessions for a tenant with pagination.
     */
    suspend fun listSessions(
        tenantId: TenantId,
        scope: ChatScope? = null,
        documentId: DocumentId? = null,
        limit: Int = 20,
        offset: Int = 0
    ): Pair<List<ChatSessionSummary>, Long>

    /**
     * Get session summary by ID.
     */
    suspend fun getSessionSummary(
        tenantId: TenantId,
        sessionId: ChatSessionId
    ): ChatSessionSummary?

    /**
     * Check if a session exists and belongs to the tenant.
     */
    suspend fun sessionExists(
        tenantId: TenantId,
        sessionId: ChatSessionId
    ): Boolean

    /**
     * Count total messages for a tenant.
     */
    suspend fun countMessagesForTenant(tenantId: TenantId): Long

    /**
     * Count sessions for a tenant.
     */
    suspend fun countSessionsForTenant(tenantId: TenantId): Long

    /**
     * Get recent sessions for a user.
     */
    suspend fun getRecentSessionsForUser(
        tenantId: TenantId,
        userId: UserId,
        limit: Int = 10
    ): List<ChatSessionSummary>
}
