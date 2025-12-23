package ai.dokus.app.contacts.viewmodel

import ai.dokus.app.contacts.repository.ContactRepository
import ai.dokus.foundation.domain.ids.ContactId
import ai.dokus.foundation.domain.model.ContactActivitySummary
import ai.dokus.foundation.domain.model.ContactDto
import ai.dokus.foundation.domain.model.ContactNoteDto
import ai.dokus.foundation.domain.model.CreateContactNoteRequest
import ai.dokus.foundation.domain.model.UpdateContactNoteRequest
import ai.dokus.foundation.domain.model.UpdateContactPeppolRequest
import ai.dokus.foundation.platform.Logger
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.app.state.emit
import tech.dokus.foundation.app.state.emitLoading
import tech.dokus.foundation.app.viewmodel.BaseViewModel

// ============================================================================
// UI STATE
// ============================================================================

/**
 * UI state for contact details screen dialogs and interactions.
 */
data class ContactDetailsUiState(
    val showAddNoteDialog: Boolean = false,
    val showEditNoteDialog: Boolean = false,
    val editingNote: ContactNoteDto? = null,
    val showDeleteNoteConfirmation: Boolean = false,
    val deletingNote: ContactNoteDto? = null,
    val showMergeDialog: Boolean = false,
    val showEnrichmentPanel: Boolean = false,
    val noteContent: String = "",
    // Notes pane/sheet visibility states (responsive UI)
    val showNotesSidePanel: Boolean = false,
    val showNotesBottomSheet: Boolean = false
)

/**
 * Enrichment suggestion from backend.
 * Represents suggested field updates based on external data sources.
 */
data class EnrichmentSuggestion(
    val field: String,
    val currentValue: String?,
    val suggestedValue: String,
    val confidence: Float,
    val source: String
)

// ============================================================================
// VIEW MODEL
// ============================================================================

/**
 * ViewModel for the Contact Details screen.
 * Manages contact details, activity summary, notes, and enrichment suggestions.
 */
