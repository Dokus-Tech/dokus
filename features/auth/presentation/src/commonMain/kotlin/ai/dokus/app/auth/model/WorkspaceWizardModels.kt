package ai.dokus.app.auth.model

import ai.dokus.foundation.domain.enums.Country
import ai.dokus.foundation.domain.enums.TenantType
import ai.dokus.foundation.domain.ids.VatNumber
import ai.dokus.foundation.domain.model.EntityLookup
import androidx.compose.runtime.Stable

/**
 * Steps in the workspace creation wizard.
 */
internal enum class WorkspaceWizardStep {
    /** Step 1: Select workspace type (Freelancer or Company) */
    TypeSelection,
    /** Step 2: Enter company name (Companies only, Freelancers skip) */
    CompanyName,
    /** Step 3: Enter VAT number and address (All types) */
    VatAndAddress;

    companion object {
        /**
         * Get the list of steps for a given tenant type.
         * Freelancers skip the CompanyName step.
         */
        fun stepsForType(type: TenantType): List<WorkspaceWizardStep> = when (type) {
            TenantType.Freelancer -> listOf(TypeSelection, VatAndAddress)
            TenantType.Company -> listOf(TypeSelection, CompanyName, VatAndAddress)
        }
    }
}

/**
 * Address form state for workspace creation.
 */
@Stable
internal data class AddressFormState(
    val streetLine1: String = "",
    val streetLine2: String = "",
    val city: String = "",
    val postalCode: String = "",
    val country: Country = Country.Belgium,
) {
    val isValid: Boolean
        get() = streetLine1.isNotBlank() && city.isNotBlank() && postalCode.isNotBlank()
}

/**
 * State for CBE company lookup.
 */
internal sealed class LookupState {
    data object Idle : LookupState()
    data object Loading : LookupState()
    data class Success(val results: List<EntityLookup>) : LookupState()
    data class Error(val message: String) : LookupState()
}

/**
 * State for the entity confirmation dialog.
 */
internal sealed class EntityConfirmationState {
    /** Dialog is hidden */
    data object Hidden : EntityConfirmationState()
    /** Single result found - show confirmation */
    data class SingleResult(val entity: EntityLookup) : EntityConfirmationState()
    /** Multiple results found - show selection list */
    data class MultipleResults(val entities: List<EntityLookup>) : EntityConfirmationState()
}

/**
 * Complete state for the workspace creation wizard.
 */
@Stable
internal data class WorkspaceWizardState(
    val step: WorkspaceWizardStep = WorkspaceWizardStep.TypeSelection,
    val tenantType: TenantType = TenantType.Company,
    val companyName: String = "",
    val lookupState: LookupState = LookupState.Idle,
    val selectedEntity: EntityLookup? = null,
    val vatNumber: VatNumber = VatNumber(""),
    val address: AddressFormState = AddressFormState(),
    val isCreating: Boolean = false,
) {
    /** Whether the current step is valid and can proceed */
    val canProceed: Boolean
        get() = when (step) {
            WorkspaceWizardStep.TypeSelection -> true // Type is always selected
            WorkspaceWizardStep.CompanyName -> companyName.length >= 3
            WorkspaceWizardStep.VatAndAddress -> vatNumber.isValid && address.isValid
        }

    /** The total number of steps for the current tenant type */
    val totalSteps: Int
        get() = WorkspaceWizardStep.stepsForType(tenantType).size

    /** The current step number (1-based) */
    val currentStepNumber: Int
        get() = WorkspaceWizardStep.stepsForType(tenantType).indexOf(step) + 1
}
