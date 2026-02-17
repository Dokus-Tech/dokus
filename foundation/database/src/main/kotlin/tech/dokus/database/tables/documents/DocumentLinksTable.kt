package tech.dokus.database.tables.documents

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.domain.enums.DocumentLinkType
import tech.dokus.foundation.backend.database.dbEnumeration

private const val ExternalReferenceMaxLength = 255

/**
 * Document links table - tracks document-to-document relationships.
 *
 * Use cases:
 * - ProForma "Convert to Invoice" → creates link with linkType=ConvertedTo
 * - CreditNote original reference → creates link with linkType=OriginalDocument
 *
 * Constraints:
 * - ConvertedTo MUST have targetDocumentId (not null)
 * - OriginalDocument may have EITHER targetDocumentId OR externalReference
 *
 * No source/target type enums needed - they are derivable from the linked documents.
 *
 * OWNER: documents service
 * CRITICAL: All queries MUST filter by tenant_id for tenant isolation.
 */
object DocumentLinksTable : UuidTable("document_links") {
    // Multi-tenancy (CRITICAL)
    val tenantId = uuid("tenant_id")
        .references(TenantTable.id, onDelete = ReferenceOption.CASCADE)
        .index()

    // Source document (the one that links TO something)
    val sourceDocumentId = uuid("source_document_id")
        .references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
        .index()

    // Target document (the one being linked TO) - nullable for external refs
    val targetDocumentId = uuid("target_document_id")
        .references(DocumentsTable.id, onDelete = ReferenceOption.SET_NULL)
        .nullable()
        .index()

    // External reference (for original documents outside system)
    // Used when CreditNote references an invoice not in our system
    val externalReference = varchar("external_reference", ExternalReferenceMaxLength).nullable()

    // Link type
    val linkType = dbEnumeration<DocumentLinkType>("link_type")

    // Timestamps
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    init {
        // Ensure unique links: one source can link to one target with one type
        uniqueIndex("uq_document_links", tenantId, sourceDocumentId, targetDocumentId, linkType)
    }
}
