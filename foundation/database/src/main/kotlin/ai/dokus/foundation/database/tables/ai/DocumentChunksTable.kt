package ai.dokus.foundation.database.tables.ai

import ai.dokus.foundation.database.columns.EmbeddingDimensions
import ai.dokus.foundation.database.columns.vector
import ai.dokus.foundation.database.tables.auth.TenantTable
import ai.dokus.foundation.database.tables.cashflow.DocumentProcessingTable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime

/**
 * Document chunks table - stores chunked document content with vector embeddings for RAG.
 *
 * OWNER: cashflow service (via processor service for write)
 * ACCESS: ai-backend (read for RAG retrieval)
 * CRITICAL: All queries MUST filter by tenant_id for multi-tenant security.
 *
 * This table stores:
 * - Document chunks (semantic/fixed-size segments of document text)
 * - Vector embeddings for similarity search (pgvector)
 * - Chunk position and metadata for citation/provenance
 * - Source page/location information for UI display
 *
 * Vector embedding dimensions are configured based on the embedding model:
 * - Ollama nomic-embed-text: 768 dimensions (default for local)
 * - OpenAI text-embedding-3-small: 1536 dimensions
 *
 * Requires pgvector extension:
 * ```sql
 * CREATE EXTENSION IF NOT EXISTS vector;
 * ```
 */
object DocumentChunksTable : UUIDTable("document_chunks") {

    // Reference to the document processing record
    val documentProcessingId = uuid("document_processing_id")
        .references(DocumentProcessingTable.id, onDelete = ReferenceOption.CASCADE)

    // Multi-tenancy (denormalized for query performance - CRITICAL for isolation)
    val tenantId = uuid("tenant_id").references(
        TenantTable.id,
        onDelete = ReferenceOption.CASCADE
    )

    // Chunk content (the actual text segment)
    val content = text("content")

    // Vector embedding for similarity search
    // Using 768 dimensions for Ollama nomic-embed-text (local default)
    // NOTE: Dimension must match the embedding model used
    val embedding = vector("embedding", EmbeddingDimensions.OLLAMA_NOMIC_EMBED_TEXT).nullable()

    // Chunk position within document (0-indexed)
    val chunkIndex = integer("chunk_index")

    // Total number of chunks in the document (for context)
    val totalChunks = integer("total_chunks")

    // Character offsets for provenance (where in raw text this chunk came from)
    val startOffset = integer("start_offset").nullable()
    val endOffset = integer("end_offset").nullable()

    // Source page number (for PDF documents, 1-indexed)
    val pageNumber = integer("page_number").nullable()

    // Chunk metadata as JSON (e.g., headers, section info, nearby entities)
    val metadata = text("metadata").nullable()

    // Embedding model used (e.g., "nomic-embed-text", "text-embedding-3-small")
    val embeddingModel = varchar("embedding_model", 100).nullable()

    // Token count of the chunk (useful for context window management)
    val tokenCount = integer("token_count").nullable()

    // Timestamps
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    init {
        // CRITICAL: Index tenant_id for all queries (tenant isolation)
        index(false, tenantId)

        // For retrieving chunks by document
        index(false, tenantId, documentProcessingId)

        // For ordered chunk retrieval within a document
        index(false, documentProcessingId, chunkIndex)

        // NOTE: HNSW index for vector similarity search should be created via SQL migration:
        // CREATE INDEX document_chunks_embedding_idx ON document_chunks
        //   USING hnsw (embedding vector_cosine_ops) WITH (m = 16, ef_construction = 64);
        //
        // Exposed doesn't support HNSW index creation natively, so this must be
        // added to the Flyway migration script alongside the table creation.
    }
}
