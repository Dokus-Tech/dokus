package ai.dokus.app.contacts.viewmodel

import tech.dokus.domain.asbtractions.RetryHandler
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.model.ContactActivitySummary
import tech.dokus.domain.model.ContactDto
import tech.dokus.domain.model.ContactNoteDto
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.foundation.app.state.DokusState

/**
 * Contract for Contact Details screen.
 *
 * Flow:
 * 1. Loading → Initial data fetch for contact, activity, and notes
 * 2. Content → Contact loaded, displays info, activity summary, and notes
 *    - Sub-states for activity and notes (independently loading/error)
 *    - UI dialogs for notes management
 * 3. Error → Failed to load contact with retry option
 *
 * Features:
 * - Contact information display with Peppol toggle
 * - Activity summary (invoices, bills, expenses)
 * - Notes management (add, edit, delete)
 * - Enrichment suggestions
 * - Contact merge functionality
 */

// ============================================================================
// STATE
// ============================================================================

@Immutable
sealed interface ContactDetailsState : MVIState, DokusState<Nothing> {

    /**
     * Loading state - initial data fetch in progress.
     */
    data class Loading(
        val contactId: ContactId,
    ) : ContactDetailsState

    /**
     * Content state - contact loaded and ready for display.
     *
     * @property contactId ID of the contact being displayed
     * @property contact The contact data
     * @property activityState State of activity summary (independent loading)
     * @property notesState State of notes list (independent loading)
     * @property enrichmentSuggestions Available enrichment suggestions
     * @property uiState UI state for dialogs and panels
     * @property isTogglingPeppol Whether Peppol toggle is in progress
     * @property isSavingNote Whether note save is in progress
     * @property isDeletingNote Whether note deletion is in progress
     */
    data class Content(
        val contactId: ContactId,
        val contact: ContactDto,
        val activityState: DokusState<ContactActivitySummary> = DokusState.loading(),
        val notesState: DokusState<List<ContactNoteDto>> = DokusState.loading(),
        val enrichmentSuggestions: List<EnrichmentSuggestion> = emptyList(),
        val uiState: ContactDetailsUiState = ContactDetailsUiState(),
        val isTogglingPeppol: Boolean = false,
        val isSavingNote: Boolean = false,
        val isDeletingNote: Boolean = false,
    ) : ContactDetailsState

    /**
     * Error state - failed to load contact.
     *
     * @property contactId ID of the contact that failed to load
     * @property exception The error that occurred
     * @property retryHandler Handler to retry the failed operation
     */
    data class Error(
        val contactId: ContactId,
        override val exception: DokusException,
        override val retryHandler: RetryHandler,
    ) : ContactDetailsState, DokusState.Error<Nothing>
}

// ============================================================================
// INTENTS (User Actions)
// ============================================================================

@Immutable
sealed interface ContactDetailsIntent : MVIIntent {

    // === Loading ===

    /** Load contact details by ID */
    data class LoadContact(val contactId: ContactId) : ContactDetailsIntent

    /** Refresh all contact data */
    data object Refresh : ContactDetailsIntent

    // === Peppol ===

    /** Toggle Peppol enabled status */
    data class TogglePeppol(val enabled: Boolean) : ContactDetailsIntent

    // === Notes Dialog Management ===

    /** Show dialog to add a new note */
    data object ShowAddNoteDialog : ContactDetailsIntent

    /** Hide the add note dialog */
    data object HideAddNoteDialog : ContactDetailsIntent

    /** Show dialog to edit an existing note */
    data class ShowEditNoteDialog(val note: ContactNoteDto) : ContactDetailsIntent

    /** Hide the edit note dialog */
    data object HideEditNoteDialog : ContactDetailsIntent

    /** Update note content in dialogs */
    data class UpdateNoteContent(val content: String) : ContactDetailsIntent

    /** Show confirmation dialog for deleting a note */
    data class ShowDeleteNoteConfirmation(val note: ContactNoteDto) : ContactDetailsIntent

    /** Hide the delete note confirmation dialog */
    data object HideDeleteNoteConfirmation : ContactDetailsIntent

    // === Notes Panel/Sheet Visibility ===

    /** Show the notes side panel (desktop) */
    data object ShowNotesSidePanel : ContactDetailsIntent

    /** Hide the notes side panel (desktop) */
    data object HideNotesSidePanel : ContactDetailsIntent

    /** Show the notes bottom sheet (mobile) */
    data object ShowNotesBottomSheet : ContactDetailsIntent

    /** Hide the notes bottom sheet (mobile) */
    data object HideNotesBottomSheet : ContactDetailsIntent

    // === Notes Operations ===

    /** Add a new note with current content */
    data object AddNote : ContactDetailsIntent

    /** Update the currently editing note */
    data object UpdateNote : ContactDetailsIntent

    /** Delete the note pending deletion */
    data object DeleteNote : ContactDetailsIntent

    // === Merge ===

    /** Show the merge dialog */
    data object ShowMergeDialog : ContactDetailsIntent

    /** Hide the merge dialog */
    data object HideMergeDialog : ContactDetailsIntent

    // === Enrichment ===

    /** Show the enrichment suggestions panel */
    data object ShowEnrichmentPanel : ContactDetailsIntent

    /** Hide the enrichment suggestions panel */
    data object HideEnrichmentPanel : ContactDetailsIntent

    /** Apply selected enrichment suggestions */
    data class ApplyEnrichmentSuggestions(
        val suggestions: List<EnrichmentSuggestion>,
    ) : ContactDetailsIntent
}

// ============================================================================
// ACTIONS (Side Effects)
// ============================================================================

@Immutable
sealed interface ContactDetailsAction : MVIAction {

    /** Navigate back to previous screen */
    data object NavigateBack : ContactDetailsAction

    /** Navigate to edit contact screen */
    data class NavigateToEditContact(val contactId: ContactId) : ContactDetailsAction

    /** Navigate to merged contact after merge completion */
    data class NavigateToMergedContact(val contactId: ContactId) : ContactDetailsAction

    /** Show error message as snackbar/toast */
    data class ShowError(val message: String) : ContactDetailsAction

    /** Show success message as snackbar/toast */
    data class ShowSuccess(val message: String) : ContactDetailsAction
}

// ============================================================================
// UI STATE
// ============================================================================

/**
 * UI state for dialogs, panels, and transient UI elements in ContactDetails.
 */
@Stable
data class ContactDetailsUiState(
    // Note dialogs
    val showAddNoteDialog: Boolean = false,
    val showEditNoteDialog: Boolean = false,
    val editingNote: ContactNoteDto? = null,
    val noteContent: String = "",
    val showDeleteNoteConfirmation: Boolean = false,
    val deletingNote: ContactNoteDto? = null,

    // Notes panel/sheet visibility
    val showNotesSidePanel: Boolean = false,
    val showNotesBottomSheet: Boolean = false,

    // Merge dialog
    val showMergeDialog: Boolean = false,

    // Enrichment panel
    val showEnrichmentPanel: Boolean = false,
)

// ============================================================================
// ENRICHMENT
// ============================================================================

/**
 * Represents an enrichment suggestion for contact data.
 * This is a placeholder for future backend integration.
 */
@Immutable
data class EnrichmentSuggestion(
    val field: String,
    val currentValue: String?,
    val suggestedValue: String,
    val source: String,
    val confidence: Float,
)
