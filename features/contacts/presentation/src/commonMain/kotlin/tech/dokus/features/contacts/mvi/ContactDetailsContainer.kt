@file:Suppress(
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
    createContactNote: CreateContactNoteUseCase,
    updateContactNote: UpdateContactNoteUseCase,
    deleteContactNote: DeleteContactNoteUseCase,
    private val getCachedContacts: GetCachedContactsUseCase,
    cacheContacts: CacheContactsUseCase,
    private val getCurrentTenantId: GetCurrentTenantIdUseCase,
    private val observeContactChanges: ObserveContactChangesUseCase,
    updateContact: UpdateContactUseCase,
) : Container<ContactDetailsState, ContactDetailsIntent, ContactDetailsAction> {

    companion object {
        data class Params(
            val contactId: ContactId
        )
    }

    private val logger = Logger.forClass<ContactDetailsContainer>()

    private val noteHandlers = ContactDetailsNoteHandlers(
        listContactNotes = listContactNotes,
        createContactNote = createContactNote,
        updateContactNote = updateContactNote,
        deleteContactNote = deleteContactNote,
    )

    private val editHandlers = ContactDetailsEditHandlers(
        updateContact = updateContact,
        getCurrentTenantId = getCurrentTenantId,
        cacheContacts = cacheContacts,
    )

    private val mergeEnrichmentHandlers = ContactDetailsMergeEnrichmentHandlers()

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
                    is ContactDetailsIntent.ShowAddNoteDialog ->
                        with(noteHandlers) { handleShowAddNoteDialog() }
                    is ContactDetailsIntent.HideAddNoteDialog ->
                        with(noteHandlers) { handleHideAddNoteDialog() }
                    is ContactDetailsIntent.ShowEditNoteDialog ->
                        with(noteHandlers) { handleShowEditNoteDialog(intent.note) }
                    is ContactDetailsIntent.HideEditNoteDialog ->
                        with(noteHandlers) { handleHideEditNoteDialog() }
                    is ContactDetailsIntent.UpdateNoteContent ->
                        with(noteHandlers) { handleUpdateNoteContent(intent.content) }
                    is ContactDetailsIntent.ShowDeleteNoteConfirmation ->
                        with(noteHandlers) { handleShowDeleteNoteConfirmation(intent.note) }
                    is ContactDetailsIntent.HideDeleteNoteConfirmation ->
                        with(noteHandlers) { handleHideDeleteNoteConfirmation() }

                    // Notes Panel/Sheet Visibility
                    is ContactDetailsIntent.ShowNotesSidePanel ->
                        with(noteHandlers) { handleShowNotesSidePanel() }
                    is ContactDetailsIntent.HideNotesSidePanel ->
                        with(noteHandlers) { handleHideNotesSidePanel() }
                    is ContactDetailsIntent.ShowNotesBottomSheet ->
                        with(noteHandlers) { handleShowNotesBottomSheet() }
                    is ContactDetailsIntent.HideNotesBottomSheet ->
                        with(noteHandlers) { handleHideNotesBottomSheet() }

                    // Notes Operations
                    is ContactDetailsIntent.AddNote ->
                        with(noteHandlers) { handleAddNote() }
                    is ContactDetailsIntent.UpdateNote ->
                        with(noteHandlers) { handleUpdateNote() }
                    is ContactDetailsIntent.DeleteNote ->
                        with(noteHandlers) { handleDeleteNote() }

                    // Merge
                    is ContactDetailsIntent.ShowMergeDialog ->
                        with(mergeEnrichmentHandlers) { handleShowMergeDialog() }
                    is ContactDetailsIntent.HideMergeDialog ->
                        with(mergeEnrichmentHandlers) { handleHideMergeDialog() }

                    // Enrichment
                    is ContactDetailsIntent.ShowEnrichmentPanel ->
                        with(mergeEnrichmentHandlers) { handleShowEnrichmentPanel() }
                    is ContactDetailsIntent.HideEnrichmentPanel ->
                        with(mergeEnrichmentHandlers) { handleHideEnrichmentPanel() }
                    is ContactDetailsIntent.ApplyEnrichmentSuggestions ->
                        with(mergeEnrichmentHandlers) {
                            handleApplyEnrichmentSuggestions(intent.suggestions)
                        }

                    // Inline Edit
                    is ContactDetailsIntent.StartEditing ->
                        with(editHandlers) { handleStartEditing() }
                    is ContactDetailsIntent.CancelEditing ->
                        with(editHandlers) { handleCancelEditing() }
                    is ContactDetailsIntent.SaveEdit ->
                        with(editHandlers) { handleSaveEdit() }
                    is ContactDetailsIntent.UpdateEditFormData ->
                        with(editHandlers) { handleUpdateEditFormData(intent.formData) }
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
                editHandlers.cacheContact(contact)
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
                editHandlers.cacheContact(contact)
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
        val notesDeferred = async { listContactNotes(contactId) }

        applyActivityResult(activityDeferred.await())
        applyInvoiceSnapshotResult(invoiceSnapshotDeferred.await())
        applyPeppolStatusResult(peppolStatusDeferred.await())
        with(noteHandlers) { applyNotesResult(notesDeferred.await()) }
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
}
