package tech.dokus.features.contacts.presentation.contacts.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_cancel
import tech.dokus.aura.resources.action_delete
import tech.dokus.aura.resources.contacts_delete_confirmation
import tech.dokus.aura.resources.contacts_delete_contact
import tech.dokus.aura.resources.contacts_delete_warning
import tech.dokus.aura.resources.contacts_edit_contact
import tech.dokus.aura.resources.contacts_update_mobile_hint
import tech.dokus.domain.ids.ContactId
import tech.dokus.features.contacts.mvi.ContactFormIntent
import tech.dokus.features.contacts.mvi.ContactFormState
import tech.dokus.features.contacts.presentation.contacts.components.ContactFormActionButtonsCompact
import tech.dokus.features.contacts.presentation.contacts.components.ContactFormContent
import tech.dokus.features.contacts.presentation.contacts.components.ContactFormFields
import tech.dokus.features.contacts.presentation.contacts.components.DuplicateWarningBanner
import tech.dokus.foundation.aura.components.dialog.DokusDialog
import tech.dokus.foundation.aura.components.dialog.DokusDialogAction
import tech.dokus.foundation.aura.components.text.SectionTitle
import tech.dokus.foundation.aura.constrains.Constrains
import tech.dokus.foundation.aura.local.LocalScreenSize

/**
 * Screen for editing an existing contact.
 *
 * For creating new contacts, use [CreateContactScreen] instead.
 *
 * Features:
 * - Pre-populated form with contact data
 * - Delete button with confirmation dialog
 * - Duplicate detection: Warning banner when similar contacts exist
 * - Validation: Real-time field validation with error display
 * - Responsive: Different layouts for desktop and mobile
 *
 * Pure UI: navigation and side effects are handled in the route.
 */
