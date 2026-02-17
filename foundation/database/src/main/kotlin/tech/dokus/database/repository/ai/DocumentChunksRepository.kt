package tech.dokus.database.repository.ai
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import tech.dokus.database.tables.ai.DocumentChunksTable
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.ChunkMetadata
import tech.dokus.domain.model.DocumentChunkDto
import tech.dokus.domain.model.DocumentChunkId
import tech.dokus.domain.repository.ChunkRepository
import tech.dokus.domain.repository.ChunkSearchResult
import tech.dokus.domain.repository.ChunkWithEmbedding
import tech.dokus.domain.repository.RetrievedChunk
import tech.dokus.domain.utils.json
import tech.dokus.foundation.backend.utils.loggerFor
import java.sql.Connection
import kotlin.time.Clock
import kotlin.uuid.Uuid

/**
 * Repository for document chunks with vector embeddings.
 *
 * Implements the ChunkRepository interface from foundation:domain for RAG operations.
 * Provides CRUD operations for document chunks and vector similarity search using pgvector.
 *
 * CRITICAL SECURITY: All queries MUST filter by tenantId for multi-tenant isolation.
 */
class DocumentChunksRepository : ChunkRepository {

    private val logger = loggerFor()

    // =========================================================================
    // ChunkRepository Interface Implementation
    // =========================================================================

