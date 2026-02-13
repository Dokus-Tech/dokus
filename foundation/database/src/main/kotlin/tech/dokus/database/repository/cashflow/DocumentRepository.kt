package tech.dokus.database.repository.cashflow

import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import tech.dokus.database.tables.documents.DocumentsTable
import tech.dokus.domain.enums.DocumentListFilter
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.DocumentDto
import java.util.UUID

/**
 * Result of listing documents with their optional drafts and ingestion info.
 */
data class DocumentWithDraftAndIngestion(
    val document: DocumentDto,
    val draft: DraftSummary?,
    val latestIngestion: IngestionRunSummary?
)

data class DocumentCreatePayload(
    val filename: String,
    val contentType: String,
    val sizeBytes: Long,
    val storageKey: String,
    val contentHash: String?,
    val source: DocumentSource = DocumentSource.Upload
)

/**
 * Repository for document CRUD operations.
 * Documents are pure file metadata. Entity linkage is handled by
 * financial entity tables (Invoice, Expense) which have documentId FK.
 *
 * CRITICAL: All queries filter by tenantId for security.
 */
class DocumentRepository {

    /**
     * Create a new document record.
     */
    suspend fun create(
        tenantId: TenantId,
        payload: DocumentCreatePayload
    ): DocumentId = newSuspendedTransaction {
        val id = DocumentId.generate()
        DocumentsTable.insert {
            it[DocumentsTable.id] = UUID.fromString(id.toString())
            it[DocumentsTable.tenantId] = UUID.fromString(tenantId.toString())
            it[DocumentsTable.filename] = payload.filename
            it[DocumentsTable.contentType] = payload.contentType
            it[DocumentsTable.sizeBytes] = payload.sizeBytes
            it[DocumentsTable.storageKey] = payload.storageKey
            it[DocumentsTable.contentHash] = payload.contentHash
            it[DocumentsTable.documentSource] = payload.source
        }
        id
    }

    /**
     * Get a document by ID.
     * CRITICAL: Must filter by tenantId.
     */
    suspend fun getById(tenantId: TenantId, documentId: DocumentId): DocumentDto? =
        newSuspendedTransaction {
            DocumentsTable.selectAll()
                .where {
                    (DocumentsTable.id eq UUID.fromString(documentId.toString())) and
                        (DocumentsTable.tenantId eq UUID.fromString(tenantId.toString()))
                }
                .map { it.toDocumentDto() }
                .singleOrNull()
        }

    /**
     * Get the content hash for a document by ID.
     * CRITICAL: Must filter by tenantId.
     */
    suspend fun getContentHash(tenantId: TenantId, documentId: DocumentId): String? =
        newSuspendedTransaction {
            DocumentsTable
                .select(DocumentsTable.contentHash)
                .where {
                    (DocumentsTable.id eq UUID.fromString(documentId.toString())) and
                        (DocumentsTable.tenantId eq UUID.fromString(tenantId.toString()))
                }
                .singleOrNull()
                ?.get(DocumentsTable.contentHash)
        }

    /**
     * Get a document by storage key.
     * CRITICAL: Must filter by tenantId.
     */
    suspend fun getByStorageKey(tenantId: TenantId, storageKey: String): DocumentDto? =
        newSuspendedTransaction {
            DocumentsTable.selectAll()
                .where {
                    (DocumentsTable.storageKey eq storageKey) and
                        (DocumentsTable.tenantId eq UUID.fromString(tenantId.toString()))
                }
                .map { it.toDocumentDto() }
                .singleOrNull()
        }

    /**
     * Get a document by content hash.
     * CRITICAL: Must filter by tenantId.
     */
    suspend fun getByContentHash(tenantId: TenantId, contentHash: String): DocumentDto? =
        newSuspendedTransaction {
            DocumentsTable.selectAll()
                .where {
                    (DocumentsTable.contentHash eq contentHash) and
                        (DocumentsTable.tenantId eq UUID.fromString(tenantId.toString()))
                }
                .map { it.toDocumentDto() }
                .singleOrNull()
        }

    /**
     * List all documents for a tenant with pagination.
     * CRITICAL: Must filter by tenantId.
     *
     * @return Pair of (documents, totalCount)
     */
    suspend fun listByTenant(
        tenantId: TenantId,
        page: Int = 0,
        limit: Int = 20
    ): Pair<List<DocumentDto>, Long> = newSuspendedTransaction {
        val tenantIdUuid = UUID.fromString(tenantId.toString())

        val baseQuery = DocumentsTable.selectAll()
            .where { DocumentsTable.tenantId eq tenantIdUuid }

        val total = baseQuery.count()

        val documents = baseQuery
            .orderBy(DocumentsTable.uploadedAt, SortOrder.DESC)
            .limit(limit)
            .offset((page * limit).toLong())
            .map { it.toDocumentDto() }

        documents to total
    }

    /**
     * List documents with optional drafts and latest ingestion info.
     * This is the primary query for the document list endpoint.
     *
     * Documents are returned regardless of whether they have drafts.
     * Filters:
     * - documentStatus: Only applies when draft exists (documents without drafts pass this filter)
     * - documentType: Only applies when draft exists
     * - ingestionStatus: Filters by latest ingestion status
     * - search: Filters by filename (ILIKE)
     *
     * CRITICAL: Must filter by tenantId.
     *
     * @return Pair of (documents with drafts/ingestion, totalCount)
     */
    suspend fun listWithDraftsAndIngestion(
        tenantId: TenantId,
        filter: DocumentListFilter? = null,
        documentStatus: DocumentStatus? = null,
        documentType: DocumentType? = null,
        ingestionStatus: IngestionStatus? = null,
        search: String? = null,
        page: Int = 0,
        limit: Int = 20
    ): Pair<List<DocumentWithDraftAndIngestion>, Long> {
        DocumentIngestionRunRepository().recoverStaleProcessingRunsForTenant(tenantId)

        return DocumentListingQuery.listWithDraftsAndIngestion(
            tenantId = tenantId,
            filter = filter,
            documentStatus = documentStatus,
            documentType = documentType,
            ingestionStatus = ingestionStatus,
            search = search,
            page = page,
            limit = limit
        )
    }

    /**
     * Delete a document.
     * CRITICAL: Must filter by tenantId.
     * Note: The actual file in MinIO should be deleted separately.
     * Note: Cascades to ingestion runs and drafts.
     */
    suspend fun delete(tenantId: TenantId, documentId: DocumentId): Boolean =
        newSuspendedTransaction {
            DocumentsTable.deleteWhere {
                (DocumentsTable.id eq UUID.fromString(documentId.toString())) and
                    (DocumentsTable.tenantId eq UUID.fromString(tenantId.toString()))
            } > 0
        }

    /**
     * Check if a document exists.
     * CRITICAL: Must filter by tenantId.
     */
    suspend fun exists(tenantId: TenantId, documentId: DocumentId): Boolean =
        newSuspendedTransaction {
            DocumentsTable.selectAll()
                .where {
                    (DocumentsTable.id eq UUID.fromString(documentId.toString())) and
                        (DocumentsTable.tenantId eq UUID.fromString(tenantId.toString()))
                }
                .count() > 0
        }

}
