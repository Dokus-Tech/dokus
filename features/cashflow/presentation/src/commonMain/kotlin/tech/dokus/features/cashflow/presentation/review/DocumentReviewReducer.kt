@file:Suppress("TooManyFunctions") // Reducer handles document review state transitions

package tech.dokus.features.cashflow.presentation.review

import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.ExtractedDocumentData
import tech.dokus.domain.model.ExtractedLineItem
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
    private val mapper = DocumentReviewExtractedDataMapper()
    private val loader = DocumentReviewLoader(getDocumentRecord, getContact, logger)
    private val editor = DocumentReviewFieldEditor()
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
        mapper,
        logger
    )

    suspend fun DocumentReviewCtx.handleLoadDocument(documentId: DocumentId) =
        with(loader) { handleLoadDocument(documentId) }

    suspend fun DocumentReviewCtx.handleRefresh() =
        with(loader) { handleRefresh() }

    suspend fun DocumentReviewCtx.handleUpdateInvoiceField(field: InvoiceField, value: Any?) =
        with(editor) { handleUpdateInvoiceField(field, value) }

    suspend fun DocumentReviewCtx.handleUpdateBillField(field: BillField, value: Any?) =
        with(editor) { handleUpdateBillField(field, value) }

    suspend fun DocumentReviewCtx.handleUpdateExpenseField(field: ExpenseField, value: Any?) =
        with(editor) { handleUpdateExpenseField(field, value) }

    suspend fun DocumentReviewCtx.handleUpdateReceiptField(field: ReceiptField, value: Any?) =
        with(editor) { handleUpdateReceiptField(field, value) }

    suspend fun DocumentReviewCtx.handleUpdateProFormaField(field: ProFormaField, value: Any?) =
        with(editor) { handleUpdateProFormaField(field, value) }

    suspend fun DocumentReviewCtx.handleUpdateCreditNoteField(field: CreditNoteField, value: Any?) =
        with(editor) { handleUpdateCreditNoteField(field, value) }

    suspend fun DocumentReviewCtx.handleSelectDocumentType(type: DocumentType) =
        with(editor) { handleSelectDocumentType(type) }

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

    suspend fun DocumentReviewCtx.handleLoadPreviewPages() =
        with(preview) { handleLoadPreviewPages() }

    suspend fun DocumentReviewCtx.handleLoadMorePages(maxPages: Int) =
        with(preview) { handleLoadMorePages(maxPages) }

    suspend fun DocumentReviewCtx.handleAddLineItem() =
        with(lineItems) { handleAddLineItem() }

    suspend fun DocumentReviewCtx.handleUpdateLineItem(index: Int, item: ExtractedLineItem) =
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

    // Failed analysis handlers
    suspend fun DocumentReviewCtx.handleRetryAnalysis() =
        with(actions) { handleRetryAnalysis() }

    suspend fun DocumentReviewCtx.handleDismissFailureBanner() =
        with(actions) { handleDismissFailureBanner() }

    fun buildExtractedDataFromEditable(
        editable: EditableExtractedData,
        original: ExtractedDocumentData?
    ): ExtractedDocumentData = mapper.buildExtractedDataFromEditable(editable, original)
}
