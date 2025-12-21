package ai.dokus.app.auth.components.steps

import ai.dokus.app.auth.model.AddressFormState
import ai.dokus.foundation.design.components.fields.PTextFieldStandard
import ai.dokus.foundation.design.components.fields.PTextFieldTaxNumber
import ai.dokus.foundation.design.components.text.SectionTitle
import ai.dokus.foundation.domain.enums.Country
import ai.dokus.foundation.domain.ids.VatNumber
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
internal fun VatAndAddressStep(
    vatNumber: VatNumber,
    address: AddressFormState,
    onVatNumberChanged: (VatNumber) -> Unit,
    onAddressChanged: (AddressFormState) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
    ) {
        SectionTitle(
            text = "Business details",
            horizontalArrangement = Arrangement.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Enter your VAT number and business address",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // VAT Number
        PTextFieldTaxNumber(
            fieldName = "VAT number",
            value = vatNumber.value,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = { onVatNumberChanged(VatNumber(it)) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Address Section Header
        Text(
            text = "Address",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Street Line 1
        PTextFieldStandard(
            fieldName = "Street address",
            value = address.streetLine1,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = { onAddressChanged(address.copy(streetLine1 = it)) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Street Line 2 (Optional)
        PTextFieldStandard(
            fieldName = "Address line 2 (optional)",
            value = address.streetLine2,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = { onAddressChanged(address.copy(streetLine2 = it)) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Postal Code and City (side by side)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PTextFieldStandard(
                fieldName = "Postal code",
                value = address.postalCode,
                modifier = Modifier.weight(1f),
                onValueChange = { onAddressChanged(address.copy(postalCode = it)) }
            )

            PTextFieldStandard(
                fieldName = "City",
                value = address.city,
                modifier = Modifier.weight(2f),
                onValueChange = { onAddressChanged(address.copy(city = it)) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Country Dropdown
        CountrySelector(
            selectedCountry = address.country,
            onCountrySelected = { onAddressChanged(address.copy(country = it)) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CountrySelector(
    selectedCountry: Country,
    onCountrySelected: (Country) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = "Country",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box {
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true },
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = selectedCountry.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                Country.entries.forEach { country ->
                    DropdownMenuItem(
                        text = { Text(country.displayName) },
                        onClick = {
                            onCountrySelected(country)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

private val Country.displayName: String
    get() = when (this) {
        Country.Belgium -> "Belgium"
        Country.Netherlands -> "Netherlands"
        Country.France -> "France"
    }
