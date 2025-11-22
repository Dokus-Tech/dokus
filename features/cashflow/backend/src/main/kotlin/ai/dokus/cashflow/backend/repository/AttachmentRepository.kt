package ai.dokus.cashflow.backend.repository

import ai.dokus.cashflow.backend.database.tables.AttachmentsTable
import ai.dokus.foundation.domain.ids.AttachmentId
import ai.dokus.foundation.domain.ids.OrganizationId
import ai.dokus.foundation.domain.enums.EntityType
import ai.dokus.foundation.domain.model.Attachment
import ai.dokus.foundation.ktor.database.dbQuery
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import java.util.UUID

/**
 * Repository for managing document attachments
 *
 * CRITICAL SECURITY RULES:
 * 1. ALWAYS filter by tenant_id in every query
 * 2. NEVER return attachments from different tenants
 * 3. All operations must be tenant-isolated
 */
class AttachmentRepository {

    /**
     * Upload a new attachment
     * CRITICAL: MUST include tenant_id for multi-tenancy security
     */
    suspend fun uploadAttachment(
        organizationId: OrganizationId,
        entityType: EntityType,
        entityId: String,
        filename: String,
        mimeType: String,
        sizeBytes: Long,
        s3Key: String,
        s3Bucket: String
    ): Result<AttachmentId> = runCatching {
        dbQuery {
            val attachmentId = AttachmentsTable.insertAndGetId {
                it[AttachmentsTable.organizationId] = UUID.fromString(organizationId.toString())
                it[AttachmentsTable.entityType] = entityType
                it[AttachmentsTable.entityId] = entityId
                it[AttachmentsTable.filename] = filename
                it[AttachmentsTable.mimeType] = mimeType
                it[AttachmentsTable.sizeBytes] = sizeBytes
                it[AttachmentsTable.s3Key] = s3Key
                it[AttachmentsTable.s3Bucket] = s3Bucket
            }

            AttachmentId.parse(attachmentId.value.toString())
        }
    }

    /**
     * Get all attachments for a specific entity
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun getAttachments(
        organizationId: OrganizationId,
        entityType: EntityType,
        entityId: String
    ): Result<List<Attachment>> = runCatching {
        dbQuery {
            AttachmentsTable.selectAll().where {
                (AttachmentsTable.organizationId eq UUID.fromString(organizationId.toString())) and
                (AttachmentsTable.entityType eq entityType) and
                (AttachmentsTable.entityId eq entityId)
            }.map { row ->
                Attachment(
                    id = AttachmentId.parse(row[AttachmentsTable.id].value.toString()),
                    organizationId = OrganizationId.parse(row[AttachmentsTable.organizationId].toString()),
                    entityType = row[AttachmentsTable.entityType],
                    entityId = row[AttachmentsTable.entityId],
                    filename = row[AttachmentsTable.filename],
                    mimeType = row[AttachmentsTable.mimeType],
                    sizeBytes = row[AttachmentsTable.sizeBytes],
                    s3Key = row[AttachmentsTable.s3Key],
                    s3Bucket = row[AttachmentsTable.s3Bucket],
                    uploadedAt = row[AttachmentsTable.uploadedAt]
                )
            }
        }
    }

    /**
     * Get a single attachment by ID
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun getAttachment(
        attachmentId: AttachmentId,
        organizationId: OrganizationId
    ): Result<Attachment?> = runCatching {
        dbQuery {
            AttachmentsTable.selectAll().where {
                (AttachmentsTable.id eq UUID.fromString(attachmentId.toString())) and
                (AttachmentsTable.organizationId eq UUID.fromString(organizationId.toString()))
            }.singleOrNull()?.let { row ->
                Attachment(
                    id = AttachmentId.parse(row[AttachmentsTable.id].value.toString()),
                    organizationId = OrganizationId.parse(row[AttachmentsTable.organizationId].toString()),
                    entityType = row[AttachmentsTable.entityType],
                    entityId = row[AttachmentsTable.entityId],
                    filename = row[AttachmentsTable.filename],
                    mimeType = row[AttachmentsTable.mimeType],
                    sizeBytes = row[AttachmentsTable.sizeBytes],
                    s3Key = row[AttachmentsTable.s3Key],
                    s3Bucket = row[AttachmentsTable.s3Bucket],
                    uploadedAt = row[AttachmentsTable.uploadedAt]
                )
            }
        }
    }

    /**
     * Delete an attachment
     * CRITICAL: MUST filter by tenant_id to prevent deletion of other tenants' attachments
     */
    suspend fun deleteAttachment(
        attachmentId: AttachmentId,
        organizationId: OrganizationId
    ): Result<Boolean> = runCatching {
        dbQuery {
            val deletedRows = AttachmentsTable.deleteWhere {
                (AttachmentsTable.id eq UUID.fromString(attachmentId.toString())) and
                (AttachmentsTable.organizationId eq UUID.fromString(organizationId.toString()))
            }
            deletedRows > 0
        }
    }

    /**
     * Delete all attachments for a specific entity
     * CRITICAL: MUST filter by tenant_id
     * Use case: When deleting an invoice/expense, also delete its attachments
     */
    suspend fun deleteAttachmentsForEntity(
        organizationId: OrganizationId,
        entityType: EntityType,
        entityId: String
    ): Result<Int> = runCatching {
        dbQuery {
            AttachmentsTable.deleteWhere {
                (AttachmentsTable.organizationId eq UUID.fromString(organizationId.toString())) and
                (AttachmentsTable.entityType eq entityType) and
                (AttachmentsTable.entityId eq entityId)
            }
        }
    }

    /**
     * Check if an attachment exists and belongs to the tenant
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun exists(
        attachmentId: AttachmentId,
        organizationId: OrganizationId
    ): Result<Boolean> = runCatching {
        dbQuery {
            AttachmentsTable.selectAll().where {
                (AttachmentsTable.id eq UUID.fromString(attachmentId.toString())) and
                (AttachmentsTable.organizationId eq UUID.fromString(organizationId.toString()))
            }.count() > 0
        }
    }
}
