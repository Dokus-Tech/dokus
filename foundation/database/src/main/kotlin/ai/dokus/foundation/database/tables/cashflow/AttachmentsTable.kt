package ai.dokus.foundation.database.tables.cashflow

import ai.dokus.foundation.database.tables.auth.TenantTable
import ai.dokus.foundation.domain.enums.EntityType
import ai.dokus.foundation.ktor.database.dbEnumeration
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime

/**
 * Attachments table - stores document/file metadata for invoices and expenses.
 * Files are stored in S3 or local filesystem, this table stores metadata.
 *
 * OWNER: cashflow service
 * CRITICAL: All queries MUST filter by tenant_id
 */
object AttachmentsTable : UUIDTable("attachments") {
    // Multi-tenancy (CRITICAL)
    val tenantId = uuid("organization_id").references(
        TenantTable.id,
        onDelete = ReferenceOption.CASCADE
    ).index()

    // Generic entity reference (can attach to invoices, expenses, etc.)
    val entityType = dbEnumeration<EntityType>("entity_type").index()
    val entityId = varchar("entity_id", 36).index() // UUID as string

    // File metadata
    val filename = varchar("filename", 255)
    val mimeType = varchar("mime_type", 100)
    val sizeBytes = long("size_bytes")

    // Storage location
    val s3Key = varchar("s3_key", 500).index()
    val s3Bucket = varchar("s3_bucket", 100)

    // Timestamp
    val uploadedAt = datetime("uploaded_at").defaultExpression(CurrentDateTime)

    init {
        // Composite index for retrieving all attachments for an entity
        index(false, tenantId, entityType, entityId)
        uniqueIndex(tenantId, s3Key)
    }
}
