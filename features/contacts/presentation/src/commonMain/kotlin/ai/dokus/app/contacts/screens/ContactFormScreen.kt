package ai.dokus.app.contacts.screens

import ai.dokus.app.contacts.components.ContactFormActionButtonsCompact
import ai.dokus.app.contacts.components.ContactFormContent
import ai.dokus.app.contacts.components.ContactFormFields
import ai.dokus.app.contacts.components.DuplicateWarningBanner
import ai.dokus.app.contacts.viewmodel.ContactFormAction
import ai.dokus.app.contacts.viewmodel.ContactFormContainer
import ai.dokus.app.contacts.viewmodel.ContactFormIntent
import ai.dokus.app.contacts.viewmodel.ContactFormState
import ai.dokus.app.contacts.viewmodel.ContactFormSuccess
import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.action_cancel
import ai.dokus.app.resources.generated.action_delete
import ai.dokus.app.resources.generated.contacts_create_success
import ai.dokus.app.resources.generated.contacts_delete_confirmation
import ai.dokus.app.resources.generated.contacts_delete_contact
import ai.dokus.app.resources.generated.contacts_delete_success
import ai.dokus.app.resources.generated.contacts_delete_warning
import ai.dokus.app.resources.generated.contacts_deleting
import ai.dokus.app.resources.generated.contacts_edit_contact
import ai.dokus.app.resources.generated.contacts_update_success
import ai.dokus.app.resources.generated.contacts_update_mobile_hint
import ai.dokus.foundation.design.components.text.SectionTitle
import ai.dokus.foundation.design.extensions.localized
import ai.dokus.foundation.design.local.LocalScreenSize
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.resources.stringResource
import org.koin.core.parameter.parametersOf
import pro.respawn.flowmvi.api.IntentReceiver
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.foundation.app.mvi.container

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
 * Navigation is handled internally using LocalNavController.
 *
 * @param contactId The ID of the contact to edit (required)
 */
