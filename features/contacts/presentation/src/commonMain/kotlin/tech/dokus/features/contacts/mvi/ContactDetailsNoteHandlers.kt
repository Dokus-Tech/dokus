@file:Suppress(
    "TooGenericExceptionCaught", // Network errors need catch-all
)

package tech.dokus.features.contacts.mvi

import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.model.contact.ContactNoteDto
import tech.dokus.domain.model.contact.CreateContactNoteRequest
import tech.dokus.domain.model.contact.UpdateContactNoteRequest
import tech.dokus.features.contacts.usecases.CreateContactNoteUseCase
import tech.dokus.features.contacts.usecases.DeleteContactNoteUseCase
import tech.dokus.features.contacts.usecases.ListContactNotesUseCase
import tech.dokus.features.contacts.usecases.UpdateContactNoteUseCase
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.app.state.isSuccess
import tech.dokus.foundation.platform.Logger

/**
 * Handles all note-related intents for the Contact Details screen.
 *
 * Covers dialog/panel visibility, content editing, and CRUD operations.
 */
internal class ContactDetailsNoteHandlers(
    private val listContactNotes: ListContactNotesUseCase,
    private val createContactNote: CreateContactNoteUseCase,
    private val updateContactNote: UpdateContactNoteUseCase,
    private val deleteContactNote: DeleteContactNoteUseCase,
) {

    private val logger = Logger.forClass<ContactDetailsNoteHandlers>()

    // ========================================================================
    // DIALOG HANDLERS
    // ========================================================================

    suspend fun ContactDetailsCtx.handleShowAddNoteDialog() {
        updateState {
            copy(uiState = uiState.resetNoteTransientState().copy(showAddNoteDialog = true))
        }
    }

    suspend fun ContactDetailsCtx.handleHideAddNoteDialog() {
        updateState {
            copy(
                uiState = uiState.copy(
                    showAddNoteDialog = false,
                    noteContent = ""
                )
            )
        }
    }

    suspend fun ContactDetailsCtx.handleShowEditNoteDialog(note: ContactNoteDto) {
        updateState {
            copy(
                uiState = uiState.resetNoteTransientState().copy(
                    showEditNoteDialog = true,
                    editingNote = note,
                    noteContent = note.content,
                )
            )
        }
    }

    suspend fun ContactDetailsCtx.handleHideEditNoteDialog() {
        updateState {
            copy(
                uiState = uiState.copy(
                    showEditNoteDialog = false,
                    editingNote = null,
                    noteContent = ""
                )
            )
        }
    }

    suspend fun ContactDetailsCtx.handleUpdateNoteContent(content: String) {
        updateState {
            copy(uiState = uiState.copy(noteContent = content))
        }
    }

    suspend fun ContactDetailsCtx.handleShowDeleteNoteConfirmation(note: ContactNoteDto) {
        updateState {
            copy(
                uiState = uiState.resetNoteTransientState().copy(
                    showDeleteNoteConfirmation = true,
                    deletingNote = note,
                )
            )
        }
    }

    suspend fun ContactDetailsCtx.handleHideDeleteNoteConfirmation() {
        updateState {
            copy(
                uiState = uiState.copy(
                    showDeleteNoteConfirmation = false,
                    deletingNote = null
                )
            )
        }
    }

    // ========================================================================
    // PANEL / SHEET HANDLERS
    // ========================================================================

    suspend fun ContactDetailsCtx.handleShowNotesSidePanel() {
        updateState {
            copy(uiState = uiState.copy(showNotesSidePanel = true))
        }
    }

    suspend fun ContactDetailsCtx.handleHideNotesSidePanel() {
        updateState {
            copy(uiState = uiState.resetNoteTransientState().copy(showNotesSidePanel = false))
        }
    }

    suspend fun ContactDetailsCtx.handleShowNotesBottomSheet() {
        updateState {
            copy(uiState = uiState.copy(showNotesBottomSheet = true))
        }
    }

    suspend fun ContactDetailsCtx.handleHideNotesBottomSheet() {
        updateState {
            copy(uiState = uiState.resetNoteTransientState().copy(showNotesBottomSheet = false))
        }
    }

    // ========================================================================
    // CRUD HANDLERS
    // ========================================================================

    suspend fun ContactDetailsCtx.handleAddNote() {
        var capturedContactId: ContactId? = null
        var capturedContent: String? = null

        withState {
            if (!contact.isSuccess()) return@withState
            capturedContactId = contactId
            capturedContent = uiState.noteContent.trim()
        }

        val noteContactId = capturedContactId ?: return
        val content = capturedContent.orEmpty()
        if (content.isBlank()) {
            logger.w { "Cannot add empty note" }
            action(ContactDetailsAction.ShowError(DokusException.Validation.NoteContentRequired))
            return
        }

        updateState { copy(isSavingNote = true) }

        logger.d { "Adding note for contact $noteContactId" }

        val request = CreateContactNoteRequest(content = content)
        createContactNote(noteContactId, request).fold(
            onSuccess = { note ->
                logger.i { "Note added: ${note.id}" }
                updateState {
                    copy(
                        isSavingNote = false,
                        uiState = uiState.copy(
                            showAddNoteDialog = false,
                            noteContent = ""
                        )
                    )
                }
                action(ContactDetailsAction.ShowSuccess(ContactDetailsSuccess.NoteAdded))
                // Reload notes to get updated list
                applyNotesResult(listContactNotes(noteContactId))
            },
            onFailure = { error ->
                logger.e(error) { "Failed to add note" }
                updateState { copy(isSavingNote = false) }
                val exception = error.asDokusException
                val displayException = if (exception is DokusException.Unknown) {
                    DokusException.ContactNoteAddFailed
                } else {
                    exception
                }
                action(ContactDetailsAction.ShowError(displayException))
            }
        )
    }

    suspend fun ContactDetailsCtx.handleUpdateNote() {
        var capturedContactId: ContactId? = null
        var capturedNote: ContactNoteDto? = null
        var capturedContent: String? = null

        withState {
            if (!contact.isSuccess()) return@withState
            capturedContactId = contactId
            capturedNote = uiState.editingNote
            capturedContent = uiState.noteContent.trim()
        }

        val noteContactId = capturedContactId ?: return
        val note = capturedNote ?: return
        val content = capturedContent.orEmpty()

        if (content.isBlank()) {
            logger.w { "Cannot update note with empty content" }
            action(ContactDetailsAction.ShowError(DokusException.Validation.NoteContentRequired))
            return
        }

        updateState { copy(isSavingNote = true) }

        logger.d { "Updating note ${note.id}" }

        val request = UpdateContactNoteRequest(content = content)
        updateContactNote(noteContactId, note.id, request).fold(
            onSuccess = { updatedNote ->
                logger.i { "Note updated: ${updatedNote.id}" }
                updateState {
                    copy(
                        isSavingNote = false,
                        uiState = uiState.copy(
                            showEditNoteDialog = false,
                            editingNote = null,
                            noteContent = ""
                        )
                    )
                }
                action(ContactDetailsAction.ShowSuccess(ContactDetailsSuccess.NoteUpdated))
                // Reload notes to get updated list
                applyNotesResult(listContactNotes(noteContactId))
            },
            onFailure = { error ->
                logger.e(error) { "Failed to update note" }
                updateState { copy(isSavingNote = false) }
                val exception = error.asDokusException
                val displayException = if (exception is DokusException.Unknown) {
                    DokusException.ContactNoteUpdateFailed
                } else {
                    exception
                }
                action(ContactDetailsAction.ShowError(displayException))
            }
        )
    }

    suspend fun ContactDetailsCtx.handleDeleteNote() {
        var capturedContactId: ContactId? = null
        var capturedNote: ContactNoteDto? = null

        withState {
            if (!contact.isSuccess()) return@withState
            capturedContactId = contactId
            capturedNote = uiState.deletingNote
        }

        val noteContactId = capturedContactId ?: return
        val note = capturedNote ?: return

        updateState { copy(isDeletingNote = true) }

        logger.d { "Deleting note ${note.id}" }

        deleteContactNote(noteContactId, note.id).fold(
            onSuccess = {
                logger.i { "Note deleted: ${note.id}" }
                updateState {
                    copy(
                        isDeletingNote = false,
                        uiState = uiState.copy(
                            showDeleteNoteConfirmation = false,
                            deletingNote = null
                        )
                    )
                }
                action(ContactDetailsAction.ShowSuccess(ContactDetailsSuccess.NoteDeleted))
                // Reload notes to get updated list
                applyNotesResult(listContactNotes(noteContactId))
            },
            onFailure = { error ->
                logger.e(error) { "Failed to delete note" }
                updateState {
                    copy(
                        isDeletingNote = false,
                        uiState = uiState.copy(
                            showDeleteNoteConfirmation = false,
                            deletingNote = null
                        )
                    )
                }
                val exception = error.asDokusException
                val displayException = if (exception is DokusException.Unknown) {
                    DokusException.ContactNoteDeleteFailed
                } else {
                    exception
                }
                action(ContactDetailsAction.ShowError(displayException))
            }
        )
    }

    // ========================================================================
    // SHARED HELPER
    // ========================================================================

    suspend fun ContactDetailsCtx.applyNotesResult(
        result: Result<List<ContactNoteDto>>,
    ) {
        result.fold(
            onSuccess = { notes ->
                logger.i { "Loaded ${notes.size} notes" }
                updateState { copy(notesState = DokusState.success(notes)) }
            },
            onFailure = { error ->
                logger.e(error) { "Failed to load notes" }
                updateState {
                    copy(
                        notesState = DokusState.error(
                            exception = error.asDokusException,
                            retryHandler = { intent(ContactDetailsIntent.Refresh) }
                        )
                    )
                }
            }
        )
    }
}
