@file:Suppress("TopLevelPropertyNaming") // Using PascalCase for constants (Kotlin convention)

package tech.dokus.features.auth.mvi

import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.domain.LegalName
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.FirmId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.entity.EntityLookup
import tech.dokus.features.auth.presentation.auth.model.AddressFormState
import tech.dokus.features.auth.presentation.auth.model.LookupState
import tech.dokus.features.auth.presentation.auth.model.WorkspaceCreateType
import tech.dokus.features.auth.presentation.auth.model.WorkspaceWizardStep
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.app.state.isSuccess

/**
 * Contract for Workspace Creation wizard screen.
 *
 * Flow:
 * 1. userInfo is loading → Initial loading of user info (check freelancer status, get username)
 * 2. userInfo is success, !isCreating → Multi-step wizard with states:
 *    - TypeSelection → User selects Freelancer, Company, or Bookkeeper
 *    - CompanyName → User enters company/practice name (Company and Bookkeeper)
 *    - VatAndAddress → User enters VAT and address
 * 3. isCreating → Workspace creation in progress
 * 4. error != null → Error occurred (form stays visible)
 */

// ============================================================================
// STATE
// ============================================================================

/**
 * User info loaded during initialization.
 */
data class WorkspaceCreateUserInfo(
    val hasFreelancerWorkspace: Boolean,
    val userName: String,
)

@Immutable
data class WorkspaceCreateState(
    val userInfo: DokusState<WorkspaceCreateUserInfo> = DokusState.loading(),
    val step: WorkspaceWizardStep = WorkspaceWizardStep.TypeSelection,
    val workspaceType: WorkspaceCreateType = WorkspaceCreateType.Company,
    val companyName: LegalName = LegalName.Empty,
    val lookupState: LookupState = LookupState.Idle,
    val selectedEntity: EntityLookup? = null,
    val vatNumber: VatNumber = VatNumber(""),
    val address: AddressFormState = AddressFormState(),
    val isCreating: Boolean = false,
    val error: DokusException? = null,
) : MVIState {

    val hasFreelancerWorkspace: Boolean
        get() = (userInfo as? DokusState.Success)?.data?.hasFreelancerWorkspace ?: false

    val userName: String
        get() = (userInfo as? DokusState.Success)?.data?.userName ?: ""

    val isReady: Boolean get() = userInfo.isSuccess()

    /** Whether the current step is valid and can proceed */
    val canProceed: Boolean
        get() = when (step) {
            WorkspaceWizardStep.TypeSelection -> true
            WorkspaceWizardStep.CompanyName -> companyName.isValid
            WorkspaceWizardStep.VatAndAddress -> vatNumber.isValid && address.isValid
        }

    companion object {
        val initial by lazy { WorkspaceCreateState() }
    }
}

// ============================================================================
// INTENTS (User Actions)
// ============================================================================

@Immutable
sealed interface WorkspaceCreateIntent : MVIIntent {
    /** Load user info on screen init */
    data object LoadUserInfo : WorkspaceCreateIntent

    /** User selected a workspace type */
    data class SelectType(val type: WorkspaceCreateType) : WorkspaceCreateIntent

    /** User changed the company name */
    data class UpdateCompanyName(val name: LegalName) : WorkspaceCreateIntent

    /** User clicked to lookup the company */
    data object LookupCompany : WorkspaceCreateIntent

    /** User selected an entity from lookup results */
    data class SelectEntity(val entity: EntityLookup) : WorkspaceCreateIntent

    /** User chose to enter manually instead of using lookup */
    data object EnterManually : WorkspaceCreateIntent

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

    /** Navigate to home screen with bookkeeper console surface active */
    data class NavigateToBookkeeperConsole(val firmId: FirmId) : WorkspaceCreateAction

    /** Navigate back to previous screen */
    data object NavigateBack : WorkspaceCreateAction

    /** Show error message when creation fails */
    data class ShowCreationError(val error: DokusException) : WorkspaceCreateAction
}
