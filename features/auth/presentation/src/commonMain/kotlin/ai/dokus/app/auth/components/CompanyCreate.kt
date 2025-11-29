package ai.dokus.app.auth.components

import ai.dokus.foundation.design.components.PPrimaryButton
import ai.dokus.foundation.design.components.fields.PTextFieldTaxNumber
import ai.dokus.foundation.design.components.fields.PTextFieldWorkspaceName
import ai.dokus.foundation.design.components.text.SectionTitle
import ai.dokus.foundation.domain.DisplayName
import ai.dokus.foundation.domain.LegalName
import ai.dokus.foundation.domain.enums.Country
import ai.dokus.foundation.domain.enums.TenantType
import ai.dokus.foundation.domain.ids.VatNumber
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CompanyCreateContent(
    tenantType: TenantType,
    legalName: LegalName,
    displayName: DisplayName,
    vatNumber: VatNumber,
    country: Country,
    isSubmitting: Boolean,
    onTenantTypeChange: (TenantType) -> Unit,
    onLegalNameChange: (LegalName) -> Unit,
    onDisplayNameChange: (DisplayName) -> Unit,
    onVatNumberChange: (VatNumber) -> Unit,
    onCountryChange: (Country) -> Unit,
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
            text = if (tenantType == TenantType.Company) "Create your company" else "Setup freelancer profile",
            horizontalArrangement = Arrangement.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        FormFields(
            tenantType = tenantType,
            legalName = legalName,
            displayName = displayName,
            vatNumber = vatNumber,
            country = country,
            isSubmitting = isSubmitting,
            onTenantTypeChange = onTenantTypeChange,
            onLegalNameChange = onLegalNameChange,
            onDisplayNameChange = onDisplayNameChange,
            onVatNumberChange = onVatNumberChange,
            onCountryChange = onCountryChange,
            onSubmit = onSubmit
        )
    }
}

@Composable
private fun FormFields(
    tenantType: TenantType,
    legalName: LegalName,
    displayName: DisplayName,
    vatNumber: VatNumber,
    country: Country,
    isSubmitting: Boolean,
    onTenantTypeChange: (TenantType) -> Unit,
    onLegalNameChange: (LegalName) -> Unit,
    onDisplayNameChange: (DisplayName) -> Unit,
    onVatNumberChange: (VatNumber) -> Unit,
    onCountryChange: (Country) -> Unit,
    onSubmit: () -> Unit,
) {
    val canSubmit = legalName.isValid && displayName.isValid && vatNumber.isValid

    // Tenant type selector
    TenantTypeSelector(
        selected = tenantType,
        onSelected = onTenantTypeChange
    )

    Spacer(modifier = Modifier.height(16.dp))

    PTextFieldWorkspaceName(
        fieldName = if (tenantType == TenantType.Company) "Legal name" else "Full name",
        value = legalName.value,
        modifier = Modifier.fillMaxWidth()
    ) { onLegalNameChange(LegalName(it)) }

    Spacer(modifier = Modifier.height(12.dp))

    PTextFieldWorkspaceName(
        fieldName = "Display name",
        value = displayName.value,
        modifier = Modifier.fillMaxWidth()
    ) { onDisplayNameChange(DisplayName(it)) }

    Spacer(modifier = Modifier.height(12.dp))

    CountrySelector(
        selected = country,
        onSelected = onCountryChange
    )

    Spacer(modifier = Modifier.height(12.dp))

    PTextFieldTaxNumber(
        fieldName = "VAT number",
        value = vatNumber.value,
        modifier = Modifier.fillMaxWidth()
    ) { onVatNumberChange(VatNumber(it)) }

    Spacer(modifier = Modifier.height(24.dp))

    PPrimaryButton(
        text = if (tenantType == TenantType.Company) "Create company" else "Create profile",
        enabled = canSubmit && !isSubmitting,
        modifier = Modifier.fillMaxWidth(),
        onClick = onSubmit
    )
}

@Composable
private fun TenantTypeSelector(
    selected: TenantType,
    onSelected: (TenantType) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        FilterChip(
            selected = selected == TenantType.Company,
            onClick = { onSelected(TenantType.Company) },
            label = { Text("Company") }
        )
        FilterChip(
            selected = selected == TenantType.Freelancer,
            onClick = { onSelected(TenantType.Freelancer) },
            label = { Text("Freelancer") }
        )
    }
}

@Composable
private fun CountrySelector(
    selected: Country,
    onSelected: (Country) -> Unit,
) {
    val (expanded, setExpanded) = remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { setExpanded(true) }, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Country: ${selected.name}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { setExpanded(false) }) {
            Country.entries.forEach { country ->
                DropdownMenuItem(
                    text = { Text(country.name) },
                    onClick = {
                        onSelected(country)
                        setExpanded(false)
                    }
                )
            }
        }
    }
}
