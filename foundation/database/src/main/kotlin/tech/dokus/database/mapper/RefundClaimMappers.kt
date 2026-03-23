package tech.dokus.database.mapper

import org.jetbrains.exposed.v1.core.ResultRow
import tech.dokus.database.entity.RefundClaimEntity
import tech.dokus.database.tables.cashflow.RefundClaimsTable
import tech.dokus.domain.Money
import tech.dokus.domain.fromDbDecimal
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.CreditNoteId
import tech.dokus.domain.ids.RefundClaimId
import tech.dokus.domain.ids.TenantId

fun RefundClaimEntity.Companion.from(row: ResultRow): RefundClaimEntity {
    val currency = row[RefundClaimsTable.currency]
    return RefundClaimEntity(
    id = RefundClaimId.parse(row[RefundClaimsTable.id].value.toString()),
    tenantId = TenantId.parse(row[RefundClaimsTable.tenantId].toString()),
    creditNoteId = CreditNoteId.parse(row[RefundClaimsTable.creditNoteId].toString()),
    counterpartyId = ContactId.parse(row[RefundClaimsTable.counterpartyId].toString()),
    amount = Money.fromDbDecimal(row[RefundClaimsTable.amount], currency),
    currency = currency,
    expectedDate = row[RefundClaimsTable.expectedDate],
    status = row[RefundClaimsTable.status],
    settledAt = row[RefundClaimsTable.settledAt],
    cashflowEntryId = row[RefundClaimsTable.cashflowEntryId]?.let {
        CashflowEntryId.parse(it.toString())
    },
    createdAt = row[RefundClaimsTable.createdAt],
    updatedAt = row[RefundClaimsTable.updatedAt],
)
}
