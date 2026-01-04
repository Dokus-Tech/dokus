package tech.dokus.features.contacts.presentation.contacts.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.contacts_active
import tech.dokus.aura.resources.contacts_address
import tech.dokus.aura.resources.contacts_address_line1
import tech.dokus.aura.resources.contacts_address_line2
import tech.dokus.aura.resources.contacts_basic_info
import tech.dokus.aura.resources.contacts_business_info
import tech.dokus.aura.resources.contacts_business_type
import tech.dokus.aura.resources.contacts_city
import tech.dokus.aura.resources.contacts_company_number
import tech.dokus.aura.resources.contacts_contact_person
import tech.dokus.aura.resources.contacts_country
import tech.dokus.aura.resources.contacts_default_vat_rate
import tech.dokus.aura.resources.contacts_email
import tech.dokus.aura.resources.contacts_inactive
import tech.dokus.aura.resources.contacts_initial_note
import tech.dokus.aura.resources.contacts_name
import tech.dokus.aura.resources.contacts_note
import tech.dokus.aura.resources.contacts_payment_defaults
import tech.dokus.aura.resources.contacts_payment_terms
import tech.dokus.aura.resources.contacts_peppol_enabled
import tech.dokus.aura.resources.contacts_peppol_id
import tech.dokus.aura.resources.contacts_peppol_id_hint
import tech.dokus.aura.resources.contacts_peppol_settings
import tech.dokus.aura.resources.contacts_phone
import tech.dokus.aura.resources.contacts_postal_code
import tech.dokus.aura.resources.contacts_status
import tech.dokus.aura.resources.contacts_tags
import tech.dokus.aura.resources.contacts_tags_hint
import tech.dokus.aura.resources.contacts_vat_number
import tech.dokus.aura.resources.field_optional
import tech.dokus.aura.resources.field_required
import tech.dokus.domain.enums.ClientType
import tech.dokus.features.contacts.mvi.ContactFormData
import tech.dokus.foundation.aura.components.DokusCard
import tech.dokus.foundation.aura.components.DokusCardPadding
import tech.dokus.foundation.aura.components.fields.PDropdownField
import tech.dokus.foundation.aura.components.fields.PTextFieldPhone
import tech.dokus.foundation.aura.components.fields.PTextFieldStandard
import tech.dokus.foundation.aura.extensions.localized

// ============================================================================
// CONTACT FORM FIELDS
// ============================================================================

/**
 * Reusable form fields component for creating and editing contacts.
 * Displays all contact fields with validation error states.
 */
