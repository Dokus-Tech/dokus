@file:Suppress(
    "TooManyFunctions", // Container handles contact details workflow
    "TooGenericExceptionCaught", // Network errors need catch-all
    "LongParameterList" // DI requires many use cases
)

package tech.dokus.features.contacts.mvi

import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.init
import pro.respawn.flowmvi.plugins.reduce
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.domain.model.contact.CreateContactNoteRequest
import tech.dokus.domain.model.contact.UpdateContactNoteRequest
import tech.dokus.features.auth.usecases.GetCurrentTenantIdUseCase
import tech.dokus.features.contacts.usecases.CacheContactsUseCase
import tech.dokus.features.contacts.usecases.CreateContactNoteUseCase
import tech.dokus.features.contacts.usecases.DeleteContactNoteUseCase
import tech.dokus.features.contacts.usecases.GetCachedContactsUseCase
import tech.dokus.features.contacts.usecases.GetContactActivityUseCase
import tech.dokus.features.contacts.usecases.GetContactInvoiceSnapshotUseCase
import tech.dokus.features.contacts.usecases.GetContactPeppolStatusUseCase
import tech.dokus.features.contacts.usecases.GetContactUseCase
import tech.dokus.features.contacts.usecases.ListContactNotesUseCase
import tech.dokus.features.contacts.usecases.ObserveContactChangesUseCase
import tech.dokus.features.contacts.usecases.UpdateContactNoteUseCase
import tech.dokus.features.contacts.usecases.UpdateContactUseCase
import tech.dokus.features.contacts.mvi.extensions.toFormData
import tech.dokus.features.contacts.mvi.extensions.toUpdateRequest
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.app.state.isSuccess
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
 * - Activity summary (invoices, inbound invoices, expenses)
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
    private val getContactInvoiceSnapshot: GetContactInvoiceSnapshotUseCase,
    private val getContactPeppolStatus: GetContactPeppolStatusUseCase,
    private val listContactNotes: ListContactNotesUseCase,
    private val createContactNote: CreateContactNoteUseCase,
    private val updateContactNote: UpdateContactNoteUseCase,
    private val deleteContactNote: DeleteContactNoteUseCase,
    private val getCachedContacts: GetCachedContactsUseCase,
    private val cacheContacts: CacheContactsUseCase,
    private val getCurrentTenantId: GetCurrentTenantIdUseCase,
    private val observeContactChanges: ObserveContactChangesUseCase,
    private val updateContact: UpdateContactUseCase,
) : Container<ContactDetailsState, ContactDetailsIntent, ContactDetailsAction> {

    companion object {
        data class Params(
            val contactId: ContactId
        )
    }

    private val logger = Logger.forClass<ContactDetailsContainer>()

    override val store: Store<ContactDetailsState, ContactDetailsIntent, ContactDetailsAction> =
        store(ContactDetailsState(contactId = contactId)) {
            init {
                launch {
                    observeContactChanges(contactId).collect {
                        logger.d { "SSE: contact changed, refreshing $contactId" }
                        intent(ContactDetailsIntent.Refresh)
                    }
                }
            }
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

                    // Inline Edit
                    is ContactDetailsIntent.StartEditing -> handleStartEditing()
                    is ContactDetailsIntent.CancelEditing -> handleCancelEditing()
                    is ContactDetailsIntent.SaveEdit -> handleSaveEdit()
                    is ContactDetailsIntent.UpdateEditFormData -> handleUpdateEditFormData(intent.formData)
                }
            }
        }

    // ============================================================================
    // LOADING HANDLERS
    // ============================================================================

    private suspend fun ContactDetailsCtx.handleLoadContact(contactId: ContactId) {
        logger.d { "Loading contact details: $contactId" }

        updateState {
            copy(
                contactId = contactId,
                contact = DokusState.loading(),
                activityState = DokusState.loading(),
                invoiceSnapshotState = DokusState.loading(),
                peppolStatusState = DokusState.loading(),
                notesState = DokusState.loading()
            )
        }

        val contact = loadContactData(contactId)
        if (contact == null) return

        loadDetailSections(contactId)
    }

    private suspend fun ContactDetailsCtx.handleRefresh() {
        var capturedContactId: ContactId? = null

        updateState {
            capturedContactId = contactId
            copy(
                activityState = DokusState.loading(),
                invoiceSnapshotState = DokusState.loading(),
                peppolStatusState = DokusState.loading(),
                notesState = DokusState.loading()
            )
        }

        val currentContactId = capturedContactId ?: return
        logger.d { "Refreshing contact details: $currentContactId" }

        getContact(currentContactId).fold(
            onSuccess = { contact ->
                updateState { copy(contact = DokusState.success(contact)) }
                cacheContact(contact)
            },
            onFailure = { error ->
                logger.e(error) { "Failed to refresh contact: $currentContactId" }
            }
        )

        loadDetailSections(currentContactId)
    }

    /**
     * Load contact data with cache-first pattern.
     * Shows cached data immediately, then refreshes from network.
     */
    private suspend fun ContactDetailsCtx.loadContactData(contactId: ContactId): ContactDto? {
        // Try cache first for immediate display
        val cached = loadContactFromCache(contactId)
        var resolvedContact: ContactDto? = cached
        if (cached != null) {
            logger.d { "Loaded contact from cache: ${cached.name}" }
            updateState { copy(contact = DokusState.success(cached)) }
        }

        // Then try network refresh
        getContact(contactId).fold(
            onSuccess = { contact ->
                logger.i { "Loaded contact from network: ${contact.name}" }
                updateState { copy(contact = DokusState.success(contact)) }
                // Update cache with fresh data
                cacheContact(contact)
                resolvedContact = contact
            },
            onFailure = { error ->
                logger.e(error) { "Failed to load contact from network: $contactId" }
                // Only show error if we have no cached data
                if (cached == null) {
                    updateState {
                        copy(
                            contact = DokusState.error(
                                exception = error.asDokusException,
                                retryHandler = { intent(ContactDetailsIntent.Refresh) }
                            )
                        )
                    }
                }
                // If we have cached data, silently keep showing it
            }
        )

        return resolvedContact
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

    private suspend fun ContactDetailsCtx.loadDetailSections(contactId: ContactId) {
        val activityDeferred = async { getContactActivity(contactId) }
        val invoiceSnapshotDeferred = async { getContactInvoiceSnapshot(contactId) }
        val peppolStatusDeferred = async { getContactPeppolStatus(contactId) }
        val notesDeferred = async { listContactNotes(contactId) }

        applyActivityResult(activityDeferred.await())
        applyInvoiceSnapshotResult(invoiceSnapshotDeferred.await())
        applyPeppolStatusResult(peppolStatusDeferred.await())
        applyNotesResult(notesDeferred.await())
    }

    private suspend fun ContactDetailsCtx.applyActivityResult(result: Result<tech.dokus.domain.model.contact.ContactActivitySummary>) {
        result.fold(
            onSuccess = { activity ->
                logger.i { "Loaded activity: invoices=${activity.invoiceCount}, inbound invoices=${activity.inboundInvoiceCount}" }
                updateState { copy(activityState = DokusState.success(activity)) }
            },
            onFailure = { error ->
                logger.e(error) { "Failed to load activity" }
                updateState {
                    copy(
                        activityState = DokusState.error(
                            exception = error.asDokusException,
                            retryHandler = { intent(ContactDetailsIntent.Refresh) }
                        )
                    )
                }
            }
        )
    }

    private suspend fun ContactDetailsCtx.applyInvoiceSnapshotResult(
        result: Result<tech.dokus.features.contacts.usecases.ContactInvoiceSnapshot>
    ) {
        result.fold(
            onSuccess = { snapshot ->
                logger.i { "Loaded invoice snapshot: docs=${snapshot.documentsCount}" }
                updateState { copy(invoiceSnapshotState = DokusState.success(snapshot)) }
            },
            onFailure = { error ->
                logger.e(error) { "Failed to load invoice snapshot" }
                updateState {
                    copy(
                        invoiceSnapshotState = DokusState.error(
                            exception = error.asDokusException,
                            retryHandler = { intent(ContactDetailsIntent.Refresh) }
                        )
                    )
                }
            }
        )
    }

    private suspend fun ContactDetailsCtx.applyPeppolStatusResult(
        result: Result<tech.dokus.domain.model.PeppolStatusResponse>
    ) {
        result.fold(
            onSuccess = { status ->
                logger.i { "Loaded PEPPOL status: ${status.status}" }
                updateState { copy(peppolStatusState = DokusState.success(status)) }
            },
            onFailure = { error ->
                logger.e(error) { "Failed to load PEPPOL status" }
                updateState {
                    copy(
                        peppolStatusState = DokusState.error(
                            exception = error.asDokusException,
                            retryHandler = { intent(ContactDetailsIntent.Refresh) }
                        )
                    )
                }
            }
        )
    }

    private suspend fun ContactDetailsCtx.applyNotesResult(result: Result<List<tech.dokus.domain.model.contact.ContactNoteDto>>) {
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

    // ============================================================================
    // NOTES DIALOG HANDLERS
    // ============================================================================

    private suspend fun ContactDetailsCtx.handleShowAddNoteDialog() {
        updateState {
            copy(uiState = uiState.resetNoteTransientState().copy(showAddNoteDialog = true))
        }
    }

    private suspend fun ContactDetailsCtx.handleHideAddNoteDialog() {
        updateState {
            copy(
                uiState = uiState.copy(
                    showAddNoteDialog = false,
                    noteContent = ""
                )
            )
        }
    }

    private suspend fun ContactDetailsCtx.handleShowEditNoteDialog(note: tech.dokus.domain.model.contact.ContactNoteDto) {
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

    private suspend fun ContactDetailsCtx.handleHideEditNoteDialog() {
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

    private suspend fun ContactDetailsCtx.handleUpdateNoteContent(content: String) {
        updateState {
            copy(uiState = uiState.copy(noteContent = content))
        }
    }

    private suspend fun ContactDetailsCtx.handleShowDeleteNoteConfirmation(note: tech.dokus.domain.model.contact.ContactNoteDto) {
        updateState {
            copy(
                uiState = uiState.resetNoteTransientState().copy(
                    showDeleteNoteConfirmation = true,
                    deletingNote = note,
                )
            )
        }
    }

    private suspend fun ContactDetailsCtx.handleHideDeleteNoteConfirmation() {
        updateState {
            copy(
                uiState = uiState.copy(
                    showDeleteNoteConfirmation = false,
                    deletingNote = null
                )
            )
        }
    }

    // ============================================================================
    // NOTES PANEL/SHEET HANDLERS
    // ============================================================================

    private suspend fun ContactDetailsCtx.handleShowNotesSidePanel() {
        updateState {
            copy(uiState = uiState.copy(showNotesSidePanel = true))
        }
    }

    private suspend fun ContactDetailsCtx.handleHideNotesSidePanel() {
        updateState {
            copy(uiState = uiState.resetNoteTransientState().copy(showNotesSidePanel = false))
        }
    }

    private suspend fun ContactDetailsCtx.handleShowNotesBottomSheet() {
        updateState {
            copy(uiState = uiState.copy(showNotesBottomSheet = true))
        }
    }

    private suspend fun ContactDetailsCtx.handleHideNotesBottomSheet() {
        updateState {
            copy(uiState = uiState.resetNoteTransientState().copy(showNotesBottomSheet = false))
        }
    }

    // ============================================================================
    // NOTES CRUD HANDLERS
    // ============================================================================

    private suspend fun ContactDetailsCtx.handleAddNote() {
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

    private suspend fun ContactDetailsCtx.handleUpdateNote() {
        var capturedContactId: ContactId? = null
        var capturedNote: tech.dokus.domain.model.contact.ContactNoteDto? = null
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

    private suspend fun ContactDetailsCtx.handleDeleteNote() {
        var capturedContactId: ContactId? = null
        var capturedNote: tech.dokus.domain.model.contact.ContactNoteDto? = null

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

    // ============================================================================
    // MERGE HANDLERS
    // ============================================================================

    private suspend fun ContactDetailsCtx.handleShowMergeDialog() {
        updateState {
            copy(uiState = uiState.copy(showMergeDialog = true))
        }
    }

    private suspend fun ContactDetailsCtx.handleHideMergeDialog() {
        updateState {
            copy(uiState = uiState.copy(showMergeDialog = false))
        }
    }

    // ============================================================================
    // ENRICHMENT HANDLERS
    // ============================================================================

    private suspend fun ContactDetailsCtx.handleShowEnrichmentPanel() {
        updateState {
            copy(uiState = uiState.copy(showEnrichmentPanel = true))
        }
    }

    private suspend fun ContactDetailsCtx.handleHideEnrichmentPanel() {
        updateState {
            copy(uiState = uiState.copy(showEnrichmentPanel = false))
        }
    }

    private suspend fun ContactDetailsCtx.handleApplyEnrichmentSuggestions(suggestions: List<EnrichmentSuggestion>) {
        if (suggestions.isEmpty()) {
            logger.w { "No enrichment suggestions selected" }
            return
        }

        // Future implementation: Build UpdateContactRequest from selected suggestions
        // and call contactRepository.updateContact
        logger.d { "Applying ${suggestions.size} enrichment suggestions" }

        updateState {
            val remainingSuggestions = enrichmentSuggestions.filterNot { it in suggestions }
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

    // ============================================================================
    // INLINE EDIT HANDLERS
    // ============================================================================

    private suspend fun ContactDetailsCtx.handleStartEditing() {
        withState {
            if (!contact.isSuccess()) return@withState
            val formData = contact.data.toFormData()
            updateState { copy(editFormData = formData) }
        }
    }

    private suspend fun ContactDetailsCtx.handleCancelEditing() {
        updateState { copy(editFormData = null) }
    }

    private suspend fun ContactDetailsCtx.handleUpdateEditFormData(formData: ContactFormData) {
        updateState { copy(editFormData = formData) }
    }

    private suspend fun ContactDetailsCtx.handleSaveEdit() {
        var capturedContactId: ContactId? = null
        var capturedFormData: ContactFormData? = null

        withState {
            capturedContactId = contactId
            capturedFormData = editFormData
        }

        val editContactId = capturedContactId ?: return
        val form = capturedFormData ?: return

        if (form.name.value.isBlank()) {
            updateState {
                copy(
                    editFormData = form.copy(
                        errors = mapOf("name" to DokusException.Validation.ContactNameRequired)
                    )
                )
            }
            return
        }

        updateState { copy(isSavingEdit = true) }
        logger.d { "Saving inline edit for contact $editContactId" }

        val request = form.toUpdateRequest()
        updateContact(editContactId, request).fold(
            onSuccess = { updatedContact ->
                logger.i { "Contact updated: ${updatedContact.id}" }
                updateState {
                    copy(
                        contact = DokusState.success(updatedContact),
                        editFormData = null,
                        isSavingEdit = false,
                    )
                }
                cacheContact(updatedContact)
                action(ContactDetailsAction.ShowSuccess(ContactDetailsSuccess.ContactUpdated))
            },
            onFailure = { error ->
                logger.e(error) { "Failed to update contact: $editContactId" }
                updateState { copy(isSavingEdit = false) }
                action(ContactDetailsAction.ShowError(error.asDokusException))
            }
        )
    }
}
