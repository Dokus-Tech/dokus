package tech.dokus.database.tables.documents

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.DocumentMatchType
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.enums.DocumentSourceStatus
import tech.dokus.domain.enums.DocumentType
import tech.dokus.foundation.backend.database.dbEnumeration

private const val HashLength = 64
private const val VatLength = 32
private const val DocNumberLength = 255
private const val FilenameLength = 255

/**
 * Evidence-level arrivals for canonical documents.
 */
object DocumentSourcesTable : UUIDTable("document_sources") {
    val tenantId = uuid("tenant_id").references(
        TenantTable.id,
        onDelete = ReferenceOption.CASCADE
    )

    val documentId = uuid("document_id").references(
        DocumentsTable.id,
        onDelete = ReferenceOption.CASCADE
    )

    val blobId = uuid("blob_id").references(
        DocumentBlobsTable.id,
        onDelete = ReferenceOption.RESTRICT
    )

    val sourceChannel = dbEnumeration<DocumentSource>("source_channel")
    val arrivalAt = datetime("arrival_at").defaultExpression(CurrentDateTime)

    val contentHash = varchar("content_hash", HashLength).nullable()
    val identityKeyHash = varchar("identity_key_hash", HashLength).nullable()
    val status = dbEnumeration<DocumentSourceStatus>("status").default(DocumentSourceStatus.Linked)
    val matchType = dbEnumeration<DocumentMatchType>("match_type").nullable()
    val isCorrective = bool("is_corrective").default(false)
    val extractedSnapshotJson = text("extracted_snapshot_json").nullable()
    val detachedAt = datetime("detached_at").nullable()

    // Normalized fields used for deterministic and fuzzy identity matching.
    val normalizedSupplierVat = varchar("normalized_supplier_vat", VatLength).nullable()
    val normalizedDocumentNumber = varchar("normalized_document_number", DocNumberLength).nullable()
    val documentType = dbEnumeration<DocumentType>("document_type").nullable()
    val direction = dbEnumeration<DocumentDirection>("direction").nullable()

    // Source-level display metadata.
    val filename = varchar("filename", FilenameLength).nullable()

    init {
        index(false, tenantId, documentId, status)
        index(false, tenantId, contentHash)
        index(false, tenantId, identityKeyHash)
        index(false, tenantId, normalizedSupplierVat, documentType, direction)
    }
}
