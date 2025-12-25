package ai.dokus.app.contacts.components

import ai.dokus.app.contacts.viewmodel.ContactFormData
import ai.dokus.app.contacts.viewmodel.PotentialDuplicate
import ai.dokus.foundation.design.components.PButton
import ai.dokus.foundation.design.components.PButtonVariant
import ai.dokus.foundation.design.components.POutlinedButton
import ai.dokus.foundation.design.components.text.SectionTitle
import ai.dokus.foundation.domain.enums.ClientType
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// ============================================================================
// CONTACT FORM CONTENT
// ============================================================================

/**
 * Reusable contact form content component that works in both full-screen and pane modes.
 *
 * This component contains the complete form UI including:
 * - Header with title and back button
 * - Description text
 * - Duplicate warning banner (if applicable)
 * - Form fields (ContactFormFields)
 * - Action buttons (Save, Cancel, Delete)
 *
 * It can be embedded in:
 * - Full-screen layouts (ContactFormScreen for mobile)
 * - Side pane layouts (ContactFormPane for desktop)
 *
 * @param isEditMode Whether the form is in edit mode (vs create mode)
 * @param formData Current form data containing field values and validation
 * @param isSaving Whether save operation is in progress
 * @param isDeleting Whether delete operation is in progress
 * @param duplicates List of potential duplicate contacts detected
 * @param showBackButton Whether to show the back button in the header
 * @param onBackPress Callback when back button is pressed
 * @param onSave Callback when save button is pressed
 * @param onCancel Callback when cancel button is pressed
 * @param onDelete Callback when delete button is pressed (edit mode only)
 * @param onDismissDuplicates Callback when user dismisses duplicate warnings
 * @param onMergeWithExisting Callback when user chooses to merge with an existing contact
 * @param modifier Modifier for the root component
 */
