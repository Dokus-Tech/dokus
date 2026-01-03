package tech.dokus.features.contacts.presentation.contacts.components.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_back
import tech.dokus.aura.resources.contacts_add_contact_manually
import tech.dokus.aura.resources.contacts_company_name
import tech.dokus.aura.resources.contacts_create_contact
import tech.dokus.aura.resources.contacts_creating
import tech.dokus.aura.resources.contacts_email
import tech.dokus.aura.resources.contacts_full_name
import tech.dokus.aura.resources.contacts_phone
import tech.dokus.aura.resources.contacts_vat_number
import tech.dokus.aura.resources.country_belgium
import tech.dokus.aura.resources.country_france
import tech.dokus.aura.resources.country_netherlands
import tech.dokus.aura.resources.field_optional
import tech.dokus.aura.resources.field_required
import tech.dokus.domain.Email
import tech.dokus.domain.PhoneNumber
import tech.dokus.domain.enums.ClientType
import tech.dokus.domain.enums.Country
import tech.dokus.features.contacts.mvi.CreateContactIntent
import tech.dokus.features.contacts.mvi.CreateContactState
import tech.dokus.features.contacts.mvi.ManualContactFormData
import tech.dokus.foundation.aura.components.PPrimaryButton
import tech.dokus.foundation.aura.components.fields.PTextFieldEmail
import tech.dokus.foundation.aura.components.fields.PTextFieldPhone
import tech.dokus.foundation.aura.components.fields.PTextFieldStandard
import tech.dokus.foundation.aura.constrains.Constrains
import tech.dokus.foundation.aura.extensions.localized

/**
 * Manual step content - add contact without VAT lookup.
 */
@Composable
fun ManualStepContent(
    state: CreateContactState.ManualStep,
    onIntent: (CreateContactIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Constrains.Spacing.large)
    ) {
        // Header with back button
        ManualHeader(
            onBack = { onIntent(CreateContactIntent.BackFromManual) }
        )

        Spacer(modifier = Modifier.height(Constrains.Spacing.medium))

        // Step indicator
        StepIndicator(
            currentStep = CreateContactStep.Details,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(Constrains.Spacing.large))

        // Scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.medium)
        ) {
            // Type selector
            TypeSelector(
                selectedType = state.contactType,
                onTypeSelected = { onIntent(CreateContactIntent.ManualTypeChanged(it)) }
            )

            Spacer(modifier = Modifier.height(Constrains.Spacing.small))

            // Form fields based on type
            if (state.contactType == ClientType.Business) {
                BusinessFields(
                    formData = state.formData,
                    onFieldChanged = { field, value ->
                        onIntent(CreateContactIntent.ManualFieldChanged(field, value))
                    },
                    onCountryChanged = { onIntent(CreateContactIntent.ManualCountryChanged(it)) }
                )
            } else {
                IndividualFields(
                    formData = state.formData,
                    onFieldChanged = { field, value ->
                        onIntent(CreateContactIntent.ManualFieldChanged(field, value))
                    }
                )
            }
        }

        // Primary action button
        PPrimaryButton(
            text = if (state.isSubmitting) {
                stringResource(Res.string.contacts_creating)
            } else {
                stringResource(Res.string.contacts_create_contact)
            },
            enabled = !state.isSubmitting && isFormValid(state.contactType, state.formData),
            isLoading = state.isSubmitting,
            onClick = { onIntent(CreateContactIntent.CreateManualContact) },
            modifier = Modifier
                .fillMaxWidth()
                .height(Constrains.Height.button)
        )
    }

    // Soft duplicate dialog
    if (state.softDuplicates != null && state.softDuplicates.isNotEmpty()) {
        SoftDuplicateDialog(
            duplicates = state.softDuplicates,
            onDismiss = { onIntent(CreateContactIntent.DismissSoftDuplicates) },
            onContinue = { onIntent(CreateContactIntent.ConfirmCreateDespiteDuplicates) },
            onViewContact = { onIntent(CreateContactIntent.ViewExistingContact(it)) }
        )
    }
}

@Composable
private fun ManualHeader(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(Res.string.action_back),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = stringResource(Res.string.contacts_add_contact_manually),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun TypeSelector(
    selectedType: ClientType,
    onTypeSelected: (ClientType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Constrains.Spacing.small)
    ) {
        FilterChip(
            selected = selectedType == ClientType.Business,
            onClick = { onTypeSelected(ClientType.Business) },
            label = { Text(ClientType.Business.localized) }
        )
        FilterChip(
            selected = selectedType == ClientType.Individual,
            onClick = { onTypeSelected(ClientType.Individual) },
            label = { Text(ClientType.Individual.localized) }
        )
    }
}

@Composable
private fun BusinessFields(
    formData: ManualContactFormData,
    onFieldChanged: (String, String) -> Unit,
    onCountryChanged: (Country) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.medium)
    ) {
        PTextFieldStandard(
            fieldName = stringResource(
                Res.string.field_required,
                stringResource(Res.string.contacts_company_name)
            ),
            value = formData.companyName.value,
            error = formData.errors["companyName"],
            onValueChange = { onFieldChanged("companyName", it) },
            modifier = Modifier.fillMaxWidth()
        )

        // Country - simple text for now, could be a dropdown
        CountrySelector(
            selectedCountry = formData.country,
            onCountrySelected = onCountryChanged,
            modifier = Modifier.fillMaxWidth()
        )

        PTextFieldStandard(
            fieldName = stringResource(
                Res.string.field_optional,
                stringResource(Res.string.contacts_vat_number)
            ),
            value = formData.vatNumber.value,
            onValueChange = { onFieldChanged("vatNumber", it) },
            modifier = Modifier.fillMaxWidth()
        )

        PTextFieldEmail(
            fieldName = stringResource(
                Res.string.field_optional,
                stringResource(Res.string.contacts_email)
            ),
            value = formData.email,
            onValueChange = { onFieldChanged("email", it.value) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun IndividualFields(
    formData: ManualContactFormData,
    onFieldChanged: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.medium)
    ) {
        PTextFieldStandard(
            fieldName = stringResource(
                Res.string.field_required,
                stringResource(Res.string.contacts_full_name)
            ),
            value = formData.fullName.value,
            error = formData.errors["fullName"],
            onValueChange = { onFieldChanged("fullName", it) },
            modifier = Modifier.fillMaxWidth()
        )

        PTextFieldEmail(
            fieldName = stringResource(Res.string.contacts_email),
            value = formData.personEmail,
            error = formData.errors["contact"],
            onValueChange = { onFieldChanged("personEmail", it.value) },
            modifier = Modifier.fillMaxWidth()
        )

        PTextFieldPhone(
            fieldName = stringResource(Res.string.contacts_phone),
            value = formData.personPhone,
            error = formData.errors["contact"],
            onValueChange = { onFieldChanged("personPhone", it.value) },
            modifier = Modifier.fillMaxWidth()
        )

        formData.errors["contact"]?.let { error ->
            Text(
                text = error.localized,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
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
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Constrains.Spacing.small)
    ) {
        Country.entries.forEach { country ->
            FilterChip(
                selected = selectedCountry == country,
                onClick = { onCountrySelected(country) },
                label = { Text(country.localizedName()) }
            )
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

private fun isFormValid(type: ClientType, data: ManualContactFormData): Boolean {
    return if (type == ClientType.Business) {
        data.companyName.value.isNotBlank()
    } else {
        data.fullName.value.isNotBlank() && (data.personEmail.value.isNotBlank() || data.personPhone.value.isNotBlank())
    }
}
