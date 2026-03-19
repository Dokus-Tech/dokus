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
import tech.dokus.domain.enums.PaymentMethod
import tech.dokus.foundation.backend.database.dbEnumeration

object ReceiptDraftsTable : UUIDTable("receipt_drafts") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val merchantName = varchar("merchant_name", 255).nullable()
    val merchantVat = varchar("merchant_vat", 30).nullable()
    val date = date("date").nullable()
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Inbound)
    val currency = dbEnumeration<Currency>("currency").default(Currency.Eur)
    val totalAmount = decimal("total_amount", 19, 4).nullable()
    val vatAmount = decimal("vat_amount", 19, 4).nullable()
    val receiptNumber = varchar("receipt_number", 50).nullable()
    val paymentMethod = dbEnumeration<PaymentMethod>("payment_method").nullable()
    val notes = text("notes").nullable()
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_receipt_drafts_document", tenantId, documentId)
    }
}
