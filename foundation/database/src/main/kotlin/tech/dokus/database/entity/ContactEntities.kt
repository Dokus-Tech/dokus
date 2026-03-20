package tech.dokus.database.entity

import kotlinx.datetime.LocalDateTime
import tech.dokus.domain.Email
import tech.dokus.domain.Name
import tech.dokus.domain.PhoneNumber
import tech.dokus.domain.VatRate
import tech.dokus.domain.enums.ClientType
import tech.dokus.domain.enums.ContactSource
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.ContactNoteId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.Iban
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.ids.VatNumber

data class ContactEntity(
    val id: ContactId,
    val tenantId: TenantId,
    val name: Name,
    val email: Email? = null,
    val iban: Iban? = null,
    val vatNumber: VatNumber? = null,
    val businessType: ClientType = ClientType.Business,
    val contactPerson: String? = null,
    val phone: PhoneNumber? = null,
    val companyNumber: String? = null,
    val defaultPaymentTerms: Int = 30,
    val defaultVatRate: VatRate? = null,
    val tags: String? = null,
    val isActive: Boolean = true,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val isSystemContact: Boolean = false,
    val createdFromDocumentId: DocumentId? = null,
    val source: ContactSource = ContactSource.Manual,
) {
    companion object
}

data class ContactNoteEntity(
    val id: ContactNoteId,
    val contactId: ContactId,
    val tenantId: TenantId,
    val content: String,
    val authorId: UserId? = null,
    val authorName: String? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object
}
