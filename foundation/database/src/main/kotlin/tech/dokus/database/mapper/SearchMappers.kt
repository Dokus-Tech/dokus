package tech.dokus.database.mapper

import org.jetbrains.exposed.v1.core.ResultRow
import tech.dokus.database.tables.cashflow.CashflowEntriesTable
import tech.dokus.database.tables.cashflow.ExpensesTable
import tech.dokus.database.tables.cashflow.InvoicesTable
import tech.dokus.database.tables.contacts.ContactsTable
import tech.dokus.database.tables.documents.DocumentsTable
import tech.dokus.domain.Money
import tech.dokus.domain.enums.CashflowDirection
import tech.dokus.domain.fromDbDecimal
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.SearchContactHitEntity
import tech.dokus.domain.model.SearchDocumentHitEntity
import tech.dokus.domain.model.SearchTransactionHitEntity
import tech.dokus.domain.model.contact.CounterpartySnapshot
import tech.dokus.domain.utils.json

internal fun SearchDocumentHitEntity.Companion.from(row: ResultRow): SearchDocumentHitEntity {
    val contactName = row.getOrNull(ContactsTable.name)
    val contactVat = row.getOrNull(ContactsTable.vatNumber)
    val snapshot = if (contactName == null || contactVat == null) {
        row.getOrNull(DocumentsTable.counterpartySnapshot)
            ?.let { runCatching { json.decodeFromString<CounterpartySnapshot>(it) }.getOrNull() }
    } else null

    return SearchDocumentHitEntity(
        documentId = DocumentId.parse(row[DocumentsTable.id].value.toString()),
        filename = row[DocumentsTable.purposeRendered] ?: "",
        documentType = row[DocumentsTable.documentType],
        status = row[DocumentsTable.documentStatus],
        counterpartyName = contactName ?: snapshot?.name,
        counterpartyVat = contactVat ?: snapshot?.vatNumber?.value,
    )
}

internal fun SearchContactHitEntity.Companion.from(row: ResultRow): SearchContactHitEntity = SearchContactHitEntity(
    contactId = ContactId.parse(row[ContactsTable.id].value.toString()),
    name = row[ContactsTable.name],
    email = row[ContactsTable.email],
    vatNumber = row[ContactsTable.vatNumber],
    companyNumber = row[ContactsTable.companyNumber],
    isActive = row[ContactsTable.isActive],
)

internal fun SearchTransactionHitEntity.Companion.from(row: ResultRow): SearchTransactionHitEntity {
    val direction = row[CashflowEntriesTable.direction]
    val absoluteAmount = Money.fromDbDecimal(row[CashflowEntriesTable.amountGross])
    val signedAmount = if (direction == CashflowDirection.Out) -absoluteAmount else absoluteAmount
    val contactName = row.getOrNull(ContactsTable.name)
    val filename = row.getOrNull(DocumentsTable.purposeRendered)
    val expenseDescription = row.getOrNull(ExpensesTable.description)
    val invoiceNumber = row.getOrNull(InvoicesTable.invoiceNumber)
    val displayText = when {
        !filename.isNullOrBlank() -> filename
        !expenseDescription.isNullOrBlank() -> expenseDescription
        !invoiceNumber.isNullOrBlank() -> invoiceNumber
        !contactName.isNullOrBlank() -> contactName
        else -> row[CashflowEntriesTable.sourceType].name
    }

    return SearchTransactionHitEntity(
        entryId = CashflowEntryId.parse(row[CashflowEntriesTable.id].value.toString()),
        displayText = displayText,
        status = row[CashflowEntriesTable.status],
        date = row[CashflowEntriesTable.eventDate],
        amount = signedAmount,
        direction = direction,
        contactName = contactName,
        documentFilename = filename,
        documentId = row.getOrNull(DocumentsTable.id)?.let { DocumentId.parse(it.value.toString()) },
    )
}
