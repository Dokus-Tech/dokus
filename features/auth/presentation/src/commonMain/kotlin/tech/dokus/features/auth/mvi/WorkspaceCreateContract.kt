@file:Suppress("TopLevelPropertyNaming") // Using PascalCase for constants (Kotlin convention)

package tech.dokus.features.auth.mvi

import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.domain.LegalName
import tech.dokus.domain.asbtractions.RetryHandler
import tech.dokus.domain.enums.TenantType
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.entity.EntityLookup
import tech.dokus.features.auth.presentation.auth.model.AddressFormState
import tech.dokus.features.auth.presentation.auth.model.EntityConfirmationState
import tech.dokus.features.auth.presentation.auth.model.LookupState
import tech.dokus.features.auth.presentation.auth.model.WorkspaceWizardStep
import tech.dokus.foundation.app.state.DokusState

/**
 * Contract for Workspace Creation wizard screen.
 *
 * Flow:
 * 1. Loading → Initial loading of user info (check freelancer status, get username)
 * 2. Wizard → Multi-step wizard with states:
 *    - TypeSelection → User selects Freelancer or Company
 *    - CompanyName → User enters company name (Company only)
 *    - VatAndAddress → User enters VAT and address
 * 3. Creating → Workspace creation in progress
 * 4. Error → Error occurred with retry option
 */

// ============================================================================
// STATE
// ============================================================================

@Immutable
sealed interface WorkspaceCreateState : MVIState, DokusState<Nothing> {

    /**
     * Initial loading state - fetching user info.
     */
    data object Loading : WorkspaceCreateState, DokusState.Loading<Nothing>

    /**
     * Wizard state - user is navigating through the wizard steps.
     */
    data class Wizard(
        val step: WorkspaceWizardStep = WorkspaceWizardStep.TypeSelection,
        val tenantType: TenantType = TenantType.Company,
        val hasFreelancerWorkspace: Boolean = false,
        val userName: String = "",
        val companyName: LegalName = LegalName.Empty,
        val lookupState: LookupState = LookupState.Idle,
        val confirmationState: EntityConfirmationState = EntityConfirmationState.Hidden,
        val selectedEntity: EntityLookup? = null,
        val vatNumber: VatNumber = VatNumber(""),
        val address: AddressFormState = AddressFormState(),
    ) : WorkspaceCreateState {

        /** Whether the current step is valid and can proceed */
        val canProceed: Boolean
            get() = when (step) {
                WorkspaceWizardStep.TypeSelection -> true
                WorkspaceWizardStep.CompanyName -> companyName.isValid
                WorkspaceWizardStep.VatAndAddress -> vatNumber.isValid && address.isValid
            }

        /** The total number of steps for the current tenant type */
        val totalSteps: Int
            get() = WorkspaceWizardStep.stepsForType(tenantType).size

        /** The current step number (1-based) */
        val currentStepNumber: Int
            get() = WorkspaceWizardStep.stepsForType(tenantType).indexOf(step) + 1

        /** Whether we can go back in the wizard */
        val canGoBack: Boolean
            get() = WorkspaceWizardStep.stepsForType(tenantType).indexOf(step) > 0
    }

    /**
     * Creating state - workspace creation in progress.
     */
    data class Creating(
        val tenantType: TenantType,
        val companyName: LegalName,
        val userName: String,
        val vatNumber: VatNumber,
        val address: AddressFormState,
        val selectedEntity: EntityLookup?,
    ) : WorkspaceCreateState

    /**
     * Error state with recovery option.
     */
    data class Error(
        override val exception: DokusException,
        override val retryHandler: RetryHandler,
        val previousWizardState: Wizard? = null,
    ) : WorkspaceCreateState, DokusState.Error<Nothing>
}

// ============================================================================
// INTENTS (User Actions)
// ============================================================================

@Immutable
sealed interface WorkspaceCreateIntent : MVIIntent {
    /** Load user info on screen init */
    data object LoadUserInfo : WorkspaceCreateIntent

    /** User selected a tenant type (Freelancer or Company) */
    data class SelectType(val type: TenantType) : WorkspaceCreateIntent

    /** User changed the company name */
    data class UpdateCompanyName(val name: LegalName) : WorkspaceCreateIntent

    /** User clicked to lookup the company */
    data object LookupCompany : WorkspaceCreateIntent

    /** User selected an entity from lookup results */
    data class SelectEntity(val entity: EntityLookup) : WorkspaceCreateIntent

    /** User chose to enter manually instead of using lookup */
    data object EnterManually : WorkspaceCreateIntent

    /** User dismissed the confirmation dialog */
    data object DismissConfirmation : WorkspaceCreateIntent

    /** User changed the VAT number */
    data class UpdateVatNumber(val vatNumber: VatNumber) : WorkspaceCreateIntent

    /** User changed the address */
    data class UpdateAddress(val address: AddressFormState) : WorkspaceCreateIntent

    /** User clicked next/continue button */
    data object NextClicked : WorkspaceCreateIntent

    /** User clicked back button */
    data object BackClicked : WorkspaceCreateIntent

    /** Retry workspace creation after error */
    data object RetryCreation : WorkspaceCreateIntent
}

// ============================================================================
// ACTIONS (Side Effects)
// ============================================================================

@Immutable
sealed interface WorkspaceCreateAction : MVIAction {
    /** Navigate to home screen after successful workspace creation */
    data object NavigateHome : WorkspaceCreateAction

    /** Navigate back to previous screen */
    data object NavigateBack : WorkspaceCreateAction

    /** Show error message when creation fails */
    data class ShowCreationError(val error: DokusException) : WorkspaceCreateAction
}
