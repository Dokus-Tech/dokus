package tech.dokus.features.cashflow.presentation.review

import pro.respawn.flowmvi.dsl.withState
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.features.cashflow.usecases.GetDocumentRecordUseCase
import tech.dokus.features.contacts.usecases.GetContactUseCase
import tech.dokus.foundation.platform.Logger

internal class DocumentReviewLoader(
    private val getDocumentRecord: GetDocumentRecordUseCase,
    private val getContact: GetContactUseCase,
    private val logger: Logger,
) {
    suspend fun DocumentReviewCtx.handleLoadDocument(documentId: DocumentId) {
        logger.d { "Loading document: $documentId" }
        val queueSnapshot = currentQueueSnapshot()
        updateState {
            DocumentReviewState.Loading(
                queueState = queueSnapshot.queueState,
                selectedQueueDocumentId = if (queueSnapshot.queueState != null) documentId else null
            )
        }

        fetchDocumentProcessing(documentId)
    }

    suspend fun DocumentReviewCtx.handleRefresh() {
        withState<DocumentReviewState.Content, _> {
            logger.d { "Refreshing document: $documentId" }
            fetchDocumentProcessing(documentId)
        }
        withState<DocumentReviewState.AwaitingExtraction, _> {
            logger.d { "Refreshing document: $documentId" }
            fetchDocumentProcessing(documentId)
        }
    }

    private suspend fun DocumentReviewCtx.fetchDocumentProcessing(documentId: DocumentId) {
        getDocumentRecord(documentId)
            .fold(
                onSuccess = { document ->
                    transitionToContent(documentId, document)
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to load document: $documentId" }
                    updateState {
                        DocumentReviewState.Error(
                            exception = error.asDokusException,
                            retryHandler = { intent(DocumentReviewIntent.LoadDocument(documentId)) }
                        )
                    }
                }
            )
    }

    private suspend fun DocumentReviewCtx.transitionToContent(
        documentId: DocumentId,
        document: DocumentRecordDto
    ) {
        val queueSnapshot = currentQueueSnapshot()
        val draft = document.draft
        val extractedData = draft?.extractedData
        if (extractedData == null) {
            val isFailed = document.latestIngestion?.status == IngestionStatus.Failed
            if (isFailed) {
                // Transition to Content with null draftData so AnalysisFailedBanner shows
                updateState {
                    DocumentReviewState.Content(
                        documentId = documentId,
                        document = document,
                        draftData = null,
                        originalData = null,
                        previewUrl = document.document.downloadUrl,
                        previewState = DocumentPreviewState.Loading,
                        queueState = queueSnapshot.queueState,
                        selectedQueueDocumentId = if (queueSnapshot.queueState != null) documentId else null,
                    )
                }
                intent(DocumentReviewIntent.LoadPreviewPages)
                return
            }
            updateState {
                    DocumentReviewState.AwaitingExtraction(
                        documentId = documentId,
                        document = document,
                        previewUrl = document.document.downloadUrl,
                        queueState = queueSnapshot.queueState,
                        selectedQueueDocumentId = if (queueSnapshot.queueState != null) documentId else null,
                    )
            }
            intent(DocumentReviewIntent.LoadPreviewPages)
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
        val counterpartyIntent = draft.counterpartyIntent ?: tech.dokus.domain.enums.CounterpartyIntent.None

        val (contactSelectionState, selectedContactId, selectedContactSnapshot) =
            buildContactSelectionState(document, draft.contactSuggestions)

        updateState {
            DocumentReviewState.Content(
                documentId = documentId,
                document = document,
                draftData = extractedData,
                originalData = draft.aiDraftData ?: extractedData,
                hasUnsavedChanges = false,
                isSaving = false,
                isConfirming = false,
                selectedFieldPath = null,
                previewUrl = document.document.downloadUrl,
                contactSuggestions = contactSuggestions,
                previewState = DocumentPreviewState.Loading,
                selectedContactId = selectedContactId,
                selectedContactSnapshot = selectedContactSnapshot,
                contactSelectionState = contactSelectionState,
                isContactRequired = extractedData.isContactRequired,
                isDocumentConfirmed = isDocumentConfirmed,
                isDocumentRejected = isDocumentRejected,
                confirmedCashflowEntryId = document.cashflowEntryId,
                counterpartyIntent = counterpartyIntent,
                queueState = queueSnapshot.queueState,
                selectedQueueDocumentId = if (queueSnapshot.queueState != null) documentId else null,
            )
        }

        intent(DocumentReviewIntent.LoadPreviewPages)
        if (document.cashflowEntryId != null) {
            intent(DocumentReviewIntent.LoadCashflowEntry)
        }

        if (selectedContactId != null && selectedContactSnapshot == null) {
            fetchContactSnapshot(selectedContactId)
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
        return Triple(
            ContactSelectionState.NoContact,
            null,
            null,
        )
    }

    private suspend fun DocumentReviewCtx.fetchContactSnapshot(contactId: ContactId) {
        getContact(contactId).fold(
            onSuccess = { contact ->
                withState<DocumentReviewState.Content, _> {
                    updateState {
                        copy(
                            selectedContactSnapshot = ContactSnapshot(
                                id = contact.id,
                                name = contact.name.value,
                                vatNumber = contact.vatNumber?.value,
                                email = contact.email?.value,
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

    private suspend fun DocumentReviewCtx.currentQueueSnapshot(): QueueSnapshot {
        var snapshot = QueueSnapshot()
        withState<DocumentReviewState.Loading, _> {
            snapshot = QueueSnapshot(queueState = queueState, selectedQueueDocumentId = selectedQueueDocumentId)
        }
        withState<DocumentReviewState.AwaitingExtraction, _> {
            snapshot = QueueSnapshot(queueState = queueState, selectedQueueDocumentId = selectedQueueDocumentId)
        }
        withState<DocumentReviewState.Content, _> {
            snapshot = QueueSnapshot(queueState = queueState, selectedQueueDocumentId = selectedQueueDocumentId)
        }
        return snapshot
    }

    private data class QueueSnapshot(
        val queueState: DocumentReviewQueueState? = null,
        val selectedQueueDocumentId: DocumentId? = null,
    )

}
