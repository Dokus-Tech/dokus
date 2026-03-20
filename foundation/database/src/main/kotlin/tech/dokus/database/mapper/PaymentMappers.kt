package tech.dokus.database.mapper

import org.jetbrains.exposed.v1.core.ResultRow
import tech.dokus.database.tables.payment.PaymentsTable
import tech.dokus.domain.Money
import tech.dokus.domain.fromDbDecimal
import tech.dokus.domain.ids.BankTransactionId
import tech.dokus.domain.ids.InvoiceId
import tech.dokus.domain.ids.PaymentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.TransactionId
import tech.dokus.domain.model.PaymentDto

internal fun ResultRow.toPaymentDto(): PaymentDto {
    return PaymentDto(
        id = PaymentId.parse(this[PaymentsTable.id].value.toString()),
        tenantId = TenantId.parse(this[PaymentsTable.tenantId].toString()),
        invoiceId = InvoiceId.parse(this[PaymentsTable.invoiceId].toString()),
        amount = Money.fromDbDecimal(this[PaymentsTable.amount]),
        paymentDate = this[PaymentsTable.paymentDate],
        paymentMethod = this[PaymentsTable.paymentMethod],
        transactionId = this[PaymentsTable.transactionId]?.let { TransactionId(it) },
        bankTransactionId = this[PaymentsTable.bankTransactionId]?.let { BankTransactionId.parse(it.toString()) },
        source = this[PaymentsTable.paymentSource],
        createdBy = this[PaymentsTable.createdBy],
        notes = this[PaymentsTable.notes],
        reversedAt = this[PaymentsTable.reversedAt],
        reversedByUserId = this[PaymentsTable.reversedByUserId]?.let { tech.dokus.domain.ids.UserId(it.toString()) },
        reversalReason = this[PaymentsTable.reversalReason],
        createdAt = this[PaymentsTable.createdAt]
    )
}
