package ai.dokus.app.cashflow.presentation.review

import ai.dokus.app.cashflow.datasource.CashflowRemoteDataSource
import ai.dokus.app.contacts.usecases.GetContactUseCase
import ai.dokus.foundation.platform.Logger
import pro.respawn.flowmvi.dsl.withState
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.DraftStatus
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.DocumentRecordDto

internal class DocumentReviewLoader(
    private val dataSource: CashflowRemoteDataSource,
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
        dataSource.getDocumentRecord(documentId)
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
        val extractedData = document.draft?.extractedData
        val editableData = EditableExtractedData.fromExtractedData(extractedData)
        val documentType = document.draft?.documentType

        val contactSuggestions = buildContactSuggestions(document)
        val isContactRequired = documentType == DocumentType.Invoice || documentType == DocumentType.Bill
        val isDocumentConfirmed = document.draft?.draftStatus == DraftStatus.Confirmed

        val (contactSelectionState, selectedContactId, selectedContactSnapshot) =
            buildContactSelectionState(document)

        updateState {
            DocumentReviewState.Content(
                documentId = documentId,
                document = document,
                editableData = editableData,
                originalData = extractedData,
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
