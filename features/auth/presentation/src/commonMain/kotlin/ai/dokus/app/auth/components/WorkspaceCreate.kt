package ai.dokus.app.auth.components

import ai.dokus.foundation.design.components.PPrimaryButton
import ai.dokus.foundation.design.components.fields.PTextFieldTaxNumber
import ai.dokus.foundation.design.components.fields.PTextFieldWorkspaceName
import ai.dokus.foundation.design.components.text.SectionTitle
import ai.dokus.foundation.domain.DisplayName
import ai.dokus.foundation.domain.LegalName
import ai.dokus.foundation.domain.enums.TenantType
import ai.dokus.foundation.domain.ids.VatNumber
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 480.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SectionTitle(
            text = "Create your workspace",
            horizontalArrangement = Arrangement.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        FormFields(
            tenantType = tenantType,
            legalName = legalName,
            displayName = displayName,
            vatNumber = vatNumber,
            userName = userName,
            isSubmitting = isSubmitting,
            hasFreelancerWorkspace = hasFreelancerWorkspace,
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
) {
    val canSubmit = legalName.isValid &&
        (!tenantType.requiresDisplayName || displayName.isValid) &&
        vatNumber.isValid

    // Workspace type selector using FlowRow
    WorkspaceTypeSelector(
        selected = tenantType,
        hasFreelancerWorkspace = hasFreelancerWorkspace,
        onSelected = onTenantTypeChange
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Legal name field - locked for Freelancer (uses user's name)
    PTextFieldWorkspaceName(
        fieldName = if (tenantType == TenantType.Company) "Legal name" else "Your name",
        value = if (tenantType.legalNameFromUser) userName else legalName.value,
        enabled = !tenantType.legalNameFromUser,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (!tenantType.legalNameFromUser) {
            onLegalNameChange(LegalName(it))
        }
    }

    // Display name field - only shown when required
    if (tenantType.requiresDisplayName) {
        Spacer(modifier = Modifier.height(12.dp))

        PTextFieldWorkspaceName(
            fieldName = "Display name",
            value = displayName.value,
            modifier = Modifier.fillMaxWidth()
        ) { onDisplayNameChange(DisplayName(it)) }
    }

    Spacer(modifier = Modifier.height(12.dp))

    PTextFieldTaxNumber(
        fieldName = "VAT number",
        value = vatNumber.value,
        modifier = Modifier.fillMaxWidth()
    ) { onVatNumberChange(VatNumber(it)) }

    Spacer(modifier = Modifier.height(24.dp))

    PPrimaryButton(
        text = "Create workspace",
        enabled = canSubmit && !isSubmitting,
        modifier = Modifier.fillMaxWidth(),
        onClick = onSubmit
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WorkspaceTypeSelector(
    selected: TenantType,
    hasFreelancerWorkspace: Boolean,
    onSelected: (TenantType) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        TenantType.entries.forEach { type ->
            val isFreelancerDisabled = type == TenantType.Freelancer && hasFreelancerWorkspace

            FilterChip(
                selected = selected == type,
                enabled = !isFreelancerDisabled,
                onClick = { onSelected(type) },
                label = {
                    Text(
                        when (type) {
                            TenantType.Freelancer -> "Freelancer"
                            TenantType.Company -> "Company"
                        }
                    )
                }
            )
        }
    }
}
