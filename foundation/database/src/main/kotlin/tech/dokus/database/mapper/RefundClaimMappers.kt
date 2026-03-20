package tech.dokus.database.mapper

import org.jetbrains.exposed.v1.core.ResultRow
import tech.dokus.database.tables.cashflow.RefundClaimsTable
import tech.dokus.domain.Money
import tech.dokus.domain.fromDbDecimal
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.CreditNoteId
import tech.dokus.domain.ids.RefundClaimId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.RefundClaimDto

internal fun ResultRow.toRefundClaimDto(): RefundClaimDto {
    return RefundClaimDto(
        id = RefundClaimId.parse(this[RefundClaimsTable.id].value.toString()),
        tenantId = TenantId.parse(this[RefundClaimsTable.tenantId].toString()),
        creditNoteId = CreditNoteId.parse(this[RefundClaimsTable.creditNoteId].toString()),
        counterpartyId = ContactId.parse(this[RefundClaimsTable.counterpartyId].toString()),
        amount = Money.fromDbDecimal(this[RefundClaimsTable.amount]),
        currency = this[RefundClaimsTable.currency],
        expectedDate = this[RefundClaimsTable.expectedDate],
        status = this[RefundClaimsTable.status],
        settledAt = this[RefundClaimsTable.settledAt],
        cashflowEntryId = this[RefundClaimsTable.cashflowEntryId]?.let {
            CashflowEntryId.parse(it.toString())
        },
        createdAt = this[RefundClaimsTable.createdAt],
        updatedAt = this[RefundClaimsTable.updatedAt]
    )
}
