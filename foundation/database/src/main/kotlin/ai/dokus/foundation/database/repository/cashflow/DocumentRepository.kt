package ai.dokus.foundation.database.repository.cashflow

import ai.dokus.foundation.database.tables.cashflow.DocumentsTable
import ai.dokus.foundation.domain.enums.EntityType
import ai.dokus.foundation.domain.ids.DocumentId
import ai.dokus.foundation.domain.ids.TenantId
import tech.dokus.domain.model.DocumentDto
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

/**
 * Repository for document CRUD operations.
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
     * Get a document linked to a specific entity.
     * CRITICAL: Must filter by tenantId.
     */
    suspend fun getByEntity(
        tenantId: TenantId,
        entityType: EntityType,
        entityId: String
    ): DocumentDto? = newSuspendedTransaction {
        DocumentsTable.selectAll()
            .where {
                (DocumentsTable.tenantId eq java.util.UUID.fromString(tenantId.toString())) and
                (DocumentsTable.entityType eq entityType) and
                (DocumentsTable.entityId eq entityId)
            }
            .map { it.toDocumentDto() }
            .singleOrNull()
    }

    /**
     * Get all documents linked to a specific entity.
     * CRITICAL: Must filter by tenantId.
     */
    suspend fun listByEntity(
        tenantId: TenantId,
        entityType: EntityType,
        entityId: String
    ): List<DocumentDto> = newSuspendedTransaction {
        DocumentsTable.selectAll()
            .where {
                (DocumentsTable.tenantId eq java.util.UUID.fromString(tenantId.toString())) and
                (DocumentsTable.entityType eq entityType) and
                (DocumentsTable.entityId eq entityId)
            }
            .orderBy(DocumentsTable.uploadedAt, SortOrder.DESC)
            .map { it.toDocumentDto() }
    }

    /**
     * Link a document to an entity.
     * CRITICAL: Must filter by tenantId.
     */
    suspend fun linkToEntity(
        tenantId: TenantId,
        documentId: DocumentId,
        entityType: EntityType,
        entityId: String
    ): Boolean = newSuspendedTransaction {
        DocumentsTable.update({
            (DocumentsTable.id eq java.util.UUID.fromString(documentId.toString())) and
            (DocumentsTable.tenantId eq java.util.UUID.fromString(tenantId.toString()))
        }) {
            it[DocumentsTable.entityType] = entityType
            it[DocumentsTable.entityId] = entityId
        } > 0
    }

    /**
     * Unlink a document from its entity.
     * CRITICAL: Must filter by tenantId.
     */
    suspend fun unlinkFromEntity(
        tenantId: TenantId,
        documentId: DocumentId
    ): Boolean = newSuspendedTransaction {
        DocumentsTable.update({
            (DocumentsTable.id eq java.util.UUID.fromString(documentId.toString())) and
            (DocumentsTable.tenantId eq java.util.UUID.fromString(tenantId.toString()))
        }) {
            it[DocumentsTable.entityType] = null
            it[DocumentsTable.entityId] = null
        } > 0
    }

    /**
     * Delete a document.
     * CRITICAL: Must filter by tenantId.
     * Note: The actual file in MinIO should be deleted separately.
     */
    suspend fun delete(tenantId: TenantId, documentId: DocumentId): Boolean =
        newSuspendedTransaction {
            DocumentsTable.deleteWhere {
                (DocumentsTable.id eq java.util.UUID.fromString(documentId.toString())) and
                (DocumentsTable.tenantId eq java.util.UUID.fromString(tenantId.toString()))
            } > 0
        }

    /**
     * Get all unlinked documents for a tenant (for cleanup purposes).
     */
    suspend fun getUnlinkedDocuments(tenantId: TenantId): List<DocumentDto> =
        newSuspendedTransaction {
            DocumentsTable.selectAll()
                .where {
                    (DocumentsTable.tenantId eq java.util.UUID.fromString(tenantId.toString())) and
                    (DocumentsTable.entityType.isNull())
                }
                .map { it.toDocumentDto() }
        }

    private fun ResultRow.toDocumentDto(): DocumentDto {
        return DocumentDto(
            id = DocumentId.parse(this[DocumentsTable.id].toString()),
            tenantId = TenantId(this[DocumentsTable.tenantId].toKotlinUuid()),
            filename = this[DocumentsTable.filename],
            contentType = this[DocumentsTable.contentType],
            sizeBytes = this[DocumentsTable.sizeBytes],
            storageKey = this[DocumentsTable.storageKey],
            entityType = this[DocumentsTable.entityType],
            entityId = this[DocumentsTable.entityId],
            uploadedAt = this[DocumentsTable.uploadedAt],
            downloadUrl = null // Generated on-demand by the service layer
        )
    }
}
