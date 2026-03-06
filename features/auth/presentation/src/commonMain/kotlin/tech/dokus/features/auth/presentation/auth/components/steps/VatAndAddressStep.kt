package tech.dokus.features.auth.presentation.auth.components.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_back
import tech.dokus.aura.resources.auth_business_details
import tech.dokus.aura.resources.auth_business_details_subtitle
import tech.dokus.aura.resources.auth_company_name_label
import tech.dokus.aura.resources.auth_registered_address
import tech.dokus.aura.resources.auth_vat_number_prefixed
import tech.dokus.aura.resources.contacts_address_line1
import tech.dokus.aura.resources.contacts_address_line2
import tech.dokus.aura.resources.contacts_city
import tech.dokus.aura.resources.contacts_country
import tech.dokus.aura.resources.contacts_postal_code
import tech.dokus.aura.resources.country_belgium
import tech.dokus.aura.resources.country_france
import tech.dokus.aura.resources.country_netherlands
import tech.dokus.aura.resources.state_creating
import tech.dokus.aura.resources.workspace_create_button
import tech.dokus.domain.enums.Country
import tech.dokus.domain.ids.VatNumber
import tech.dokus.features.auth.presentation.auth.model.AddressFormState
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.DokusGlassSurface
import tech.dokus.foundation.aura.components.PPrimaryButton
import tech.dokus.foundation.aura.components.common.PBackIconButton
import tech.dokus.foundation.aura.components.fields.PTextFieldStandard
import tech.dokus.foundation.aura.components.icons.LockIcon
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
    canCreate: Boolean,
    isSubmitting: Boolean,
    onCompanyNameChanged: (String) -> Unit,
    onVatNumberChanged: (VatNumber) -> Unit,
    onAddressChanged: (AddressFormState) -> Unit,
    onCreate: () -> Unit,
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
                    onBackPress = onBackPress,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )

                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                RegisteredAddressPane(
                    address = address,
                    canCreate = canCreate,
                    isSubmitting = isSubmitting,
                    isLargeScreen = true,
                    onAddressChanged = onAddressChanged,
                    onCreate = onCreate,
                    modifier = Modifier
                        .weight(2f)
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
                    onBackPress = onBackPress,
                    modifier = Modifier.fillMaxWidth(),
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                RegisteredAddressPane(
                    address = address,
                    canCreate = canCreate,
                    isSubmitting = isSubmitting,
                    isLargeScreen = false,
                    onAddressChanged = onAddressChanged,
                    onCreate = onCreate,
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
    onBackPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(Constraints.Spacing.xLarge),
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

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Spacer(modifier = Modifier.height(Constraints.Spacing.small))

        PTextFieldStandard(
            fieldName = stringResource(Res.string.auth_company_name_label),
            value = companyName,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = onCompanyNameChanged,
        )

        PTextFieldStandard(
            fieldName = stringResource(Res.string.auth_vat_number_prefixed),
            value = vatNumber.value,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = { onVatNumberChanged(VatNumber(it)) },
        )

        CountryField(
            selectedCountry = selectedCountry,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun RegisteredAddressPane(
    address: AddressFormState,
    canCreate: Boolean,
    isSubmitting: Boolean,
    isLargeScreen: Boolean,
    onAddressChanged: (AddressFormState) -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(Constraints.Spacing.xLarge),
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.medium),
    ) {
        Text(
            text = stringResource(Res.string.auth_registered_address),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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

        if (isLargeScreen) {
            Spacer(modifier = Modifier.weight(1f))
        }

        PPrimaryButton(
            text = if (isSubmitting) {
                stringResource(Res.string.state_creating)
            } else {
                stringResource(Res.string.workspace_create_button)
            },
            enabled = canCreate && !isSubmitting,
            modifier = Modifier.fillMaxWidth(),
            onClick = onCreate,
        )
    }
}

@Composable
private fun CountryField(
    selectedCountry: Country,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(Res.string.contacts_country),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(Constraints.Spacing.xSmall))

        DokusCardSurface(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Constraints.Spacing.large),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
            ) {
                Text(
                    text = selectedCountry.localizedName(),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                LockIcon(
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f),
                )
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
            canCreate = true,
            isSubmitting = false,
            onCompanyNameChanged = {},
            onVatNumberChanged = {},
            onAddressChanged = {},
            onCreate = {},
            onBackPress = {},
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Business Details Desktop",
    widthDp = 1200,
    heightDp = 760,
)
@Composable
private fun VatAndAddressStepDesktopPreview(
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
            canCreate = true,
            isSubmitting = false,
            onCompanyNameChanged = {},
            onVatNumberChanged = {},
            onAddressChanged = {},
            onCreate = {},
            onBackPress = {},
        )
    }
}
