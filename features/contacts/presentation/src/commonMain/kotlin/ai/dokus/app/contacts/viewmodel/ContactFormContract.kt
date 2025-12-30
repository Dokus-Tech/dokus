package ai.dokus.app.contacts.viewmodel

import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.contacts_duplicate_match_email
import ai.dokus.app.resources.generated.contacts_duplicate_match_name_country
import ai.dokus.app.resources.generated.contacts_duplicate_match_vat
import androidx.compose.runtime.Immutable
import org.jetbrains.compose.resources.StringResource
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.domain.asbtractions.RetryHandler
import tech.dokus.domain.enums.ClientType
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.foundation.app.state.DokusState

/**
 * Contract for Contact Form screen (Create/Edit).
 *
 * Flow:
 * 1. Editing (Create Mode) → Empty form for new contact
 * 2. LoadingContact → Fetching existing contact for edit mode
 * 3. Editing (Edit Mode) → Form populated with contact data
 * 4. Saving → Creating or updating contact
 * 5. Deleting → Deleting the contact
 * 6. Error → Failed operation with retry option
 *
 * Features:
 * - Form field validation (name required, email format, Peppol ID format)
 * - Duplicate detection with debouncing
 * - Country and business type pickers
 * - Delete confirmation dialog
 */

// ============================================================================
// FORM DATA
// ============================================================================

/**
 * Data class representing all editable contact fields.
 * Kept separate from state for reusability.
 */
@Immutable
data class ContactFormData(
    // Basic info
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val contactPerson: String = "",

    // Business info
    val vatNumber: String = "",
    val companyNumber: String = "",
    val businessType: ClientType = ClientType.Business,

    // Address
    val addressLine1: String = "",
    val addressLine2: String = "",
    val city: String = "",
    val postalCode: String = "",
    val country: String = "",

    // Peppol settings
    val peppolId: String = "",
    val peppolEnabled: Boolean = false,

    // Defaults
    val defaultPaymentTerms: Int = 30,
    val defaultVatRate: String = "",

    // Tags and notes
    val tags: String = "",
    val initialNote: String = "",

    // Status
    val isActive: Boolean = true,

    // Validation errors
    val errors: Map<String, String> = emptyMap(),
) {
    /**
     * Check if the form has the minimum required data.
     */
    val isValid: Boolean
        get() = name.isNotBlank() && errors.isEmpty()

    /**
     * Check if email format is valid (if provided).
     */
    val isEmailValid: Boolean
        get() = email.isBlank() || email.contains("@")

    /**
     * Check if Peppol ID format is valid (if provided).
     * Expected format: scheme:identifier (e.g., "0208:BE0123456789")
     */
    val isPeppolIdValid: Boolean
        get() = peppolId.isBlank() || peppolId.contains(":")
}

/**
 * UI state for pickers and dialogs.
 */
@Immutable
data class ContactFormUi(
    val showDeleteConfirmation: Boolean = false,
    val showCountryPicker: Boolean = false,
    val showBusinessTypePicker: Boolean = false,
    val countrySearchQuery: String = "",
)

/**
 * Represents a potential duplicate contact detected during form entry.
 */
@Immutable
data class PotentialDuplicate(
    val contact: ContactDto,
    val matchReason: DuplicateReason,
)

/**
 * Reason for duplicate detection match.
 */
enum class DuplicateReason(val labelRes: StringResource) {
    VatNumber(Res.string.contacts_duplicate_match_vat),
    Email(Res.string.contacts_duplicate_match_email),
    NameAndCountry(Res.string.contacts_duplicate_match_name_country),
}

// ============================================================================
// STATE
// ============================================================================

@Immutable
sealed interface ContactFormState : MVIState, DokusState<Nothing> {

    /**
     * Loading existing contact for edit mode.
     */
    data class LoadingContact(
        val contactId: ContactId,
    ) : ContactFormState

    /**
     * Main editing state - handles both create and edit modes.
     *
     * @property contactId ID of the contact being edited (null for create mode)
     * @property originalContact Original contact data when editing (for comparison)
     * @property formData Current form field values
     * @property ui UI state for pickers and dialogs
     * @property duplicates Detected potential duplicate contacts
     * @property isDuplicateCheckInProgress Whether duplicate check is running
     * @property isSaving Whether save operation is in progress
     * @property isDeleting Whether delete operation is in progress
     */
    data class Editing(
        val contactId: ContactId? = null,
        val originalContact: ContactDto? = null,
        val formData: ContactFormData = ContactFormData(),
        val ui: ContactFormUi = ContactFormUi(),
        val duplicates: List<PotentialDuplicate> = emptyList(),
        val isDuplicateCheckInProgress: Boolean = false,
        val isSaving: Boolean = false,
        val isDeleting: Boolean = false,
    ) : ContactFormState {
        /**
         * Whether the form is in edit mode (vs create mode).
         */
        val isEditMode: Boolean get() = contactId != null

        /**
         * Whether the form has unsaved changes.
         */
        val hasChanges: Boolean
            get() = originalContact?.let { original ->
                formData.name != original.name.value ||
                    formData.email != (original.email?.value ?: "") ||
                    formData.phone != (original.phone ?: "") ||
                    formData.vatNumber != (original.vatNumber?.value ?: "") ||
                    formData.businessType != original.businessType ||
                    formData.addressLine1 != (original.addressLine1 ?: "") ||
                    formData.city != (original.city ?: "") ||
                    formData.country != (original.country ?: "") ||
                    formData.peppolId != (original.peppolId ?: "") ||
                    formData.peppolEnabled != original.peppolEnabled
            } ?: formData.name.isNotBlank()
    }

