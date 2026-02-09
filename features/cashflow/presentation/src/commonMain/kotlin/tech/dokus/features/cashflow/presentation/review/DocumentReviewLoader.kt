package tech.dokus.features.cashflow.presentation.review

import pro.respawn.flowmvi.dsl.withState
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.DocumentStatus
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
        updateState { DocumentReviewState.Loading }

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
        val draft = document.draft
        val extractedData = draft?.extractedData
        if (extractedData == null) {
            updateState {
                DocumentReviewState.AwaitingExtraction(
                    documentId = documentId,
                    document = document,
                    previewUrl = document.document.downloadUrl
                )
            }
            return
        }

        val editableData = EditableExtractedData.fromDraftData(extractedData)
        val documentType = draft.documentType ?: editableData.documentType

        val contactSuggestions = emptyList<ContactSuggestion>()
        val isContactRequired = documentType in listOf(DocumentType.Invoice, DocumentType.Bill)
        val documentStatus = draft.documentStatus
        val isDocumentConfirmed = documentStatus == DocumentStatus.Confirmed
        val isDocumentRejected = documentStatus == DocumentStatus.Rejected
        val counterpartyIntent = draft.counterpartyIntent ?: tech.dokus.domain.enums.CounterpartyIntent.None

        val (contactSelectionState, selectedContactId, selectedContactSnapshot) =
            buildContactSelectionState(document)

        updateState {
            DocumentReviewState.Content(
                documentId = documentId,
                document = document,
                editableData = editableData,
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
                isContactRequired = isContactRequired,
                isDocumentConfirmed = isDocumentConfirmed,
                isDocumentRejected = isDocumentRejected,
                counterpartyIntent = counterpartyIntent,
            )
        }

        intent(DocumentReviewIntent.LoadPreviewPages)

        if (selectedContactId != null && selectedContactSnapshot == null) {
            fetchContactSnapshot(selectedContactId)
        }
    }

    private fun buildContactSelectionState(
        document: DocumentRecordDto
    ): Triple<ContactSelectionState, ContactId?, ContactSnapshot?> {
        val draft = document.draft ?: return Triple(ContactSelectionState.NoContact, null, null)
        val linkedContactId = draft.linkedContactId
        if (linkedContactId != null) {
            return Triple(ContactSelectionState.Selected, linkedContactId, null)
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

}
