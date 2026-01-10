@file:Suppress(
    "TooManyFunctions", // Container handles contact details workflow
    "TooGenericExceptionCaught", // Network errors need catch-all
    "LongParameterList" // DI requires many use cases
)

package tech.dokus.features.contacts.mvi

import kotlinx.coroutines.async
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.dsl.withState
import pro.respawn.flowmvi.plugins.reduce
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.domain.model.contact.ContactNoteDto
import tech.dokus.domain.model.contact.CreateContactNoteRequest
import tech.dokus.domain.model.contact.UpdateContactNoteRequest
import tech.dokus.features.auth.usecases.GetCurrentTenantIdUseCase
import tech.dokus.features.contacts.usecases.CacheContactsUseCase
import tech.dokus.features.contacts.usecases.CreateContactNoteUseCase
import tech.dokus.features.contacts.usecases.DeleteContactNoteUseCase
import tech.dokus.features.contacts.usecases.GetCachedContactsUseCase
import tech.dokus.features.contacts.usecases.GetContactActivityUseCase
import tech.dokus.features.contacts.usecases.GetContactUseCase
import tech.dokus.features.contacts.usecases.ListContactNotesUseCase
import tech.dokus.features.contacts.usecases.UpdateContactNoteUseCase
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.platform.Logger

internal typealias ContactDetailsCtx = PipelineContext<ContactDetailsState, ContactDetailsIntent, ContactDetailsAction>

/**
 * Container for the Contact Details screen using FlowMVI.
 *
 * Manages contact details display, notes CRUD, merge, and enrichment features.
 * Implements cache-first strategy for offline support.
 *
 * Features:
 * - Contact information display
 * - Activity summary (invoices, bills, expenses)
 * - Notes management (add, edit, delete)
 * - Enrichment suggestions
 * - Contact merge functionality
 *
 * Use with Koin's `container<>` DSL for automatic ViewModel wrapping and lifecycle management.
 */
internal class ContactDetailsContainer(
    contactId: ContactId,
    private val getContact: GetContactUseCase,
    private val getContactActivity: GetContactActivityUseCase,
    private val listContactNotes: ListContactNotesUseCase,
    private val createContactNote: CreateContactNoteUseCase,
    private val updateContactNote: UpdateContactNoteUseCase,
    private val deleteContactNote: DeleteContactNoteUseCase,
    private val getCachedContacts: GetCachedContactsUseCase,
    private val cacheContacts: CacheContactsUseCase,
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
                    is ContactDetailsIntent.ApplyEnrichmentSuggestions -> handleApplyEnrichmentSuggestions(
                        intent.suggestions
                    )
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
        getContact(contactId).fold(
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
        val tenantId = getCurrentTenantId() ?: return null
        return try {
            getCachedContacts(tenantId).find { it.id == contactId }
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
            cacheContacts(tenantId, listOf(contact))
            logger.d { "Cached contact: ${contact.name}" }
        } catch (e: Exception) {
            logger.w(e) { "Failed to cache contact" }
        }
    }

    /**
     * Load activity summary from API.
     */
    private suspend fun ContactDetailsCtx.loadActivityData(contactId: ContactId) {
        getContactActivity(contactId).fold(
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
        listContactNotes(contactId).fold(
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
                action(ContactDetailsAction.ShowError(DokusException.Validation.NoteContentRequired))
                return@withState
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
                            uiState = uiState.copy(
                                showAddNoteDialog = false,
                                noteContent = ""
                            )
                        )
                    }
                    action(ContactDetailsAction.ShowSuccess(ContactDetailsSuccess.NoteAdded))
                    // Reload notes to get updated list
                    loadNotesData(contactId)
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
    }

    private suspend fun ContactDetailsCtx.handleUpdateNote() {
        withState<ContactDetailsState.Content, _> {
            val note = uiState.editingNote ?: return@withState
            val content = uiState.noteContent.trim()

            if (content.isBlank()) {
                logger.w { "Cannot update note with empty content" }
                action(ContactDetailsAction.ShowError(DokusException.Validation.NoteContentRequired))
                return@withState
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
                            uiState = uiState.copy(
                                showEditNoteDialog = false,
                                editingNote = null,
                                noteContent = ""
                            )
                        )
                    }
                    action(ContactDetailsAction.ShowSuccess(ContactDetailsSuccess.NoteUpdated))
                    // Reload notes to get updated list
                    loadNotesData(contactId)
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
    }

    private suspend fun ContactDetailsCtx.handleDeleteNote() {
        withState<ContactDetailsState.Content, _> {
            val note = uiState.deletingNote ?: return@withState

            updateState { copy(isDeletingNote = true) }

            logger.d { "Deleting note ${note.id}" }

            deleteContactNote(contactId, note.id).fold(
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

            action(
                ContactDetailsAction.ShowSuccess(
                    ContactDetailsSuccess.EnrichmentApplied(suggestions.size)
                )
            )
        }
    }
}
