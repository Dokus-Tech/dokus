package ai.dokus.foundation.database.tables

import ai.dokus.foundation.database.*
import ai.dokus.foundation.domain.enums.PaymentMethod
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDateTime
import org.jetbrains.exposed.sql.kotlin.datetime.date
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

/**
 * Payment transactions against invoices
 * Track when and how invoices are paid
 */
object PaymentsTable : UUIDTable("payments") {
    val tenantId = reference("tenant_id", TenantsTable, onDelete = ReferenceOption.CASCADE)
    val invoiceId = reference("invoice_id", InvoicesTable, onDelete = ReferenceOption.RESTRICT)

    val amount = decimal("amount", 12, 2)
    val paymentDate = date("payment_date")

    val paymentMethod = paymentMethodEnumeration("payment_method")

    val transactionId = varchar("transaction_id", 255).nullable()  // External ID
    val notes = text("notes").nullable()
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    init {
        index(false, tenantId)
        index(false, invoiceId)
        index(false, paymentDate)
        index(false, tenantId, paymentDate)
    }
}