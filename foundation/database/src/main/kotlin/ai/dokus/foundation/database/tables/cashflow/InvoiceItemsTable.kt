package ai.dokus.foundation.database.tables.cashflow

import org.jetbrains.exposed.v1.core.dao.id.UUIDTable

/**
 * Invoice items table - line items for each invoice.
 * One invoice can have multiple items.
 *
 * OWNER: cashflow service
 */
object InvoiceItemsTable : UUIDTable("invoice_items") {
    // Foreign key to invoice
    val invoiceId = uuid("invoice_id").references(InvoicesTable.id).index()

    // Item details
    val description = text("description")
    val quantity = decimal("quantity", 10, 2)
    val unitPrice = decimal("unit_price", 12, 2)
    val vatRate = decimal("vat_rate", 5, 4) // e.g., 0.2100 for 21%

    // Calculated amounts (stored for audit trail)
    val lineTotal = decimal("line_total", 12, 2)
    val vatAmount = decimal("vat_amount", 12, 2)

    // Ordering
    val sortOrder = integer("sort_order").default(0)

    init {}
}
