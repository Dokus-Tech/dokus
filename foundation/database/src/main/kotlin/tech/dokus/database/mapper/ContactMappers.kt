package tech.dokus.database.mapper

import org.jetbrains.exposed.v1.core.ResultRow
import tech.dokus.database.entity.ContactEntity
import tech.dokus.database.entity.ContactNoteEntity
import tech.dokus.database.tables.contacts.ContactNotesTable
import tech.dokus.database.tables.contacts.ContactsTable
import tech.dokus.domain.Email
import tech.dokus.domain.Name
import tech.dokus.domain.PhoneNumber
import tech.dokus.domain.VatRate
import tech.dokus.domain.fromDbDecimal
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.ContactNoteId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.Iban
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.ids.VatNumber

fun ContactEntity.Companion.from(row: ResultRow): ContactEntity = ContactEntity(
    id = ContactId.parse(row[ContactsTable.id].value.toString()),
    tenantId = TenantId.parse(row[ContactsTable.tenantId].toString()),
    name = Name(row[ContactsTable.name]),
    email = row[ContactsTable.email]?.let { Email(it) },
    iban = row[ContactsTable.iban]?.let { Iban(it) },
    vatNumber = row[ContactsTable.vatNumber]?.let { VatNumber(it) },
    businessType = row[ContactsTable.businessType],
    contactPerson = row[ContactsTable.contactPerson],
    phone = row[ContactsTable.phone]?.let { PhoneNumber(it) },
    companyNumber = row[ContactsTable.companyNumber],
    defaultPaymentTerms = row[ContactsTable.defaultPaymentTerms],
    defaultVatRate = row[ContactsTable.defaultVatRate]?.let { VatRate.fromDbDecimal(it) },
    tags = row[ContactsTable.tags],
    isActive = row[ContactsTable.isActive],
    createdAt = row[ContactsTable.createdAt],
    updatedAt = row[ContactsTable.updatedAt],
    isSystemContact = row[ContactsTable.isSystemContact],
    createdFromDocumentId = row[ContactsTable.createdFromDocumentId]?.let {
        DocumentId.parse(it.toString())
    },
    source = row[ContactsTable.contactSource],
)

fun ContactNoteEntity.Companion.from(row: ResultRow): ContactNoteEntity = ContactNoteEntity(
    id = ContactNoteId.parse(row[ContactNotesTable.id].value.toString()),
    contactId = ContactId.parse(row[ContactNotesTable.contactId].toString()),
    tenantId = TenantId.parse(row[ContactNotesTable.tenantId].toString()),
    content = row[ContactNotesTable.content],
    authorId = row[ContactNotesTable.authorId]?.let { UserId(it.toString()) },
    authorName = row[ContactNotesTable.authorName],
    createdAt = row[ContactNotesTable.createdAt],
    updatedAt = row[ContactNotesTable.updatedAt],
)
