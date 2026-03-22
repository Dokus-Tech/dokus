package tech.dokus.features.contacts.mvi.notes

import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.model.contact.ContactNoteDto
import tech.dokus.foundation.app.state.DokusState

@Immutable
data class ContactNotesChildState(
    val notes: DokusState<List<ContactNoteDto>> = DokusState.loading(),
    val showAddNoteDialog: Boolean = false,
    val showEditNoteDialog: Boolean = false,
    val editingNote: ContactNoteDto? = null,
    val noteContent: String = "",
    val showDeleteNoteConfirmation: Boolean = false,
    val deletingNote: ContactNoteDto? = null,
    val isSavingNote: Boolean = false,
    val isDeletingNote: Boolean = false,
    val showNotesSidePanel: Boolean = false,
    val showNotesBottomSheet: Boolean = false,
) : MVIState {
    fun resetTransientState(): ContactNotesChildState = copy(
        showAddNoteDialog = false,
        showEditNoteDialog = false,
        editingNote = null,
        showDeleteNoteConfirmation = false,
        deletingNote = null,
        noteContent = "",
    )
}

@Immutable
sealed interface ContactNotesIntent : MVIIntent {
    data object LoadNotes : ContactNotesIntent
    data object ShowAddNoteDialog : ContactNotesIntent
    data object HideAddNoteDialog : ContactNotesIntent
    data class ShowEditNoteDialog(val note: ContactNoteDto) : ContactNotesIntent
    data object HideEditNoteDialog : ContactNotesIntent
    data class UpdateNoteContent(val content: String) : ContactNotesIntent
    data object AddNote : ContactNotesIntent
    data object UpdateNote : ContactNotesIntent
    data class ShowDeleteNoteConfirmation(val note: ContactNoteDto) : ContactNotesIntent
    data object HideDeleteNoteConfirmation : ContactNotesIntent
    data object DeleteNote : ContactNotesIntent
    data object ShowNotesSidePanel : ContactNotesIntent
    data object HideNotesSidePanel : ContactNotesIntent
    data object ShowNotesBottomSheet : ContactNotesIntent
    data object HideNotesBottomSheet : ContactNotesIntent
}

@Immutable
sealed interface ContactNotesAction : MVIAction {
    data object NoteAdded : ContactNotesAction
    data object NoteUpdated : ContactNotesAction
    data object NoteDeleted : ContactNotesAction
    data class ShowError(val error: DokusException) : ContactNotesAction
}
