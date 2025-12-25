package ai.dokus.app.contacts.viewmodel

import ai.dokus.app.auth.usecases.GetCurrentTenantIdUseCase
import ai.dokus.app.contacts.cache.ContactLocalDataSource
import ai.dokus.app.contacts.repository.ContactRepository
import ai.dokus.foundation.domain.exceptions.asDokusException
import ai.dokus.foundation.domain.ids.ContactId
import ai.dokus.foundation.domain.model.ContactDto
import ai.dokus.foundation.domain.model.ContactNoteDto
import ai.dokus.foundation.domain.model.CreateContactNoteRequest
import ai.dokus.foundation.domain.model.UpdateContactNoteRequest
import ai.dokus.foundation.domain.model.UpdateContactPeppolRequest
import ai.dokus.foundation.platform.Logger
import kotlinx.coroutines.async
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.dsl.withState
import pro.respawn.flowmvi.plugins.reduce
import tech.dokus.foundation.app.state.DokusState

internal typealias ContactDetailsCtx = PipelineContext<ContactDetailsState, ContactDetailsIntent, ContactDetailsAction>

/**
 * Container for the Contact Details screen using FlowMVI.
 *
 * Manages contact details display, notes CRUD, Peppol toggle, merge, and enrichment features.
 * Implements cache-first strategy for offline support.
 *
 * Features:
 * - Contact information display with Peppol toggle
 * - Activity summary (invoices, bills, expenses)
 * - Notes management (add, edit, delete)
 * - Enrichment suggestions
 * - Contact merge functionality
 *
 * Use with Koin's `container<>` DSL for automatic ViewModel wrapping and lifecycle management.
 */
