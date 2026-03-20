package tech.dokus.features.contacts.presentation.contacts.screen

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDateTime
import tech.dokus.domain.Name
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.features.contacts.mvi.ContactDetailsIntent
import tech.dokus.features.contacts.mvi.ContactDetailsState
import tech.dokus.features.contacts.mvi.EnrichmentSuggestion
import tech.dokus.features.contacts.presentation.contacts.components.ContactDetailsContent
import tech.dokus.features.contacts.presentation.contacts.components.ContactDetailsTopBar
import tech.dokus.features.contacts.presentation.contacts.components.ContactNoteDeleteDialog
import tech.dokus.features.contacts.presentation.contacts.components.ContactNotesPane
import tech.dokus.features.contacts.presentation.contacts.components.EnrichmentSuggestionsDialog
import tech.dokus.features.contacts.presentation.contacts.components.NotesBottomSheet
import tech.dokus.features.contacts.presentation.contacts.components.shouldShowNoteComposer
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ContactDetailsScreen(
    state: ContactDetailsState,
    showBackButton: Boolean,
    isOnline: Boolean,
    snackbarHostState: SnackbarHostState,
    onIntent: (ContactDetailsIntent) -> Unit,
    onBackClick: () -> Unit,
    onDocumentClick: (DocumentId) -> Unit,
) {
    BoxWithConstraints {
        val isDesktop = maxWidth >= 600.dp

        ContactDetailsScreenContent(
            state = state,
            showBackButton = showBackButton,
            isDesktop = isDesktop,
            isOnline = isOnline,
            snackbarHostState = snackbarHostState,
            onIntent = onIntent,
            onBackClick = onBackClick,
            onDocumentClick = onDocumentClick,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactDetailsScreenContent(
    state: ContactDetailsState,
    showBackButton: Boolean,
    isDesktop: Boolean,
    isOnline: Boolean,
    snackbarHostState: SnackbarHostState,
    onIntent: (ContactDetailsIntent) -> Unit,
    onBackClick: () -> Unit,
    onDocumentClick: (DocumentId) -> Unit,
) {
    val contactState = state.contact
    val invoiceSnapshotState = state.invoiceSnapshotState
    val peppolStatusState = state.peppolStatusState
    val notesState = state.notesState
    val enrichmentSuggestions = state.enrichmentSuggestions
    val uiState = state.uiState
    val isEditing = state.editFormData != null
    val isEmbeddedDesktop = isDesktop && !showBackButton
    val showNoteComposer = shouldShowNoteComposer(
        showAddNoteDialog = uiState.showAddNoteDialog,
        showEditNoteDialog = uiState.showEditNoteDialog
    )

    val openNotesSurface = remember(isDesktop) {
        {
            if (isDesktop) {
                onIntent(ContactDetailsIntent.ShowNotesSidePanel)
            } else {
                onIntent(ContactDetailsIntent.ShowNotesBottomSheet)
            }
        }
    }

    Scaffold(
        topBar = {
            if (!isEmbeddedDesktop) {
                ContactDetailsTopBar(
                    contactState = contactState,
                    showBackButton = showBackButton,
                    hasEnrichmentSuggestions = enrichmentSuggestions.isNotEmpty(),
                    isEditing = isEditing,
                    isSavingEdit = state.isSavingEdit,
                    onBackClick = {
                        if (isEditing) {
                            onIntent(ContactDetailsIntent.CancelEditing)
                        } else {
                            onBackClick()
                        }
                    },
                    onEditClick = { onIntent(ContactDetailsIntent.StartEditing) },
                    onSaveClick = { onIntent(ContactDetailsIntent.SaveEdit) },
                    onCancelEditClick = { onIntent(ContactDetailsIntent.CancelEditing) },
                    onEnrichmentClick = { onIntent(ContactDetailsIntent.ShowEnrichmentPanel) },
                    onMergeClick = { onIntent(ContactDetailsIntent.ShowMergeDialog) },
                    isOnline = isOnline
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { contentPadding ->
        ContactDetailsContent(
            contactState = contactState,
            invoiceSnapshotState = invoiceSnapshotState,
            peppolStatusState = peppolStatusState,
            notesState = notesState,
            isOnline = isOnline,
            contentPadding = contentPadding,
            showInlineActions = isEmbeddedDesktop,
            hasEnrichmentSuggestions = enrichmentSuggestions.isNotEmpty(),
            isEditing = isEditing,
            isSavingEdit = state.isSavingEdit,
            editFormData = state.editFormData,
            onEditFormDataChange = { onIntent(ContactDetailsIntent.UpdateEditFormData(it)) },
            onEditContact = { onIntent(ContactDetailsIntent.StartEditing) },
            onSaveEdit = { onIntent(ContactDetailsIntent.SaveEdit) },
            onCancelEdit = { onIntent(ContactDetailsIntent.CancelEditing) },
            onMergeContact = { onIntent(ContactDetailsIntent.ShowMergeDialog) },
            onShowEnrichment = { onIntent(ContactDetailsIntent.ShowEnrichmentPanel) },
            onDocumentClick = onDocumentClick,
            onAddNote = {
                openNotesSurface()
                onIntent(ContactDetailsIntent.ShowAddNoteDialog)
            },
            onEditNote = { note ->
                openNotesSurface()
                onIntent(ContactDetailsIntent.ShowEditNoteDialog(note))
            },
            onDeleteNote = { note -> onIntent(ContactDetailsIntent.ShowDeleteNoteConfirmation(note)) }
        )
    }

    if (isDesktop) {
        ContactNotesPane(
            isVisible = uiState.showNotesSidePanel,
            notesState = notesState,
            noteContent = uiState.noteContent,
            onNoteContentChange = { onIntent(ContactDetailsIntent.UpdateNoteContent(it)) },
            isSavingNote = state.isSavingNote,
            editingNote = uiState.editingNote,
            showComposer = showNoteComposer,
            onShowAddNote = { onIntent(ContactDetailsIntent.ShowAddNoteDialog) },
            onSaveNote = {
                if (uiState.showEditNoteDialog) onIntent(ContactDetailsIntent.UpdateNote)
                else onIntent(ContactDetailsIntent.AddNote)
            },
            onEditNoteClick = { onIntent(ContactDetailsIntent.ShowEditNoteDialog(it)) },
            onDeleteNoteClick = { onIntent(ContactDetailsIntent.ShowDeleteNoteConfirmation(it)) },
            onDismissComposer = {
                when {
                    uiState.showEditNoteDialog -> onIntent(ContactDetailsIntent.HideEditNoteDialog)
                    uiState.showAddNoteDialog -> onIntent(ContactDetailsIntent.HideAddNoteDialog)
                }
            },
            onDismiss = { onIntent(ContactDetailsIntent.HideNotesSidePanel) }
        )
    } else {
        NotesBottomSheet(
            isVisible = uiState.showNotesBottomSheet,
            onDismiss = { onIntent(ContactDetailsIntent.HideNotesBottomSheet) },
            notesState = notesState,
            noteContent = uiState.noteContent,
            onNoteContentChange = { onIntent(ContactDetailsIntent.UpdateNoteContent(it)) },
            isSavingNote = state.isSavingNote,
            editingNote = uiState.editingNote,
            showComposer = showNoteComposer,
            onShowAddNote = { onIntent(ContactDetailsIntent.ShowAddNoteDialog) },
            onSaveNote = {
                if (uiState.showEditNoteDialog) onIntent(ContactDetailsIntent.UpdateNote)
                else onIntent(ContactDetailsIntent.AddNote)
            },
            onEditNoteClick = { onIntent(ContactDetailsIntent.ShowEditNoteDialog(it)) },
            onDeleteNoteClick = { onIntent(ContactDetailsIntent.ShowDeleteNoteConfirmation(it)) },
            onDismissComposer = {
                when {
                    uiState.showEditNoteDialog -> onIntent(ContactDetailsIntent.HideEditNoteDialog)
                    uiState.showAddNoteDialog -> onIntent(ContactDetailsIntent.HideAddNoteDialog)
                }
            }
        )
    }

    if (uiState.showDeleteNoteConfirmation && uiState.deletingNote != null) {
        ContactNoteDeleteDialog(
            note = uiState.deletingNote,
            isDeleting = state.isDeletingNote,
            onConfirm = { onIntent(ContactDetailsIntent.DeleteNote) },
            onDismiss = { onIntent(ContactDetailsIntent.HideDeleteNoteConfirmation) }
        )
    }

    if (state.uiState.showEnrichmentPanel && state.enrichmentSuggestions.isNotEmpty()) {
        EnrichmentSuggestionsDialog(
            suggestions = enrichmentSuggestions,
            onApply = { selected ->
                onIntent(ContactDetailsIntent.ApplyEnrichmentSuggestions(selected))
            },
            onDismiss = { onIntent(ContactDetailsIntent.HideEnrichmentPanel) }
        )
    }
}

@Preview
@Composable
private fun ContactDetailsScreenPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    val mockContactId = ContactId.generate()
    val now = LocalDateTime(2026, 1, 1, 0, 0)
    TestWrapper(parameters) {
        ContactDetailsScreen(
            state = ContactDetailsState(
                contactId = mockContactId,
                contact = DokusState.success(
                    ContactDto(
                        id = mockContactId,
                        tenantId = TenantId.generate(),
                        name = Name("Acme Corp"),
                        createdAt = now,
                        updatedAt = now
                    )
                )
            ),
            showBackButton = true,
            isOnline = true,
            snackbarHostState = remember { SnackbarHostState() },
            onIntent = {},
            onBackClick = {},
            onDocumentClick = {}
        )
    }
}
