package tech.dokus.database.mapper

import org.jetbrains.exposed.v1.core.ResultRow
import tech.dokus.database.entity.CashflowEntryEntity
import tech.dokus.database.tables.cashflow.CashflowEntriesTable
import tech.dokus.domain.Money
import tech.dokus.domain.fromDbDecimal
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.CashflowContactRefDto
import tech.dokus.domain.model.CashflowEntryDto

internal fun CashflowEntryEntity.Companion.from(
    row: ResultRow,
    contactName: String? = null
): CashflowEntryEntity {
    return CashflowEntryEntity(
        id = CashflowEntryId.parse(row[CashflowEntriesTable.id].value.toString()),
        tenantId = TenantId.parse(row[CashflowEntriesTable.tenantId].toString()),
        sourceType = row[CashflowEntriesTable.sourceType],
        sourceId = row[CashflowEntriesTable.sourceId].toString(),
        documentId = row[CashflowEntriesTable.documentId]?.let { DocumentId.parse(it.toString()) },
        direction = row[CashflowEntriesTable.direction],
        eventDate = row[CashflowEntriesTable.eventDate],
        amountGross = Money.fromDbDecimal(row[CashflowEntriesTable.amountGross]),
        amountVat = Money.fromDbDecimal(row[CashflowEntriesTable.amountVat]),
        remainingAmount = Money.fromDbDecimal(row[CashflowEntriesTable.remainingAmount]),
        currency = row[CashflowEntriesTable.currency],
        status = row[CashflowEntriesTable.status],
        paidAt = row[CashflowEntriesTable.paidAt],
        contact = row[CashflowEntriesTable.counterpartyId]?.let { counterpartyId ->
            CashflowContactRefDto(
                id = ContactId.parse(counterpartyId.toString()),
                name = contactName,
            )
        },
        description = null, // Will be AI-generated in future
        createdAt = row[CashflowEntriesTable.createdAt],
        updatedAt = row[CashflowEntriesTable.updatedAt]
    )
}

fun CashflowEntryDto.Companion.from(entity: CashflowEntryEntity): CashflowEntryDto = CashflowEntryDto(
    id = entity.id,
    tenantId = entity.tenantId,
    sourceType = entity.sourceType,
    sourceId = entity.sourceId,
    documentId = entity.documentId,
    direction = entity.direction,
    eventDate = entity.eventDate,
    amountGross = entity.amountGross,
    amountVat = entity.amountVat,
    remainingAmount = entity.remainingAmount,
    currency = entity.currency,
    status = entity.status,
    paidAt = entity.paidAt,
    contact = entity.contact,
    description = entity.description,
    createdAt = entity.createdAt,
    updatedAt = entity.updatedAt,
)
