package ai.dokus.foundation.domain.model

import ai.dokus.foundation.domain.ids.DocumentProcessingId
import ai.dokus.foundation.domain.ids.TenantId
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

// =============================================================================
// Document Chunk ID
// =============================================================================

/**
 * Strongly typed ID for document chunks.
 */
@OptIn(ExperimentalUuidApi::class)
@Serializable
@JvmInline
value class DocumentChunkId(val value: Uuid) {
    override fun toString(): String = value.toString()

    companion object {
        fun generate(): DocumentChunkId = DocumentChunkId(Uuid.random())
        fun parse(value: String): DocumentChunkId = DocumentChunkId(Uuid.parse(value))
    }
}

// =============================================================================
// Provenance Models
// =============================================================================

/**
 * Character offset range within the source document text.
 * Used to link extracted data back to its source location.
 */
@Serializable
data class TextOffsets(
    /** Start character offset (0-indexed, inclusive) */
    val start: Int,

    /** End character offset (0-indexed, exclusive) */
    val end: Int
) {
    /** Length of the text span */
    val length: Int get() = end - start
}

/**
 * Complete provenance information for a document chunk.
 * Tracks where the chunk came from in the original document.
 */
@Serializable
data class ChunkProvenance(
    /** Character offsets in the raw document text */
    val offsets: TextOffsets? = null,

    /** Page number in the original document (1-indexed for PDFs) */
    val pageNumber: Int? = null,

    /** Section or heading the chunk belongs to (if detected) */
    val sectionTitle: String? = null,

    /** Nearby headers/titles for context */
    val nearbyHeaders: List<String>? = null
)

// =============================================================================
// Chunk Metadata
// =============================================================================

/**
 * Additional metadata for a document chunk.
 * Stored as JSON in the database.
 */
@Serializable
data class ChunkMetadata(
    /** Section title or header nearest to this chunk */
    val sectionTitle: String? = null,

    /** Detected language of the chunk content */
    val language: String? = null,

    /** Key entities detected in the chunk (e.g., names, amounts) */
    val entities: List<String>? = null,

    /** Whether this chunk contains tabular data */
    val containsTable: Boolean = false,

    /** Whether this chunk contains monetary values */
    val containsAmounts: Boolean = false,

    /** Chunk type hint (e.g., "header", "body", "table", "footer") */
    val chunkType: String? = null
)

// =============================================================================
// Document Chunk DTOs
// =============================================================================

/**
 * Document chunk DTO - represents a segment of document text with embeddings.
 *
 * Chunks are created during document processing for RAG (Retrieval Augmented Generation).
 * Each chunk contains:
 * - The text content of the segment
 * - Vector embedding for semantic similarity search
 * - Provenance information linking back to source location
 * - Metadata for context and filtering
 *
 * CRITICAL: All chunk operations must filter by tenantId for multi-tenant isolation.
 */
@Serializable
data class DocumentChunkDto(
    /** Chunk ID */
    val id: DocumentChunkId,

    /** Reference to the source document processing record */
    val documentProcessingId: DocumentProcessingId,

    /** Tenant for multi-tenant isolation */
    val tenantId: TenantId,

    /** Text content of this chunk */
    val content: String,

    /** Position of this chunk within the document (0-indexed) */
    val chunkIndex: Int,

    /** Total number of chunks in the document */
    val totalChunks: Int,

    /** Character offsets in the source document */
    val startOffset: Int? = null,

    /** End character offset in the source document */
    val endOffset: Int? = null,

    /** Page number in the source document (1-indexed for PDFs) */
    val pageNumber: Int? = null,

    /** Embedding model used to generate the vector */
    val embeddingModel: String? = null,

    /** Token count of this chunk (for context window management) */
    val tokenCount: Int? = null,

    /** Additional metadata as structured object */
    val metadata: ChunkMetadata? = null,

    /** When the chunk was created */
    val createdAt: LocalDateTime,

    // =========================================================================
    // Runtime fields (populated during retrieval, not stored in DB)
    // =========================================================================

    /** Similarity score from vector search (0.0 - 1.0, higher is more similar) */
    val similarityScore: Float? = null,

    /** Source document filename for display */
    val documentName: String? = null
)

/**
 * Summary view for chunk lists and search results.
 * Lighter weight than full DocumentChunkDto.
 */
@Serializable
data class DocumentChunkSummary(
    val id: DocumentChunkId,
    val documentProcessingId: DocumentProcessingId,
    val chunkIndex: Int,
    val content: String,
    val pageNumber: Int? = null,
    val similarityScore: Float? = null,
    val documentName: String? = null
)

/**
 * Paginated response for document chunk queries.
 */
@Serializable
data class DocumentChunkListResponse(
    val items: List<DocumentChunkDto>,
    val total: Long,
    val page: Int,
    val limit: Int,
    val hasMore: Boolean
)

// =============================================================================
// RAG Retrieval Models
// =============================================================================

/**
 * Request to retrieve relevant chunks for RAG.
 */
@Serializable
data class ChunkRetrievalRequest(
    /** Query text to find similar chunks for */
    val query: String,

    /** Maximum number of chunks to return */
    val topK: Int = 5,

    /** Minimum similarity threshold (0.0 - 1.0) */
    val minSimilarity: Float = 0.3f,

    /** Filter to specific document (null for cross-document search) */
    val documentProcessingId: DocumentProcessingId? = null,

    /** Include only chunks from confirmed documents */
    val confirmedOnly: Boolean = true
)

/**
 * Response from chunk retrieval with context.
 */
@Serializable
data class ChunkRetrievalResponse(
    /** Retrieved chunks ordered by relevance */
    val chunks: List<DocumentChunkSummary>,

    /** Total chunks searched */
    val totalSearched: Long,

    /** Query embedding model used */
    val embeddingModel: String,

    /** Time taken for retrieval in milliseconds */
    val retrievalTimeMs: Int
)

// =============================================================================
// Chunking Configuration
// =============================================================================

/**
 * Configuration for document chunking strategy.
 */
@Serializable
data class ChunkingConfig(
    /** Target chunk size in characters */
    val targetChunkSize: Int = 500,

    /** Maximum chunk size in characters */
    val maxChunkSize: Int = 1000,

    /** Overlap between chunks in characters */
    val overlapSize: Int = 50,

    /** Minimum chunk size (avoid tiny chunks) */
    val minChunkSize: Int = 100,

    /** Whether to try splitting on sentence boundaries */
    val splitOnSentences: Boolean = true,

    /** Whether to try splitting on paragraph boundaries */
    val splitOnParagraphs: Boolean = true
)
