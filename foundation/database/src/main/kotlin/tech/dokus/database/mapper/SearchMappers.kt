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
import tech.dokus.domain.model.SearchContactHit
import tech.dokus.domain.model.SearchDocumentHit
import tech.dokus.domain.model.SearchTransactionHit
import tech.dokus.domain.model.contact.CounterpartySnapshot
import tech.dokus.domain.utils.json

internal fun ResultRow.toSearchDocumentHit(): SearchDocumentHit {
    val contactName = this.getOrNull(ContactsTable.name)
    val contactVat = this.getOrNull(ContactsTable.vatNumber)
    val snapshot = if (contactName == null || contactVat == null) {
        this.getOrNull(DocumentsTable.counterpartySnapshot)
            ?.let { runCatching { json.decodeFromString<CounterpartySnapshot>(it) }.getOrNull() }
    } else null

    return SearchDocumentHit(
        documentId = DocumentId.parse(this[DocumentsTable.id].value.toString()),
        filename = this[DocumentsTable.purposeRendered] ?: "",
        documentType = this[DocumentsTable.documentType],
        status = this[DocumentsTable.documentStatus],
        counterpartyName = contactName ?: snapshot?.name,
        counterpartyVat = contactVat ?: snapshot?.vatNumber?.value,
    )
}

internal fun ResultRow.toSearchContactHit(): SearchContactHit = SearchContactHit(
    contactId = ContactId.parse(this[ContactsTable.id].value.toString()),
    name = this[ContactsTable.name],
    email = this[ContactsTable.email],
    vatNumber = this[ContactsTable.vatNumber],
    companyNumber = this[ContactsTable.companyNumber],
    isActive = this[ContactsTable.isActive],
)

internal fun ResultRow.toSearchTransactionHit(): SearchTransactionHit {
    val direction = this[CashflowEntriesTable.direction]
    val absoluteAmount = Money.fromDbDecimal(this[CashflowEntriesTable.amountGross])
    val signedAmount = if (direction == CashflowDirection.Out) -absoluteAmount else absoluteAmount
    val contactName = this.getOrNull(ContactsTable.name)
    val filename = this.getOrNull(DocumentsTable.purposeRendered)
    val expenseDescription = this.getOrNull(ExpensesTable.description)
    val invoiceNumber = this.getOrNull(InvoicesTable.invoiceNumber)
    val displayText = when {
        !filename.isNullOrBlank() -> filename
        !expenseDescription.isNullOrBlank() -> expenseDescription
        !invoiceNumber.isNullOrBlank() -> invoiceNumber
        !contactName.isNullOrBlank() -> contactName
        else -> this[CashflowEntriesTable.sourceType].name
    }

    return SearchTransactionHit(
        entryId = CashflowEntryId.parse(this[CashflowEntriesTable.id].value.toString()),
        displayText = displayText,
        status = this[CashflowEntriesTable.status],
        date = this[CashflowEntriesTable.eventDate],
        amount = signedAmount,
        direction = direction,
        contactName = contactName,
        documentFilename = filename,
        documentId = this.getOrNull(DocumentsTable.id)?.let { DocumentId.parse(it.value.toString()) },
    )
}
