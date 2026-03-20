package tech.dokus.database.mapper

import org.jetbrains.exposed.v1.core.ResultRow
import tech.dokus.database.tables.cashflow.CashflowEntriesTable
import tech.dokus.domain.Money
import tech.dokus.domain.fromDbDecimal
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.CashflowContactRef
import tech.dokus.domain.model.CashflowEntry

internal fun ResultRow.toCashflowEntry(
    contactName: String? = null
): CashflowEntry {
    return CashflowEntry(
        id = CashflowEntryId.parse(this[CashflowEntriesTable.id].value.toString()),
        tenantId = TenantId.parse(this[CashflowEntriesTable.tenantId].toString()),
        sourceType = this[CashflowEntriesTable.sourceType],
        sourceId = this[CashflowEntriesTable.sourceId].toString(),
        documentId = this[CashflowEntriesTable.documentId]?.let { DocumentId.parse(it.toString()) },
        direction = this[CashflowEntriesTable.direction],
        eventDate = this[CashflowEntriesTable.eventDate],
        amountGross = Money.fromDbDecimal(this[CashflowEntriesTable.amountGross]),
        amountVat = Money.fromDbDecimal(this[CashflowEntriesTable.amountVat]),
        remainingAmount = Money.fromDbDecimal(this[CashflowEntriesTable.remainingAmount]),
        currency = this[CashflowEntriesTable.currency],
        status = this[CashflowEntriesTable.status],
        paidAt = this[CashflowEntriesTable.paidAt],
        contact = this[CashflowEntriesTable.counterpartyId]?.let { counterpartyId ->
            CashflowContactRef(
                id = ContactId.parse(counterpartyId.toString()),
                name = contactName,
            )
        },
        description = null, // Will be AI-generated in future
        createdAt = this[CashflowEntriesTable.createdAt],
        updatedAt = this[CashflowEntriesTable.updatedAt]
    )
}
