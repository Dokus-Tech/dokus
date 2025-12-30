package tech.dokus.contacts.components.create

import tech.dokus.contacts.viewmodel.CreateContactIntent
import tech.dokus.contacts.viewmodel.CreateContactState
import tech.dokus.contacts.viewmodel.ManualContactFormData
import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.action_back
import ai.dokus.app.resources.generated.contacts_add_contact_manually
import ai.dokus.app.resources.generated.contacts_business
import ai.dokus.app.resources.generated.contacts_company_name
import ai.dokus.app.resources.generated.contacts_create_contact
import ai.dokus.app.resources.generated.contacts_creating
import ai.dokus.app.resources.generated.contacts_email
import ai.dokus.app.resources.generated.contacts_full_name
import ai.dokus.app.resources.generated.contacts_individual
import ai.dokus.app.resources.generated.contacts_phone
import ai.dokus.app.resources.generated.contacts_vat_number
import ai.dokus.app.resources.generated.country_belgium
import ai.dokus.app.resources.generated.country_france
import ai.dokus.app.resources.generated.country_netherlands
import ai.dokus.app.resources.generated.field_optional
import ai.dokus.app.resources.generated.field_required
import ai.dokus.foundation.design.components.PPrimaryButton
import ai.dokus.foundation.design.components.fields.PTextFieldEmail
import ai.dokus.foundation.design.components.fields.PTextFieldPhone
import ai.dokus.foundation.design.components.fields.PTextFieldStandard
import ai.dokus.foundation.design.constrains.Constrains
import ai.dokus.foundation.design.extensions.localized
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
import tech.dokus.domain.Email
import tech.dokus.domain.PhoneNumber
import tech.dokus.domain.enums.ClientType
import tech.dokus.domain.enums.Country
import org.jetbrains.compose.resources.stringResource

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
            label = { Text(stringResource(Res.string.contacts_business)) }
        )
        FilterChip(
            selected = selectedType == ClientType.Individual,
            onClick = { onTypeSelected(ClientType.Individual) },
            label = { Text(stringResource(Res.string.contacts_individual)) }
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
            value = formData.companyName,
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
            value = formData.vatNumber,
            onValueChange = { onFieldChanged("vatNumber", it) },
            modifier = Modifier.fillMaxWidth()
        )

        PTextFieldEmail(
            fieldName = stringResource(
                Res.string.field_optional,
                stringResource(Res.string.contacts_email)
            ),
            value = Email(formData.email),
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
            value = formData.fullName,
            error = formData.errors["fullName"],
            onValueChange = { onFieldChanged("fullName", it) },
            modifier = Modifier.fillMaxWidth()
        )

        PTextFieldEmail(
            fieldName = stringResource(Res.string.contacts_email),
            value = Email(formData.personEmail),
            error = formData.errors["contact"],
            onValueChange = { onFieldChanged("personEmail", it.value) },
            modifier = Modifier.fillMaxWidth()
        )

        PTextFieldPhone(
            fieldName = stringResource(Res.string.contacts_phone),
            value = PhoneNumber(formData.personPhone),
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
        data.companyName.isNotBlank()
    } else {
        data.fullName.isNotBlank() && (data.personEmail.isNotBlank() || data.personPhone.isNotBlank())
    }
}
