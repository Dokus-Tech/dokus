@file:Suppress("TooManyFunctions") // Reducer handles document review state transitions

package tech.dokus.features.cashflow.presentation.review

import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.ExtractedDocumentData
import tech.dokus.domain.model.ExtractedLineItem
import tech.dokus.features.cashflow.datasource.CashflowRemoteDataSource
import tech.dokus.features.contacts.usecases.GetContactUseCase
import tech.dokus.foundation.platform.Logger

internal class DocumentReviewReducer(
    private val dataSource: CashflowRemoteDataSource,
    private val getContact: GetContactUseCase,
    private val logger: Logger,
) {
    private val mapper = DocumentReviewExtractedDataMapper()
    private val loader = DocumentReviewLoader(dataSource, getContact, logger)
    private val editor = DocumentReviewFieldEditor()
    private val contactBinder = DocumentReviewContactBinder(dataSource, getContact, logger)
    private val preview = DocumentReviewPreview(dataSource, logger)
    private val lineItems = DocumentReviewLineItems()
    private val provenance = DocumentReviewProvenance()
    private val actions = DocumentReviewActions(logger)

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

    suspend fun DocumentReviewCtx.handleSelectContact(contactId: ContactId) =
        with(contactBinder) { handleSelectContact(contactId) }

    suspend fun DocumentReviewCtx.handleAcceptSuggestedContact() =
        with(contactBinder) { handleAcceptSuggestedContact() }

    suspend fun DocumentReviewCtx.handleClearSelectedContact() =
        with(contactBinder) { handleClearSelectedContact() }

    suspend fun DocumentReviewCtx.handleContactCreated(contactId: ContactId) =
        with(contactBinder) { handleContactCreated(contactId) }

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

    suspend fun DocumentReviewCtx.handleConfirm() =
        with(actions) { handleConfirm() }

    suspend fun DocumentReviewCtx.handleReject() =
        with(actions) { handleReject() }

    suspend fun DocumentReviewCtx.handleOpenChat() =
        with(actions) { handleOpenChat() }

    fun buildExtractedDataFromEditable(
        editable: EditableExtractedData,
        original: ExtractedDocumentData?
    ): ExtractedDocumentData = mapper.buildExtractedDataFromEditable(editable, original)
}
