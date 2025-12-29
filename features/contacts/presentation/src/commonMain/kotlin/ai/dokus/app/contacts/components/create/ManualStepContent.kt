package ai.dokus.app.contacts.components.create

import ai.dokus.app.contacts.viewmodel.CreateContactIntent
import ai.dokus.app.contacts.viewmodel.CreateContactState
import ai.dokus.app.contacts.viewmodel.ManualContactFormData
import ai.dokus.app.contacts.viewmodel.SoftDuplicateUi
import ai.dokus.foundation.design.components.PPrimaryButton
import ai.dokus.foundation.design.components.fields.PTextFieldEmail
import ai.dokus.foundation.design.components.fields.PTextFieldPhone
import ai.dokus.foundation.design.components.fields.PTextFieldStandard
import ai.dokus.foundation.design.constrains.Constrains
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
            text = if (state.isSubmitting) "Creating..." else "Create Contact",
            enabled = !state.isSubmitting && isFormValid(state.contactType, state.formData),
            loading = state.isSubmitting,
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
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = "Add Contact Manually",
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
            label = { Text("Business") }
        )
        FilterChip(
            selected = selectedType == ClientType.Individual,
            onClick = { onTypeSelected(ClientType.Individual) },
            label = { Text("Individual") }
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
            fieldName = "Company Name *",
            value = formData.companyName,
            error = formData.errors["companyName"]?.let {
                tech.dokus.domain.exceptions.DokusException.Validation.RequiredFieldMissing
            },
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
            fieldName = "VAT Number (optional)",
            value = formData.vatNumber,
            onValueChange = { onFieldChanged("vatNumber", it) },
            modifier = Modifier.fillMaxWidth()
        )

        PTextFieldEmail(
            fieldName = "Email (optional)",
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
            fieldName = "Full Name *",
            value = formData.fullName,
            error = formData.errors["fullName"]?.let {
                tech.dokus.domain.exceptions.DokusException.Validation.RequiredFieldMissing
            },
            onValueChange = { onFieldChanged("fullName", it) },
            modifier = Modifier.fillMaxWidth()
        )

        PTextFieldEmail(
            fieldName = "Email",
            value = Email(formData.personEmail),
            error = formData.errors["contact"]?.let {
                tech.dokus.domain.exceptions.DokusException.Validation.RequiredFieldMissing
            },
            onValueChange = { onFieldChanged("personEmail", it.value) },
            modifier = Modifier.fillMaxWidth()
        )

        PTextFieldPhone(
            fieldName = "Phone",
            value = PhoneNumber(formData.personPhone),
            error = formData.errors["contact"]?.let {
                tech.dokus.domain.exceptions.DokusException.Validation.RequiredFieldMissing
            },
            onValueChange = { onFieldChanged("personPhone", it.value) },
            modifier = Modifier.fillMaxWidth()
        )

        if (formData.errors["contact"] != null) {
            Text(
                text = "Email or phone is required",
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
                label = { Text(country.dbValue) }
            )
        }
    }
}

private fun isFormValid(type: ClientType, data: ManualContactFormData): Boolean {
    return if (type == ClientType.Business) {
        data.companyName.isNotBlank()
    } else {
        data.fullName.isNotBlank() && (data.personEmail.isNotBlank() || data.personPhone.isNotBlank())
    }
}
