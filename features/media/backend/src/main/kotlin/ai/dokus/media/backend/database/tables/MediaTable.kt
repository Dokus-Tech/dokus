package ai.dokus.media.backend.database.tables

import ai.dokus.foundation.domain.enums.EntityType
import ai.dokus.foundation.domain.enums.MediaStatus
import ai.dokus.foundation.ktor.database.dbEnumeration
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime

/**
 * Media table - stores uploaded documents and their processing state.
 *
 * CRITICAL: Always scope queries by tenantId for tenant isolation.
 */
object MediaTable : UUIDTable("media") {
    val tenantId = uuid("tenant_id")

    // File metadata
    val filename = varchar("filename", 255)
    val mimeType = varchar("mime_type", 150)
    val sizeBytes = long("size_bytes")

    // Processing
    val status = dbEnumeration<MediaStatus>("status")
    val processingSummary = text("processing_summary").nullable()
    val errorMessage = text("error_message").nullable()

    // Storage
    val storageKey = varchar("storage_key", 600)
    val storageBucket = varchar("storage_bucket", 120)
    val extractionJson = text("extraction_json").nullable()

    // Attachment to domain entities
    val attachedEntityType = dbEnumeration<EntityType>("attached_entity_type").nullable()
    val attachedEntityId = varchar("attached_entity_id", 64).nullable()

    // Audit
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        index(false, tenantId)
        index(false, status)
        index(false, tenantId, status)
    }
}
