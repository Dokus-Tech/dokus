package tech.dokus.features.auth.presentation.auth.components.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_back
import tech.dokus.aura.resources.auth_business_details
import tech.dokus.aura.resources.auth_business_details_subtitle
import tech.dokus.aura.resources.auth_company_name_label
import tech.dokus.aura.resources.contacts_address_line1
import tech.dokus.aura.resources.contacts_address_line2
import tech.dokus.aura.resources.contacts_city
import tech.dokus.aura.resources.contacts_country
import tech.dokus.aura.resources.contacts_postal_code
import tech.dokus.aura.resources.country_belgium
import tech.dokus.aura.resources.country_france
import tech.dokus.aura.resources.country_netherlands
import tech.dokus.aura.resources.workspace_address
import tech.dokus.aura.resources.workspace_vat_number
import tech.dokus.domain.enums.Country
import tech.dokus.domain.ids.VatNumber
import tech.dokus.features.auth.presentation.auth.model.AddressFormState
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.DokusGlassSurface
import tech.dokus.foundation.aura.components.common.PBackIconButton
import tech.dokus.foundation.aura.components.fields.PTextFieldStandard
import tech.dokus.foundation.aura.components.fields.PTextFieldTaxNumber
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

@Composable
internal fun VatAndAddressStep(
    companyName: String,
    vatNumber: VatNumber,
    address: AddressFormState,
    onCompanyNameChanged: (String) -> Unit,
    onVatNumberChanged: (VatNumber) -> Unit,
    onAddressChanged: (AddressFormState) -> Unit,
    onBackPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLargeScreen = LocalScreenSize.current.isLarge

    DokusGlassSurface(modifier) {
        if (isLargeScreen) {
            Row(modifier = Modifier.fillMaxSize()) {
                CompanyDetailsPane(
                    companyName = companyName,
                    vatNumber = vatNumber,
                    selectedCountry = address.country,
                    onCompanyNameChanged = onCompanyNameChanged,
                    onVatNumberChanged = onVatNumberChanged,
                    onCountrySelected = { onAddressChanged(address.copy(country = it)) },
                    onBackPress = onBackPress,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )

                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                RegisteredAddressPane(
                    address = address,
                    onAddressChanged = onAddressChanged,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                CompanyDetailsPane(
                    companyName = companyName,
                    vatNumber = vatNumber,
                    selectedCountry = address.country,
                    onCompanyNameChanged = onCompanyNameChanged,
                    onVatNumberChanged = onVatNumberChanged,
                    onCountrySelected = { onAddressChanged(address.copy(country = it)) },
                    onBackPress = onBackPress,
                    modifier = Modifier.fillMaxWidth(),
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                RegisteredAddressPane(
                    address = address,
                    onAddressChanged = onAddressChanged,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun CompanyDetailsPane(
    companyName: String,
    vatNumber: VatNumber,
    selectedCountry: Country,
    onCompanyNameChanged: (String) -> Unit,
    onVatNumberChanged: (VatNumber) -> Unit,
    onCountrySelected: (Country) -> Unit,
    onBackPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(Constraints.Spacing.xLarge)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.medium),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.medium),
        ) {
            PBackIconButton(
                onClick = onBackPress,
                contentDescription = stringResource(Res.string.action_back),
            )
            Text(
                text = stringResource(Res.string.auth_business_details),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Text(
            text = stringResource(Res.string.auth_business_details_subtitle),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(Constraints.Spacing.small))

        PTextFieldStandard(
            fieldName = stringResource(Res.string.auth_company_name_label),
            value = companyName,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = onCompanyNameChanged,
        )

        PTextFieldTaxNumber(
            fieldName = stringResource(Res.string.workspace_vat_number),
            value = vatNumber.value,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = { onVatNumberChanged(VatNumber(it)) },
        )

        CountrySelector(
            selectedCountry = selectedCountry,
            onCountrySelected = onCountrySelected,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun RegisteredAddressPane(
    address: AddressFormState,
    onAddressChanged: (AddressFormState) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(Constraints.Spacing.xLarge)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.medium),
    ) {
        Text(
            text = stringResource(Res.string.workspace_address),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
        ) {
            PTextFieldStandard(
                fieldName = stringResource(Res.string.contacts_address_line1),
                value = address.streetLine1,
                modifier = Modifier.weight(2f),
                onValueChange = { onAddressChanged(address.copy(streetLine1 = it)) },
            )

            PTextFieldStandard(
                fieldName = stringResource(Res.string.contacts_address_line2),
                value = address.streetLine2,
                modifier = Modifier.weight(1f),
                onValueChange = { onAddressChanged(address.copy(streetLine2 = it)) },
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
        ) {
            PTextFieldStandard(
                fieldName = stringResource(Res.string.contacts_postal_code),
                value = address.postalCode,
                modifier = Modifier.weight(1f),
                onValueChange = { onAddressChanged(address.copy(postalCode = it)) },
            )

            PTextFieldStandard(
                fieldName = stringResource(Res.string.contacts_city),
                value = address.city,
                modifier = Modifier.weight(2f),
                onValueChange = { onAddressChanged(address.copy(city = it)) },
            )
        }
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
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(Constraints.Spacing.xSmall))

        Box {
            DokusCardSurface(
                modifier = Modifier.fillMaxWidth(),
                onClick = { expanded = true },
            ) {
                Text(
                    text = selectedCountry.localizedName(),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(Constraints.Spacing.large),
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                Country.entries.forEach { country ->
                    DropdownMenuItem(
                        text = { Text(country.localizedName()) },
                        onClick = {
                            onCountrySelected(country)
                            expanded = false
                        },
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

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun VatAndAddressStepPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(
        PreviewParametersProvider::class,
    ) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        VatAndAddressStep(
            companyName = "Invoid Vision",
            vatNumber = VatNumber("BE0123456789"),
            address = AddressFormState(
                streetLine1 = "Balegemstraat",
                streetLine2 = "17",
                city = "Oosterzele",
                postalCode = "9860",
                country = Country.Belgium,
            ),
            onCompanyNameChanged = {},
            onVatNumberChanged = {},
            onAddressChanged = {},
            onBackPress = {},
        )
    }
}
