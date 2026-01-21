package tech.dokus.features.cashflow.presentation.review

import pro.respawn.flowmvi.dsl.withState
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.DraftStatus
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
        val extractionSnapshot = document.draft?.extractedData
            ?: document.latestIngestion?.rawExtraction
        val editableData = EditableExtractedData.fromExtractedData(extractionSnapshot)
        val documentType = document.draft?.documentType ?: extractionSnapshot?.documentType

        val contactSuggestions = buildContactSuggestions(document)
        val isContactRequired = documentType == DocumentType.Invoice
        val draftStatus = document.draft?.draftStatus
        val isDocumentConfirmed = draftStatus == DraftStatus.Confirmed
        val isDocumentRejected = draftStatus == DraftStatus.Rejected
        val counterpartyIntent = document.draft?.counterpartyIntent ?: tech.dokus.domain.enums.CounterpartyIntent.None

        val (contactSelectionState, selectedContactId, selectedContactSnapshot) =
            buildContactSelectionState(document)

        updateState {
            DocumentReviewState.Content(
                documentId = documentId,
                document = document,
                editableData = editableData,
                originalData = document.draft?.aiDraftData ?: extractionSnapshot,
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

        val suggestedContactId = draft.suggestedContactId ?: return Triple(
            ContactSelectionState.NoContact,
            null,
            null,
        )

        val extractedName = when (draft.documentType) {
            DocumentType.Invoice -> draft.extractedData?.invoice?.clientName
            DocumentType.Bill -> draft.extractedData?.bill?.supplierName
            else -> null
        }
        val extractedVat = when (draft.documentType) {
            DocumentType.Invoice -> draft.extractedData?.invoice?.clientVatNumber
            DocumentType.Bill -> draft.extractedData?.bill?.supplierVatNumber
            else -> null
        }

        val suggested = ContactSelectionState.Suggested(
            contactId = suggestedContactId,
            name = extractedName.orEmpty(),
            vatNumber = extractedVat,
            confidence = draft.contactSuggestionConfidence ?: 0f,
            reason = draft.contactSuggestionReason
                ?.takeIf { it.isNotBlank() }
                ?.let { ContactSuggestionReason.Custom(it) }
                ?: ContactSuggestionReason.AiSuggested,
        )
        return Triple(suggested, null, null)
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

    private fun buildContactSuggestions(document: DocumentRecordDto): List<ContactSuggestion> {
        val suggestions = mutableListOf<ContactSuggestion>()

        document.draft?.suggestedContactId?.let { contactId ->
            val extractedName = when (document.draft?.documentType) {
                DocumentType.Invoice -> document.draft?.extractedData?.invoice?.clientName
                DocumentType.Bill -> document.draft?.extractedData?.bill?.supplierName
                DocumentType.Expense -> document.draft?.extractedData?.expense?.merchant
                else -> null
            }
            val extractedVat = when (document.draft?.documentType) {
                DocumentType.Invoice -> document.draft?.extractedData?.invoice?.clientVatNumber
                DocumentType.Bill -> document.draft?.extractedData?.bill?.supplierVatNumber
                DocumentType.Expense -> document.draft?.extractedData?.expense?.merchantVatNumber
                else -> null
            }

            suggestions.add(
                ContactSuggestion(
                    contactId = contactId,
                    name = extractedName.orEmpty(),
                    vatNumber = extractedVat,
                    matchConfidence = 0f,
                    matchReason = ContactSuggestionReason.AiSuggested
                )
            )
        }

        return suggestions
    }
}
