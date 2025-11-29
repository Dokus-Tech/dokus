package ai.dokus.app.auth.components

import ai.dokus.foundation.design.components.PPrimaryButton
import ai.dokus.foundation.design.components.fields.PTextFieldEmail
import ai.dokus.foundation.design.components.fields.PTextFieldTaxNumber
import ai.dokus.foundation.design.components.fields.PTextFieldWorkspaceName
import ai.dokus.foundation.design.components.text.SectionTitle
import ai.dokus.foundation.domain.Email
import ai.dokus.foundation.domain.LegalName
import ai.dokus.foundation.domain.enums.Country
import ai.dokus.foundation.domain.ids.VatNumber
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
    legalName: LegalName,
    email: Email,
    vatNumber: VatNumber,
    country: Country,
    isSubmitting: Boolean,
    onLegalNameChange: (LegalName) -> Unit,
    onEmailChange: (Email) -> Unit,
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
            text = "Create your company",
            horizontalArrangement = Arrangement.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        FormFields(
            legalName = legalName,
            email = email,
            vatNumber = vatNumber,
            country = country,
            isSubmitting = isSubmitting,
            onLegalNameChange = onLegalNameChange,
            onEmailChange = onEmailChange,
            onVatNumberChange = onVatNumberChange,
            onCountryChange = onCountryChange,
            onSubmit = onSubmit
        )
    }
}

@Composable
private fun FormFields(
    legalName: LegalName,
    email: Email,
    vatNumber: VatNumber,
    country: Country,
    isSubmitting: Boolean,
    onLegalNameChange: (LegalName) -> Unit,
    onEmailChange: (Email) -> Unit,
    onVatNumberChange: (VatNumber) -> Unit,
    onCountryChange: (Country) -> Unit,
    onSubmit: () -> Unit,
) {
    val canSubmit = legalName.isValid && email.isValid && vatNumber.isValid

    PTextFieldWorkspaceName(
        fieldName = "Company name",
        value = legalName.value,
        modifier = Modifier.fillMaxWidth()
    ) { onLegalNameChange(LegalName(it)) }

    Spacer(modifier = Modifier.height(12.dp))

    PTextFieldEmail(
        fieldName = "Company email",
        value = email,
        modifier = Modifier.fillMaxWidth()
    ) { onEmailChange(it) }

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
        text = "Create company",
        enabled = canSubmit && !isSubmitting,
        modifier = Modifier.fillMaxWidth(),
        onClick = onSubmit
    )
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