@Composable
internal fun ContactFormScreen(
    state: ContactFormState,
    snackbarHostState: SnackbarHostState,
    onIntent: (ContactFormIntent) -> Unit,
    onNavigateToDuplicate: (ContactId) -> Unit
) {
    val isLargeScreen = LocalScreenSize.current.isLarge
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { contentPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (state) {
                is ContactFormState.LoadingContact -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(contentPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is ContactFormState.Editing -> {
                    if (state.ui.showDeleteConfirmation) {
                        DeleteContactConfirmationDialog(
                            contactName = state.formData.name.value,
                            isDeleting = state.isDeleting,
                            onConfirm = { onIntent(ContactFormIntent.Delete) }
                        ) { onIntent(ContactFormIntent.HideDeleteConfirmation) }
                    }
                    if (isLargeScreen) {
                        DesktopFormLayout(
                            contentPadding = contentPadding,
                            state = state,
                            onBackPress = { onIntent(ContactFormIntent.Cancel) },
                            onIntent = onIntent,
                            onNavigateToDuplicate = onNavigateToDuplicate
                        )
                    } else {
                        MobileFormLayout(
                            contentPadding = contentPadding,
                            state = state,
                            onBackPress = { onIntent(ContactFormIntent.Cancel) },
                            onIntent = onIntent,
                            onNavigateToDuplicate = onNavigateToDuplicate
                        )
                    }
                }

                is ContactFormState.Error -> {
                    val editingState = ContactFormState.Editing(
                        contactId = state.contactId,
                        formData = state.formData
                    )
                    if (isLargeScreen) {
                        DesktopFormLayout(
                            contentPadding = contentPadding,
                            state = editingState,
                            onBackPress = { onIntent(ContactFormIntent.Cancel) },
                            onIntent = onIntent,
                            onNavigateToDuplicate = onNavigateToDuplicate
                        )
                    } else {
                        MobileFormLayout(
                            contentPadding = contentPadding,
                            state = editingState,
                            onBackPress = { onIntent(ContactFormIntent.Cancel) },
                            onIntent = onIntent,
                            onNavigateToDuplicate = onNavigateToDuplicate
                        )
                    }
                }
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
    state: ContactFormState.Editing,
    onBackPress: () -> Unit,
    onIntent: (ContactFormIntent) -> Unit,
    onNavigateToDuplicate: (ContactId) -> Unit
) {
    ContactFormContent(
        isEditMode = state.isEditMode,
        formData = state.formData,
        isSaving = state.isSaving,
        isDeleting = state.isDeleting,
        duplicates = state.duplicates,
        showBackButton = true,
        onBackPress = onBackPress,
        onNameChange = { onIntent(ContactFormIntent.UpdateName(it)) },
        onEmailChange = { onIntent(ContactFormIntent.UpdateEmail(it)) },
        onPhoneChange = { onIntent(ContactFormIntent.UpdatePhone(it)) },
        onContactPersonChange = { onIntent(ContactFormIntent.UpdateContactPerson(it)) },
        onVatNumberChange = { onIntent(ContactFormIntent.UpdateVatNumber(it)) },
        onCompanyNumberChange = { onIntent(ContactFormIntent.UpdateCompanyNumber(it)) },
        onBusinessTypeChange = { onIntent(ContactFormIntent.UpdateBusinessType(it)) },
        onAddressLine1Change = { onIntent(ContactFormIntent.UpdateAddressLine1(it)) },
        onAddressLine2Change = { onIntent(ContactFormIntent.UpdateAddressLine2(it)) },
        onCityChange = { onIntent(ContactFormIntent.UpdateCity(it)) },
        onPostalCodeChange = { onIntent(ContactFormIntent.UpdatePostalCode(it)) },
        onCountryChange = { onIntent(ContactFormIntent.UpdateCountry(it)) },
        onPeppolIdChange = { onIntent(ContactFormIntent.UpdatePeppolId(it)) },
        onPeppolEnabledChange = { onIntent(ContactFormIntent.UpdatePeppolEnabled(it)) },
        onDefaultPaymentTermsChange = { onIntent(ContactFormIntent.UpdateDefaultPaymentTerms(it)) },
        onDefaultVatRateChange = { onIntent(ContactFormIntent.UpdateDefaultVatRate(it)) },
        onTagsChange = { onIntent(ContactFormIntent.UpdateTags(it)) },
        onInitialNoteChange = { onIntent(ContactFormIntent.UpdateInitialNote(it)) },
        onIsActiveChange = { onIntent(ContactFormIntent.UpdateIsActive(it)) },
        onSave = { onIntent(ContactFormIntent.Save) },
        onCancel = onBackPress,
        onDelete = { onIntent(ContactFormIntent.ShowDeleteConfirmation) },
        onDismissDuplicates = { onIntent(ContactFormIntent.DismissDuplicateWarnings) },
        onMergeWithExisting = { duplicate ->
            // Navigate to the duplicate contact's details page for merging
            onNavigateToDuplicate(duplicate.contact.id)
        },
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
    state: ContactFormState.Editing,
    onBackPress: () -> Unit,
    onIntent: (ContactFormIntent) -> Unit,
    onNavigateToDuplicate: (ContactId) -> Unit
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
                text = stringResource(Res.string.contacts_edit_contact),
                onBackPress = onBackPress
            )

            // Description (shorter for mobile)
            Text(
                text = stringResource(Res.string.contacts_update_mobile_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Duplicate warning banner
            if (state.duplicates.isNotEmpty()) {
                DuplicateWarningBanner(
                    duplicates = state.duplicates,
                    onContinueAnyway = { onIntent(ContactFormIntent.DismissDuplicateWarnings) },
                    onMergeWithExisting = { duplicate ->
                        // Navigate to the duplicate contact's details page for merging
                        onNavigateToDuplicate(duplicate.contact.id)
                    },
                    onCancel = onBackPress
                )
            }

            // Form fields
            ContactFormFields(
                formData = state.formData,
                onNameChange = { onIntent(ContactFormIntent.UpdateName(it)) },
                onEmailChange = { onIntent(ContactFormIntent.UpdateEmail(it)) },
                onPhoneChange = { onIntent(ContactFormIntent.UpdatePhone(it)) },
                onContactPersonChange = { onIntent(ContactFormIntent.UpdateContactPerson(it)) },
                onVatNumberChange = { onIntent(ContactFormIntent.UpdateVatNumber(it)) },
                onCompanyNumberChange = { onIntent(ContactFormIntent.UpdateCompanyNumber(it)) },
                onBusinessTypeChange = { onIntent(ContactFormIntent.UpdateBusinessType(it)) },
                onAddressLine1Change = { onIntent(ContactFormIntent.UpdateAddressLine1(it)) },
                onAddressLine2Change = { onIntent(ContactFormIntent.UpdateAddressLine2(it)) },
                onCityChange = { onIntent(ContactFormIntent.UpdateCity(it)) },
                onPostalCodeChange = { onIntent(ContactFormIntent.UpdatePostalCode(it)) },
                onCountryChange = { onIntent(ContactFormIntent.UpdateCountry(it)) },
                onPeppolIdChange = { onIntent(ContactFormIntent.UpdatePeppolId(it)) },
                onPeppolEnabledChange = { onIntent(ContactFormIntent.UpdatePeppolEnabled(it)) },
                onDefaultPaymentTermsChange = {
                    onIntent(
                        ContactFormIntent.UpdateDefaultPaymentTerms(
                            it
                        )
                    )
                },
                onDefaultVatRateChange = { onIntent(ContactFormIntent.UpdateDefaultVatRate(it)) },
                onTagsChange = { onIntent(ContactFormIntent.UpdateTags(it)) },
                onInitialNoteChange = { onIntent(ContactFormIntent.UpdateInitialNote(it)) },
                onIsActiveChange = { onIntent(ContactFormIntent.UpdateIsActive(it)) },
                showInitialNote = false // Initial note is only for create mode (use CreateContactScreen)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Bottom action bar (fixed at bottom for mobile)
        ContactFormActionButtonsCompact(
            isEditMode = state.isEditMode,
            isSaving = state.isSaving,
            isDeleting = state.isDeleting,
            isValid = state.formData.isValid,
            onSave = { onIntent(ContactFormIntent.Save) },
            onCancel = onBackPress,
            onDelete = { onIntent(ContactFormIntent.ShowDeleteConfirmation) },
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
    DokusDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        title = stringResource(Res.string.contacts_delete_contact),
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        content = {
            Column(
                verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.small)
            ) {
                Text(
                    text = stringResource(Res.string.contacts_delete_confirmation, contactName),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(Res.string.contacts_delete_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        primaryAction = DokusDialogAction(
            text = stringResource(Res.string.action_delete),
            onClick = onConfirm,
            isLoading = isDeleting,
            isDestructive = true,
            enabled = !isDeleting
        ),
        secondaryAction = DokusDialogAction(
            text = stringResource(Res.string.action_cancel),
            onClick = onDismiss,
            enabled = !isDeleting
        ),
        dismissOnBackPress = !isDeleting,
        dismissOnClickOutside = !isDeleting
    )
}
