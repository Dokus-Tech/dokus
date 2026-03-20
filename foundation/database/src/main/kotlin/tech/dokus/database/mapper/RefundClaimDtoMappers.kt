package tech.dokus.database.mapper

import tech.dokus.database.entity.RefundClaimEntity
import tech.dokus.domain.model.RefundClaimDto

fun RefundClaimDto.Companion.from(entity: RefundClaimEntity) = RefundClaimDto(
    id = entity.id,
    tenantId = entity.tenantId,
    creditNoteId = entity.creditNoteId,
    counterpartyId = entity.counterpartyId,
    amount = entity.amount,
    currency = entity.currency,
    expectedDate = entity.expectedDate,
    status = entity.status,
    settledAt = entity.settledAt,
    cashflowEntryId = entity.cashflowEntryId,
    createdAt = entity.createdAt,
    updatedAt = entity.updatedAt,
)
