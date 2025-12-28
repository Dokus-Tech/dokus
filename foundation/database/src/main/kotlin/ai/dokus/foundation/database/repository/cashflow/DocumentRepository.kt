package ai.dokus.foundation.database.repository.cashflow

import ai.dokus.foundation.database.tables.cashflow.DocumentsTable
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.DocumentDto
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

/**
 * Repository for document CRUD operations.
 * Documents are pure file metadata. Entity linkage is handled by
 * financial entity tables (Invoice, Bill, Expense) which have documentId FK.
 *
 * CRITICAL: All queries filter by tenantId for security.
 */
@OptIn(ExperimentalUuidApi::class)
class DocumentRepository {

    /**
     * Create a new document record.
     */
    suspend fun create(
        tenantId: TenantId,
        filename: String,
        contentType: String,
        sizeBytes: Long,
        storageKey: String
    ): DocumentId = newSuspendedTransaction {
        val id = DocumentId.generate()
        DocumentsTable.insert {
            it[DocumentsTable.id] = java.util.UUID.fromString(id.toString())
            it[DocumentsTable.tenantId] = java.util.UUID.fromString(tenantId.toString())
            it[DocumentsTable.filename] = filename
            it[DocumentsTable.contentType] = contentType
            it[DocumentsTable.sizeBytes] = sizeBytes
            it[DocumentsTable.storageKey] = storageKey
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
                    (DocumentsTable.id eq java.util.UUID.fromString(documentId.toString())) and
                    (DocumentsTable.tenantId eq java.util.UUID.fromString(tenantId.toString()))
                }
                .map { it.toDocumentDto() }
                .singleOrNull()
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
                    (DocumentsTable.tenantId eq java.util.UUID.fromString(tenantId.toString()))
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
        val tenantIdUuid = java.util.UUID.fromString(tenantId.toString())

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
     * Delete a document.
     * CRITICAL: Must filter by tenantId.
     * Note: The actual file in MinIO should be deleted separately.
     * Note: Cascades to ingestion runs and drafts.
     */
    suspend fun delete(tenantId: TenantId, documentId: DocumentId): Boolean =
        newSuspendedTransaction {
            DocumentsTable.deleteWhere {
                (DocumentsTable.id eq java.util.UUID.fromString(documentId.toString())) and
                (DocumentsTable.tenantId eq java.util.UUID.fromString(tenantId.toString()))
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
                    (DocumentsTable.id eq java.util.UUID.fromString(documentId.toString())) and
                    (DocumentsTable.tenantId eq java.util.UUID.fromString(tenantId.toString()))
                }
                .count() > 0
        }

    private fun ResultRow.toDocumentDto(): DocumentDto {
        return DocumentDto(
            id = DocumentId.parse(this[DocumentsTable.id].toString()),
            tenantId = TenantId(this[DocumentsTable.tenantId].toKotlinUuid()),
            filename = this[DocumentsTable.filename],
            contentType = this[DocumentsTable.contentType],
            sizeBytes = this[DocumentsTable.sizeBytes],
            storageKey = this[DocumentsTable.storageKey],
            uploadedAt = this[DocumentsTable.uploadedAt],
            downloadUrl = null // Generated on-demand by the service layer
        )
    }
}