internal class ContactDetailsViewModel :
    BaseViewModel<DokusState<ContactDto>>(DokusState.idle()),
    KoinComponent {

    private val logger = Logger.forClass<ContactDetailsViewModel>()
    private val contactRepository: ContactRepository by inject()

    // Contact ID being viewed
    private val _contactId = MutableStateFlow<ContactId?>(null)
    val contactId: StateFlow<ContactId?> = _contactId.asStateFlow()

    // Activity summary state
    private val _activityState =
        MutableStateFlow<DokusState<ContactActivitySummary>>(DokusState.idle())
    val activityState: StateFlow<DokusState<ContactActivitySummary>> = _activityState.asStateFlow()

    // Notes state
    private val _notesState = MutableStateFlow<DokusState<List<ContactNoteDto>>>(DokusState.idle())
    val notesState: StateFlow<DokusState<List<ContactNoteDto>>> = _notesState.asStateFlow()

    // Enrichment suggestions state (for future backend integration)
    private val _enrichmentSuggestions = MutableStateFlow<List<EnrichmentSuggestion>>(emptyList())
    val enrichmentSuggestions: StateFlow<List<EnrichmentSuggestion>> =
        _enrichmentSuggestions.asStateFlow()

    // UI state
    private val _uiState = MutableStateFlow(ContactDetailsUiState())
    val uiState: StateFlow<ContactDetailsUiState> = _uiState.asStateFlow()

    // Operation states
    private val _isTogglingPeppol = MutableStateFlow(false)
    val isTogglingPeppol: StateFlow<Boolean> = _isTogglingPeppol.asStateFlow()

    private val _isSavingNote = MutableStateFlow(false)
    val isSavingNote: StateFlow<Boolean> = _isSavingNote.asStateFlow()

    private val _isDeletingNote = MutableStateFlow(false)
    val isDeletingNote: StateFlow<Boolean> = _isDeletingNote.asStateFlow()

    // ============================================================================
    // INITIALIZATION
    // ============================================================================

    /**
     * Load contact details by ID.
     * Also loads activity summary and notes in parallel.
     */
    fun loadContact(contactId: ContactId) {
        logger.d { "Loading contact details: $contactId" }
        _contactId.value = contactId

        scope.launch {
            mutableState.emitLoading()
            _activityState.value = DokusState.loading()
            _notesState.value = DokusState.loading()

            // Load all data in parallel
            val contactJob = async { loadContactData(contactId) }
            val activityJob = async { loadActivityData(contactId) }
            val notesJob = async { loadNotesData(contactId) }

            contactJob.await()
            activityJob.await()
            notesJob.await()
        }
    }

    /**
     * Refresh all contact data.
     */
    fun refresh() {
        val id = _contactId.value ?: return
        loadContact(id)
    }

    /**
     * Load contact data from API.
     */
    private suspend fun loadContactData(contactId: ContactId) {
        contactRepository.getContact(contactId).fold(
            onSuccess = { contact ->
                logger.i { "Loaded contact: ${contact.name}" }
                mutableState.value = DokusState.success(contact)
            },
            onFailure = { error ->
                logger.e(error) { "Failed to load contact: $contactId" }
                mutableState.emit(error) { refresh() }
            }
        )
    }

    /**
     * Load activity summary from API.
     */
    private suspend fun loadActivityData(contactId: ContactId) {
        contactRepository.getContactActivity(contactId).fold(
            onSuccess = { activity ->
                logger.i { "Loaded activity: invoices=${activity.invoiceCount}, bills=${activity.billCount}" }
                _activityState.value = DokusState.success(activity)
            },
            onFailure = { error ->
                logger.e(error) { "Failed to load activity: $contactId" }
                emitActivityError(error) { loadActivityData(contactId) }
            }
        )
    }

    /**
     * Load notes from API.
     */
    private suspend fun loadNotesData(contactId: ContactId) {
        contactRepository.listNotes(contactId).fold(
            onSuccess = { notes ->
                logger.i { "Loaded ${notes.size} notes" }
                _notesState.value = DokusState.success(notes)
            },
            onFailure = { error ->
                logger.e(error) { "Failed to load notes: $contactId" }
                emitNotesError(error) { loadNotesData(contactId) }
            }
        )
    }

    // ============================================================================
    // PEPPOL OPERATIONS
    // ============================================================================

    /**
     * Toggle Peppol enabled status for the contact.
     */
    fun togglePeppol(enabled: Boolean) {
        val contactId = _contactId.value ?: return
        val currentContact = (mutableState.value as? DokusState.Success)?.data ?: return

        // If enabling Peppol, require a Peppol ID
        if (enabled && currentContact.peppolId.isNullOrBlank()) {
            logger.w { "Cannot enable Peppol without Peppol ID" }
            return
        }

        _isTogglingPeppol.value = true

        scope.launch {
            logger.d { "Toggling Peppol: $enabled for contact $contactId" }

            val request = UpdateContactPeppolRequest(
                peppolId = currentContact.peppolId,
                peppolEnabled = enabled
            )
            contactRepository.updateContactPeppol(contactId, request).fold(
                onSuccess = { updatedContact ->
                    logger.i { "Peppol updated: ${updatedContact.peppolEnabled}" }
                    mutableState.value = DokusState.success(updatedContact)
                    _isTogglingPeppol.value = false
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to toggle Peppol" }
                    _isTogglingPeppol.value = false
                }
            )
        }
    }

    // ============================================================================
    // NOTES OPERATIONS
    // ============================================================================

    /**
     * Show dialog to add a new note.
     */
    fun showAddNoteDialog() {
        _uiState.update { it.copy(showAddNoteDialog = true, noteContent = "") }
    }

    /**
     * Hide the add note dialog.
     */
    fun hideAddNoteDialog() {
        _uiState.update { it.copy(showAddNoteDialog = false, noteContent = "") }
    }

    /**
     * Show dialog to edit an existing note.
     */
    fun showEditNoteDialog(note: ContactNoteDto) {
        _uiState.update {
            it.copy(
                showEditNoteDialog = true,
                editingNote = note,
                noteContent = note.content
            )
        }
    }

    /**
     * Hide the edit note dialog.
     */
    fun hideEditNoteDialog() {
        _uiState.update {
            it.copy(
                showEditNoteDialog = false,
                editingNote = null,
                noteContent = ""
            )
        }
    }

    /**
     * Update the note content in dialogs.
     */
    fun updateNoteContent(content: String) {
        _uiState.update { it.copy(noteContent = content) }
    }

    /**
     * Show confirmation dialog for deleting a note.
     */
    fun showDeleteNoteConfirmation(note: ContactNoteDto) {
        _uiState.update {
            it.copy(
                showDeleteNoteConfirmation = true,
                deletingNote = note
            )
        }
    }

    /**
     * Hide the delete note confirmation dialog.
     */
    fun hideDeleteNoteConfirmation() {
        _uiState.update {
            it.copy(
                showDeleteNoteConfirmation = false,
                deletingNote = null
            )
        }
    }

    // ============================================================================
    // NOTES PANE/SHEET VISIBILITY
    // ============================================================================

    /**
     * Show the notes side panel (desktop).
     */
    fun showNotesSidePanel() {
        _uiState.update { it.copy(showNotesSidePanel = true) }
    }

    /**
     * Hide the notes side panel (desktop).
     */
    fun hideNotesSidePanel() {
        _uiState.update {
            it.copy(
                showNotesSidePanel = false,
                // Also reset any note editing state when closing
                showAddNoteDialog = false,
                showEditNoteDialog = false,
                editingNote = null,
                noteContent = ""
            )
        }
    }

    /**
     * Show the notes bottom sheet (mobile).
     */
    fun showNotesBottomSheet() {
        _uiState.update { it.copy(showNotesBottomSheet = true) }
    }

    /**
     * Hide the notes bottom sheet (mobile).
     */
    fun hideNotesBottomSheet() {
        _uiState.update {
            it.copy(
                showNotesBottomSheet = false,
                // Also reset any note editing state when closing
                showAddNoteDialog = false,
                showEditNoteDialog = false,
                editingNote = null,
                noteContent = ""
            )
        }
    }

    /**
     * Add a new note to the contact.
     */
    fun addNote() {
        val contactId = _contactId.value ?: return
        val content = _uiState.value.noteContent.trim()

        if (content.isBlank()) {
            logger.w { "Cannot add empty note" }
            return
        }

        _isSavingNote.value = true

        scope.launch {
            logger.d { "Adding note for contact $contactId" }

            val request = CreateContactNoteRequest(content = content)
            contactRepository.createNote(contactId, request).fold(
                onSuccess = { note ->
                    logger.i { "Note added: ${note.id}" }
                    _isSavingNote.value = false
                    hideAddNoteDialog()
                    // Reload notes to get updated list
                    loadNotesData(contactId)
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to add note" }
                    _isSavingNote.value = false
                }
            )
        }
    }

    /**
     * Update an existing note.
     */
    fun updateNote() {
        val contactId = _contactId.value ?: return
        val note = _uiState.value.editingNote ?: return
        val content = _uiState.value.noteContent.trim()

        if (content.isBlank()) {
            logger.w { "Cannot update note with empty content" }
            return
        }

        _isSavingNote.value = true

        scope.launch {
            logger.d { "Updating note ${note.id}" }

            val request = UpdateContactNoteRequest(content = content)
            contactRepository.updateNote(contactId, note.id, request).fold(
                onSuccess = { updatedNote ->
                    logger.i { "Note updated: ${updatedNote.id}" }
                    _isSavingNote.value = false
                    hideEditNoteDialog()
                    // Reload notes to get updated list
                    loadNotesData(contactId)
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to update note" }
                    _isSavingNote.value = false
                }
            )
        }
    }

    /**
     * Delete a note.
     */
    fun deleteNote() {
        val contactId = _contactId.value ?: return
        val note = _uiState.value.deletingNote ?: return

        _isDeletingNote.value = true

        scope.launch {
            logger.d { "Deleting note ${note.id}" }

            contactRepository.deleteNote(contactId, note.id).fold(
                onSuccess = {
                    logger.i { "Note deleted: ${note.id}" }
                    _isDeletingNote.value = false
                    hideDeleteNoteConfirmation()
                    // Reload notes to get updated list
                    loadNotesData(contactId)
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to delete note" }
                    _isDeletingNote.value = false
                    hideDeleteNoteConfirmation()
                }
            )
        }
    }

    // ============================================================================
    // MERGE OPERATIONS
    // ============================================================================

    /**
     * Show the merge dialog.
     */
    fun showMergeDialog() {
        _uiState.update { it.copy(showMergeDialog = true) }
    }

    /**
     * Hide the merge dialog.
     */
    fun hideMergeDialog() {
        _uiState.update { it.copy(showMergeDialog = false) }
    }

    // ============================================================================
    // ENRICHMENT OPERATIONS
    // ============================================================================

    /**
     * Show the enrichment suggestions panel.
     */
    fun showEnrichmentPanel() {
        _uiState.update { it.copy(showEnrichmentPanel = true) }
    }

    /**
     * Hide the enrichment suggestions panel.
     */
    fun hideEnrichmentPanel() {
        _uiState.update { it.copy(showEnrichmentPanel = false) }
    }

    /**
     * Check if there are enrichment suggestions available.
     */
    fun hasEnrichmentSuggestions(): Boolean {
        return _enrichmentSuggestions.value.isNotEmpty()
    }

    /**
     * Apply selected enrichment suggestions.
     * This would update the contact with the suggested values.
     *
     * @param suggestions The suggestions to apply
     */
    fun applyEnrichmentSuggestions(suggestions: List<EnrichmentSuggestion>) {
        val contactId = _contactId.value ?: return

        if (suggestions.isEmpty()) {
            logger.w { "No enrichment suggestions selected" }
            return
        }

        // Future implementation: Build UpdateContactRequest from selected suggestions
        // and call contactRepository.updateContact
        logger.d { "Applying ${suggestions.size} enrichment suggestions for $contactId" }

        // Remove applied suggestions from the list
        _enrichmentSuggestions.update { current ->
            current.filterNot { it in suggestions }
        }

        hideEnrichmentPanel()
    }

    // ============================================================================
    // HELPER EXTENSIONS
    // ============================================================================

    /**
     * Emit error state for activity StateFlow.
     */
    private fun emitActivityError(error: Throwable, retry: suspend () -> Unit) {
        _activityState.value = DokusState.error(error) { scope.launch { retry() } }
    }

    /**
     * Emit error state for notes StateFlow.
     */
    private fun emitNotesError(error: Throwable, retry: suspend () -> Unit) {
        _notesState.value = DokusState.error(error) { scope.launch { retry() } }
    }
}
