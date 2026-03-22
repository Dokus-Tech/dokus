package tech.dokus.database.mapper

import tech.dokus.database.entity.ContactEntity
import tech.dokus.database.entity.ContactNoteEntity
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.domain.model.contact.ContactNoteDto

fun ContactDto.Companion.from(entity: ContactEntity) = ContactDto(
    id = entity.id,
    tenantId = entity.tenantId,
    name = entity.name,
    email = entity.email,
    iban = entity.iban,
    vatNumber = entity.vatNumber,
    businessType = entity.businessType,
    contactPerson = entity.contactPerson,
    phone = entity.phone,
    companyNumber = entity.companyNumber,
    defaultPaymentTerms = entity.defaultPaymentTerms,
    defaultVatRate = entity.defaultVatRate,
    tags = entity.tags,
    isActive = entity.isActive,
    createdAt = entity.createdAt,
    updatedAt = entity.updatedAt,
    isSystemContact = entity.isSystemContact,
    createdFromDocumentId = entity.createdFromDocumentId,
    source = entity.source,
    // addresses, derivedRoles, activitySummary populated by service layer on demand
)

fun ContactNoteDto.Companion.from(entity: ContactNoteEntity) = ContactNoteDto(
    id = entity.id,
    contactId = entity.contactId,
    tenantId = entity.tenantId,
    content = entity.content,
    authorId = entity.authorId,
    authorName = entity.authorName,
    createdAt = entity.createdAt,
    updatedAt = entity.updatedAt,
)
