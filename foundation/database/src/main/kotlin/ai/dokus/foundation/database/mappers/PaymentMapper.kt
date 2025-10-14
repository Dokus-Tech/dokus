package ai.dokus.foundation.database.mappers

import ai.dokus.foundation.database.tables.PaymentsTable
import ai.dokus.foundation.database.utils.toKotlinLocalDate
import ai.dokus.foundation.database.utils.toKotlinLocalDateTime
import ai.dokus.foundation.domain.*
import ai.dokus.foundation.domain.model.Payment
import org.jetbrains.exposed.sql.ResultRow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
object PaymentMapper {

    fun ResultRow.toPayment(): Payment = Payment(
        id = PaymentId(this[PaymentsTable.id].value.toKotlinUuid()),
        tenantId = TenantId(this[PaymentsTable.tenantId].value.toKotlinUuid()),
        invoiceId = InvoiceId(this[PaymentsTable.invoiceId].value.toKotlinUuid()),
        amount = Money(this[PaymentsTable.amount].toString()),
        paymentDate = this[PaymentsTable.paymentDate].toKotlinLocalDate(),
        paymentMethod = this[PaymentsTable.paymentMethod],
        transactionId = this[PaymentsTable.transactionId]?.let { TransactionId(it) },
        notes = this[PaymentsTable.notes],
        createdAt = this[PaymentsTable.createdAt].toKotlinLocalDateTime()
    )
}
