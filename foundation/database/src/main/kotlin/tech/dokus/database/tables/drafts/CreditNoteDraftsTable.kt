package tech.dokus.database.tables.drafts

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.date
import org.jetbrains.exposed.v1.datetime.datetime
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.database.tables.documents.DocumentsTable
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.foundation.backend.database.dbEnumeration

object CreditNoteDraftsTable : UUIDTable("credit_note_drafts") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val contactId = uuid("contact_id").nullable()
    val creditNoteNumber = varchar("credit_note_number", 50).nullable()
    val issueDate = date("issue_date").nullable()
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val currency = dbEnumeration<Currency>("currency").default(Currency.Eur)
    val subtotalAmount = decimal("subtotal_amount", 12, 2).nullable()
    val vatAmount = decimal("vat_amount", 12, 2).nullable()
    val totalAmount = decimal("total_amount", 12, 2).nullable()
    val originalInvoiceNumber = varchar("original_invoice_number", 50).nullable()
    val reason = text("reason").nullable()
    val notes = text("notes").nullable()
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_credit_note_drafts_document", tenantId, documentId)
    }
}

object CreditNoteDraftItemsTable : UUIDTable("credit_note_draft_items") {
    val draftId = uuid("draft_id").references(CreditNoteDraftsTable.id, onDelete = ReferenceOption.CASCADE).index()
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
