package tech.dokus.database.mapper

import org.jetbrains.exposed.v1.core.ResultRow
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
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.domain.model.contact.ContactNoteDto

internal fun ResultRow.toContactDto(): ContactDto {
    return ContactDto(
        id = ContactId.parse(this[ContactsTable.id].value.toString()),
        tenantId = TenantId.parse(this[ContactsTable.tenantId].toString()),
        name = Name(this[ContactsTable.name]),
        email = this[ContactsTable.email]?.let { Email(it) },
        iban = this[ContactsTable.iban]?.let { Iban(it) },
        vatNumber = this[ContactsTable.vatNumber]?.let { VatNumber(it) },
        businessType = this[ContactsTable.businessType],
        // Addresses are now in ContactAddressesTable, populated by caller
        contactPerson = this[ContactsTable.contactPerson],
        phone = this[ContactsTable.phone]?.let { PhoneNumber(it) },
        companyNumber = this[ContactsTable.companyNumber],
        defaultPaymentTerms = this[ContactsTable.defaultPaymentTerms],
        defaultVatRate = this[ContactsTable.defaultVatRate]?.let { VatRate.fromDbDecimal(it) },
        // NOTE: peppolId/peppolEnabled removed - PEPPOL status is now in PeppolDirectoryCacheTable
        tags = this[ContactsTable.tags],
        isActive = this[ContactsTable.isActive],
        createdAt = this[ContactsTable.createdAt],
        updatedAt = this[ContactsTable.updatedAt],
        // UI Contract fields
        isSystemContact = this[ContactsTable.isSystemContact],
        createdFromDocumentId = this[ContactsTable.createdFromDocumentId]?.let {
            DocumentId.parse(it.toString())
        },
        source = this[ContactsTable.contactSource]
        // addresses, derivedRoles and activitySummary are populated by service layer on demand
    )
}

internal fun ResultRow.toContactNoteDto(): ContactNoteDto {
    return ContactNoteDto(
        id = ContactNoteId.parse(this[ContactNotesTable.id].value.toString()),
        contactId = ContactId.parse(this[ContactNotesTable.contactId].toString()),
        tenantId = TenantId.parse(this[ContactNotesTable.tenantId].toString()),
        content = this[ContactNotesTable.content],
        authorId = this[ContactNotesTable.authorId]?.let { UserId(it.toString()) },
        authorName = this[ContactNotesTable.authorName],
        createdAt = this[ContactNotesTable.createdAt],
        updatedAt = this[ContactNotesTable.updatedAt]
    )
}