    /**
     * Error state - failed to load contact or save.
     *
     * @property contactId ID of the contact (if editing)
     * @property formData Current form data to preserve user input
     * @property exception The error that occurred
     * @property retryHandler Handler to retry the failed operation
     */
    data class Error(
        val contactId: ContactId?,
        val formData: ContactFormData,
        override val exception: DokusException,
        override val retryHandler: RetryHandler,
    ) : ContactFormState, DokusState.Error<Nothing>
}

// ============================================================================
// INTENTS (User Actions)
// ============================================================================

@Immutable
sealed interface ContactFormIntent : MVIIntent {

    // === Initialization ===

    /** Initialize form for creating a new contact */
    data object InitForCreate : ContactFormIntent

    /** Initialize form for editing an existing contact */
    data class InitForEdit(val contactId: ContactId) : ContactFormIntent

    // === Basic Info Field Updates ===

    /** Update contact name field */
    data class UpdateName(val value: String) : ContactFormIntent

    /** Update email field */
    data class UpdateEmail(val value: String) : ContactFormIntent

    /** Update phone field */
    data class UpdatePhone(val value: String) : ContactFormIntent

    /** Update contact person field */
    data class UpdateContactPerson(val value: String) : ContactFormIntent

    // === Business Info Field Updates ===

    /** Update VAT number field */
    data class UpdateVatNumber(val value: String) : ContactFormIntent

    /** Update company number field */
    data class UpdateCompanyNumber(val value: String) : ContactFormIntent

    /** Update business type (after picker selection) */
    data class UpdateBusinessType(val value: ClientType) : ContactFormIntent

    // === Address Field Updates ===

    /** Update address line 1 field */
    data class UpdateAddressLine1(val value: String) : ContactFormIntent

    /** Update address line 2 field */
    data class UpdateAddressLine2(val value: String) : ContactFormIntent

    /** Update city field */
    data class UpdateCity(val value: String) : ContactFormIntent

    /** Update postal code field */
    data class UpdatePostalCode(val value: String) : ContactFormIntent

    /** Update country (after picker selection) */
    data class UpdateCountry(val value: String) : ContactFormIntent

    // === Peppol Field Updates ===

    /** Update Peppol ID field */
    data class UpdatePeppolId(val value: String) : ContactFormIntent

    /** Toggle Peppol enabled status */
    data class UpdatePeppolEnabled(val value: Boolean) : ContactFormIntent

    // === Defaults Field Updates ===

    /** Update default payment terms */
    data class UpdateDefaultPaymentTerms(val value: Int) : ContactFormIntent

    /** Update default VAT rate */
    data class UpdateDefaultVatRate(val value: String) : ContactFormIntent

    // === Tags and Notes Field Updates ===

    /** Update tags field */
    data class UpdateTags(val value: String) : ContactFormIntent

    /** Update initial note field (create mode only) */
    data class UpdateInitialNote(val value: String) : ContactFormIntent

    // === Status Field Updates ===

    /** Toggle active status */
    data class UpdateIsActive(val value: Boolean) : ContactFormIntent

    // === Country Picker ===

    /** Show the country picker dialog */
    data object ShowCountryPicker : ContactFormIntent

    /** Hide the country picker dialog */
    data object HideCountryPicker : ContactFormIntent

    /** Update search query in country picker */
    data class UpdateCountrySearchQuery(val value: String) : ContactFormIntent

    // === Business Type Picker ===

    /** Show the business type picker dialog */
    data object ShowBusinessTypePicker : ContactFormIntent

    /** Hide the business type picker dialog */
    data object HideBusinessTypePicker : ContactFormIntent

    // === Delete Confirmation ===

    /** Show the delete confirmation dialog */
    data object ShowDeleteConfirmation : ContactFormIntent

    /** Hide the delete confirmation dialog */
    data object HideDeleteConfirmation : ContactFormIntent

    // === Duplicate Detection ===

    /** Dismiss duplicate warnings and proceed */
    data object DismissDuplicateWarnings : ContactFormIntent

    // === Form Actions ===

    /** Save the contact (create or update) */
    data object Save : ContactFormIntent

    /** Delete the contact (edit mode only) */
    data object Delete : ContactFormIntent

    /** Cancel and go back */
    data object Cancel : ContactFormIntent
}

// ============================================================================
// ACTIONS (Side Effects)
// ============================================================================

@Immutable
sealed interface ContactFormAction : MVIAction {

    /** Navigate back to previous screen */
    data object NavigateBack : ContactFormAction

    /** Navigate to the saved contact's details screen */
    data class NavigateToContact(val contactId: ContactId) : ContactFormAction

    /** Show error message as snackbar/toast */
    data class ShowError(val message: String) : ContactFormAction

    /** Show success message as snackbar/toast */
    data class ShowSuccess(val message: String) : ContactFormAction

    /** Show validation error for a specific field */
    data class ShowFieldError(val field: String, val message: String) : ContactFormAction
}