@Composable
internal fun ContactFormFields(
    formData: ContactFormData,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onContactPersonChange: (String) -> Unit,
    onVatNumberChange: (String) -> Unit,
    onCompanyNumberChange: (String) -> Unit,
    onBusinessTypeChange: (ClientType) -> Unit,
    onAddressLine1Change: (String) -> Unit,
    onAddressLine2Change: (String) -> Unit,
    onCityChange: (String) -> Unit,
    onPostalCodeChange: (String) -> Unit,
    onCountryChange: (String) -> Unit,
    onPeppolIdChange: (String) -> Unit,
    onPeppolEnabledChange: (Boolean) -> Unit,
    onDefaultPaymentTermsChange: (Int) -> Unit,
    onDefaultVatRateChange: (String) -> Unit,
    onTagsChange: (String) -> Unit,
    onInitialNoteChange: (String) -> Unit,
    onIsActiveChange: (Boolean) -> Unit,
    showInitialNote: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Basic Information Section
        ContactFormSection(title = stringResource(Res.string.contacts_basic_info)) {
            // Name (Required)
            PTextFieldStandard(
                fieldName = stringResource(Res.string.contacts_name),
                value = formData.name.value,
                onValueChange = onNameChange,
                error = formData.errors["name"],
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Email
            PTextFieldStandard(
                fieldName = stringResource(Res.string.contacts_email),
                value = formData.email.value,
                onValueChange = onEmailChange,
                error = formData.errors["email"],
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    capitalization = KeyboardCapitalization.None,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Phone
            PTextFieldPhone(
                fieldName = stringResource(Res.string.contacts_phone),
                value = formData.phone,
                onValueChange = { onPhoneChange(it.value) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Contact Person
            PTextFieldStandard(
                fieldName = stringResource(Res.string.contacts_contact_person),
                value = formData.contactPerson,
                onValueChange = onContactPersonChange,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Business Information Section
        ContactFormSection(title = stringResource(Res.string.contacts_business_info)) {
            // Business Type
            BusinessTypeSelector(
                selectedType = formData.businessType,
                onTypeSelected = onBusinessTypeChange
            )

            Spacer(modifier = Modifier.height(12.dp))

            // VAT Number
            PTextFieldStandard(
                fieldName = stringResource(Res.string.contacts_vat_number),
                value = formData.vatNumber.value,
                onValueChange = onVatNumberChange,
                error = formData.errors["vatNumber"],
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Company Number
            PTextFieldStandard(
                fieldName = stringResource(Res.string.contacts_company_number),
                value = formData.companyNumber,
                onValueChange = onCompanyNumberChange,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Address Section
        ContactFormSection(title = stringResource(Res.string.contacts_address)) {
            // Address Line 1
            PTextFieldStandard(
                fieldName = stringResource(Res.string.contacts_address_line1),
                value = formData.addressLine1,
                onValueChange = onAddressLine1Change,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Address Line 2
            PTextFieldStandard(
                fieldName = stringResource(
                    Res.string.field_optional,
                    stringResource(Res.string.contacts_address_line2)
                ),
                value = formData.addressLine2,
                onValueChange = onAddressLine2Change,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Postal Code and City (side by side)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PTextFieldStandard(
                    fieldName = stringResource(Res.string.contacts_postal_code),
                    value = formData.postalCode,
                    onValueChange = onPostalCodeChange,
                    modifier = Modifier.weight(1f)
                )

                PTextFieldStandard(
                    fieldName = stringResource(Res.string.contacts_city),
                    value = formData.city.value,
                    onValueChange = onCityChange,
                    modifier = Modifier.weight(2f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Country
            PTextFieldStandard(
                fieldName = stringResource(Res.string.contacts_country),
                value = formData.country,
                onValueChange = onCountryChange,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Peppol Settings Section
        ContactFormSection(title = stringResource(Res.string.contacts_peppol_settings)) {
            // Peppol Enabled Toggle
            FormField(label = stringResource(Res.string.contacts_peppol_enabled)) {
                Switch(
                    checked = formData.peppolEnabled,
                    onCheckedChange = onPeppolEnabledChange
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Peppol ID (only shown/required when Peppol is enabled)
            PTextFieldStandard(
                fieldName = if (formData.peppolEnabled) {
                    stringResource(
                        Res.string.field_required,
                        stringResource(Res.string.contacts_peppol_id)
                    )
                } else {
                    stringResource(Res.string.contacts_peppol_id)
                },
                value = formData.peppolId,
                onValueChange = onPeppolIdChange,
                error = formData.errors["peppolId"],
                modifier = Modifier.fillMaxWidth()
            )

            // Peppol ID format hint
            Text(
                text = stringResource(Res.string.contacts_peppol_id_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Defaults Section
        ContactFormSection(title = stringResource(Res.string.contacts_payment_defaults)) {
            // Default Payment Terms
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PTextFieldStandard(
                    fieldName = stringResource(Res.string.contacts_payment_terms),
                    value = formData.defaultPaymentTerms.toString(),
                    onValueChange = { value ->
                        value.toIntOrNull()?.let { onDefaultPaymentTermsChange(it) }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.weight(1f)
                )

                // Default VAT Rate
                PTextFieldStandard(
                    fieldName = stringResource(Res.string.contacts_default_vat_rate),
                    value = formData.defaultVatRate,
                    onValueChange = onDefaultVatRateChange,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Tags Section
        ContactFormSection(title = stringResource(Res.string.contacts_tags)) {
            PTextFieldStandard(
                fieldName = stringResource(Res.string.contacts_tags),
                value = formData.tags,
                onValueChange = onTagsChange,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = stringResource(Res.string.contacts_tags_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Initial Note Section (only for create mode)
        if (showInitialNote) {
            ContactFormSection(title = stringResource(Res.string.contacts_initial_note)) {
                PTextFieldStandard(
                    fieldName = stringResource(
                        Res.string.field_optional,
                        stringResource(Res.string.contacts_note)
                    ),
                    value = formData.initialNote,
                    onValueChange = onInitialNoteChange,
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Status Section
        ContactFormSection(title = stringResource(Res.string.contacts_status)) {
            FormField(label = stringResource(Res.string.contacts_active)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = formData.isActive,
                            onClick = { onIsActiveChange(true) }
                        )
                        Text(
                            text = stringResource(Res.string.contacts_active),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = !formData.isActive,
                            onClick = { onIsActiveChange(false) }
                        )
                        Text(
                            text = stringResource(Res.string.contacts_inactive),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // General error message
        formData.errors["general"]?.let { error ->
            Text(
                text = error.localized,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}

// ============================================================================
// HELPER COMPONENTS
// ============================================================================

/**
 * A card section for grouping related form fields.
 */
@Composable
private fun ContactFormSection(
    title: String,
    content: @Composable () -> Unit
) {
    DokusCard(
        modifier = Modifier.fillMaxWidth(),
        padding = DokusCardPadding.Default,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            content()
        }
    }
}

/**
 * Form field with label and custom content.
 */
@Composable
private fun FormField(
    label: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        content()
    }
}

/**
 * Business type selector dropdown.
 */
@Composable
private fun BusinessTypeSelector(
    selectedType: ClientType,
    onTypeSelected: (ClientType) -> Unit,
    modifier: Modifier = Modifier
) {
    PDropdownField(
        label = stringResource(Res.string.contacts_business_type),
        value = selectedType,
        onValueChange = { type -> type?.let(onTypeSelected) },
        options = ClientType.entries,
        optionLabel = { it.localized },
        placeholder = stringResource(Res.string.contacts_business_type),
        modifier = modifier,
    )
}
