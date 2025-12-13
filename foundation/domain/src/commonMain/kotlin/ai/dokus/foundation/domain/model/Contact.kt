package ai.dokus.foundation.domain.model

import ai.dokus.foundation.domain.Email
import ai.dokus.foundation.domain.Name
import ai.dokus.foundation.domain.VatRate
import ai.dokus.foundation.domain.enums.ClientType
import ai.dokus.foundation.domain.enums.ContactType
import ai.dokus.foundation.domain.ids.ContactId
import ai.dokus.foundation.domain.ids.ContactNoteId
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.ids.UserId
import ai.dokus.foundation.domain.ids.VatNumber
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ============================================================================
// CONTACT DTOs
// ============================================================================

/**
 * Main contact DTO representing a customer, vendor, or both.
 * Contacts are used for invoices (customers) and bills/expenses (vendors).
 */
@Serializable
data class ContactDto(
    val id: ContactId,
    val tenantId: TenantId,
    val name: Name,
    val email: Email? = null,
    val vatNumber: VatNumber? = null,
    val contactType: ContactType = ContactType.Customer,
    val businessType: ClientType = ClientType.Business,
    val addressLine1: String? = null,
    val addressLine2: String? = null,
    val city: String? = null,
    val postalCode: String? = null,
    val country: String? = null,
    val contactPerson: String? = null,
    val phone: String? = null,
    val companyNumber: String? = null,
    val defaultPaymentTerms: Int = 30,
    val defaultVatRate: VatRate? = null,
    val peppolId: String? = null,
    val peppolEnabled: Boolean = false,
    val tags: String? = null,
    val isActive: Boolean = true,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    // Aggregated counts for UI (optional, populated by service layer)
    val invoiceCount: Long = 0,
    val billCount: Long = 0,
    val expenseCount: Long = 0,
    val notesCount: Long = 0
)

/**
 * Contact note with history tracking.
 * Each note is timestamped and tracks the author.
 */
@Serializable
data class ContactNoteDto(
    val id: ContactNoteId,
    val contactId: ContactId,
    val tenantId: TenantId,
    val content: String,
    val authorId: UserId? = null,
    val authorName: String? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

// ============================================================================
// REQUEST DTOs
// ============================================================================

/**
 * Request DTO for creating a contact.
 */
@Serializable
data class CreateContactRequest(
    val name: String,
    val email: String? = null,
    val phone: String? = null,
    val vatNumber: String? = null,
    val contactType: ContactType = ContactType.Customer,
    val businessType: ClientType = ClientType.Business,
    val addressLine1: String? = null,
    val addressLine2: String? = null,
    val city: String? = null,
    val postalCode: String? = null,
    val country: String? = null,
    val contactPerson: String? = null,
    val companyNumber: String? = null,
    val defaultPaymentTerms: Int = 30,
    val defaultVatRate: String? = null,
    val peppolId: String? = null,
    val peppolEnabled: Boolean = false,
    val tags: String? = null,
    val initialNote: String? = null
)

/**
 * Request DTO for updating a contact.
 * All fields are optional - only provided fields will be updated.
 */
@Serializable
data class UpdateContactRequest(
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val vatNumber: String? = null,
    val contactType: ContactType? = null,
    val businessType: ClientType? = null,
    val addressLine1: String? = null,
    val addressLine2: String? = null,
    val city: String? = null,
    val postalCode: String? = null,
    val country: String? = null,
    val contactPerson: String? = null,
    val companyNumber: String? = null,
    val defaultPaymentTerms: Int? = null,
    val defaultVatRate: String? = null,
    val peppolId: String? = null,
    val peppolEnabled: Boolean? = null,
    val tags: String? = null,
    val isActive: Boolean? = null
)

/**
 * Request DTO for updating Peppol settings only.
 */
@Serializable
data class UpdateContactPeppolRequest(
    val peppolId: String?,
    val peppolEnabled: Boolean
)

/**
 * Request DTO for creating a contact note.
 */
@Serializable
data class CreateContactNoteRequest(
    val content: String
)

/**
 * Request DTO for updating a contact note.
 */
@Serializable
data class UpdateContactNoteRequest(
    val content: String
)

// ============================================================================
// STATISTICS
// ============================================================================

/**
 * Contact statistics for dashboard.
 */
@Serializable
data class ContactStats(
    val totalContacts: Long,
    val activeContacts: Long,
    val inactiveContacts: Long,
    val customerCount: Long,
    val vendorCount: Long,
    val bothCount: Long,
    val peppolEnabledContacts: Long
)

// ============================================================================
// EVENTS
// ============================================================================

/**
 * Real-time contact events for reactive UI updates.
 */
@Serializable
sealed class ContactEvent {
    @Serializable
    @SerialName("ContactEvent.ContactCreated")
    data class ContactCreated(val contact: ContactDto) : ContactEvent()

    @Serializable
    @SerialName("ContactEvent.ContactUpdated")
    data class ContactUpdated(val contact: ContactDto) : ContactEvent()

    @Serializable
    @SerialName("ContactEvent.ContactDeleted")
    data class ContactDeleted(val contactId: ContactId) : ContactEvent()

    @Serializable
    @SerialName("ContactEvent.NoteAdded")
    data class NoteAdded(val contactId: ContactId, val note: ContactNoteDto) : ContactEvent()

    @Serializable
    @SerialName("ContactEvent.NoteUpdated")
    data class NoteUpdated(val contactId: ContactId, val note: ContactNoteDto) : ContactEvent()

    @Serializable
    @SerialName("ContactEvent.NoteDeleted")
    data class NoteDeleted(val contactId: ContactId, val noteId: ContactNoteId) : ContactEvent()
}
