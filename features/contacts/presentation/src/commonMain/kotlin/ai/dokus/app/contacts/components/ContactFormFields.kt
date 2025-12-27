package ai.dokus.app.contacts.components

import ai.dokus.app.contacts.viewmodel.ContactFormData
import ai.dokus.foundation.design.components.fields.PTextFieldPhone
import ai.dokus.foundation.design.components.fields.PTextFieldStandard
import tech.dokus.domain.PhoneNumber
import ai.dokus.foundation.domain.enums.ClientType
import ai.dokus.foundation.domain.exceptions.DokusException
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

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
        ContactFormSection(title = "Basic Information") {
            // Name (Required)
            PTextFieldStandard(
                fieldName = "Name *",
                value = formData.name,
                onValueChange = onNameChange,
                error = formData.errors["name"]?.toValidationError(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Email
            PTextFieldStandard(
                fieldName = "Email",
                value = formData.email,
                onValueChange = onEmailChange,
                error = formData.errors["email"]?.toValidationError(),
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
                fieldName = "Phone",
                value = PhoneNumber(formData.phone),
                onValueChange = { onPhoneChange(it.value) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Contact Person
            PTextFieldStandard(
                fieldName = "Contact Person",
                value = formData.contactPerson,
                onValueChange = onContactPersonChange,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Business Information Section
        ContactFormSection(title = "Business Information") {
            // Business Type
            BusinessTypeSelector(
                selectedType = formData.businessType,
                onTypeSelected = onBusinessTypeChange
            )

            Spacer(modifier = Modifier.height(12.dp))

            // VAT Number
            PTextFieldStandard(
                fieldName = "VAT Number",
                value = formData.vatNumber,
                onValueChange = onVatNumberChange,
                error = formData.errors["vatNumber"]?.toValidationError(),
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
                fieldName = "Company Number",
                value = formData.companyNumber,
                onValueChange = onCompanyNumberChange,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Address Section
        ContactFormSection(title = "Address") {
            // Address Line 1
            PTextFieldStandard(
                fieldName = "Street Address",
                value = formData.addressLine1,
                onValueChange = onAddressLine1Change,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Address Line 2
            PTextFieldStandard(
                fieldName = "Address Line 2 (optional)",
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
                    fieldName = "Postal Code",
                    value = formData.postalCode,
                    onValueChange = onPostalCodeChange,
                    modifier = Modifier.weight(1f)
                )

                PTextFieldStandard(
                    fieldName = "City",
                    value = formData.city,
                    onValueChange = onCityChange,
                    modifier = Modifier.weight(2f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Country
            PTextFieldStandard(
                fieldName = "Country",
                value = formData.country,
                onValueChange = onCountryChange,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Peppol Settings Section
        ContactFormSection(title = "Peppol Settings") {
            // Peppol Enabled Toggle
            FormField(label = "Peppol Enabled") {
                Switch(
                    checked = formData.peppolEnabled,
                    onCheckedChange = onPeppolEnabledChange
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Peppol ID (only shown/required when Peppol is enabled)
            PTextFieldStandard(
                fieldName = if (formData.peppolEnabled) "Peppol ID *" else "Peppol ID",
                value = formData.peppolId,
                onValueChange = onPeppolIdChange,
                error = formData.errors["peppolId"]?.toValidationError(),
                modifier = Modifier.fillMaxWidth()
            )

            // Peppol ID format hint
            Text(
                text = "Format: scheme:identifier (e.g., 0208:BE0123456789)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Defaults Section
        ContactFormSection(title = "Payment Defaults") {
            // Default Payment Terms
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PTextFieldStandard(
                    fieldName = "Payment Terms (days)",
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
                    fieldName = "Default VAT Rate (%)",
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
        ContactFormSection(title = "Tags") {
            PTextFieldStandard(
                fieldName = "Tags (comma-separated)",
                value = formData.tags,
                onValueChange = onTagsChange,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Enter tags separated by commas (e.g., VIP, Partner, EU)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Initial Note Section (only for create mode)
        if (showInitialNote) {
            ContactFormSection(title = "Initial Note") {
                PTextFieldStandard(
                    fieldName = "Note (optional)",
                    value = formData.initialNote,
                    onValueChange = onInitialNoteChange,
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Status Section
        ContactFormSection(title = "Status") {
            FormField(label = "Active") {
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
                            text = "Active",
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
                            text = "Inactive",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // General error message
        formData.errors["general"]?.let { error ->
            Text(
                text = error,
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

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
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = "Business Type",
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
                    text = selectedType.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                ClientType.entries.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.displayName) },
                        onClick = {
                            onTypeSelected(type)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

// ============================================================================
// EXTENSIONS
// ============================================================================

/**
 * Display name for ClientType enum.
 */
private val ClientType.displayName: String
    get() = when (this) {
        ClientType.Individual -> "Individual"
        ClientType.Business -> "Business"
        ClientType.Government -> "Government"
    }

/**
 * Convert a string error message to a DokusException for display.
 */
private fun String.toValidationError(): DokusException =
    DokusException.Validation.Generic(this)
