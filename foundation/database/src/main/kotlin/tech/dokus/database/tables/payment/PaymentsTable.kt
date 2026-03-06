package tech.dokus.database.tables.payment

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.date
import org.jetbrains.exposed.v1.datetime.datetime
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.database.tables.cashflow.InvoicesTable
import tech.dokus.database.tables.documents.ImportedBankTransactionsTable
import tech.dokus.domain.enums.PaymentCreatedBy
import tech.dokus.domain.enums.PaymentMethod
import tech.dokus.domain.enums.PaymentSource
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
    val paymentSource = dbEnumeration<PaymentSource>("source").default(PaymentSource.Manual)
    val createdBy = dbEnumeration<PaymentCreatedBy>("created_by").default(PaymentCreatedBy.User)

    val transactionId = varchar("transaction_id", 255).nullable() // External ID
    val bankTransactionId = uuid("bank_transaction_id")
        .references(ImportedBankTransactionsTable.id, onDelete = ReferenceOption.SET_NULL)
        .nullable()
    val notes = text("notes").nullable()
    val reversedAt = datetime("reversed_at").nullable()
    val reversedByUserId = uuid("reversed_by_user_id").nullable()
    val reversalReason = text("reversal_reason").nullable()
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    init {
        index(false, tenantId, paymentDate)
        index(false, tenantId, bankTransactionId)
        // Partial unique indexes for duplicate prevention are defined in V9 migration
    }
}
