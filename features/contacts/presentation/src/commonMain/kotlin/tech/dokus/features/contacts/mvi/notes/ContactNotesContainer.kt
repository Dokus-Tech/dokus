@file:Suppress("TooGenericExceptionCaught")

package tech.dokus.features.contacts.mvi.notes

import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.reduce
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
import tech.dokus.foundation.platform.Logger

private typealias NotesCtx = PipelineContext<ContactNotesChildState, ContactNotesIntent, ContactNotesAction>

internal class ContactNotesContainer(
    private val contactId: ContactId,
    private val listContactNotes: ListContactNotesUseCase,
    private val createContactNote: CreateContactNoteUseCase,
    private val updateContactNote: UpdateContactNoteUseCase,
    private val deleteContactNote: DeleteContactNoteUseCase,
) : Container<ContactNotesChildState, ContactNotesIntent, ContactNotesAction> {

    private val logger = Logger.forClass<ContactNotesContainer>()

    override val store: Store<ContactNotesChildState, ContactNotesIntent, ContactNotesAction> =
        store(ContactNotesChildState()) {
            reduce { intent ->
                when (intent) {
                    is ContactNotesIntent.LoadNotes -> handleLoadNotes()
                    is ContactNotesIntent.ShowAddNoteDialog -> handleShowAddNoteDialog()
                    is ContactNotesIntent.HideAddNoteDialog -> handleHideAddNoteDialog()
                    is ContactNotesIntent.ShowEditNoteDialog -> handleShowEditNoteDialog(intent.note)
                    is ContactNotesIntent.HideEditNoteDialog -> handleHideEditNoteDialog()
                    is ContactNotesIntent.UpdateNoteContent -> handleUpdateNoteContent(intent.content)
                    is ContactNotesIntent.AddNote -> handleAddNote()
                    is ContactNotesIntent.UpdateNote -> handleUpdateNote()
                    is ContactNotesIntent.ShowDeleteNoteConfirmation ->
                        handleShowDeleteNoteConfirmation(intent.note)
                    is ContactNotesIntent.HideDeleteNoteConfirmation -> handleHideDeleteNoteConfirmation()
                    is ContactNotesIntent.DeleteNote -> handleDeleteNote()
                    is ContactNotesIntent.ShowNotesSidePanel -> handleShowNotesSidePanel()
                    is ContactNotesIntent.HideNotesSidePanel -> handleHideNotesSidePanel()
                    is ContactNotesIntent.ShowNotesBottomSheet -> handleShowNotesBottomSheet()
                    is ContactNotesIntent.HideNotesBottomSheet -> handleHideNotesBottomSheet()
                }
            }
        }

    // ========================================================================
    // LOAD
    // ========================================================================

    private suspend fun NotesCtx.handleLoadNotes() {
        updateState { copy(notes = DokusState.loading()) }
        applyNotesResult(listContactNotes(contactId))
    }

    // ========================================================================
    // DIALOG HANDLERS
    // ========================================================================

    private suspend fun NotesCtx.handleShowAddNoteDialog() {
        updateState { resetTransientState().copy(showAddNoteDialog = true) }
    }

    private suspend fun NotesCtx.handleHideAddNoteDialog() {
        updateState { copy(showAddNoteDialog = false, noteContent = "") }
    }

    private suspend fun NotesCtx.handleShowEditNoteDialog(note: ContactNoteDto) {
        updateState {
            resetTransientState().copy(
                showEditNoteDialog = true,
                editingNote = note,
                noteContent = note.content,
            )
        }
    }

    private suspend fun NotesCtx.handleHideEditNoteDialog() {
        updateState { copy(showEditNoteDialog = false, editingNote = null, noteContent = "") }
    }

    private suspend fun NotesCtx.handleUpdateNoteContent(content: String) {
        updateState { copy(noteContent = content) }
    }

    private suspend fun NotesCtx.handleShowDeleteNoteConfirmation(note: ContactNoteDto) {
        updateState {
            resetTransientState().copy(
                showDeleteNoteConfirmation = true,
                deletingNote = note,
            )
        }
    }

    private suspend fun NotesCtx.handleHideDeleteNoteConfirmation() {
        updateState { copy(showDeleteNoteConfirmation = false, deletingNote = null) }
    }

    // ========================================================================
    // PANEL / SHEET HANDLERS
    // ========================================================================

    private suspend fun NotesCtx.handleShowNotesSidePanel() {
        updateState { copy(showNotesSidePanel = true) }
    }

    private suspend fun NotesCtx.handleHideNotesSidePanel() {
        updateState { resetTransientState().copy(showNotesSidePanel = false) }
    }

    private suspend fun NotesCtx.handleShowNotesBottomSheet() {
        updateState { copy(showNotesBottomSheet = true) }
    }

    private suspend fun NotesCtx.handleHideNotesBottomSheet() {
        updateState { resetTransientState().copy(showNotesBottomSheet = false) }
    }

    // ========================================================================
    // CRUD HANDLERS
    // ========================================================================

    private suspend fun NotesCtx.handleAddNote() {
        var capturedContent: String? = null
        withState { capturedContent = noteContent.trim() }

        val content = capturedContent.orEmpty()
        if (content.isBlank()) {
            logger.w { "Cannot add empty note" }
            action(ContactNotesAction.ShowError(DokusException.Validation.NoteContentRequired))
            return
        }

        updateState { copy(isSavingNote = true) }
        logger.d { "Adding note for contact $contactId" }

        val request = CreateContactNoteRequest(content = content)
        createContactNote(contactId, request).fold(
            onSuccess = { note ->
                logger.i { "Note added: ${note.id}" }
                updateState {
                    copy(
                        isSavingNote = false,
                        showAddNoteDialog = false,
                        noteContent = "",
                    )
                }
                action(ContactNotesAction.NoteAdded)
                applyNotesResult(listContactNotes(contactId))
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
                action(ContactNotesAction.ShowError(displayException))
            }
        )
    }

    private suspend fun NotesCtx.handleUpdateNote() {
        var capturedNote: ContactNoteDto? = null
        var capturedContent: String? = null

        withState {
            capturedNote = editingNote
            capturedContent = noteContent.trim()
        }

        val note = capturedNote ?: return
        val content = capturedContent.orEmpty()

        if (content.isBlank()) {
            logger.w { "Cannot update note with empty content" }
            action(ContactNotesAction.ShowError(DokusException.Validation.NoteContentRequired))
            return
        }

        updateState { copy(isSavingNote = true) }
        logger.d { "Updating note ${note.id}" }

        val request = UpdateContactNoteRequest(content = content)
        updateContactNote(contactId, note.id, request).fold(
            onSuccess = { updatedNote ->
                logger.i { "Note updated: ${updatedNote.id}" }
                updateState {
                    copy(
                        isSavingNote = false,
                        showEditNoteDialog = false,
                        editingNote = null,
                        noteContent = "",
                    )
                }
                action(ContactNotesAction.NoteUpdated)
                applyNotesResult(listContactNotes(contactId))
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
                action(ContactNotesAction.ShowError(displayException))
            }
        )
    }

    private suspend fun NotesCtx.handleDeleteNote() {
        var capturedNote: ContactNoteDto? = null
        withState { capturedNote = deletingNote }

        val note = capturedNote ?: return

        updateState { copy(isDeletingNote = true) }
        logger.d { "Deleting note ${note.id}" }

        deleteContactNote(contactId, note.id).fold(
            onSuccess = {
                logger.i { "Note deleted: ${note.id}" }
                updateState {
                    copy(
                        isDeletingNote = false,
                        showDeleteNoteConfirmation = false,
                        deletingNote = null,
                    )
                }
                action(ContactNotesAction.NoteDeleted)
                applyNotesResult(listContactNotes(contactId))
            },
            onFailure = { error ->
                logger.e(error) { "Failed to delete note" }
                updateState {
                    copy(
                        isDeletingNote = false,
                        showDeleteNoteConfirmation = false,
                        deletingNote = null,
                    )
                }
                val exception = error.asDokusException
                val displayException = if (exception is DokusException.Unknown) {
                    DokusException.ContactNoteDeleteFailed
                } else {
                    exception
                }
                action(ContactNotesAction.ShowError(displayException))
            }
        )
    }

    // ========================================================================
    // SHARED HELPER
    // ========================================================================

    private suspend fun NotesCtx.applyNotesResult(result: Result<List<ContactNoteDto>>) {
        result.fold(
            onSuccess = { notes ->
                logger.i { "Loaded ${notes.size} notes" }
                updateState { copy(notes = DokusState.success(notes)) }
            },
            onFailure = { error ->
                logger.e(error) { "Failed to load notes" }
                updateState {
                    copy(
                        notes = DokusState.error(
                            exception = error.asDokusException,
                            retryHandler = { intent(ContactNotesIntent.LoadNotes) }
                        )
                    )
                }
            }
        )
    }
}
