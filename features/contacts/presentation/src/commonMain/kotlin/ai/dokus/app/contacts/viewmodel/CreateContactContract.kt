package ai.dokus.app.contacts.viewmodel

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.domain.enums.ClientType
import tech.dokus.domain.enums.Country
import tech.dokus.domain.enums.Language
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.model.entity.EntityLookup

/**
 * Contract for Create Contact flow using VAT-first, split-step approach.
 *
 * Flow:
 * 1. LookupStep (default) - Search by company name or VAT number
 * 2. ConfirmStep - Review fetched company data + add billing email
 * 3. ManualStep (alternative) - Manual business/individual entry
 *
 * Features:
 * - VAT duplicate detection (hard block) - checks local before remote
 * - Soft duplicate warning on manual submit (name+country)
 * - Debounced name search, immediate VAT lookup
 */

// ============================================================================
// UI MODELS (minimal, stable - avoid ContactDto in state)
// ============================================================================

/**
 * Minimal UI model for duplicate VAT display.
 * Avoids storing ContactDto in state.
 */
@Immutable
data class DuplicateVatUi(
    val contactId: ContactId,
    val displayName: String,
    val vatNumber: String,
)

/**
 * Sealed lookup state - prevents invalid combinations.
 */
@Immutable
sealed interface LookupUiState {
    data object Idle : LookupUiState
    data object Loading : LookupUiState
    data class Success(val results: List<EntityLookup>) : LookupUiState
    data object Empty : LookupUiState
    data class Error(val message: String) : LookupUiState
}

/**
 * Manual entry form data for business or individual contacts.
 */
@Immutable
data class ManualContactFormData(
    // Business fields
    val companyName: String = "",
    val country: Country = Country.Belgium,
    val vatNumber: String = "",
    val email: String = "",
    // Individual fields
    val fullName: String = "",
    val personEmail: String = "",
    val personPhone: String = "",
    // Validation errors
    val errors: Map<String, String> = emptyMap(),
) {
    val isBusinessValid: Boolean
        get() = companyName.isNotBlank() && errors.isEmpty()

    val isIndividualValid: Boolean
        get() = fullName.isNotBlank() &&
            (personEmail.isNotBlank() || personPhone.isNotBlank()) &&
            errors.isEmpty()
}

/**
 * Potential duplicate contact for soft warning on manual creation.
 */
@Immutable
data class SoftDuplicateUi(
    val contactId: ContactId,
    val displayName: String,
    val matchReason: String,
)

// ============================================================================
// STATE
// ============================================================================

@Stable
sealed interface CreateContactState : MVIState {

    /**
     * Step 1: Business Lookup (default entry point)
     *
     * User searches by company name or VAT number.
     * If VAT exists locally, shows hard block banner.
     */
    data class LookupStep(
        val query: String = "",
        val lookupState: LookupUiState = LookupUiState.Idle,
        val duplicateVat: DuplicateVatUi? = null,
    ) : CreateContactState

    /**
     * Step 2: Confirm selected company details.
     *
     * User reviews fetched company data and adds billing email.
     */
    data class ConfirmStep(
        val selectedEntity: EntityLookup,
        val billingEmail: String = "",
        val phone: String = "",
        val language: Language? = null,
        val showAddressDetails: Boolean = false,
        val isSubmitting: Boolean = false,
        val emailError: String? = null,
    ) : CreateContactState

    /**
     * Alternative: Manual entry (no VAT lookup).
     *
     * User enters business or individual contact data manually.
     * Soft duplicate check happens on submit only.
     */
    data class ManualStep(
        val contactType: ClientType = ClientType.Business,
        val formData: ManualContactFormData = ManualContactFormData(),
        val isSubmitting: Boolean = false,
        val showCountryPicker: Boolean = false,
        val softDuplicates: List<SoftDuplicateUi>? = null,
    ) : CreateContactState
}

// ============================================================================
// INTENTS (User Actions)
// ============================================================================

@Immutable
sealed interface CreateContactIntent : MVIIntent {

    // === Lookup Step ===

    /** User typed in the search field */
    data class QueryChanged(val query: String) : CreateContactIntent

    /** User selected a result from the lookup list */
    data class SelectResult(val entity: EntityLookup) : CreateContactIntent

    /** User wants to add contact without VAT lookup */
    data object GoToManualEntry : CreateContactIntent

    // === Confirm Step ===

    /** User updated billing email */
    data class BillingEmailChanged(val email: String) : CreateContactIntent

    /** User updated phone */
    data class PhoneChanged(val phone: String) : CreateContactIntent

    /** User selected a language */
    data class LanguageChanged(val language: Language?) : CreateContactIntent

    /** User toggled address details visibility */
    data object ToggleAddressDetails : CreateContactIntent

    /** User clicked "Create Contact" on confirm step */
    data object ConfirmAndCreate : CreateContactIntent

    /** User wants to go back to lookup */
    data object BackToLookup : CreateContactIntent

    // === Manual Step ===

    /** User changed contact type (Business/Individual) */
    data class ManualTypeChanged(val type: ClientType) : CreateContactIntent

    /** User changed a field in the manual form */
    data class ManualFieldChanged(val field: String, val value: String) : CreateContactIntent

    /** User selected a country */
    data class ManualCountryChanged(val country: Country) : CreateContactIntent

    /** Show/hide country picker */
    data object ShowCountryPicker : CreateContactIntent
    data object HideCountryPicker : CreateContactIntent

    /** User clicked "Create Contact" on manual step */
    data object CreateManualContact : CreateContactIntent

    /** User confirmed creation despite soft duplicates */
    data object ConfirmCreateDespiteDuplicates : CreateContactIntent

    /** User dismissed soft duplicate warning */
    data object DismissSoftDuplicates : CreateContactIntent

    /** User wants to go back to lookup from manual */
    data object BackFromManual : CreateContactIntent

    // === Common ===

    /** User cancelled the flow */
    data object Cancel : CreateContactIntent

    /** User wants to view existing contact (from duplicate warning) */
    data class ViewExistingContact(val contactId: ContactId) : CreateContactIntent
}

// ============================================================================
// ACTIONS (Side Effects)
// ============================================================================

@Immutable
sealed interface CreateContactAction : MVIAction {

    /** Navigate back to previous screen */
    data object NavigateBack : CreateContactAction

    /** Navigate to the created/existing contact's details */
    data class NavigateToContact(val contactId: ContactId) : CreateContactAction

    /** Contact was created successfully - return to caller with result */
    data class ContactCreated(val contactId: ContactId, val displayName: String) : CreateContactAction

    /** Show error message */
    data class ShowError(val message: String) : CreateContactAction
}
