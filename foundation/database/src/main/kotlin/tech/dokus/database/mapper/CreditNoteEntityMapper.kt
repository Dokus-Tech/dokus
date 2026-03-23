package tech.dokus.database.mapper

import org.jetbrains.exposed.v1.core.ResultRow
import tech.dokus.database.entity.CreditNoteEntity
import tech.dokus.database.tables.cashflow.CreditNotesTable
import tech.dokus.domain.Money
import tech.dokus.domain.fromDbDecimal
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.CreditNoteId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId

fun CreditNoteEntity.Companion.from(row: ResultRow): CreditNoteEntity {
    val currency = row[CreditNotesTable.currency]
    return CreditNoteEntity(
    id = CreditNoteId.parse(row[CreditNotesTable.id].value.toString()),
    tenantId = TenantId.parse(row[CreditNotesTable.tenantId].toString()),
    contactId = ContactId.parse(row[CreditNotesTable.contactId].toString()),
    creditNoteType = row[CreditNotesTable.creditNoteType],
    creditNoteNumber = row[CreditNotesTable.creditNoteNumber],
    issueDate = row[CreditNotesTable.issueDate],
    subtotalAmount = Money.fromDbDecimal(row[CreditNotesTable.subtotalAmount], currency),
    vatAmount = Money.fromDbDecimal(row[CreditNotesTable.vatAmount], currency),
    totalAmount = Money.fromDbDecimal(row[CreditNotesTable.totalAmount], currency),
    status = row[CreditNotesTable.status],
    settlementIntent = row[CreditNotesTable.settlementIntent],
    documentId = row[CreditNotesTable.documentId]?.let { DocumentId.parse(it.toString()) },
    reason = row[CreditNotesTable.reason],
    currency = currency,
    notes = row[CreditNotesTable.notes],
    confirmedAt = row[CreditNotesTable.confirmedAt],
    confirmedBy = row[CreditNotesTable.confirmedBy]?.let { UserId.parse(it.toString()) },
    createdAt = row[CreditNotesTable.createdAt],
    updatedAt = row[CreditNotesTable.updatedAt],
)
}
