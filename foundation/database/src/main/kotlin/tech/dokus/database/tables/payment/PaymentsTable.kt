package tech.dokus.database.tables.payment

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.date
import org.jetbrains.exposed.v1.datetime.datetime
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.database.tables.cashflow.InvoicesTable
import tech.dokus.domain.enums.PaymentMethod
import tech.dokus.foundation.backend.database.dbEnumeration

/**
 * Payment transactions against invoices
 * Track when and how invoices are paid
 */
object PaymentsTable : UUIDTable("payments") {
    val tenantId = uuid("tenant_id").references(
        TenantTable.id,
        onDelete = ReferenceOption.CASCADE
    ).index()
    val invoiceId = uuid("invoice_id").references(
        InvoicesTable.id,
        onDelete = ReferenceOption.CASCADE
    ).index()

    val amount = decimal("amount", 12, 2)
    val paymentDate = date("payment_date").index()

    val paymentMethod = dbEnumeration<PaymentMethod>("payment_method")

    val transactionId = varchar("transaction_id", 255).nullable() // External ID
    val notes = text("notes").nullable()
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    init {
        index(false, tenantId, paymentDate)
        // Prevent duplicate payment records per invoice/transaction
        uniqueIndex(invoiceId, transactionId)
    }
}
