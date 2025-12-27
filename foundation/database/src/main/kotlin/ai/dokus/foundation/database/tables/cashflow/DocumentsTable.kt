package ai.dokus.foundation.database.tables.cashflow

import ai.dokus.foundation.database.tables.auth.TenantTable
import ai.dokus.foundation.domain.enums.EntityType
import tech.dokus.foundation.ktor.database.dbEnumeration
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime

/**
 * Documents table - stores metadata for files uploaded to object storage (MinIO).
 *
 * Documents have their own lifecycle:
 * 1. Created on upload (entityType/entityId null)
 * 2. Optionally linked to an entity (Invoice, Bill, Expense)
 *
 * The actual file content is stored in MinIO, referenced by storageKey.
 * Download URLs are generated on-demand (presigned URLs expire).
 *
 * OWNER: cashflow service
 * ACCESS: processor service (read-only)
 * CRITICAL: All queries MUST filter by tenant_id for tenant isolation.
 */
object DocumentsTable : UUIDTable("documents") {
    // Multi-tenancy (CRITICAL)
    val tenantId = uuid("tenant_id").references(
        TenantTable.id,
        onDelete = ReferenceOption.CASCADE
    ).index()

    // File metadata
    val filename = varchar("filename", 255)
    val contentType = varchar("content_type", 100)
    val sizeBytes = long("size_bytes")

    // Storage reference (MinIO key)
    val storageKey = varchar("storage_key", 500).index()

    // Optional link to entity (null until attached)
    val entityType = dbEnumeration<EntityType>("entity_type").nullable()
    val entityId = varchar("entity_id", 36).nullable()

    // Timestamps
    val uploadedAt = datetime("uploaded_at").defaultExpression(CurrentDateTime)

    init {
        // Index for fetching documents by entity
        index(false, tenantId, entityType, entityId)

        uniqueIndex(tenantId, storageKey)
    }
}
