package tech.dokus.features.auth.presentation.auth.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.auth_your_name
import tech.dokus.aura.resources.workspace_create_button
import tech.dokus.aura.resources.workspace_create_title
import tech.dokus.aura.resources.workspace_display_name
import tech.dokus.aura.resources.workspace_legal_name
import tech.dokus.aura.resources.workspace_type_company
import tech.dokus.aura.resources.workspace_type_freelancer
import tech.dokus.aura.resources.workspace_vat_number
import tech.dokus.domain.DisplayName
import tech.dokus.domain.LegalName
import tech.dokus.domain.enums.TenantType
import tech.dokus.domain.ids.VatNumber
import tech.dokus.foundation.aura.components.PPrimaryButton
import tech.dokus.foundation.aura.components.fields.PTextFieldTaxNumber
import tech.dokus.foundation.aura.components.fields.PTextFieldWorkspaceName
import tech.dokus.foundation.aura.components.text.SectionTitle

/**
 * Internal data class for workspace creation form fields.
 * Encapsulates form state and provides validation logic.
 */
internal data class WorkspaceFormFields(
    val tenantType: TenantType,
    val legalName: LegalName,
    val displayName: DisplayName,
    val vatNumber: VatNumber,
    val userName: String,
    val hasFreelancerWorkspace: Boolean,
) {
    /**
     * The effective legal name based on tenant type.
     * For Freelancer, uses userName; for Company, uses legalName.
     */
    val effectiveLegalName: String
        get() = if (tenantType.legalNameFromUser) userName else legalName.value

    /**
     * Whether the legal name field is valid.
     * For Freelancer: checks userName is not blank.
     * For Company: checks legalName.isValid.
     */
    val isLegalNameValid: Boolean
        get() = if (tenantType.legalNameFromUser) {
            userName.isNotBlank()
        } else {
            legalName.isValid
        }

    /**
     * Whether the display name field is valid.
     * Only required for Company type.
     */
    val isDisplayNameValid: Boolean
        get() = !tenantType.requiresDisplayName || displayName.isValid

    /**
     * Whether the VAT number is valid.
     */
    val isVatNumberValid: Boolean
        get() = vatNumber.isValid

    /**
     * Whether the form can be submitted.
     * All required fields must be valid.
     */
    val canSubmit: Boolean
        get() = isLegalNameValid && isDisplayNameValid && isVatNumberValid

    /**
     * Whether the current tenant type selection is allowed.
     * Freelancer is disabled if user already has one.
     */
    fun isTenantTypeAllowed(type: TenantType): Boolean {
        return !(type == TenantType.Freelancer && hasFreelancerWorkspace)
    }
}

@Composable
fun WorkspaceCreateContent(
    tenantType: TenantType,
    legalName: LegalName,
    displayName: DisplayName,
    vatNumber: VatNumber,
    userName: String,
    isSubmitting: Boolean,
    hasFreelancerWorkspace: Boolean,
    onTenantTypeChange: (TenantType) -> Unit,
    onLegalNameChange: (LegalName) -> Unit,
    onDisplayNameChange: (DisplayName) -> Unit,
    onVatNumberChange: (VatNumber) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val formFields = WorkspaceFormFields(
        tenantType = tenantType,
        legalName = legalName,
        displayName = displayName,
        vatNumber = vatNumber,
        userName = userName,
        hasFreelancerWorkspace = hasFreelancerWorkspace
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SectionTitle(
            text = stringResource(Res.string.workspace_create_title),
            horizontalArrangement = Arrangement.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        FormFields(
            formFields = formFields,
            isSubmitting = isSubmitting,
            onTenantTypeChange = onTenantTypeChange,
            onLegalNameChange = onLegalNameChange,
            onDisplayNameChange = onDisplayNameChange,
            onVatNumberChange = onVatNumberChange,
            onSubmit = onSubmit
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FormFields(
    formFields: WorkspaceFormFields,
    isSubmitting: Boolean,
    onTenantTypeChange: (TenantType) -> Unit,
    onLegalNameChange: (LegalName) -> Unit,
    onDisplayNameChange: (DisplayName) -> Unit,
    onVatNumberChange: (VatNumber) -> Unit,
    onSubmit: () -> Unit,
) {
    // Workspace type selector using FlowRow
    WorkspaceTypeSelector(
        selected = formFields.tenantType,
        formFields = formFields,
        onSelected = onTenantTypeChange
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Legal name field - locked for Freelancer (uses user's name)
    PTextFieldWorkspaceName(
        fieldName = if (formFields.tenantType == TenantType.Company) {
            stringResource(Res.string.workspace_legal_name)
        } else {
            stringResource(Res.string.auth_your_name)
        },
        value = formFields.effectiveLegalName,
        enabled = !formFields.tenantType.legalNameFromUser,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (!formFields.tenantType.legalNameFromUser) {
            onLegalNameChange(LegalName(it))
        }
    }

    // Display name field - only shown when required
    if (formFields.tenantType.requiresDisplayName) {
        Spacer(modifier = Modifier.height(12.dp))

        PTextFieldWorkspaceName(
            fieldName = stringResource(Res.string.workspace_display_name),
            value = formFields.displayName.value,
            modifier = Modifier.fillMaxWidth()
        ) { onDisplayNameChange(DisplayName(it)) }
    }

    Spacer(modifier = Modifier.height(12.dp))

    PTextFieldTaxNumber(
        fieldName = stringResource(Res.string.workspace_vat_number),
        value = formFields.vatNumber.value,
        modifier = Modifier.fillMaxWidth()
    ) { onVatNumberChange(VatNumber(it)) }

    Spacer(modifier = Modifier.height(24.dp))

    PPrimaryButton(
        text = stringResource(Res.string.workspace_create_button),
        enabled = formFields.canSubmit && !isSubmitting,
        modifier = Modifier.fillMaxWidth(),
        onClick = onSubmit
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WorkspaceTypeSelector(
    selected: TenantType,
    formFields: WorkspaceFormFields,
    onSelected: (TenantType) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        TenantType.entries.forEach { type ->
            FilterChip(
                selected = selected == type,
                enabled = formFields.isTenantTypeAllowed(type),
                onClick = { onSelected(type) },
                label = {
                    Text(
                        when (type) {
                            TenantType.Freelancer -> stringResource(Res.string.workspace_type_freelancer)
                            TenantType.Company -> stringResource(Res.string.workspace_type_company)
                        }
                    )
                }
            )
        }
    }
}