@Composable
internal fun ContactFormContent(
    isEditMode: Boolean,
    formData: ContactFormData,
    isSaving: Boolean,
    isDeleting: Boolean,
    duplicates: List<PotentialDuplicate>,
    showBackButton: Boolean = true,
    onBackPress: () -> Unit,
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
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onDismissDuplicates: () -> Unit,
    onMergeWithExisting: (PotentialDuplicate) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 800.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Header with back button
            SectionTitle(
                text = if (isEditMode) "Edit Contact" else "Create Contact",
                onBackPress = if (showBackButton) onBackPress else null
            )

            // Description
            Text(
                text = if (isEditMode) {
                    "Update contact information. Changes are saved when you click Save."
                } else {
                    "Fill in the contact details below. Required fields are marked with *."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Duplicate warning banner
            if (duplicates.isNotEmpty()) {
                DuplicateWarningBanner(
                    duplicates = duplicates,
                    onContinueAnyway = onDismissDuplicates,
                    onMergeWithExisting = onMergeWithExisting,
                    onCancel = onCancel
                )
            }

            // Form fields
            ContactFormFields(
                formData = formData,
                onNameChange = onNameChange,
                onEmailChange = onEmailChange,
                onPhoneChange = onPhoneChange,
                onContactPersonChange = onContactPersonChange,
                onVatNumberChange = onVatNumberChange,
                onCompanyNumberChange = onCompanyNumberChange,
                onBusinessTypeChange = onBusinessTypeChange,
                onAddressLine1Change = onAddressLine1Change,
                onAddressLine2Change = onAddressLine2Change,
                onCityChange = onCityChange,
                onPostalCodeChange = onPostalCodeChange,
                onCountryChange = onCountryChange,
                onPeppolIdChange = onPeppolIdChange,
                onPeppolEnabledChange = onPeppolEnabledChange,
                onDefaultPaymentTermsChange = onDefaultPaymentTermsChange,
                onDefaultVatRateChange = onDefaultVatRateChange,
                onTagsChange = onTagsChange,
                onInitialNoteChange = onInitialNoteChange,
                onIsActiveChange = onIsActiveChange,
                showInitialNote = !isEditMode
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons
            ContactFormActionButtons(
                isEditMode = isEditMode,
                isSaving = isSaving,
                isDeleting = isDeleting,
                isValid = formData.isValid,
                onSave = onSave,
                onCancel = onCancel,
                onDelete = onDelete
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * Compact variant of ContactFormContent for use in panes with limited width.
 * Uses a more compact layout suitable for side panels.
 */
@Composable
internal fun ContactFormContentCompact(
    isEditMode: Boolean,
    formData: ContactFormData,
    isSaving: Boolean,
    isDeleting: Boolean,
    duplicates: List<PotentialDuplicate>,
    showBackButton: Boolean = false,
    onBackPress: () -> Unit,
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
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onDismissDuplicates: () -> Unit,
    onMergeWithExisting: (PotentialDuplicate) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header (optional back button for pane mode)
        if (showBackButton) {
            SectionTitle(
                text = if (isEditMode) "Edit Contact" else "Create Contact",
                onBackPress = onBackPress
            )
        }

        // Description (shorter for compact mode)
        Text(
            text = if (isEditMode) {
                "Update contact information."
            } else {
                "Required fields are marked with *."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Duplicate warning banner
        if (duplicates.isNotEmpty()) {
            DuplicateWarningBanner(
                duplicates = duplicates,
                onContinueAnyway = onDismissDuplicates,
                onMergeWithExisting = onMergeWithExisting,
                onCancel = onCancel
            )
        }

        // Form fields
        ContactFormFields(
            formData = formData,
            onNameChange = onNameChange,
            onEmailChange = onEmailChange,
            onPhoneChange = onPhoneChange,
            onContactPersonChange = onContactPersonChange,
            onVatNumberChange = onVatNumberChange,
            onCompanyNumberChange = onCompanyNumberChange,
            onBusinessTypeChange = onBusinessTypeChange,
            onAddressLine1Change = onAddressLine1Change,
            onAddressLine2Change = onAddressLine2Change,
            onCityChange = onCityChange,
            onPostalCodeChange = onPostalCodeChange,
            onCountryChange = onCountryChange,
            onPeppolIdChange = onPeppolIdChange,
            onPeppolEnabledChange = onPeppolEnabledChange,
            onDefaultPaymentTermsChange = onDefaultPaymentTermsChange,
            onDefaultVatRateChange = onDefaultVatRateChange,
            onTagsChange = onTagsChange,
            onInitialNoteChange = onInitialNoteChange,
            onIsActiveChange = onIsActiveChange,
            showInitialNote = !isEditMode
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Action buttons (compact layout)
        ContactFormActionButtonsCompact(
            isEditMode = isEditMode,
            isSaving = isSaving,
            isDeleting = isDeleting,
            isValid = formData.isValid,
            onSave = onSave,
            onCancel = onCancel,
            onDelete = onDelete
        )
    }
}

// ============================================================================
// ACTION BUTTONS
// ============================================================================

/**
 * Action buttons for the form (standard layout).
 * Delete button on left (edit mode), Save/Cancel on right.
 */
@Composable
internal fun ContactFormActionButtons(
    isEditMode: Boolean,
    isSaving: Boolean,
    isDeleting: Boolean,
    isValid: Boolean,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Delete button (edit mode only, left side)
        if (isEditMode) {
            POutlinedButton(
                text = if (isDeleting) "Deleting..." else "Delete Contact",
                onClick = onDelete,
                enabled = !isSaving && !isDeleting,
                isLoading = isDeleting
            )
        } else {
            // Empty spacer to maintain layout
            Spacer(modifier = Modifier.weight(1f))
        }

        // Save/Cancel buttons (right side)
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PButton(
                text = "Cancel",
                variant = PButtonVariant.Outline,
                onClick = onCancel,
                isEnabled = !isSaving && !isDeleting
            )

            PButton(
                text = if (isSaving) "Saving..." else "Save Contact",
                variant = PButtonVariant.Default,
                onClick = onSave,
                isEnabled = isValid && !isSaving && !isDeleting,
                isLoading = isSaving
            )
        }
    }
}

/**
 * Compact action buttons for the form (row layout for mobile/pane).
 * All buttons in a single row with equal weights.
 */
@Composable
internal fun ContactFormActionButtonsCompact(
    isEditMode: Boolean,
    isSaving: Boolean,
    isDeleting: Boolean,
    isValid: Boolean,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Delete button (edit mode only, left-aligned)
        if (isEditMode) {
            POutlinedButton(
                text = if (isDeleting) "Deleting..." else "Delete",
                onClick = onDelete,
                enabled = !isSaving && !isDeleting,
                isLoading = isDeleting,
                modifier = Modifier.weight(1f)
            )
        }

        // Cancel button
        PButton(
            text = "Cancel",
            variant = PButtonVariant.Outline,
            onClick = onCancel,
            isEnabled = !isSaving && !isDeleting,
            modifier = Modifier.weight(1f)
        )

        // Save button
        PButton(
            text = if (isSaving) "Saving..." else "Save",
            variant = PButtonVariant.Default,
            onClick = onSave,
            isEnabled = isValid && !isSaving && !isDeleting,
            isLoading = isSaving,
            modifier = Modifier.weight(1f)
        )
    }
}
