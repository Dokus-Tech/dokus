package ai.dokus.app.contacts.screens

import ai.dokus.app.contacts.components.ContactFormActionButtonsCompact
import ai.dokus.app.contacts.components.ContactFormContent
import ai.dokus.app.contacts.components.ContactFormFields
import ai.dokus.app.contacts.components.DuplicateWarningBanner
import ai.dokus.app.contacts.viewmodel.ContactFormViewModel
import ai.dokus.app.contacts.viewmodel.DuplicateContact
import ai.dokus.foundation.design.components.text.SectionTitle
import ai.dokus.foundation.design.local.LocalScreenSize
import ai.dokus.foundation.domain.ids.ContactId
import ai.dokus.foundation.navigation.destinations.ContactsDestination
import ai.dokus.foundation.navigation.local.LocalNavController
import ai.dokus.foundation.navigation.navigateTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import tech.dokus.foundation.app.state.isLoading
import tech.dokus.foundation.app.state.isSuccess

/**
 * Screen for creating a new contact or editing an existing contact.
 *
 * Features:
 * - Create mode: Empty form for new contact with initial note field
 * - Edit mode: Pre-populated form with contact data, delete button
 * - Duplicate detection: Warning banner when similar contacts exist
 * - Validation: Real-time field validation with error display
 * - Responsive: Different layouts for desktop and mobile
 *
 * Navigation is handled internally using LocalNavController.
 *
 * @param contactId If provided, opens in edit mode; otherwise opens in create mode
 */
@Composable
internal fun ContactFormScreen(
    contactId: ContactId? = null,
    viewModel: ContactFormViewModel = koinViewModel()
) {
    val navController = LocalNavController.current
    val isLargeScreen = LocalScreenSize.current.isLarge

    val state by viewModel.state.collectAsState()
    val formState by viewModel.formState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val duplicates by viewModel.duplicates.collectAsState()
    val savedContactId by viewModel.savedContactId.collectAsState()

    val isEditMode = uiState.isEditMode

    // Initialize form based on mode
    LaunchedEffect(contactId) {
        if (contactId != null) {
            viewModel.initForEdit(contactId)
        } else {
            viewModel.initForCreate()
        }
    }

    // Navigate when contact is saved successfully
    LaunchedEffect(savedContactId, state) {
        if (savedContactId != null && state.isSuccess()) {
            // Navigate back - the contact list will refresh and show the new/updated contact
            navController.popBackStack()
        }
    }

    // Handle deletion completion - when deleting finishes and state becomes idle, navigate back
    LaunchedEffect(state, uiState.isContactDeleted) {
        if (uiState.isContactDeleted) {
            // Contact was deleted successfully, navigate back to contacts list
            navController.popBackStack()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { contentPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Loading state for edit mode (loading existing contact)
            if (isEditMode && state.isLoading() && formState.name.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                if (isLargeScreen) {
                    DesktopFormLayout(
                        contentPadding = contentPadding,
                        isEditMode = isEditMode,
                        formState = formState,
                        duplicates = duplicates,
                        onBackPress = { navController.popBackStack() },
                        onNameChange = viewModel::updateName,
                        onEmailChange = viewModel::updateEmail,
                        onPhoneChange = viewModel::updatePhone,
                        onContactPersonChange = viewModel::updateContactPerson,
                        onVatNumberChange = viewModel::updateVatNumber,
                        onCompanyNumberChange = viewModel::updateCompanyNumber,
                        onBusinessTypeChange = viewModel::updateBusinessType,
                        onAddressLine1Change = viewModel::updateAddressLine1,
                        onAddressLine2Change = viewModel::updateAddressLine2,
                        onCityChange = viewModel::updateCity,
                        onPostalCodeChange = viewModel::updatePostalCode,
                        onCountryChange = viewModel::updateCountry,
                        onPeppolIdChange = viewModel::updatePeppolId,
                        onPeppolEnabledChange = viewModel::updatePeppolEnabled,
                        onDefaultPaymentTermsChange = viewModel::updateDefaultPaymentTerms,
                        onDefaultVatRateChange = viewModel::updateDefaultVatRate,
                        onTagsChange = viewModel::updateTags,
                        onInitialNoteChange = viewModel::updateInitialNote,
                        onIsActiveChange = viewModel::updateIsActive,
                        onSave = viewModel::save,
                        onCancel = { navController.popBackStack() },
                        onDelete = viewModel::showDeleteConfirmation,
                        onDismissDuplicates = viewModel::dismissDuplicateWarnings,
                        onMergeWithExisting = { duplicate ->
                            // Navigate to the duplicate contact's details page for merging
                            navController.navigateTo(
                                ContactsDestination.ContactDetails(duplicate.contact.id.toString())
                            )
                        }
                    )
                } else {
                    MobileFormLayout(
                        contentPadding = contentPadding,
                        isEditMode = isEditMode,
                        formState = formState,
                        duplicates = duplicates,
                        onBackPress = { navController.popBackStack() },
                        onNameChange = viewModel::updateName,
                        onEmailChange = viewModel::updateEmail,
                        onPhoneChange = viewModel::updatePhone,
                        onContactPersonChange = viewModel::updateContactPerson,
                        onVatNumberChange = viewModel::updateVatNumber,
                        onCompanyNumberChange = viewModel::updateCompanyNumber,
                        onBusinessTypeChange = viewModel::updateBusinessType,
                        onAddressLine1Change = viewModel::updateAddressLine1,
                        onAddressLine2Change = viewModel::updateAddressLine2,
                        onCityChange = viewModel::updateCity,
                        onPostalCodeChange = viewModel::updatePostalCode,
                        onCountryChange = viewModel::updateCountry,
                        onPeppolIdChange = viewModel::updatePeppolId,
                        onPeppolEnabledChange = viewModel::updatePeppolEnabled,
                        onDefaultPaymentTermsChange = viewModel::updateDefaultPaymentTerms,
                        onDefaultVatRateChange = viewModel::updateDefaultVatRate,
                        onTagsChange = viewModel::updateTags,
                        onInitialNoteChange = viewModel::updateInitialNote,
                        onIsActiveChange = viewModel::updateIsActive,
                        onSave = viewModel::save,
                        onCancel = { navController.popBackStack() },
                        onDelete = viewModel::showDeleteConfirmation,
                        onDismissDuplicates = viewModel::dismissDuplicateWarnings,
                        onMergeWithExisting = { duplicate ->
                            // Navigate to the duplicate contact's details page for merging
                            navController.navigateTo(
                                ContactsDestination.ContactDetails(duplicate.contact.id.toString())
                            )
                        }
                    )
                }
            }

            // Delete confirmation dialog
            if (uiState.showDeleteConfirmation) {
                DeleteContactConfirmationDialog(
                    contactName = formState.name,
                    isDeleting = formState.isDeleting,
                    onConfirm = {
                        viewModel.deleteContact()
                        // Navigate back will happen via LaunchedEffect when deletion completes
                    },
                    onDismiss = viewModel::hideDeleteConfirmation
                )
            }
        }
    }
}

/**
 * Desktop layout for the contact form.
 * Uses ContactFormContent with additional horizontal padding for desktop.
 */
@Composable
private fun DesktopFormLayout(
    contentPadding: PaddingValues,
    isEditMode: Boolean,
    formState: ai.dokus.app.contacts.viewmodel.ContactFormState,
    duplicates: List<DuplicateContact>,
    onBackPress: () -> Unit,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onContactPersonChange: (String) -> Unit,
    onVatNumberChange: (String) -> Unit,
    onCompanyNumberChange: (String) -> Unit,
    onBusinessTypeChange: (ai.dokus.foundation.domain.enums.ClientType) -> Unit,
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
    onMergeWithExisting: (DuplicateContact) -> Unit
) {
    ContactFormContent(
        isEditMode = isEditMode,
        formState = formState,
        duplicates = duplicates,
        showBackButton = true,
        onBackPress = onBackPress,
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
        onSave = onSave,
        onCancel = onCancel,
        onDelete = onDelete,
        onDismissDuplicates = onDismissDuplicates,
        onMergeWithExisting = onMergeWithExisting,
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 16.dp)
    )
}