    /**
     * Search for similar chunks using vector similarity (cosine distance).
     *
     * Uses pgvector's `<=>` operator for cosine distance.
     * CRITICAL: Always filters by tenantId for multi-tenant security.
     *
     * @param confirmedOnly If true, only search chunks from documents with Confirmed draft status.
     *                      This is the default for chat to ensure only verified documents are used.
     */
    override suspend fun searchSimilarChunks(
        tenantId: TenantId,
        queryEmbedding: List<Float>,
        documentId: DocumentId?,
        topK: Int,
        minSimilarity: Float,
        confirmedOnly: Boolean
    ): ChunkSearchResult = newSuspendedTransaction {
        // Build the vector string for pgvector
        val vectorString = "[${queryEmbedding.joinToString(",")}]"

        // Count total chunks for this tenant (or document)
        val baseCountQuery = DocumentChunksTable.selectAll()
            .where { DocumentChunksTable.tenantId eq tenantId.value }

        val countQuery = if (documentId != null) {
            baseCountQuery.andWhere {
                DocumentChunksTable.documentId eq documentId.value
            }
        } else {
            baseCountQuery
        }
        val totalSearched = countQuery.count()

        // Perform vector similarity search using raw SQL for pgvector operators
        // When confirmedOnly=true, join with document_drafts to filter by document_status = 'CONFIRMED'
        val sql = buildString {
            append("SELECT ")
            append("dc.id, dc.document_id, dc.content, dc.chunk_index, ")
            append("dc.page_number, dc.metadata, ")
            append("(1 - (dc.embedding <=> ?::vector)) as similarity, ")
            append("d.filename as document_name ")
            append("FROM document_chunks dc ")
            append("LEFT JOIN documents d ON dc.document_id = d.id ")
            if (confirmedOnly) {
                // Join with drafts table to filter by Confirmed status
                append(
                    "INNER JOIN document_drafts dd ON dc.document_id = dd.document_id AND dc.tenant_id = dd.tenant_id "
                )
            }
            append("WHERE dc.tenant_id = ? ")
            append("AND dc.embedding IS NOT NULL ")
            if (documentId != null) {
                append("AND dc.document_id = ? ")
            }
            if (confirmedOnly) {
                append("AND dd.document_status = 'CONFIRMED' ")
            }
            append("AND (1 - (dc.embedding <=> ?::vector)) >= ? ")
            append("ORDER BY dc.embedding <=> ?::vector ")
            append("LIMIT ?")
        }

        logger.debug("Executing vector search SQL (confirmedOnly=$confirmedOnly)")

        val chunks = mutableListOf<RetrievedChunk>()

        // Execute raw SQL with parameterized query
        val connection = this.connection.connection as Connection
        connection.prepareStatement(sql).use { stmt ->
            var paramIdx = 1
            stmt.setString(paramIdx++, vectorString) // similarity calc
            stmt.setObject(paramIdx++, tenantId.value) // tenant filter
            if (documentId != null) {
                stmt.setObject(paramIdx++, documentId.value) // document filter
            }
            stmt.setString(paramIdx++, vectorString) // similarity threshold
            stmt.setFloat(paramIdx++, minSimilarity)
            stmt.setString(paramIdx++, vectorString) // order by
            stmt.setInt(paramIdx, topK)

            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    chunks.add(
                        RetrievedChunk(
                            id = rs.getString("id"),
                            documentId = rs.getString("document_id"),
                            content = rs.getString("content"),
                            chunkIndex = rs.getInt("chunk_index"),
                            pageNumber = rs.getObject("page_number") as? Int,
                            similarityScore = rs.getFloat("similarity"),
                            documentName = rs.getString("document_name")
                        )
                    )
                }
            }
        }

        logger.debug("Vector search returned ${chunks.size} chunks with similarity >= $minSimilarity")

        ChunkSearchResult(
            chunks = chunks,
            totalSearched = totalSearched
        )
    }

    /**
     * Store chunks with embeddings for a document.
     * Includes contentHash for deduplication on reprocessing.
     */
    override suspend fun storeChunks(
        tenantId: TenantId,
        documentId: DocumentId,
        contentHash: String,
        chunks: List<ChunkWithEmbedding>
    ): Unit = newSuspendedTransaction {
        if (chunks.isEmpty()) {
            logger.debug("No chunks to store for document {}", documentId)
            return@newSuspendedTransaction
        }

        val tenantUuid = tenantId.value
        val documentUuid = documentId.value
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        logger.info("Storing ${chunks.size} chunks for document $documentId, tenant $tenantId, hash=$contentHash")

        DocumentChunksTable.batchInsert(chunks) { chunk ->
            this[DocumentChunksTable.id] = Uuid.random()
            this[DocumentChunksTable.tenantId] = tenantUuid
            this[DocumentChunksTable.documentId] = documentUuid
            this[DocumentChunksTable.content] = chunk.content
            this[DocumentChunksTable.contentHash] = contentHash
            this[DocumentChunksTable.chunkIndex] = chunk.chunkIndex
            this[DocumentChunksTable.totalChunks] = chunk.totalChunks
            this[DocumentChunksTable.embedding] = chunk.embedding
            this[DocumentChunksTable.embeddingModel] = chunk.embeddingModel
            this[DocumentChunksTable.startOffset] = chunk.startOffset
            this[DocumentChunksTable.endOffset] = chunk.endOffset
            this[DocumentChunksTable.pageNumber] = chunk.pageNumber
            this[DocumentChunksTable.metadata] = chunk.metadata
            this[DocumentChunksTable.tokenCount] = chunk.tokenCount
            this[DocumentChunksTable.indexedAt] = now
            this[DocumentChunksTable.createdAt] = now
        }

        logger.debug("Successfully stored ${chunks.size} chunks")
    }

    /**
     * Get the content hash for a document's chunks (for deduplication).
     * Returns the content hash if chunks exist, null otherwise.
     */
    override suspend fun getContentHashForDocument(
        tenantId: TenantId,
        documentId: DocumentId
    ): String? = newSuspendedTransaction {
        val tenantUuid = tenantId.value
        val documentUuid = documentId.value

        DocumentChunksTable
            .selectAll()
            .where {
                (DocumentChunksTable.tenantId eq tenantUuid) and
                    (DocumentChunksTable.documentId eq documentUuid)
            }
            .limit(1)
            .singleOrNull()
            ?.get(DocumentChunksTable.contentHash)
    }

    /**
     * Delete all chunks for a document.
     */
    override suspend fun deleteChunksForDocument(
        tenantId: TenantId,
        documentId: DocumentId
    ): Int = newSuspendedTransaction {
        val tenantUuid = tenantId.value
        val documentUuid = documentId.value

        logger.info("Deleting chunks for document $documentId, tenant $tenantId")

        val deleted = DocumentChunksTable.deleteWhere {
            (DocumentChunksTable.tenantId eq tenantUuid) and
                (DocumentChunksTable.documentId eq documentUuid)
        }

        logger.debug("Deleted $deleted chunks")
        deleted
    }

    // =========================================================================
    // Additional Repository Methods
    // =========================================================================

    /**
     * Get all chunks for a document, ordered by chunk index.
     */
    override suspend fun getChunksForDocument(
        tenantId: TenantId,
        documentId: DocumentId
    ): List<DocumentChunkDto> = newSuspendedTransaction {
        val tenantUuid = tenantId.value
        val documentUuid = documentId.value

        DocumentChunksTable
            .selectAll()
            .where {
                (DocumentChunksTable.tenantId eq tenantUuid) and
                    (DocumentChunksTable.documentId eq documentUuid)
            }
            .orderBy(DocumentChunksTable.chunkIndex to SortOrder.ASC)
            .map { it.toChunkDto() }
    }

    /**
     * Get a single chunk by ID.
     */
    override suspend fun getChunkById(
        tenantId: TenantId,
        chunkId: DocumentChunkId
    ): DocumentChunkDto? = newSuspendedTransaction {
        val tenantUuid = tenantId.value
        val chunkUuid = chunkId.value

        DocumentChunksTable
            .selectAll()
            .where {
                (DocumentChunksTable.id eq chunkUuid) and
                    (DocumentChunksTable.tenantId eq tenantUuid)
            }
            .singleOrNull()
            ?.toChunkDto()
    }

    /**
     * Count chunks for a document.
     */
    override suspend fun countChunksForDocument(
        tenantId: TenantId,
        documentId: DocumentId
    ): Long = newSuspendedTransaction {
        val tenantUuid = tenantId.value
        val documentUuid = documentId.value

        DocumentChunksTable
            .selectAll()
            .where {
                (DocumentChunksTable.tenantId eq tenantUuid) and
                    (DocumentChunksTable.documentId eq documentUuid)
            }
            .count()
    }

    /**
     * Check if a document has chunks.
     */
    override suspend fun hasChunks(
        tenantId: TenantId,
        documentId: DocumentId
    ): Boolean = countChunksForDocument(tenantId, documentId) > 0

    /**
     * Get the total chunk count for a tenant.
     */
    override suspend fun countTotalChunksForTenant(
        tenantId: TenantId
    ): Long = newSuspendedTransaction {
        val tenantUuid = tenantId.value

        DocumentChunksTable
            .selectAll()
            .where { DocumentChunksTable.tenantId eq tenantUuid }
            .count()
    }

    // =========================================================================
    // Private Helpers
    // =========================================================================

    private fun ResultRow.toChunkDto(): DocumentChunkDto {
        val metadataJson = this[DocumentChunksTable.metadata]
        val metadata = metadataJson?.let {
            try {
                json.decodeFromString<ChunkMetadata>(it)
            } catch (e: Exception) {
                logger.warn("Failed to parse chunk metadata: ${e.message}")
                null
            }
        }

        return DocumentChunkDto(
            id = DocumentChunkId(this[DocumentChunksTable.id].value),
            documentId = DocumentId.parse(
                this[DocumentChunksTable.documentId].toString()
            ),
            tenantId = TenantId(this[DocumentChunksTable.tenantId]),
            content = this[DocumentChunksTable.content],
            chunkIndex = this[DocumentChunksTable.chunkIndex],
            totalChunks = this[DocumentChunksTable.totalChunks],
            startOffset = this[DocumentChunksTable.startOffset],
            endOffset = this[DocumentChunksTable.endOffset],
            pageNumber = this[DocumentChunksTable.pageNumber],
            embeddingModel = this[DocumentChunksTable.embeddingModel],
            tokenCount = this[DocumentChunksTable.tokenCount],
            metadata = metadata,
            createdAt = this[DocumentChunksTable.createdAt]
        )
    }
}
