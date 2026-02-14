package tech.dokus.database.tables.documents

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.foundation.backend.database.dbEnumeration

private const val FilenameMaxLength = 255
private const val ContentTypeMaxLength = 100
private const val StorageKeyMaxLength = 500
private const val ContentHashLength = 64

/**
 * Documents table - stores metadata for files uploaded to object storage (MinIO).
 *
 * Documents are pure file metadata. Entity linkage is handled by the financial
 * entity tables (Invoice, Expense) which have a documentId FK.
 *
 * The actual file content is stored in MinIO, referenced by storageKey.
 * Download URLs are generated on-demand (presigned URLs expire).
 *
 * OWNER: documents service
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
    val filename = varchar("filename", FilenameMaxLength)
    val contentType = varchar("content_type", ContentTypeMaxLength)
    val sizeBytes = long("size_bytes")

    // Storage reference (MinIO key)
    val storageKey = varchar("storage_key", StorageKeyMaxLength).index()

    // Content fingerprint for deduplication (SHA-256 hex)
    val contentHash = varchar("content_hash", ContentHashLength).nullable()

    // Canonical identity fingerprint for legal identity matching (scoped types only).
    val identityKeyHash = varchar("identity_key_hash", ContentHashLength).nullable()

    // Document source (where it came from)
    val documentSource = dbEnumeration<DocumentSource>("document_source").default(DocumentSource.Upload)

    // Timestamps
    val uploadedAt = datetime("uploaded_at").defaultExpression(CurrentDateTime)

    init {
        // Unique constraint: one storage key per tenant
        uniqueIndex(tenantId, storageKey)
        // Canonical identity uniqueness (null when identity is not deterministic).
        uniqueIndex(tenantId, identityKeyHash)
    }
}
