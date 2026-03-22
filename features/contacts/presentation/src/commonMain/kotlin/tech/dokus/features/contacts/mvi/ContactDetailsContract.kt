package tech.dokus.features.contacts.mvi

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.model.PeppolStatusResponse
import tech.dokus.domain.model.contact.ContactActivitySummary
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.domain.model.contact.ContactNoteDto
import tech.dokus.features.contacts.mvi.notes.ContactNotesIntent
import tech.dokus.features.contacts.usecases.ContactInvoiceSnapshot
import tech.dokus.foundation.app.state.DokusState

/**
 * Contract for Contact Details screen.
 *
 * Flat data class state — network/loaded data wrapped in [DokusState],
 * UI state (enrichment, saving flags, note drafts) always top-level.
 *
 * Features:
 * - Contact information display
 * - Activity summary (invoices, inbound invoices, expenses)
 * - Notes management (add, edit, delete)
 * - Enrichment suggestions
 * - Contact merge functionality
 */

// ============================================================================
// STATE
// ============================================================================

/**
 * @property contactId ID of the contact being displayed
 * @property contact Contact data (loading / success / error)
 * @property activityState Activity summary (independent loading)
 * @property invoiceSnapshotState Invoice snapshot (count/totals/recent docs)
 * @property peppolStatusState PEPPOL lookup status
 * @property notesState Notes list (projected from child store)
 * @property enrichmentSuggestions Available enrichment suggestions
 * @property uiState UI state for dialogs and panels
 * @property isSavingNote Whether note save is in progress (projected from child)
 * @property isDeletingNote Whether note deletion is in progress (projected from child)
 */
@Immutable
data class ContactDetailsState(
    val contactId: ContactId,
    val contact: DokusState<ContactDto> = DokusState.loading(),
    val activityState: DokusState<ContactActivitySummary> = DokusState.loading(),
    val invoiceSnapshotState: DokusState<ContactInvoiceSnapshot> = DokusState.loading(),
    val peppolStatusState: DokusState<PeppolStatusResponse> = DokusState.loading(),
    val notesState: DokusState<List<ContactNoteDto>> = DokusState.loading(),
    val enrichmentSuggestions: List<EnrichmentSuggestion> = emptyList(),
    val uiState: ContactDetailsUiState = ContactDetailsUiState(),
    val isSavingNote: Boolean = false,
    val isDeletingNote: Boolean = false,
    // Inline edit
    val editFormData: ContactFormData? = null,
    val isSavingEdit: Boolean = false,
    // Action error (inline banner)
    val actionError: DokusException? = null,
) : MVIState

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

    // === Notes (delegated to child store) ===
    data class Notes(val intent: ContactNotesIntent) : ContactDetailsIntent

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

    // === Inline Edit ===

    /** Enter inline edit mode */
    data object StartEditing : ContactDetailsIntent

    /** Cancel inline edit, discard changes */
    data object CancelEditing : ContactDetailsIntent

    /** Save inline edits */
    data object SaveEdit : ContactDetailsIntent

    /** Update form data during inline edit */
    data class UpdateEditFormData(val formData: ContactFormData) : ContactDetailsIntent

    /** Dismiss action error banner */
    data object DismissActionError : ContactDetailsIntent
}

// ============================================================================
// ACTIONS (Side Effects)
// ============================================================================

@Immutable
sealed interface ContactDetailsAction : MVIAction {

    /** Navigate back to previous screen */
    data object NavigateBack : ContactDetailsAction

    /** Navigate to merged contact after merge completion */
    data class NavigateToMergedContact(val contactId: ContactId) : ContactDetailsAction
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
) {
    /** Resets all note-related transient UI state to defaults. */
    fun resetNoteTransientState(): ContactDetailsUiState = copy(
        showAddNoteDialog = false,
        showEditNoteDialog = false,
        editingNote = null,
        showDeleteNoteConfirmation = false,
        deletingNote = null,
        noteContent = "",
    )
}

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
