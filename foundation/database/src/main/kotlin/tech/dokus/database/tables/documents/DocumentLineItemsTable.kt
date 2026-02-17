package tech.dokus.database.tables.documents

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.domain.enums.Currency
import tech.dokus.foundation.backend.database.dbEnumeration

/**
 * Generic line items table for ALL document types (Invoice, Receipt, ProForma, CreditNote, etc.).
 *
 * This replaces per-type item tables (InvoiceItemsTable, ProFormaItemsTable, etc.) with a single
 * unified table keyed by document_id + tenant_id.
 *
 * Line Item Persistence Rules:
 * - During AI extraction: Persist extracted items if present
 * - If NO items extracted: Do NOT create synthetic rows during ingestion
 * - Synthetic row creation: Only at confirmation time (when totals are final)
 *   OR render virtually in UI until confirmation
 * - This avoids stale duplicates from reprocessing
 *
 * OWNER: documents service
 * CRITICAL: All queries MUST filter by tenant_id for tenant isolation.
 */
object DocumentLineItemsTable : UuidTable("document_line_items") {
    // Multi-tenancy (CRITICAL)
    val tenantId = uuid("tenant_id")
        .references(TenantTable.id, onDelete = ReferenceOption.CASCADE)
        .index()

    // Parent document (CASCADE delete)
    val documentId = uuid("document_id")
        .references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
        .index()

    // Ordering - unique per document
    val position = integer("position")

    // Line item fields
    val description = text("description")
    val quantity = decimal("quantity", 10, 2).nullable()
    val unitPrice = decimal("unit_price", 12, 2).nullable()

    // Amounts - net and gross are required for stable money model
    val netAmount = decimal("net_amount", 12, 2)  // NOT NULL
    val vatRate = decimal("vat_rate", 5, 4).nullable() // e.g., 0.2100 for 21%
    val vatAmount = decimal("vat_amount", 12, 2).nullable()
    val grossAmount = decimal("gross_amount", 12, 2)  // NOT NULL

    // Currency
    val currency = dbEnumeration<Currency>("currency")

    // Is this a synthetic item created from totals at confirmation?
    val isSynthetic = bool("is_synthetic").default(false)

    // Timestamps
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        // Unique constraint: one position per document per tenant
        uniqueIndex("uq_document_line_items_position", tenantId, documentId, position)
    }
}
