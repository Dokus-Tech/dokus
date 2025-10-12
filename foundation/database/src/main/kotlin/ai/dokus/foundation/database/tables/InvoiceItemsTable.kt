package ai.dokus.foundation.database.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDateTime
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

/**
 * Line items on an invoice
 * Individual products/services with pricing and VAT
 */
object InvoiceItemsTable : UUIDTable("invoice_items") {
    val invoiceId = reference("invoice_id", InvoicesTable, onDelete = ReferenceOption.CASCADE)

    val description = text("description")
    val quantity = decimal("quantity", 10, 2)
    val unitPrice = decimal("unit_price", 12, 2)
    val vatRate = decimal("vat_rate", 5, 2) // e.g., 21.00 for 21%

    // Calculated fields
    val lineTotal = decimal("line_total", 12, 2)    // quantity × unitPrice
    val vatAmount = decimal("vat_amount", 12, 2)    // lineTotal × (vatRate/100)

    val sortOrder = integer("sort_order").default(0)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    init {
        index(false, invoiceId)
    }
}