internal class ContactDetailsContainer(
    contactId: ContactId,
    private val contactRepository: ContactRepository,
    private val localDataSource: ContactLocalDataSource,
    private val getCurrentTenantId: GetCurrentTenantIdUseCase,
) : Container<ContactDetailsState, ContactDetailsIntent, ContactDetailsAction> {

    companion object {
        data class Params(
            val contactId: ContactId
        )
    }

    private val logger = Logger.forClass<ContactDetailsContainer>()

    override val store: Store<ContactDetailsState, ContactDetailsIntent, ContactDetailsAction> =
        store(ContactDetailsState.Loading(contactId)) {
            reduce { intent ->
                when (intent) {
                    // Loading
                    is ContactDetailsIntent.LoadContact -> handleLoadContact(intent.contactId)
                    is ContactDetailsIntent.Refresh -> handleRefresh()

                    // Peppol
                    is ContactDetailsIntent.TogglePeppol -> handleTogglePeppol(intent.enabled)

                    // Notes Dialog Management
                    is ContactDetailsIntent.ShowAddNoteDialog -> handleShowAddNoteDialog()
                    is ContactDetailsIntent.HideAddNoteDialog -> handleHideAddNoteDialog()
                    is ContactDetailsIntent.ShowEditNoteDialog -> handleShowEditNoteDialog(intent.note)
                    is ContactDetailsIntent.HideEditNoteDialog -> handleHideEditNoteDialog()
                    is ContactDetailsIntent.UpdateNoteContent -> handleUpdateNoteContent(intent.content)
                    is ContactDetailsIntent.ShowDeleteNoteConfirmation -> handleShowDeleteNoteConfirmation(intent.note)
                    is ContactDetailsIntent.HideDeleteNoteConfirmation -> handleHideDeleteNoteConfirmation()

                    // Notes Panel/Sheet Visibility
                    is ContactDetailsIntent.ShowNotesSidePanel -> handleShowNotesSidePanel()
                    is ContactDetailsIntent.HideNotesSidePanel -> handleHideNotesSidePanel()
                    is ContactDetailsIntent.ShowNotesBottomSheet -> handleShowNotesBottomSheet()
                    is ContactDetailsIntent.HideNotesBottomSheet -> handleHideNotesBottomSheet()

                    // Notes Operations
                    is ContactDetailsIntent.AddNote -> handleAddNote()
                    is ContactDetailsIntent.UpdateNote -> handleUpdateNote()
                    is ContactDetailsIntent.DeleteNote -> handleDeleteNote()

                    // Merge
                    is ContactDetailsIntent.ShowMergeDialog -> handleShowMergeDialog()
                    is ContactDetailsIntent.HideMergeDialog -> handleHideMergeDialog()

                    // Enrichment
                    is ContactDetailsIntent.ShowEnrichmentPanel -> handleShowEnrichmentPanel()
                    is ContactDetailsIntent.HideEnrichmentPanel -> handleHideEnrichmentPanel()
                    is ContactDetailsIntent.ApplyEnrichmentSuggestions -> handleApplyEnrichmentSuggestions(intent.suggestions)
                }
            }
        }

    // ============================================================================
    // LOADING HANDLERS
    // ============================================================================

    private suspend fun ContactDetailsCtx.handleLoadContact(contactId: ContactId) {
        logger.d { "Loading contact details: $contactId" }

        updateState { ContactDetailsState.Loading(contactId) }

        // Load all data in parallel
        val contactDeferred = async { loadContactData(contactId) }
        val activityDeferred = async { loadActivityData(contactId) }
        val notesDeferred = async { loadNotesData(contactId) }

        contactDeferred.await()
        activityDeferred.await()
        notesDeferred.await()
    }

    private suspend fun ContactDetailsCtx.handleRefresh() {
        withState<ContactDetailsState.Content, _> {
            logger.d { "Refreshing contact details: $contactId" }

            // Reset sub-states to loading
            updateState {
                copy(
                    activityState = DokusState.loading(),
                    notesState = DokusState.loading()
                )
            }

            // Reload all data in parallel
            val contactDeferred = async { loadContactData(contactId) }
            val activityDeferred = async { loadActivityData(contactId) }
            val notesDeferred = async { loadNotesData(contactId) }

            contactDeferred.await()
            activityDeferred.await()
            notesDeferred.await()
        }

        // Also handle refresh from error state
        withState<ContactDetailsState.Error, _> {
            logger.d { "Refreshing contact details from error: $contactId" }
            handleLoadContact(contactId)
        }
    }

    /**
     * Load contact data with cache-first pattern.
     * Shows cached data immediately, then refreshes from network.
     */
    private suspend fun ContactDetailsCtx.loadContactData(contactId: ContactId) {
        // Try cache first for immediate display
        val cached = loadContactFromCache(contactId)
        if (cached != null) {
            logger.d { "Loaded contact from cache: ${cached.name}" }
            transitionToContent(contactId, cached)
        }

        // Then try network refresh
        contactRepository.getContact(contactId).fold(
            onSuccess = { contact ->
                logger.i { "Loaded contact from network: ${contact.name}" }
                transitionToContent(contactId, contact)
                // Update cache with fresh data
                cacheContact(contact)
            },
            onFailure = { error ->
                logger.e(error) { "Failed to load contact from network: $contactId" }
                // Only show error if we have no cached data
                if (cached == null) {
                    updateState {
                        ContactDetailsState.Error(
                            contactId = contactId,
                            exception = error.asDokusException,
                            retryHandler = { intent(ContactDetailsIntent.Refresh) }
                        )
                    }
                }
                // If we have cached data, silently keep showing it
            }
        )
    }

    /**
     * Transition to Content state or update existing Content state.
     */
    private suspend fun ContactDetailsCtx.transitionToContent(contactId: ContactId, contact: ContactDto) {
        updateState {
            when (this) {
                is ContactDetailsState.Content -> copy(contact = contact)
                else -> ContactDetailsState.Content(
                    contactId = contactId,
                    contact = contact
                )
            }
        }
    }

    /**
     * Load contact from local cache.
     */
    private suspend fun loadContactFromCache(contactId: ContactId): ContactDto? {
        return try {
            localDataSource.getById(contactId)
        } catch (e: Exception) {
            logger.e(e) { "Failed to load contact from cache" }
            null
        }
    }

    /**
     * Cache contact for offline access.
     */
    private suspend fun cacheContact(contact: ContactDto) {
        val tenantId = getCurrentTenantId() ?: return
        try {
            localDataSource.upsertAll(tenantId, listOf(contact))
            logger.d { "Cached contact: ${contact.name}" }
        } catch (e: Exception) {
            logger.w(e) { "Failed to cache contact" }
        }
    }

    /**
     * Load activity summary from API.
     */
    private suspend fun ContactDetailsCtx.loadActivityData(contactId: ContactId) {
        contactRepository.getContactActivity(contactId).fold(
            onSuccess = { activity ->
                logger.i { "Loaded activity: invoices=${activity.invoiceCount}, bills=${activity.billCount}" }
                withState<ContactDetailsState.Content, _> {
                    updateState {
                        copy(activityState = DokusState.success(activity))
                    }
                }
            },
            onFailure = { error ->
                logger.e(error) { "Failed to load activity: $contactId" }
                withState<ContactDetailsState.Content, _> {
                    updateState {
                        copy(
                            activityState = DokusState.error(error) {
                                intent(ContactDetailsIntent.Refresh)
                            }
                        )
                    }
                }
            }
        )
    }

    /**
     * Load notes from API.
     */
    private suspend fun ContactDetailsCtx.loadNotesData(contactId: ContactId) {
        contactRepository.listNotes(contactId).fold(
            onSuccess = { notes ->
                logger.i { "Loaded ${notes.size} notes" }
                withState<ContactDetailsState.Content, _> {
                    updateState {
                        copy(notesState = DokusState.success(notes))
                    }
                }
            },
            onFailure = { error ->
                logger.e(error) { "Failed to load notes: $contactId" }
                withState<ContactDetailsState.Content, _> {
                    updateState {
                        copy(
                            notesState = DokusState.error(error) {
                                intent(ContactDetailsIntent.Refresh)
                            }
                        )
                    }
                }
            }
        )
    }

    // ============================================================================
    // PEPPOL HANDLERS
    // ============================================================================

    private suspend fun ContactDetailsCtx.handleTogglePeppol(enabled: Boolean) {
        withState<ContactDetailsState.Content, _> {
            // If enabling Peppol, require a Peppol ID
            if (enabled && contact.peppolId.isNullOrBlank()) {
                logger.w { "Cannot enable Peppol without Peppol ID" }
                action(ContactDetailsAction.ShowError("Cannot enable Peppol without Peppol ID"))
                return@withState
            }

            updateState { copy(isTogglingPeppol = true) }

            logger.d { "Toggling Peppol: $enabled for contact $contactId" }

            val request = UpdateContactPeppolRequest(
                peppolId = contact.peppolId,
                peppolEnabled = enabled
            )

            contactRepository.updateContactPeppol(contactId, request).fold(
                onSuccess = { updatedContact ->
                    logger.i { "Peppol updated: ${updatedContact.peppolEnabled}" }
                    updateState {
                        copy(
                            contact = updatedContact,
                            isTogglingPeppol = false
                        )
                    }
                    action(ContactDetailsAction.ShowSuccess("Peppol settings updated"))
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to toggle Peppol" }
                    updateState { copy(isTogglingPeppol = false) }
                    action(ContactDetailsAction.ShowError("Failed to update Peppol settings"))
                }
            )
        }
    }

    // ============================================================================
    // NOTES DIALOG HANDLERS
    // ============================================================================

    private suspend fun ContactDetailsCtx.handleShowAddNoteDialog() {
        withState<ContactDetailsState.Content, _> {
            updateState {
                copy(
                    uiState = uiState.copy(
                        showAddNoteDialog = true,
                        noteContent = ""
                    )
                )
            }
        }
    }

    private suspend fun ContactDetailsCtx.handleHideAddNoteDialog() {
        withState<ContactDetailsState.Content, _> {
            updateState {
                copy(
                    uiState = uiState.copy(
                        showAddNoteDialog = false,
                        noteContent = ""
                    )
                )
            }
        }
    }

    private suspend fun ContactDetailsCtx.handleShowEditNoteDialog(note: ContactNoteDto) {
        withState<ContactDetailsState.Content, _> {
            updateState {
                copy(
                    uiState = uiState.copy(
                        showEditNoteDialog = true,
                        editingNote = note,
                        noteContent = note.content
                    )
                )
            }
        }
    }

    private suspend fun ContactDetailsCtx.handleHideEditNoteDialog() {
        withState<ContactDetailsState.Content, _> {
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
    }

    private suspend fun ContactDetailsCtx.handleUpdateNoteContent(content: String) {
        withState<ContactDetailsState.Content, _> {
            updateState {
                copy(uiState = uiState.copy(noteContent = content))
            }
        }
    }

    private suspend fun ContactDetailsCtx.handleShowDeleteNoteConfirmation(note: ContactNoteDto) {
        withState<ContactDetailsState.Content, _> {
            updateState {
                copy(
                    uiState = uiState.copy(
                        showDeleteNoteConfirmation = true,
                        deletingNote = note
                    )
                )
            }
        }
    }

    private suspend fun ContactDetailsCtx.handleHideDeleteNoteConfirmation() {
        withState<ContactDetailsState.Content, _> {
            updateState {
                copy(
                    uiState = uiState.copy(
                        showDeleteNoteConfirmation = false,
                        deletingNote = null
                    )
                )
            }
        }
    }

    // ============================================================================
    // NOTES PANEL/SHEET HANDLERS
    // ============================================================================

    private suspend fun ContactDetailsCtx.handleShowNotesSidePanel() {
        withState<ContactDetailsState.Content, _> {
            updateState {
                copy(uiState = uiState.copy(showNotesSidePanel = true))
            }
        }
    }

    private suspend fun ContactDetailsCtx.handleHideNotesSidePanel() {
        withState<ContactDetailsState.Content, _> {
            updateState {
                copy(
                    uiState = uiState.copy(
                        showNotesSidePanel = false,
                        // Also reset any note editing state when closing
                        showAddNoteDialog = false,
                        showEditNoteDialog = false,
                        editingNote = null,
                        noteContent = ""
                    )
                )
            }
        }
    }

    private suspend fun ContactDetailsCtx.handleShowNotesBottomSheet() {
        withState<ContactDetailsState.Content, _> {
            updateState {
                copy(uiState = uiState.copy(showNotesBottomSheet = true))
            }
        }
    }

    private suspend fun ContactDetailsCtx.handleHideNotesBottomSheet() {
        withState<ContactDetailsState.Content, _> {
            updateState {
                copy(
                    uiState = uiState.copy(
                        showNotesBottomSheet = false,
                        // Also reset any note editing state when closing
                        showAddNoteDialog = false,
                        showEditNoteDialog = false,
                        editingNote = null,
                        noteContent = ""
                    )
                )
            }
        }
    }

    // ============================================================================
    // NOTES CRUD HANDLERS
    // ============================================================================

    private suspend fun ContactDetailsCtx.handleAddNote() {
        withState<ContactDetailsState.Content, _> {
            val content = uiState.noteContent.trim()

            if (content.isBlank()) {
                logger.w { "Cannot add empty note" }
                action(ContactDetailsAction.ShowError("Note content cannot be empty"))
                return@withState
            }

            updateState { copy(isSavingNote = true) }

            logger.d { "Adding note for contact $contactId" }

            val request = CreateContactNoteRequest(content = content)
            contactRepository.createNote(contactId, request).fold(
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
                    action(ContactDetailsAction.ShowSuccess("Note added"))
                    // Reload notes to get updated list
                    loadNotesData(contactId)
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to add note" }
                    updateState { copy(isSavingNote = false) }
                    action(ContactDetailsAction.ShowError("Failed to add note"))
                }
            )
        }
    }

    private suspend fun ContactDetailsCtx.handleUpdateNote() {
        withState<ContactDetailsState.Content, _> {
            val note = uiState.editingNote ?: return@withState
            val content = uiState.noteContent.trim()

            if (content.isBlank()) {
                logger.w { "Cannot update note with empty content" }
                action(ContactDetailsAction.ShowError("Note content cannot be empty"))
                return@withState
            }

            updateState { copy(isSavingNote = true) }

            logger.d { "Updating note ${note.id}" }

            val request = UpdateContactNoteRequest(content = content)
            contactRepository.updateNote(contactId, note.id, request).fold(
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
                    action(ContactDetailsAction.ShowSuccess("Note updated"))
                    // Reload notes to get updated list
                    loadNotesData(contactId)
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to update note" }
                    updateState { copy(isSavingNote = false) }
                    action(ContactDetailsAction.ShowError("Failed to update note"))
                }
            )
        }
    }

    private suspend fun ContactDetailsCtx.handleDeleteNote() {
        withState<ContactDetailsState.Content, _> {
            val note = uiState.deletingNote ?: return@withState

            updateState { copy(isDeletingNote = true) }

            logger.d { "Deleting note ${note.id}" }

            contactRepository.deleteNote(contactId, note.id).fold(
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
                    action(ContactDetailsAction.ShowSuccess("Note deleted"))
                    // Reload notes to get updated list
                    loadNotesData(contactId)
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
                    action(ContactDetailsAction.ShowError("Failed to delete note"))
                }
            )
        }
    }

    // ============================================================================
    // MERGE HANDLERS
    // ============================================================================

    private suspend fun ContactDetailsCtx.handleShowMergeDialog() {
        withState<ContactDetailsState.Content, _> {
            updateState {
                copy(uiState = uiState.copy(showMergeDialog = true))
            }
        }
    }

    private suspend fun ContactDetailsCtx.handleHideMergeDialog() {
        withState<ContactDetailsState.Content, _> {
            updateState {
                copy(uiState = uiState.copy(showMergeDialog = false))
            }
        }
    }

    // ============================================================================
    // ENRICHMENT HANDLERS
    // ============================================================================

    private suspend fun ContactDetailsCtx.handleShowEnrichmentPanel() {
        withState<ContactDetailsState.Content, _> {
            updateState {
                copy(uiState = uiState.copy(showEnrichmentPanel = true))
            }
        }
    }

    private suspend fun ContactDetailsCtx.handleHideEnrichmentPanel() {
        withState<ContactDetailsState.Content, _> {
            updateState {
                copy(uiState = uiState.copy(showEnrichmentPanel = false))
            }
        }
    }

    private suspend fun ContactDetailsCtx.handleApplyEnrichmentSuggestions(suggestions: List<EnrichmentSuggestion>) {
        withState<ContactDetailsState.Content, _> {
            if (suggestions.isEmpty()) {
                logger.w { "No enrichment suggestions selected" }
                return@withState
            }

            // Future implementation: Build UpdateContactRequest from selected suggestions
            // and call contactRepository.updateContact
            logger.d { "Applying ${suggestions.size} enrichment suggestions for $contactId" }

            // Remove applied suggestions from the list
            val remainingSuggestions = enrichmentSuggestions.filterNot { it in suggestions }

            updateState {
                copy(
                    enrichmentSuggestions = remainingSuggestions,
                    uiState = uiState.copy(showEnrichmentPanel = false)
                )
            }

            action(ContactDetailsAction.ShowSuccess("Applied ${suggestions.size} enrichment suggestion(s)"))
        }
    }

}
