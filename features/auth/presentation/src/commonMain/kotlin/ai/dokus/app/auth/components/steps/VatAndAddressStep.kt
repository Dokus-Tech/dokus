package ai.dokus.app.auth.components.steps

import ai.dokus.app.auth.model.AddressFormState
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.auth_business_details
import tech.dokus.aura.resources.auth_business_details_subtitle
import tech.dokus.aura.resources.contacts_address_line1
import tech.dokus.aura.resources.contacts_address_line2
import tech.dokus.aura.resources.contacts_city
import tech.dokus.aura.resources.contacts_country
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.aura.resources.contacts_postal_code
import tech.dokus.aura.resources.country_belgium
import tech.dokus.aura.resources.country_france
import tech.dokus.aura.resources.country_netherlands
import tech.dokus.aura.resources.workspace_address
import tech.dokus.aura.resources.workspace_vat_number
import tech.dokus.foundation.aura.components.fields.PTextFieldStandard
import tech.dokus.foundation.aura.components.fields.PTextFieldTaxNumber
import tech.dokus.foundation.aura.components.text.SectionTitle
import tech.dokus.domain.enums.Country
import tech.dokus.domain.ids.VatNumber
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
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun VatAndAddressStep(
    vatNumber: VatNumber,
    address: AddressFormState,
    onVatNumberChanged: (VatNumber) -> Unit,
    onAddressChanged: (AddressFormState) -> Unit,
    onBackPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
    ) {
        SectionTitle(
            text = stringResource(Res.string.auth_business_details),
            horizontalArrangement = Arrangement.Start,
            onBackPress = onBackPress
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(Res.string.auth_business_details_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // VAT Number
        PTextFieldTaxNumber(
            fieldName = stringResource(Res.string.workspace_vat_number),
            value = vatNumber.value,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = { onVatNumberChanged(VatNumber(it)) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Address Section Header
        Text(
            text = stringResource(Res.string.workspace_address),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Street Line 1
        PTextFieldStandard(
            fieldName = stringResource(Res.string.contacts_address_line1),
            value = address.streetLine1,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = { onAddressChanged(address.copy(streetLine1 = it)) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Street Line 2 (Optional)
        PTextFieldStandard(
            fieldName = stringResource(Res.string.contacts_address_line2),
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
                fieldName = stringResource(Res.string.contacts_postal_code),
                value = address.postalCode,
                modifier = Modifier.weight(1f),
                onValueChange = { onAddressChanged(address.copy(postalCode = it)) }
            )

            PTextFieldStandard(
                fieldName = stringResource(Res.string.contacts_city),
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
            text = stringResource(Res.string.contacts_country),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box {
            DokusCardSurface(
                modifier = Modifier.fillMaxWidth(),
                onClick = { expanded = true },
            ) {
                Text(
                    text = selectedCountry.localizedName(),
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
                        text = { Text(country.localizedName()) },
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

@Composable
private fun Country.localizedName(): String =
    when (this) {
        Country.Belgium -> stringResource(Res.string.country_belgium)
        Country.Netherlands -> stringResource(Res.string.country_netherlands)
        Country.France -> stringResource(Res.string.country_france)
    }
