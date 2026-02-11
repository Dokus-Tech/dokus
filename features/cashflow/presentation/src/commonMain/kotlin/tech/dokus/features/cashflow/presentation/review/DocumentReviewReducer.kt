@file:Suppress("TooManyFunctions") // Reducer handles document review state transitions

package tech.dokus.features.cashflow.presentation.review

import pro.respawn.flowmvi.dsl.withState
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.BillDraftData
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.FinancialLineItem
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.ReceiptDraftData
import tech.dokus.features.cashflow.usecases.ConfirmDocumentUseCase
import tech.dokus.features.cashflow.usecases.GetDocumentPagesUseCase
import tech.dokus.features.cashflow.usecases.GetDocumentRecordUseCase
import tech.dokus.features.cashflow.usecases.RejectDocumentUseCase
import tech.dokus.features.cashflow.usecases.ReprocessDocumentUseCase
import tech.dokus.features.cashflow.usecases.UpdateDocumentDraftContactUseCase
import tech.dokus.features.cashflow.usecases.UpdateDocumentDraftUseCase
import tech.dokus.features.contacts.usecases.GetContactUseCase
import tech.dokus.foundation.platform.Logger

@Suppress("LongParameterList") // Keeps use case dependencies explicit in the reducer.
internal class DocumentReviewReducer(
    private val getDocumentRecord: GetDocumentRecordUseCase,
    private val updateDocumentDraft: UpdateDocumentDraftUseCase,
    private val updateDocumentDraftContact: UpdateDocumentDraftContactUseCase,
    private val confirmDocument: ConfirmDocumentUseCase,
    private val rejectDocument: RejectDocumentUseCase,
    private val reprocessDocument: ReprocessDocumentUseCase,
    private val getDocumentPages: GetDocumentPagesUseCase,
    private val getContact: GetContactUseCase,
    private val logger: Logger,
) {
    private val loader = DocumentReviewLoader(getDocumentRecord, getContact, logger)
    private val contactBinder = DocumentReviewContactBinder(updateDocumentDraftContact, getContact, logger)
    private val preview = DocumentReviewPreview(getDocumentPages, logger)
    private val lineItems = DocumentReviewLineItems()
    private val provenance = DocumentReviewProvenance()
    private val actions = DocumentReviewActions(
        updateDocumentDraft,
        confirmDocument,
        rejectDocument,
        reprocessDocument,
        getDocumentRecord,
        logger
    )

    suspend fun DocumentReviewCtx.handleLoadDocument(documentId: DocumentId) =
        with(loader) { handleLoadDocument(documentId) }

    suspend fun DocumentReviewCtx.handleRefresh() =
        with(loader) { handleRefresh() }

    suspend fun DocumentReviewCtx.handleSelectDocumentType(type: DocumentType) {
        withState<DocumentReviewState.Content, _> {
            if (type == DocumentType.Unknown) return@withState

            val newDraftData = when (type) {
                DocumentType.Invoice -> InvoiceDraftData()
                DocumentType.Bill -> BillDraftData()
                DocumentType.Receipt -> ReceiptDraftData()
                DocumentType.CreditNote -> CreditNoteDraftData()
                else -> return@withState
            }

            updateState {
                copy(
                    draftData = newDraftData,
                    hasUnsavedChanges = true,
                )
            }
        }
    }

    suspend fun DocumentReviewCtx.handleSelectContact(contactId: ContactId) =
        with(contactBinder) { handleSelectContact(contactId) }

    suspend fun DocumentReviewCtx.handleAcceptSuggestedContact() =
        with(contactBinder) { handleAcceptSuggestedContact() }

    suspend fun DocumentReviewCtx.handleClearSelectedContact() =
        with(contactBinder) { handleClearSelectedContact() }

    suspend fun DocumentReviewCtx.handleContactCreated(contactId: ContactId) =
        with(contactBinder) { handleContactCreated(contactId) }

    suspend fun DocumentReviewCtx.handleSetCounterpartyIntent(intent: tech.dokus.domain.enums.CounterpartyIntent) =
        with(contactBinder) { handleSetCounterpartyIntent(intent) }

    suspend fun DocumentReviewCtx.handleOpenPreviewSheet() =
        with(preview) { handleOpenPreviewSheet() }

    suspend fun DocumentReviewCtx.handleClosePreviewSheet() =
        with(preview) { handleClosePreviewSheet() }

    // Contact sheet handlers
    suspend fun DocumentReviewCtx.handleOpenContactSheet() =
        with(contactBinder) { handleOpenContactSheet() }

    suspend fun DocumentReviewCtx.handleCloseContactSheet() =
        with(contactBinder) { handleCloseContactSheet() }

    suspend fun DocumentReviewCtx.handleUpdateContactSheetSearch(query: String) =
        with(contactBinder) { handleUpdateContactSheetSearch(query) }

    suspend fun DocumentReviewCtx.handleLoadPreviewPages() =
        with(preview) { handleLoadPreviewPages() }

    suspend fun DocumentReviewCtx.handleLoadMorePages(maxPages: Int) =
        with(preview) { handleLoadMorePages(maxPages) }

    suspend fun DocumentReviewCtx.handleAddLineItem() =
        with(lineItems) { handleAddLineItem() }

    suspend fun DocumentReviewCtx.handleUpdateLineItem(index: Int, item: FinancialLineItem) =
        with(lineItems) { handleUpdateLineItem(index, item) }

    suspend fun DocumentReviewCtx.handleRemoveLineItem(index: Int) =
        with(lineItems) { handleRemoveLineItem(index) }

    suspend fun DocumentReviewCtx.handleSelectFieldForProvenance(fieldPath: String?) =
        with(provenance) { handleSelectFieldForProvenance(fieldPath) }

    suspend fun DocumentReviewCtx.handleSaveDraft() =
        with(actions) { handleSaveDraft() }

    suspend fun DocumentReviewCtx.handleDiscardChanges() =
        with(actions) { handleDiscardChanges() }

    suspend fun DocumentReviewCtx.handleConfirmDiscardChanges() =
        with(actions) { handleConfirmDiscardChanges() }

    suspend fun DocumentReviewCtx.handleConfirm() =
        with(actions) { handleConfirm() }

    // Reject dialog handlers
    suspend fun DocumentReviewCtx.handleShowRejectDialog() =
        with(actions) { handleShowRejectDialog() }

    suspend fun DocumentReviewCtx.handleDismissRejectDialog() =
        with(actions) { handleDismissRejectDialog() }

    suspend fun DocumentReviewCtx.handleSelectRejectReason(reason: tech.dokus.domain.enums.DocumentRejectReason) =
        with(actions) { handleSelectRejectReason(reason) }

    suspend fun DocumentReviewCtx.handleUpdateRejectNote(note: String) =
        with(actions) { handleUpdateRejectNote(note) }

    suspend fun DocumentReviewCtx.handleConfirmReject() =
        with(actions) { handleConfirmReject() }

    suspend fun DocumentReviewCtx.handleOpenChat() =
        with(actions) { handleOpenChat() }

    suspend fun DocumentReviewCtx.handleViewCashflowEntry() =
        with(actions) { handleViewCashflowEntry() }

    suspend fun DocumentReviewCtx.handleViewEntity() =
        with(actions) { handleViewEntity() }

    // Feedback dialog handlers
    suspend fun DocumentReviewCtx.handleShowFeedbackDialog() =
        with(actions) { handleShowFeedbackDialog() }

    suspend fun DocumentReviewCtx.handleDismissFeedbackDialog() =
        with(actions) { handleDismissFeedbackDialog() }

    suspend fun DocumentReviewCtx.handleUpdateFeedbackText(text: String) =
        with(actions) { handleUpdateFeedbackText(text) }

    suspend fun DocumentReviewCtx.handleSubmitFeedback() =
        with(actions) { handleSubmitFeedback() }

    // Failed analysis handlers
    suspend fun DocumentReviewCtx.handleRetryAnalysis() =
        with(actions) { handleRetryAnalysis() }

    suspend fun DocumentReviewCtx.handleDismissFailureBanner() =
        with(actions) { handleDismissFailureBanner() }
}
