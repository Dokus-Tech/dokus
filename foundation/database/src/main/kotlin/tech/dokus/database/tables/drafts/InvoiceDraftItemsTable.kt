package tech.dokus.database.tables.drafts

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime

object InvoiceDraftItemsTable : UUIDTable("invoice_draft_items") {
    val draftId = uuid("draft_id").references(InvoiceDraftsTable.id, onDelete = ReferenceOption.CASCADE).index()
    val description = text("description")
    val quantity = decimal("quantity", 10, 2).nullable()
    val unitPrice = decimal("unit_price", 12, 2).nullable()
    val vatRate = decimal("vat_rate", 5, 4).nullable()
    val lineTotal = decimal("line_total", 12, 2).nullable()
    val vatAmount = decimal("vat_amount", 12, 2).nullable()
    val sortOrder = integer("sort_order").default(0)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)
}
