package ai.dokus.foundation.database.tables

import ai.dokus.foundation.database.*
import ai.dokus.foundation.domain.enums.EntityType
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDateTime
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

/**
 * File uploads metadata
 * Track receipts, invoices, documents
 */
object AttachmentsTable : UUIDTable("attachments") {
    val tenantId = reference("tenant_id", TenantsTable, onDelete = ReferenceOption.CASCADE)

    // Entity linking
    val entityType = dbEnumeration<EntityType>("entity_type")
    val entityId = uuid("entity_id")

    // File metadata
    val filename = varchar("filename", 255)
    val mimeType = varchar("mime_type", 100)
    val sizeBytes = long("size_bytes")

    // Storage (S3/MinIO/etc)
    val s3Key = varchar("s3_key", 500)
    val s3Bucket = varchar("s3_bucket", 255)

    val uploadedAt = datetime("uploaded_at").defaultExpression(CurrentDateTime)

    init {
        index(false, tenantId)
        index(false, entityType, entityId)
    }
}