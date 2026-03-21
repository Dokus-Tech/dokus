@file:Suppress(
    "TooGenericExceptionCaught", // Network errors need catch-all
    "LongParameterList" // DI requires many use cases
)

package tech.dokus.features.contacts.mvi

import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import pro.respawn.flowmvi.annotation.ExperimentalFlowMVIAPI
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.delegate.delegate
import pro.respawn.flowmvi.plugins.init
import pro.respawn.flowmvi.plugins.reduce
import pro.respawn.flowmvi.plugins.whileSubscribed
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.features.auth.usecases.GetCurrentTenantIdUseCase
import tech.dokus.features.contacts.mvi.extensions.toFormData
import tech.dokus.features.contacts.mvi.extensions.toUpdateRequest
import tech.dokus.features.contacts.mvi.notes.ContactNotesAction
import tech.dokus.features.contacts.mvi.notes.ContactNotesContainer
import tech.dokus.features.contacts.mvi.notes.ContactNotesIntent
import tech.dokus.features.contacts.usecases.CacheContactsUseCase
import tech.dokus.features.contacts.usecases.GetCachedContactsUseCase
import tech.dokus.features.contacts.usecases.GetContactActivityUseCase
import tech.dokus.features.contacts.usecases.GetContactInvoiceSnapshotUseCase
import tech.dokus.features.contacts.usecases.GetContactPeppolStatusUseCase
import tech.dokus.features.contacts.usecases.GetContactUseCase
import tech.dokus.features.contacts.usecases.ObserveContactChangesUseCase
import tech.dokus.features.contacts.usecases.UpdateContactUseCase
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
 * - Notes management (delegated to [ContactNotesContainer])
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
    private val getCachedContacts: GetCachedContactsUseCase,
    private val cacheContacts: CacheContactsUseCase,
    private val getCurrentTenantId: GetCurrentTenantIdUseCase,
    private val observeContactChanges: ObserveContactChangesUseCase,
    private val updateContact: UpdateContactUseCase,
    val notesContainer: ContactNotesContainer,
) : Container<ContactDetailsState, ContactDetailsIntent, ContactDetailsAction> {

    companion object {
        data class Params(
            val contactId: ContactId
        )
    }

    private val logger = Logger.forClass<ContactDetailsContainer>()

    @OptIn(ExperimentalFlowMVIAPI::class)
    override val store: Store<ContactDetailsState, ContactDetailsIntent, ContactDetailsAction> =
        store(ContactDetailsState(contactId = contactId)) {
            val notesState by delegate(notesContainer.store) { notesAction ->
                when (notesAction) {
                    is ContactNotesAction.NoteAdded ->
                        action(ContactDetailsAction.ShowSuccess(ContactDetailsSuccess.NoteAdded))
                    is ContactNotesAction.NoteUpdated ->
                        action(ContactDetailsAction.ShowSuccess(ContactDetailsSuccess.NoteUpdated))
                    is ContactNotesAction.NoteDeleted ->
                        action(ContactDetailsAction.ShowSuccess(ContactDetailsSuccess.NoteDeleted))
                    is ContactNotesAction.ShowError ->
                        action(ContactDetailsAction.ShowError(notesAction.error))
                }
            }

            whileSubscribed {
                notesState.collect { childState ->
                    updateState {
                        copy(
                            notesState = childState.notes,
                            isSavingNote = childState.isSavingNote,
                            isDeletingNote = childState.isDeletingNote,
                            uiState = uiState.copy(
                                showAddNoteDialog = childState.showAddNoteDialog,
                                showEditNoteDialog = childState.showEditNoteDialog,
                                editingNote = childState.editingNote,
                                noteContent = childState.noteContent,
                                showDeleteNoteConfirmation = childState.showDeleteNoteConfirmation,
                                deletingNote = childState.deletingNote,
                                showNotesSidePanel = childState.showNotesSidePanel,
                                showNotesBottomSheet = childState.showNotesBottomSheet,
                            ),
                        )
                    }
                }
            }

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

                    // Notes (delegated to child store)
                    is ContactDetailsIntent.Notes ->
                        notesContainer.store.intent(intent.intent)

                    // Merge
                    is ContactDetailsIntent.ShowMergeDialog -> handleShowMergeDialog()
                    is ContactDetailsIntent.HideMergeDialog -> handleHideMergeDialog()

                    // Enrichment
                    is ContactDetailsIntent.ShowEnrichmentPanel -> handleShowEnrichmentPanel()
                    is ContactDetailsIntent.HideEnrichmentPanel -> handleHideEnrichmentPanel()
                    is ContactDetailsIntent.ApplyEnrichmentSuggestions ->
                        handleApplyEnrichmentSuggestions(intent.suggestions)

                    // Inline Edit
                    is ContactDetailsIntent.StartEditing -> handleStartEditing()
                    is ContactDetailsIntent.CancelEditing -> handleCancelEditing()
                    is ContactDetailsIntent.SaveEdit -> handleSaveEdit()
                    is ContactDetailsIntent.UpdateEditFormData ->
                        handleUpdateEditFormData(intent.formData)
                }
            }
        }

    // ========================================================================
    // LOADING HANDLERS
    // ========================================================================

    private suspend fun ContactDetailsCtx.handleLoadContact(contactId: ContactId) {
        logger.d { "Loading contact details: $contactId" }

        updateState {
            copy(
                contactId = contactId,
                contact = DokusState.loading(),
                activityState = DokusState.loading(),
                invoiceSnapshotState = DokusState.loading(),
                peppolStatusState = DokusState.loading(),
            )
        }
        notesContainer.store.intent(ContactNotesIntent.LoadNotes)

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
            )
        }
        notesContainer.store.intent(ContactNotesIntent.LoadNotes)

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

    private suspend fun ContactDetailsCtx.loadDetailSections(contactId: ContactId) {
        val activityDeferred = async { getContactActivity(contactId) }
        val invoiceSnapshotDeferred = async { getContactInvoiceSnapshot(contactId) }
        val peppolStatusDeferred = async { getContactPeppolStatus(contactId) }

        applyActivityResult(activityDeferred.await())
        applyInvoiceSnapshotResult(invoiceSnapshotDeferred.await())
        applyPeppolStatusResult(peppolStatusDeferred.await())
    }

    private suspend fun ContactDetailsCtx.applyActivityResult(
        result: Result<tech.dokus.domain.model.contact.ContactActivitySummary>,
    ) {
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
        result: Result<tech.dokus.features.contacts.usecases.ContactInvoiceSnapshot>,
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
        result: Result<tech.dokus.domain.model.PeppolStatusResponse>,
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

    // ========================================================================
    // MERGE / ENRICHMENT (inlined from ContactDetailsMergeEnrichmentHandlers)
    // ========================================================================

    private suspend fun ContactDetailsCtx.handleShowMergeDialog() {
        updateState { copy(uiState = uiState.copy(showMergeDialog = true)) }
    }

    private suspend fun ContactDetailsCtx.handleHideMergeDialog() {
        updateState { copy(uiState = uiState.copy(showMergeDialog = false)) }
    }

    private suspend fun ContactDetailsCtx.handleShowEnrichmentPanel() {
        updateState { copy(uiState = uiState.copy(showEnrichmentPanel = true)) }
    }

    private suspend fun ContactDetailsCtx.handleHideEnrichmentPanel() {
        updateState { copy(uiState = uiState.copy(showEnrichmentPanel = false)) }
    }

    private suspend fun ContactDetailsCtx.handleApplyEnrichmentSuggestions(
        suggestions: List<EnrichmentSuggestion>,
    ) {
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

    // ========================================================================
    // INLINE EDIT (inlined from ContactDetailsEditHandlers)
    // ========================================================================

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

    // ========================================================================
    // CACHE HELPER
    // ========================================================================

    @Suppress("TooGenericExceptionCaught")
    private suspend fun cacheContact(contact: ContactDto) {
        val tenantId = getCurrentTenantId() ?: return
        try {
            cacheContacts(tenantId, listOf(contact))
            logger.d { "Cached contact: ${contact.name}" }
        } catch (e: Exception) {
            logger.w(e) { "Failed to cache contact" }
        }
    }
}
