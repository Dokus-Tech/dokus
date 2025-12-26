package ai.dokus.backend.repository.cashflow

import ai.dokus.ai.services.ChunkRepository
import ai.dokus.ai.services.ChunkSearchResult
import ai.dokus.ai.services.ChunkWithEmbedding
import ai.dokus.ai.services.RAGService
import ai.dokus.foundation.database.tables.ai.DocumentChunksTable
import ai.dokus.foundation.domain.ids.DocumentProcessingId
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.model.ChunkMetadata
import ai.dokus.foundation.domain.model.DocumentChunkDto
import ai.dokus.foundation.domain.model.DocumentChunkId
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi

/**
 * Repository for document chunks with vector embeddings.
 *
 * Implements the ChunkRepository interface from ai-backend for RAG operations.
 * Provides CRUD operations for document chunks and vector similarity search.
 *
 * CRITICAL SECURITY: All queries MUST filter by tenantId for multi-tenant isolation.
 */
@OptIn(ExperimentalUuidApi::class)
class DocumentChunksRepository : ChunkRepository {

    private val logger = LoggerFactory.getLogger(DocumentChunksRepository::class.java)

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // =========================================================================
    // ChunkRepository Interface Implementation
    // =========================================================================

    /**
     * Search for similar chunks using vector similarity (cosine distance).
     *
     * Uses pgvector's `<=>` operator for cosine distance.
     * CRITICAL: Always filters by tenantId for multi-tenant security.
     *
     * @param tenantId The tenant to filter by (REQUIRED for security)
     * @param queryEmbedding The query embedding vector
     * @param documentId Optional document ID to filter to a single document
     * @param topK Maximum number of chunks to return
     * @param minSimilarity Minimum cosine similarity threshold (0.0 - 1.0)
     * @return ChunkSearchResult containing matched chunks
     */
    override suspend fun searchSimilarChunks(
        tenantId: TenantId,
        queryEmbedding: List<Float>,
        documentId: DocumentProcessingId?,
        topK: Int,
        minSimilarity: Float
    ): ChunkSearchResult = newSuspendedTransaction {
        val tenantUuid = UUID.fromString(tenantId.toString())

        // Build the vector string for pgvector
        val vectorString = "[${queryEmbedding.joinToString(",")}]"

        // Count total chunks for this tenant (or document)
        val baseCountQuery = DocumentChunksTable.selectAll()
            .where { DocumentChunksTable.tenantId eq tenantUuid }

        val countQuery = if (documentId != null) {
            baseCountQuery.andWhere {
                DocumentChunksTable.documentProcessingId eq UUID.fromString(documentId.toString())
            }
        } else {
            baseCountQuery
        }
        val totalSearched = countQuery.count()

        // Convert minSimilarity to maxDistance (cosine distance = 1 - similarity)
        val maxDistance = 1.0 - minSimilarity

        // Perform vector similarity search using raw SQL for pgvector operators
        // The <=> operator returns cosine distance (0 = identical, 2 = opposite)
        val sql = buildString {
            append("SELECT ")
            append("dc.id, dc.document_processing_id, dc.content, dc.chunk_index, ")
            append("dc.page_number, dc.metadata, ")
            append("(1 - (dc.embedding <=> '$vectorString'::vector)) as similarity, ")
            append("d.filename as document_name ")
            append("FROM document_chunks dc ")
            append("LEFT JOIN document_processing dp ON dc.document_processing_id = dp.id ")
            append("LEFT JOIN documents d ON dp.document_id = d.id ")
            append("WHERE dc.tenant_id = '$tenantUuid' ")
            append("AND dc.embedding IS NOT NULL ")
            if (documentId != null) {
                append("AND dc.document_processing_id = '${UUID.fromString(documentId.toString())}' ")
            }
            append("AND (1 - (dc.embedding <=> '$vectorString'::vector)) >= $minSimilarity ")
            append("ORDER BY dc.embedding <=> '$vectorString'::vector ")
            append("LIMIT $topK")
        }

        logger.debug("Executing vector search SQL: ${sql.take(200)}...")

        val chunks = mutableListOf<RAGService.RetrievedChunk>()

        // Execute raw SQL and map results
        val connection = this.connection.connection as java.sql.Connection
        connection.createStatement().use { stmt ->
            stmt.executeQuery(sql).use { rs ->
                while (rs.next()) {
                    chunks.add(
                        RAGService.RetrievedChunk(
                            id = rs.getString("id"),
                            documentProcessingId = rs.getString("document_processing_id"),
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
     *
     * @param tenantId The tenant owning the document
     * @param documentId The document processing ID
     * @param chunks The chunks to store with their embeddings
     */
    override suspend fun storeChunks(
        tenantId: TenantId,
        documentId: DocumentProcessingId,
        chunks: List<ChunkWithEmbedding>
    ): Unit = newSuspendedTransaction {
        if (chunks.isEmpty()) {
            logger.debug("No chunks to store for document $documentId")
            return@newSuspendedTransaction
        }

        val tenantUuid = UUID.fromString(tenantId.toString())
        val documentUuid = UUID.fromString(documentId.toString())
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        logger.info("Storing ${chunks.size} chunks for document $documentId, tenant $tenantId")

        DocumentChunksTable.batchInsert(chunks) { chunk ->
            this[DocumentChunksTable.id] = UUID.randomUUID()
            this[DocumentChunksTable.tenantId] = tenantUuid
            this[DocumentChunksTable.documentProcessingId] = documentUuid
            this[DocumentChunksTable.content] = chunk.content
            this[DocumentChunksTable.chunkIndex] = chunk.chunkIndex
            this[DocumentChunksTable.totalChunks] = chunk.totalChunks
            this[DocumentChunksTable.embedding] = chunk.embedding
            this[DocumentChunksTable.embeddingModel] = chunk.embeddingModel
            this[DocumentChunksTable.startOffset] = chunk.startOffset
            this[DocumentChunksTable.endOffset] = chunk.endOffset
            this[DocumentChunksTable.pageNumber] = chunk.pageNumber
            this[DocumentChunksTable.metadata] = chunk.metadata
            this[DocumentChunksTable.tokenCount] = chunk.tokenCount
            this[DocumentChunksTable.createdAt] = now
        }

        logger.debug("Successfully stored ${chunks.size} chunks")
    }

    /**
     * Delete all chunks for a document.
     *
     * @param tenantId The tenant owning the document
     * @param documentId The document processing ID
     * @return Number of chunks deleted
     */
    override suspend fun deleteChunksForDocument(
        tenantId: TenantId,
        documentId: DocumentProcessingId
    ): Int = newSuspendedTransaction {
        val tenantUuid = UUID.fromString(tenantId.toString())
        val documentUuid = UUID.fromString(documentId.toString())

        logger.info("Deleting chunks for document $documentId, tenant $tenantId")

        val deleted = DocumentChunksTable.deleteWhere {
            (DocumentChunksTable.tenantId eq tenantUuid) and
                    (DocumentChunksTable.documentProcessingId eq documentUuid)
        }

        logger.debug("Deleted $deleted chunks")
        deleted
    }

    // =========================================================================
    // Additional Repository Methods
    // =========================================================================

    /**
     * Get all chunks for a document, ordered by chunk index.
     * CRITICAL: MUST filter by tenantId.
     */
    suspend fun getChunksForDocument(
        tenantId: TenantId,
        documentId: DocumentProcessingId
    ): List<DocumentChunkDto> = newSuspendedTransaction {
        val tenantUuid = UUID.fromString(tenantId.toString())
        val documentUuid = UUID.fromString(documentId.toString())

        DocumentChunksTable
            .selectAll()
            .where {
                (DocumentChunksTable.tenantId eq tenantUuid) and
                        (DocumentChunksTable.documentProcessingId eq documentUuid)
            }
            .orderBy(DocumentChunksTable.chunkIndex to SortOrder.ASC)
            .map { it.toChunkDto() }
    }

    /**
     * Get a single chunk by ID.
     * CRITICAL: MUST filter by tenantId.
     */
    suspend fun getChunkById(
        tenantId: TenantId,
        chunkId: DocumentChunkId
    ): DocumentChunkDto? = newSuspendedTransaction {
        val tenantUuid = UUID.fromString(tenantId.toString())
        val chunkUuid = UUID.fromString(chunkId.toString())

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
     * CRITICAL: MUST filter by tenantId.
     */
    suspend fun countChunksForDocument(
        tenantId: TenantId,
        documentId: DocumentProcessingId
    ): Long = newSuspendedTransaction {
        val tenantUuid = UUID.fromString(tenantId.toString())
        val documentUuid = UUID.fromString(documentId.toString())

        DocumentChunksTable
            .selectAll()
            .where {
                (DocumentChunksTable.tenantId eq tenantUuid) and
                        (DocumentChunksTable.documentProcessingId eq documentUuid)
            }
            .count()
    }

    /**
     * Check if a document has chunks.
     */
    suspend fun hasChunks(
        tenantId: TenantId,
        documentId: DocumentProcessingId
    ): Boolean = countChunksForDocument(tenantId, documentId) > 0

    /**
     * Get the total chunk count for a tenant.
     * Useful for usage monitoring.
     */
    suspend fun countTotalChunksForTenant(
        tenantId: TenantId
    ): Long = newSuspendedTransaction {
        val tenantUuid = UUID.fromString(tenantId.toString())

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
            id = DocumentChunkId.parse(this[DocumentChunksTable.id].value.toString()),
            documentProcessingId = DocumentProcessingId.parse(
                this[DocumentChunksTable.documentProcessingId].toString()
            ),
            tenantId = TenantId.parse(this[DocumentChunksTable.tenantId].toString()),
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
