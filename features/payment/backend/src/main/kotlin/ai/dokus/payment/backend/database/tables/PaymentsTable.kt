package ai.dokus.payment.backend.database.tables

import ai.dokus.foundation.domain.database.dbEnumeration
import ai.dokus.foundation.domain.enums.PaymentMethod
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.date
import org.jetbrains.exposed.v1.datetime.datetime
import java.util.UUID as JavaUUID

/**
 * Payment transactions against invoices
 * Track when and how invoices are paid
 */
object PaymentsTable : UUIDTable("payments") {
    val tenantId = uuid("tenant_id")
    val invoiceId = uuid("invoice_id")

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
    }
}
