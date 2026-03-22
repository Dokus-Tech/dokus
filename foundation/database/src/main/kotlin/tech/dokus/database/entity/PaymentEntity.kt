package tech.dokus.database.entity

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import tech.dokus.domain.Money
import tech.dokus.domain.enums.PaymentCreatedBy
import tech.dokus.domain.enums.PaymentMethod
import tech.dokus.domain.enums.PaymentSource
import tech.dokus.domain.ids.BankTransactionId
import tech.dokus.domain.ids.InvoiceId
import tech.dokus.domain.ids.PaymentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.TransactionId
import tech.dokus.domain.ids.UserId

data class PaymentEntity(
    val id: PaymentId,
    val tenantId: TenantId,
    val invoiceId: InvoiceId,
    val amount: Money,
    val paymentDate: LocalDate,
    val paymentMethod: PaymentMethod,
    val transactionId: TransactionId? = null,
    val bankTransactionId: BankTransactionId? = null,
    val source: PaymentSource = PaymentSource.Manual,
    val createdBy: PaymentCreatedBy = PaymentCreatedBy.User,
    val notes: String? = null,
    val reversedAt: LocalDateTime? = null,
    val reversedByUserId: UserId? = null,
    val reversalReason: String? = null,
    val createdAt: LocalDateTime,
) {
    companion object
}
