package tech.dokus.database.tables.cashflow

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime
import tech.dokus.database.tables.auth.TenantTable

/**
 * Documents table - stores metadata for files uploaded to object storage (MinIO).
 *
 * Documents are pure file metadata. Entity linkage is handled by the financial
 * entity tables (Invoice, Bill, Expense) which have a documentId FK.
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

    // Timestamps
    val uploadedAt = datetime("uploaded_at").defaultExpression(CurrentDateTime)

    init {
        // Unique constraint: one storage key per tenant
        uniqueIndex(tenantId, storageKey)
    }
}
