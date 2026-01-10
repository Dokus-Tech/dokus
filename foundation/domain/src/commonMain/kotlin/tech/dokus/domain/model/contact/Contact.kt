package tech.dokus.domain.model.contact

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.dokus.domain.City
import tech.dokus.domain.Email
import tech.dokus.domain.Name
import tech.dokus.domain.PhoneNumber
import tech.dokus.domain.VatRate
import tech.dokus.domain.enums.ClientType
import tech.dokus.domain.enums.ContactSource
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.ContactNoteId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.ids.VatNumber

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
    val businessType: ClientType = ClientType.Business,
    val addressLine1: String? = null,
    val addressLine2: String? = null,
    val city: City? = null,
    val postalCode: String? = null,
    val country: String? = null,
    val contactPerson: String? = null,
    val phone: PhoneNumber? = null,
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
    val notesCount: Long = 0,
    // UI Contract: New fields for contacts module extension
    /** Computed roles from cashflow items (customer, supplier, vendor) */
    val derivedRoles: DerivedContactRoles? = null,
    /** Full activity summary (optional, populated on demand for detail views) */
    val activitySummary: ContactActivitySummary? = null,
    /** True for system-managed contacts like "Unknown / Unassigned" */
    val isSystemContact: Boolean = false,
    /** Source document ID if contact was created from AI extraction */
    val createdFromDocumentId: DocumentId? = null,
    /** How this contact was created (Manual, AI, Peppol) */
    val source: ContactSource = ContactSource.Manual
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
 * Address input for contact create/update requests.
 * Encapsulates all address-related fields.
 */
@Serializable
data class ContactAddress(
    val streetLine1: String,
    val streetLine2: String? = null,
    val city: City,
    val postalCode: String,
    val country: String
)

/**
 * Request DTO for creating a contact.
 */
@Serializable
data class CreateContactRequest(
    val name: Name,
    val email: Email? = null,
    val phone: PhoneNumber? = null,
    val vatNumber: VatNumber? = null,
    val businessType: ClientType = ClientType.Business,
    val address: ContactAddress? = null,
    val contactPerson: String? = null,
    val companyNumber: String? = null,
    val defaultPaymentTerms: Int = 30,
    val defaultVatRate: String? = null,
    val peppolId: String? = null,
    val peppolEnabled: Boolean = false,
    val tags: String? = null,
    val initialNote: String? = null,
    /** How this contact was created (Manual, AI, Peppol) */
    val source: ContactSource = ContactSource.Manual
)

/**
 * Request DTO for updating a contact.
 * All fields are optional - only provided fields will be updated.
 */
@Serializable
data class UpdateContactRequest(
    val name: Name? = null,
    val email: Email? = null,
    val phone: PhoneNumber? = null,
    val vatNumber: VatNumber? = null,
    val businessType: ClientType? = null,
    val address: ContactAddress? = null,
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
// DERIVED ROLES (Computed from cashflow items)
// ============================================================================

/**
 * Contact role derived from actual usage in cashflow items.
 */
@Serializable
enum class ContactRole {
    @SerialName("customer")
    Customer, // Has outgoing invoices

    @SerialName("supplier")
    Supplier, // Has incoming bills

    @SerialName("vendor")
    Vendor // Has expenses
}

/**
 * Derived roles computed from cashflow items (invoices, bills, expenses).
 * Replaces the manual ContactType field.
 */
@Serializable
data class DerivedContactRoles(
    val isCustomer: Boolean = false, // Has outgoing invoices
    val isSupplier: Boolean = false, // Has incoming bills
    val isVendor: Boolean = false, // Has expenses
    val primaryRole: ContactRole? = null // Most common role by transaction count
)

// ============================================================================
// CONTACT MATCHING (AI suggestion workflow)
// ============================================================================

/**
 * Reason for how a contact match was determined.
 */
@Serializable
enum class ContactMatchReason {
    @SerialName("vat_number")
    VatNumber, // Matched by VAT number (high confidence)

    @SerialName("peppol_id")
    PeppolId, // Matched by Peppol participant ID (high confidence)

    @SerialName("company_number")
    CompanyNumber, // Matched by company registration number

    @SerialName("name_country")
    NameAndCountry, // Matched by name + country (medium confidence)

    @SerialName("name_only")
    NameOnly, // Matched by name only (low confidence)

    @SerialName("no_match")
    NoMatch // No existing contact matched
}

/**
 * Contact suggestion result from AI matching during document processing.
 * Used when AI extracts counterparty info and attempts to match to existing contacts.
 */
@Serializable
data class ContactSuggestion(
    val contactId: ContactId?, // Matched contact ID (null if no match)
    val contact: ContactDto?, // Full contact details if matched
    val confidence: Float, // 0.0 - 1.0 confidence score
    val matchReason: ContactMatchReason, // How the match was determined
    val matchDetails: String? = null // Human-readable explanation (e.g., "Matched VAT: BE0123456789")
)

// ============================================================================
// ACTIVITY VIEWS
// ============================================================================

/**
 * Summary of a contact's activity across all cashflow item types.
 * Used for contact detail views and dashboards.
 */
@Serializable
data class ContactActivitySummary(
    val contactId: ContactId,
    val invoiceCount: Long = 0,
    val invoiceTotal: String = "0.00", // Decimal as string for precision
    val billCount: Long = 0,
    val billTotal: String = "0.00",
    val expenseCount: Long = 0,
    val expenseTotal: String = "0.00",
    val lastActivityDate: LocalDateTime? = null,
    val pendingApprovalCount: Long = 0 // Documents with this contact as suggested
)

// ============================================================================
// MERGE / DEDUPE
// ============================================================================

/**
 * Result of merging two contacts.
 * Contains counts of reassigned items for UI feedback.
 */
@Serializable
data class ContactMergeResult(
    val sourceContactId: ContactId,
    val targetContactId: ContactId,
    val invoicesReassigned: Int,
    val billsReassigned: Int,
    val expensesReassigned: Int,
    val notesReassigned: Int,
    val sourceArchived: Boolean
)