@Composable
internal fun ContactFormScreen(
    contactId: ContactId,
    container: ContactFormContainer = container {
        parametersOf(
            ContactFormContainer.Companion.Params(contactId)
        )
    }
) {
    val navController = LocalNavController.current
    val isLargeScreen = LocalScreenSize.current.isLarge
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingSuccess by remember { mutableStateOf<ContactFormSuccess?>(null) }
    var pendingError by remember { mutableStateOf<DokusException?>(null) }

    val successMessage = pendingSuccess?.let { success ->
        when (success) {
            ContactFormSuccess.Created -> stringResource(Res.string.contacts_create_success)
            ContactFormSuccess.Updated -> stringResource(Res.string.contacts_update_success)
            ContactFormSuccess.Deleted -> stringResource(Res.string.contacts_delete_success)
        }
    }
    val errorMessage = pendingError?.localized

    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            snackbarHostState.showSnackbar(successMessage)
            pendingSuccess = null
        }
    }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            pendingError = null
        }
    }

    with(container.store) {
        val state by subscribe { action ->
            when (action) {
                ContactFormAction.NavigateBack -> navController.popBackStack()
                is ContactFormAction.NavigateToContact -> {
                    // Navigate back to the contact list - the contact will be visible there
                    navController.popBackStack()
                }

                is ContactFormAction.ShowError -> {
                    pendingError = action.error
                }

                is ContactFormAction.ShowSuccess -> {
                    pendingSuccess = action.success
                }

                is ContactFormAction.ShowFieldError -> {
                    // Field errors are shown inline in the form
                }
            }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background
        ) { contentPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                when (val state = state) {
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
                            with(container.store) {
                                DeleteContactConfirmationDialog(
                                    contactName = state.formData.name,
                                    isDeleting = state.isDeleting,
                                    onConfirm = { intent(ContactFormIntent.Delete) }
                                ) { intent(ContactFormIntent.HideDeleteConfirmation) }
                            }
                        }
                        if (isLargeScreen) {
                            DesktopFormLayout(
                                contentPadding = contentPadding,
                                state = state,
                                onBackPress = { intent(ContactFormIntent.Cancel) }
                            )
                        } else {
                            MobileFormLayout(
                                contentPadding = contentPadding,
                                state = state,
                                onBackPress = { intent(ContactFormIntent.Cancel) }
                            )
                        }
                    }

                    is ContactFormState.Error -> {
                        val editingState = ContactFormState.Editing(
                            contactId = state.contactId,
                            formData = state.formData
                        )
                        with(container.store) {
                            if (isLargeScreen) {
                                DesktopFormLayout(
                                    contentPadding = contentPadding,
                                    state = editingState,
                                    onBackPress = { intent(ContactFormIntent.Cancel) }
                                )
                            } else {
                                MobileFormLayout(
                                    contentPadding = contentPadding,
                                    state = editingState,
                                    onBackPress = { intent(ContactFormIntent.Cancel) }
                                )
                            }
                        }
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
private fun IntentReceiver<ContactFormIntent>.DesktopFormLayout(
    contentPadding: PaddingValues,
    state: ContactFormState.Editing,
    onBackPress: () -> Unit
) {
    val navController = LocalNavController.current

    ContactFormContent(
        isEditMode = state.isEditMode,
        formData = state.formData,
        isSaving = state.isSaving,
        isDeleting = state.isDeleting,
        duplicates = state.duplicates,
        showBackButton = true,
        onBackPress = onBackPress,
        onNameChange = { intent(ContactFormIntent.UpdateName(it)) },
        onEmailChange = { intent(ContactFormIntent.UpdateEmail(it)) },
        onPhoneChange = { intent(ContactFormIntent.UpdatePhone(it)) },
        onContactPersonChange = { intent(ContactFormIntent.UpdateContactPerson(it)) },
        onVatNumberChange = { intent(ContactFormIntent.UpdateVatNumber(it)) },
        onCompanyNumberChange = { intent(ContactFormIntent.UpdateCompanyNumber(it)) },
        onBusinessTypeChange = { intent(ContactFormIntent.UpdateBusinessType(it)) },
        onAddressLine1Change = { intent(ContactFormIntent.UpdateAddressLine1(it)) },
        onAddressLine2Change = { intent(ContactFormIntent.UpdateAddressLine2(it)) },
        onCityChange = { intent(ContactFormIntent.UpdateCity(it)) },
        onPostalCodeChange = { intent(ContactFormIntent.UpdatePostalCode(it)) },
        onCountryChange = { intent(ContactFormIntent.UpdateCountry(it)) },
        onPeppolIdChange = { intent(ContactFormIntent.UpdatePeppolId(it)) },
        onPeppolEnabledChange = { intent(ContactFormIntent.UpdatePeppolEnabled(it)) },
        onDefaultPaymentTermsChange = { intent(ContactFormIntent.UpdateDefaultPaymentTerms(it)) },
        onDefaultVatRateChange = { intent(ContactFormIntent.UpdateDefaultVatRate(it)) },
        onTagsChange = { intent(ContactFormIntent.UpdateTags(it)) },
        onInitialNoteChange = { intent(ContactFormIntent.UpdateInitialNote(it)) },
        onIsActiveChange = { intent(ContactFormIntent.UpdateIsActive(it)) },
        onSave = { intent(ContactFormIntent.Save) },
        onCancel = onBackPress,
        onDelete = { intent(ContactFormIntent.ShowDeleteConfirmation) },
        onDismissDuplicates = { intent(ContactFormIntent.DismissDuplicateWarnings) },
        onMergeWithExisting = { duplicate ->
            // Navigate to the duplicate contact's details page for merging
            navController.navigateTo(ContactsDestination.ContactDetails(duplicate.contact.id.toString()))
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
private fun IntentReceiver<ContactFormIntent>.MobileFormLayout(
    contentPadding: PaddingValues,
    state: ContactFormState.Editing,
    onBackPress: () -> Unit
) {
    val navController = LocalNavController.current

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
                    onContinueAnyway = { intent(ContactFormIntent.DismissDuplicateWarnings) },
                    onMergeWithExisting = { duplicate ->
                        // Navigate to the duplicate contact's details page for merging
                        navController.navigateTo(
                            ContactsDestination.ContactDetails(duplicate.contact.id.toString())
                        )
                    },
                    onCancel = onBackPress
                )
            }

            // Form fields
            ContactFormFields(
                formData = state.formData,
                onNameChange = { intent(ContactFormIntent.UpdateName(it)) },
                onEmailChange = { intent(ContactFormIntent.UpdateEmail(it)) },
                onPhoneChange = { intent(ContactFormIntent.UpdatePhone(it)) },
                onContactPersonChange = { intent(ContactFormIntent.UpdateContactPerson(it)) },
                onVatNumberChange = { intent(ContactFormIntent.UpdateVatNumber(it)) },
                onCompanyNumberChange = { intent(ContactFormIntent.UpdateCompanyNumber(it)) },
                onBusinessTypeChange = { intent(ContactFormIntent.UpdateBusinessType(it)) },
                onAddressLine1Change = { intent(ContactFormIntent.UpdateAddressLine1(it)) },
                onAddressLine2Change = { intent(ContactFormIntent.UpdateAddressLine2(it)) },
                onCityChange = { intent(ContactFormIntent.UpdateCity(it)) },
                onPostalCodeChange = { intent(ContactFormIntent.UpdatePostalCode(it)) },
                onCountryChange = { intent(ContactFormIntent.UpdateCountry(it)) },
                onPeppolIdChange = { intent(ContactFormIntent.UpdatePeppolId(it)) },
                onPeppolEnabledChange = { intent(ContactFormIntent.UpdatePeppolEnabled(it)) },
                onDefaultPaymentTermsChange = {
                    intent(
                        ContactFormIntent.UpdateDefaultPaymentTerms(
                            it
                        )
                    )
                },
                onDefaultVatRateChange = { intent(ContactFormIntent.UpdateDefaultVatRate(it)) },
                onTagsChange = { intent(ContactFormIntent.UpdateTags(it)) },
                onInitialNoteChange = { intent(ContactFormIntent.UpdateInitialNote(it)) },
                onIsActiveChange = { intent(ContactFormIntent.UpdateIsActive(it)) },
                showInitialNote = false  // Initial note is only for create mode (use CreateContactScreen)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Bottom action bar (fixed at bottom for mobile)
        ContactFormActionButtonsCompact(
            isEditMode = state.isEditMode,
            isSaving = state.isSaving,
            isDeleting = state.isDeleting,
            isValid = state.formData.isValid,
            onSave = { intent(ContactFormIntent.Save) },
            onCancel = onBackPress,
            onDelete = { intent(ContactFormIntent.ShowDeleteConfirmation) },
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
                text = stringResource(Res.string.contacts_delete_contact),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(Res.string.contacts_delete_confirmation, contactName),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.contacts_delete_warning),
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
                        text = stringResource(Res.string.contacts_deleting),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                TextButton(onClick = onConfirm) {
                    Text(
                        text = stringResource(Res.string.action_delete),
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
                        text = stringResource(Res.string.action_cancel),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    )
}
