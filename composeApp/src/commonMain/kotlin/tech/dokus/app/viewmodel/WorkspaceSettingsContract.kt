package tech.dokus.app.viewmodel

import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.domain.asbtractions.RetryHandler
import tech.dokus.domain.enums.PeppolRegistrationStatus
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.model.PeppolActivityDto
import tech.dokus.domain.model.PeppolRegistrationDto
import tech.dokus.domain.model.Tenant
import tech.dokus.domain.model.TenantSettings
import tech.dokus.domain.model.common.Thumbnail
import tech.dokus.foundation.app.state.DokusState

/**
 * Contract for Workspace Settings screen.
 *
 * Manages tenant/company settings including company info, banking details,
 * invoice configuration, and avatar management.
 *
 * Flow:
 * 1. Loading → Initial data fetch
 * 2. Content → Settings form loaded
 *    - User can edit company info
 *    - User can update banking details
 *    - User can configure invoice settings
 *    - User can upload/delete avatar
 *    - User can save changes
 * 3. Error → Failed to load with retry option
 */

// ============================================================================
// STATE
// ============================================================================

@Immutable
sealed interface WorkspaceSettingsState : MVIState, DokusState<Nothing> {

    /**
     * Initial loading state.
     */
    data object Loading : WorkspaceSettingsState

    /**
     * Content state with workspace settings form.
     *
     * @property tenant The current tenant
     * @property settings The tenant settings
     * @property form The form state for editing
     * @property saveState Current save operation state
     * @property avatarState Current avatar operation state
     * @property currentAvatar Current company avatar
     * @property peppolRegistration Current PEPPOL registration state
     * @property peppolActivity PEPPOL activity timestamps
     * @property editingSection Currently active editing section (null = view mode)
     */
    @Immutable
    data class Content(
        val tenant: Tenant,
        val settings: TenantSettings,
        val form: FormState = FormState(),
        val saveState: SaveState = SaveState.Idle,
        val avatarState: AvatarState = AvatarState.Idle,
        val currentAvatar: Thumbnail? = null,
        val peppolRegistration: PeppolRegistrationDto? = null,
        val peppolActivity: PeppolActivityDto? = null,
        val editingSection: EditingSection? = null,
    ) : WorkspaceSettingsState {

        /**
         * Whether legal identity fields (Legal Name, VAT Number) are locked.
         * Locked after PEPPOL registration becomes active.
         */
        val isLegalIdentityLocked: Boolean
            get() = peppolRegistration?.status == PeppolRegistrationStatus.Active

        /**
         * Sections that can be edited independently.
         */
        enum class EditingSection {
            LegalIdentity,
            Banking,
            InvoiceFormat,
            PaymentTerms
        }

        /**
         * Form state for workspace settings.
         */
        @Immutable
        data class FormState(
            val companyName: String = "",
            val legalName: String = "",
            val vatNumber: String = "",
            val iban: String = "",
            val bic: String = "",
            val address: String = "",
            val invoicePrefix: String = "INV",
            val defaultPaymentTerms: Int = 30,
            val invoiceYearlyReset: Boolean = true,
            val invoicePadding: Int = 4,
            val invoiceIncludeYear: Boolean = true,
            val invoiceTimezone: String = "Europe/Brussels",
            val paymentTermsText: String = "",
        )

        /**
         * State for save operation.
         */
        @Immutable
        sealed interface SaveState {
            data object Idle : SaveState
            data object Saving : SaveState
            data object Success : SaveState
            data class Error(val error: DokusException) : SaveState
        }

        /**
         * State for avatar upload/delete operations.
         */
        @Immutable
        sealed interface AvatarState {
            data object Idle : AvatarState
            data class Uploading(val progress: Float) : AvatarState
            data object Deleting : AvatarState
            data object Success : AvatarState
            data class Error(val error: DokusException) : AvatarState
        }
    }

    /**
     * Error state with recovery option.
     *
     * @property exception The error that occurred
     * @property retryHandler Handler to retry the failed operation
     */
    data class Error(
        override val exception: DokusException,
        override val retryHandler: RetryHandler,
    ) : WorkspaceSettingsState, DokusState.Error<Nothing>
}

// ============================================================================
// INTENTS (User Actions)
// ============================================================================

@Immutable
sealed interface WorkspaceSettingsIntent : MVIIntent {

    /** Load workspace settings */
    data object Load : WorkspaceSettingsIntent

    // Form field updates
    /** Update company name field */
    data class UpdateCompanyName(val value: String) : WorkspaceSettingsIntent

    /** Update VAT number field */
    data class UpdateVatNumber(val value: String) : WorkspaceSettingsIntent

    /** Update IBAN field */
    data class UpdateIban(val value: String) : WorkspaceSettingsIntent

    /** Update BIC field */
    data class UpdateBic(val value: String) : WorkspaceSettingsIntent

    /** Update address field */
    data class UpdateAddress(val value: String) : WorkspaceSettingsIntent

    /** Update invoice prefix field */
    data class UpdateInvoicePrefix(val value: String) : WorkspaceSettingsIntent

    /** Update default payment terms */
    data class UpdateDefaultPaymentTerms(val value: String) : WorkspaceSettingsIntent

    /** Update invoice yearly reset setting */
    data class UpdateInvoiceYearlyReset(val value: Boolean) : WorkspaceSettingsIntent

    /** Update invoice padding */
    data class UpdateInvoicePadding(val value: Int) : WorkspaceSettingsIntent

    /** Update invoice include year setting */
    data class UpdateInvoiceIncludeYear(val value: Boolean) : WorkspaceSettingsIntent

    /** Update invoice timezone */
    data class UpdateInvoiceTimezone(val value: String) : WorkspaceSettingsIntent

    /** Update payment terms text */
    data class UpdatePaymentTermsText(val value: String) : WorkspaceSettingsIntent

    // Section edit mode
    /** Enter edit mode for a specific section */
    data class EnterEditMode(val section: WorkspaceSettingsState.Content.EditingSection) : WorkspaceSettingsIntent

    /** Cancel edit mode without saving */
    data object CancelEditMode : WorkspaceSettingsIntent

    /** Save the current section being edited */
    data class SaveSection(val section: WorkspaceSettingsState.Content.EditingSection) : WorkspaceSettingsIntent

    // Save operations (legacy - for full save)
    /** Save workspace settings */
    data object SaveSettings : WorkspaceSettingsIntent

    /** Reset save state to idle */
    data object ResetSaveState : WorkspaceSettingsIntent

    // Avatar operations
    /** Upload a new avatar image */
    data class UploadAvatar(val imageBytes: ByteArray, val filename: String) :
        WorkspaceSettingsIntent

    /** Delete the current avatar */
    data object DeleteAvatar : WorkspaceSettingsIntent

    /** Reset avatar state to idle */
    data object ResetAvatarState : WorkspaceSettingsIntent
}

// ============================================================================
// ACTIONS (Side Effects)
// ============================================================================

@Immutable
sealed interface WorkspaceSettingsAction : MVIAction {

    /** Show a success message */
    data class ShowSuccess(val success: WorkspaceSettingsSuccess) : WorkspaceSettingsAction

    /** Show an error message */
    data class ShowError(val error: DokusException) : WorkspaceSettingsAction
}

enum class WorkspaceSettingsSuccess {
    SettingsSaved,
}
