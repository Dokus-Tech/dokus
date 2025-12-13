package ai.dokus.foundation.database.tables.payment

import ai.dokus.foundation.database.tables.auth.TenantTable
import ai.dokus.foundation.database.tables.cashflow.InvoicesTable
import ai.dokus.foundation.domain.enums.PaymentMethod
import ai.dokus.foundation.ktor.database.dbEnumeration
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.date
import org.jetbrains.exposed.v1.datetime.datetime

/**
 * Payment transactions against invoices
 * Track when and how invoices are paid
 */
object PaymentsTable : UUIDTable("payments") {
    val tenantId = uuid("tenant_id").references(
        TenantTable.id,
        onDelete = ReferenceOption.CASCADE
    )
    val invoiceId = uuid("invoice_id").references(
        InvoicesTable.id,
        onDelete = ReferenceOption.CASCADE
    )

    val amount = decimal("amount", 12, 2)
    val paymentDate = date("payment_date")

    val paymentMethod = dbEnumeration<PaymentMethod>("payment_method")

    val transactionId = varchar("transaction_id", 255).nullable()  // External ID
    val notes = text("notes").nullable()
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    init {
        index(false, tenantId)
        index(false, invoiceId)
        index(false, paymentDate)
        index(false, tenantId, paymentDate)
        // Prevent duplicate payment records per invoice/transaction
        uniqueIndex(invoiceId, transactionId)
    }
}
