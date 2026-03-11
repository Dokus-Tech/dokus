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
 * Documents table - canonical document record.
 *
 * A document represents a financial fact (invoice, receipt, credit note, etc.),
 * not a file. File metadata lives on DocumentSourcesTable → DocumentBlobsTable.
 * The file columns here are vestigial and will be removed when this table
 * merges with DocumentDraftsTable (Stage 4).
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

    // File metadata (vestigial — canonical source is DocumentSourcesTable → DocumentBlobsTable)
    val filename = varchar("filename", FilenameMaxLength)
    val contentType = varchar("content_type", ContentTypeMaxLength)
    val sizeBytes = long("size_bytes")

    // Storage reference (vestigial — resolve via selectPreferredSource → blob)
    val storageKey = varchar("storage_key", StorageKeyMaxLength).index()

    // Canonical content fingerprint for deduplication (SHA-256 hex)
    val canonicalContentHash = varchar("content_hash", ContentHashLength).nullable()

    // Canonical identity fingerprint for legal identity matching (scoped types only).
    val canonicalIdentityKey = varchar("identity_key_hash", ContentHashLength).nullable()

    // Effective origin — highest-trust source channel for this document.
    val effectiveOrigin = dbEnumeration<DocumentSource>("document_source").default(DocumentSource.Upload)

    // Timestamps
    val uploadedAt = datetime("uploaded_at").defaultExpression(CurrentDateTime)

    init {
        // Unique constraint: one storage key per tenant
        uniqueIndex(tenantId, storageKey)
        // Identity key for matching — NOT unique because "DIFFERENT" resolution can
        // create two distinct documents with the same identity hash (same supplier VAT
        // + invoice number but different financial facts).
        index(false, tenantId, canonicalIdentityKey)
    }
}
