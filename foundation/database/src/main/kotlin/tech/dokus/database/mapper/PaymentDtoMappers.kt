package tech.dokus.database.mapper

import tech.dokus.database.entity.PaymentEntity
import tech.dokus.domain.model.PaymentDto

fun PaymentDto.Companion.from(entity: PaymentEntity) = PaymentDto(
    id = entity.id,
    tenantId = entity.tenantId,
    invoiceId = entity.invoiceId,
    amount = entity.amount,
    paymentDate = entity.paymentDate,
    paymentMethod = entity.paymentMethod,
    transactionId = entity.transactionId,
    bankTransactionId = entity.bankTransactionId,
    source = entity.source,
    createdBy = entity.createdBy,
    notes = entity.notes,
    reversedAt = entity.reversedAt,
    reversedByUserId = entity.reversedByUserId,
    reversalReason = entity.reversalReason,
    createdAt = entity.createdAt,
)
