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

fun CreditNoteEntity.Companion.from(row: ResultRow): CreditNoteEntity = CreditNoteEntity(
    id = CreditNoteId.parse(row[CreditNotesTable.id].value.toString()),
    tenantId = TenantId.parse(row[CreditNotesTable.tenantId].toString()),
    contactId = ContactId.parse(row[CreditNotesTable.contactId].toString()),
    creditNoteType = row[CreditNotesTable.creditNoteType],
    creditNoteNumber = row[CreditNotesTable.creditNoteNumber],
    issueDate = row[CreditNotesTable.issueDate],
    subtotalAmount = Money.fromDbDecimal(row[CreditNotesTable.subtotalAmount]),
    vatAmount = Money.fromDbDecimal(row[CreditNotesTable.vatAmount]),
    totalAmount = Money.fromDbDecimal(row[CreditNotesTable.totalAmount]),
    status = row[CreditNotesTable.status],
    settlementIntent = row[CreditNotesTable.settlementIntent],
    documentId = row[CreditNotesTable.documentId]?.let { DocumentId.parse(it.toString()) },
    reason = row[CreditNotesTable.reason],
    currency = row[CreditNotesTable.currency],
    notes = row[CreditNotesTable.notes],
    createdAt = row[CreditNotesTable.createdAt],
    updatedAt = row[CreditNotesTable.updatedAt],
)
