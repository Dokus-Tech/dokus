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

object InvoiceDraftsTable : UUIDTable("invoice_drafts") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val contactId = uuid("contact_id").nullable()
    val invoiceNumber = varchar("invoice_number", 50).nullable()
    val issueDate = date("issue_date").nullable()
    val dueDate = date("due_date").nullable()
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val currency = dbEnumeration<Currency>("currency").default(Currency.Eur)
    val subtotalAmount = decimal("subtotal_amount", 12, 2).nullable()
    val vatAmount = decimal("vat_amount", 12, 2).nullable()
    val totalAmount = decimal("total_amount", 12, 2).nullable()
    val notes = text("notes").nullable()
    val senderIban = varchar("sender_iban", 34).nullable()
    val structuredCommunication = varchar("structured_communication", 32).nullable()
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_invoice_drafts_document", tenantId, documentId)
    }
}
