package tech.dokus.features.cashflow.presentation.review

import pro.respawn.flowmvi.dsl.withState
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.model.contact.SuggestedContact
import tech.dokus.features.cashflow.usecases.GetDocumentRecordUseCase
import tech.dokus.features.contacts.usecases.GetContactUseCase
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.app.state.isSuccess
import tech.dokus.foundation.platform.Logger

internal class DocumentReviewLoader(
    private val getDocumentRecord: GetDocumentRecordUseCase,
    private val getContact: GetContactUseCase,
    private val logger: Logger,
) {
    suspend fun DocumentReviewCtx.handleLoadDocument(documentId: DocumentId) {
        logger.d { "Loading document: $documentId" }
        updateState {
            copy(
                document = document.asLoading,
                isAwaitingExtraction = false,
                selectedQueueDocumentId = if (queueState != null) documentId else selectedQueueDocumentId,
            )
        }

        fetchDocumentProcessing(documentId)
    }

    suspend fun DocumentReviewCtx.handleRefresh() {
        withState {
            val activeDocumentId = documentId ?: return@withState
            if (hasContent || isAwaitingExtraction) {
                logger.d { "Refreshing document: $activeDocumentId" }
                fetchDocumentProcessing(activeDocumentId)
            }
        }
    }

    suspend fun DocumentReviewCtx.handleApplyRemoteSnapshot(record: DocumentRecordDto) {
        withState {
            val activeDocumentId = documentId ?: run {
                logger.d { "Dropping remote snapshot: no active document in current state" }
                return@withState
            }
            if (activeDocumentId != record.document.id) return@withState

            logger.d { "Applying remote snapshot for document: $activeDocumentId" }
            transitionToDocumentState(
                documentId = activeDocumentId,
                document = record,
            )
        }
    }

    private suspend fun DocumentReviewCtx.fetchDocumentProcessing(documentId: DocumentId) {
        getDocumentRecord(documentId)
            .fold(
                onSuccess = { document ->
                    transitionToDocumentState(
                        documentId = documentId,
                        document = document,
                    )
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to load document: $documentId" }
                    updateState {
                        copy(
                            document = DokusState.error(
                                exception = error.asDokusException,
                                retryHandler = { intent(DocumentReviewIntent.LoadDocument(documentId)) },
                            ),
                            isAwaitingExtraction = false,
                        )
                    }
                }
            )
    }

    private suspend fun DocumentReviewCtx.transitionToDocumentState(
        documentId: DocumentId,
        document: DocumentRecordDto,
    ) {
        val previewUrl = document.document.downloadUrl
        var previousPreviewUrl: String? = null
        var previousPreviewState: DocumentPreviewState = DocumentPreviewState.Loading
        withState {
            previousPreviewUrl = this.previewUrl
            previousPreviewState = this.previewState
        }
        val shouldReloadPreview = previousPreviewUrl != previewUrl ||
            previousPreviewState is DocumentPreviewState.Loading
        val previewState = if (previousPreviewUrl == previewUrl) {
            previousPreviewState
        } else {
            DocumentPreviewState.Loading
        }
        var selectedQueueDocumentId: DocumentId? = null
        withState {
            selectedQueueDocumentId = if (queueState != null) documentId else null
        }
        val draft = document.draft
        val extractedData = draft?.extractedData

        if (extractedData == null) {
            val isFailed = document.latestIngestion?.status == IngestionStatus.Failed
            if (isFailed) {
                // Failed extraction -> show as content with null draft data
                updateState {
                    copy(
                        document = DokusState.success(
                            ReviewDocumentData(
                                documentId = documentId,
                                documentRecord = document,
                                draftData = null,
                                originalData = null,
                                previewUrl = previewUrl,
                                contactSuggestions = emptyList(),
                            )
                        ),
                        isAwaitingExtraction = false,
                        previewState = previewState,
                        // Preserve existing UI state where available
                        isContactRequired = false,
                        counterpartyIntent = draft?.counterpartyIntent ?: tech.dokus.domain.enums.CounterpartyIntent.None,
                        isDocumentConfirmed = draft?.documentStatus == DocumentStatus.Confirmed,
                        isDocumentRejected = draft?.documentStatus == DocumentStatus.Rejected,
                        confirmedCashflowEntryId = document.cashflowEntryId,
                        selectedQueueDocumentId = selectedQueueDocumentId ?: this@transitionToDocumentState.let {
                            var id: DocumentId? = null
                            withState { id = this.selectedQueueDocumentId }
                            id
                        },
                    )
                }
            } else {
                // Still awaiting extraction
                updateState {
                    copy(
                        document = DokusState.success(
                            ReviewDocumentData(
                                documentId = documentId,
                                documentRecord = document,
                                draftData = null,
                                originalData = null,
                                previewUrl = previewUrl,
                                contactSuggestions = emptyList(),
                            )
                        ),
                        isAwaitingExtraction = true,
                        previewState = previewState,
                        selectedQueueDocumentId = selectedQueueDocumentId ?: this@transitionToDocumentState.let {
                            var id: DocumentId? = null
                            withState { id = this.selectedQueueDocumentId }
                            id
                        },
                    )
                }
            }

            if (shouldReloadPreview) {
                intent(DocumentReviewIntent.LoadPreviewPages)
            }
            return
        }

        val contactSuggestions = draft.contactSuggestions.map { suggestion ->
            ContactSuggestion(
                contactId = suggestion.contactId,
                name = suggestion.name,
                vatNumber = suggestion.vatNumber?.value
            )
        }
        val documentStatus = draft.documentStatus
        val isDocumentConfirmed = documentStatus == DocumentStatus.Confirmed
        val isDocumentRejected = documentStatus == DocumentStatus.Rejected
        val counterpartyIntent = draft.counterpartyIntent
        val (contactSelectionState, linkedContactId, selectedContactSnapshot) =
            buildContactSelectionState(document, draft.contactSuggestions)

        // Preserve contact snapshot if same contact
        var preservedContactSnapshot: ContactSnapshot? = selectedContactSnapshot
        withState {
            if (linkedContactId != null && linkedContactId == this.selectedContactId) {
                preservedContactSnapshot = this.selectedContactSnapshot
            }
        }

        // Read previous cashflow state for preservation
        var previousCashflowEntryState: DokusState<tech.dokus.domain.model.CashflowEntry> = DokusState.idle()
        var previousAutoPaymentStatus: DokusState<tech.dokus.domain.model.AutoPaymentStatusDto> = DokusState.idle()
        var previousConfirmedCashflowEntryId: tech.dokus.domain.ids.CashflowEntryId? = null
        withState {
            if (hasContent) {
                previousCashflowEntryState = cashflowEntryState
                previousAutoPaymentStatus = autoPaymentStatus
                previousConfirmedCashflowEntryId = confirmedCashflowEntryId
            }
        }

        updateState {
            copy(
                document = DokusState.success(
                    ReviewDocumentData(
                        documentId = documentId,
                        documentRecord = document,
                        draftData = extractedData,
                        originalData = draft.aiDraftData ?: extractedData,
                        previewUrl = previewUrl,
                        contactSuggestions = contactSuggestions,
                    )
                ),
                isAwaitingExtraction = false,
                previewState = previewState,
                selectedContactId = linkedContactId,
                selectedContactSnapshot = preservedContactSnapshot,
                contactSelectionState = contactSelectionState,
                isContactRequired = extractedData.isContactRequired,
                counterpartyIntent = counterpartyIntent,
                isDocumentConfirmed = isDocumentConfirmed,
                isDocumentRejected = isDocumentRejected,
                confirmedCashflowEntryId = document.cashflowEntryId,
                cashflowEntryState = previousCashflowEntryState,
                autoPaymentStatus = previousAutoPaymentStatus,
                selectedQueueDocumentId = selectedQueueDocumentId ?: this@transitionToDocumentState.let {
                    var id: DocumentId? = null
                    withState { id = this.selectedQueueDocumentId }
                    id
                },
            )
        }

        if (shouldReloadPreview) {
            intent(DocumentReviewIntent.LoadPreviewPages)
        }
        if (document.cashflowEntryId != null && document.cashflowEntryId != previousConfirmedCashflowEntryId) {
            intent(DocumentReviewIntent.LoadCashflowEntry)
        }
        if (linkedContactId != null && preservedContactSnapshot == null) {
            fetchContactSnapshot(linkedContactId)
        }
    }

    private fun buildContactSelectionState(
        document: DocumentRecordDto,
        suggestions: List<tech.dokus.domain.model.contact.SuggestedContact>
    ): Triple<ContactSelectionState, ContactId?, ContactSnapshot?> {
        val draft = document.draft ?: return Triple(ContactSelectionState.NoContact, null, null)
        val linkedContactId = draft.linkedContactId
        if (linkedContactId != null) {
            return Triple(ContactSelectionState.Selected, linkedContactId, null)
        }
        val topSuggestion = suggestions.firstOrNull()
        if (topSuggestion != null) {
            return Triple(
                ContactSelectionState.Suggested(
                    contactId = topSuggestion.contactId,
                    name = topSuggestion.name,
                    vatNumber = topSuggestion.vatNumber?.value,
                ),
                null,
                null
            )
        }
        return Triple(ContactSelectionState.NoContact, null, null)
    }

    private suspend fun DocumentReviewCtx.fetchContactSnapshot(contactId: ContactId) {
        getContact(contactId).fold(
            onSuccess = { contact ->
                withState {
                    updateState {
                        copy(
                            selectedContactSnapshot = ContactSnapshot(
                                id = contact.id,
                                name = contact.name.value,
                                vatNumber = contact.vatNumber?.value,
                                email = contact.email?.value,
                                avatarUrl = contact.avatar?.small,
                            )
                        )
                    }
                }
            },
            onFailure = { error ->
                logger.w(error) { "Failed to fetch contact snapshot for $contactId" }
            }
        )
    }
}