/**
 * Mobile layout for the contact form.
 * Full-width content with fixed bottom action bar.
 */
@Composable
private fun MobileFormLayout(
    contentPadding: PaddingValues,
    isEditMode: Boolean,
    formState: ai.dokus.app.contacts.viewmodel.ContactFormState,
    duplicates: List<DuplicateContact>,
    onBackPress: () -> Unit,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onContactPersonChange: (String) -> Unit,
    onVatNumberChange: (String) -> Unit,
    onCompanyNumberChange: (String) -> Unit,
    onBusinessTypeChange: (ai.dokus.foundation.domain.enums.ClientType) -> Unit,
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
    onMergeWithExisting: (DuplicateContact) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        // Scrollable form content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Header with back button
            SectionTitle(
                text = if (isEditMode) "Edit Contact" else "Create Contact",
                onBackPress = onBackPress
            )

            // Description (shorter for mobile)
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
                formState = formState,
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
        }

        // Bottom action bar (fixed at bottom for mobile)
        ContactFormActionButtonsCompact(
            isEditMode = isEditMode,
            isSaving = formState.isSaving,
            isDeleting = formState.isDeleting,
            isValid = formState.isValid,
            onSave = onSave,
            onCancel = onCancel,
            onDelete = onDelete,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}

/**
 * Confirmation dialog for deleting a contact.
 */
@Composable
private fun DeleteContactConfirmationDialog(
    contactName: String,
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = "Delete Contact",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column {
                Text(
                    text = "Are you sure you want to delete \"$contactName\"?",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This action cannot be undone. Any associated invoices, bills, and expenses will lose their contact reference.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            if (isDeleting) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(16.dp).width(16.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "Deleting...",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                TextButton(onClick = onConfirm) {
                    Text(
                        text = "Delete",
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        dismissButton = {
            if (!isDeleting) {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = "Cancel",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    )
}
