package tech.dokus.database.entity

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import tech.dokus.domain.Money
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.enums.RefundClaimStatus
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.CreditNoteId
import tech.dokus.domain.ids.RefundClaimId
import tech.dokus.domain.ids.TenantId

data class RefundClaimEntity(
    val id: RefundClaimId,
    val tenantId: TenantId,
    val creditNoteId: CreditNoteId,
    val counterpartyId: ContactId,
    val amount: Money,
    val currency: Currency,
    val expectedDate: LocalDate? = null,
    val status: RefundClaimStatus,
    val settledAt: LocalDateTime? = null,
    val cashflowEntryId: CashflowEntryId? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object
}
