package tech.dokus.database.mapper

import org.jetbrains.exposed.v1.core.ResultRow
import tech.dokus.database.entity.PaymentEntity
import tech.dokus.database.tables.payment.PaymentsTable
import tech.dokus.domain.Money
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.fromDbDecimal
import tech.dokus.domain.ids.BankTransactionId
import tech.dokus.domain.ids.InvoiceId
import tech.dokus.domain.ids.PaymentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.TransactionId
import tech.dokus.domain.ids.UserId

fun PaymentEntity.Companion.from(row: ResultRow): PaymentEntity = PaymentEntity(
    id = PaymentId.parse(row[PaymentsTable.id].value.toString()),
    tenantId = TenantId.parse(row[PaymentsTable.tenantId].toString()),
    invoiceId = InvoiceId.parse(row[PaymentsTable.invoiceId].toString()),
    amount = Money.fromDbDecimal(row[PaymentsTable.amount], Currency.Eur),
    paymentDate = row[PaymentsTable.paymentDate],
    paymentMethod = row[PaymentsTable.paymentMethod],
    transactionId = row[PaymentsTable.transactionId]?.let { TransactionId(it) },
    bankTransactionId = row[PaymentsTable.bankTransactionId]?.let { BankTransactionId.parse(it.toString()) },
    source = row[PaymentsTable.paymentSource],
    createdBy = row[PaymentsTable.createdBy],
    notes = row[PaymentsTable.notes],
    reversedAt = row[PaymentsTable.reversedAt],
    reversedByUserId = row[PaymentsTable.reversedByUserId]?.let { UserId(it.toString()) },
    reversalReason = row[PaymentsTable.reversalReason],
    createdAt = row[PaymentsTable.createdAt],
)
