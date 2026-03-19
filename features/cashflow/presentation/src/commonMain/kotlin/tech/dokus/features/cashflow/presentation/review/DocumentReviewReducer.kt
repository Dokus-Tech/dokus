@file:Suppress("TooManyFunctions") // Reducer handles document review state transitions

package tech.dokus.features.cashflow.presentation.review

import pro.respawn.flowmvi.dsl.withState
import tech.dokus.foundation.app.state.DokusState
import kotlinx.datetime.LocalDate
import tech.dokus.domain.Money
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.DocumentSourceId
import tech.dokus.domain.model.DocDto
import tech.dokus.domain.model.DocumentDetailDto
import tech.dokus.domain.model.isContactRequired
import tech.dokus.features.cashflow.usecases.ConfirmDocumentUseCase
import tech.dokus.features.cashflow.usecases.GetAutoPaymentStatusUseCase
import tech.dokus.features.cashflow.usecases.GetCashflowEntryUseCase
import tech.dokus.features.cashflow.usecases.GetCashflowPaymentCandidatesUseCase
import tech.dokus.features.cashflow.usecases.GetDocumentPagesUseCase
import tech.dokus.features.cashflow.usecases.GetDocumentRecordUseCase
import tech.dokus.features.cashflow.usecases.GetDocumentSourceContentUseCase
import tech.dokus.features.cashflow.usecases.GetDocumentSourcePagesUseCase
import tech.dokus.features.cashflow.usecases.RecordCashflowPaymentUseCase
import tech.dokus.features.cashflow.usecases.RejectDocumentUseCase
import tech.dokus.features.cashflow.usecases.ReprocessDocumentUseCase
import tech.dokus.features.cashflow.usecases.ResolveDocumentMatchReviewUseCase
import tech.dokus.features.cashflow.usecases.UndoAutoPaymentUseCase
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
    private val resolveDocumentMatchReview: ResolveDocumentMatchReviewUseCase,
    private val getDocumentPages: GetDocumentPagesUseCase,
    private val getDocumentSourcePages: GetDocumentSourcePagesUseCase,
    private val getDocumentSourceContent: GetDocumentSourceContentUseCase,
    private val getCashflowEntry: GetCashflowEntryUseCase,
    private val getCashflowPaymentCandidates: GetCashflowPaymentCandidatesUseCase,
    private val getAutoPaymentStatus: GetAutoPaymentStatusUseCase,
    private val recordCashflowPayment: RecordCashflowPaymentUseCase,
    private val undoAutoPayment: UndoAutoPaymentUseCase,
    private val getContact: GetContactUseCase,
    private val logger: Logger,
) {
    private val loader = DocumentReviewLoader(getDocumentRecord, logger)
    private val contactBinder = DocumentReviewContactBinder(updateDocumentDraftContact, getContact, logger)
    private val preview = DocumentReviewPreview(
        getDocumentPages = getDocumentPages,
        getDocumentSourcePages = getDocumentSourcePages,
        getDocumentSourceContent = getDocumentSourceContent,
        logger = logger
    )
    private val provenance = DocumentReviewProvenance()
    private val actions = DocumentReviewActions(
        updateDocumentDraft,
        updateDocumentDraftContact,
        confirmDocument,
        rejectDocument,
        getDocumentRecord,
        logger
    )
    private val paymentActions = DocumentReviewPaymentActions(
        getCashflowEntry = getCashflowEntry,
        getCashflowPaymentCandidates = getCashflowPaymentCandidates,
        getAutoPaymentStatus = getAutoPaymentStatus,
        recordCashflowPayment = recordCashflowPayment,
        undoAutoPayment = undoAutoPayment,
        logger = logger,
    )
    private val feedbackActions = DocumentReviewFeedbackActions(
        reprocessDocument = reprocessDocument,
        resolveDocumentMatchReview = resolveDocumentMatchReview,
        refreshAfterDraftUpdate = { documentId ->
            with(actions) { refreshAfterDraftUpdate(documentId) }
        },
        logger = logger,
    )

    suspend fun DocumentReviewCtx.handleLoadDocument(documentId: DocumentId) =
        with(loader) { handleLoadDocument(documentId) }

    suspend fun DocumentReviewCtx.handleRefresh() =
        with(loader) { handleRefresh() }

    suspend fun DocumentReviewCtx.handleApplyRemoteSnapshot(record: DocumentDetailDto) =
        with(loader) { handleApplyRemoteSnapshot(record) }

    suspend fun DocumentReviewCtx.handleSelectDocumentType(type: DocumentType) {
        var shouldPersist = false
        withState {
            if (!hasContent) return@withState
            if (type == DocumentType.Unknown) return@withState
            if (draftData.documentType == type) return@withState

            val newContent: DocDto = when (type) {
                DocumentType.Invoice -> DocDto.Invoice.Draft()
                DocumentType.Receipt -> DocDto.Receipt.Draft()
                DocumentType.CreditNote -> DocDto.CreditNote.Draft()
                else -> return@withState
            }

            val currentData = documentData ?: return@withState
            updateState {
                copy(
                    document = DokusState.success(currentData.copy(draftData = newContent)),
                    hasUnsavedChanges = true,
                    isContactRequired = newContent.isContactRequired,
                )
            }
            shouldPersist = true
        }

        if (shouldPersist) {
            with(actions) { syncDraftImmediately() }
        }
    }

    suspend fun DocumentReviewCtx.handleSelectDirection(direction: DocumentDirection) {
        var shouldPersist = false
        withState {
            if (!hasContent) return@withState
            if (direction == DocumentDirection.Unknown) return@withState

            val updatedContent = when (val data = draftData) {
                is DocDto.Invoice.Draft -> {
                    if (data.direction == direction) return@withState
                    data.copy(direction = direction)
                }
                is DocDto.CreditNote.Draft -> {
                    if (data.direction == direction) return@withState
                    data.copy(direction = direction)
                }
                else -> return@withState
            }

            val currentData = documentData ?: return@withState
            updateState {
                copy(
                    document = DokusState.success(currentData.copy(draftData = updatedContent)),
                    hasUnsavedChanges = true,
                )
            }
            shouldPersist = true
        }

        if (shouldPersist) {
            with(actions) { syncDraftImmediately() }
        }
    }

    suspend fun DocumentReviewCtx.handleUpdateField(field: EditableField, value: String) {
        var shouldPersist = false
        withState {
            if (!hasContent) return@withState
            val currentData = documentData ?: return@withState
            val updatedContent = applyFieldUpdate(draftData, field, value) ?: return@withState

            updateState {
                copy(
                    document = DokusState.success(currentData.copy(draftData = updatedContent)),
                    hasUnsavedChanges = true,
                )
            }
            shouldPersist = true
        }

        if (shouldPersist) {
            with(actions) { syncDraftImmediately() }
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

    suspend fun DocumentReviewCtx.handleSetPendingCreation() =
        with(contactBinder) { handleSetPendingCreation() }

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

    suspend fun DocumentReviewCtx.handleOpenSourceModal(sourceId: DocumentSourceId) =
        with(preview) { handleOpenSourceModal(sourceId) }

    suspend fun DocumentReviewCtx.handleCloseSourceModal() =
        with(preview) { handleCloseSourceModal() }

    suspend fun DocumentReviewCtx.handleToggleSourceTechnicalDetails() =
        with(preview) { handleToggleSourceTechnicalDetails() }

    suspend fun DocumentReviewCtx.handleSelectFieldForProvenance(fieldPath: String?) =
        with(provenance) { handleSelectFieldForProvenance(fieldPath) }

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

    suspend fun DocumentReviewCtx.handleViewCashflowEntry() =
        with(actions) { handleViewCashflowEntry() }

    suspend fun DocumentReviewCtx.handleViewEntity() =
        with(actions) { handleViewEntity() }

    suspend fun DocumentReviewCtx.handleLoadCashflowEntry() =
        with(paymentActions) { handleLoadCashflowEntry() }

    suspend fun DocumentReviewCtx.handleLoadAutoPaymentStatus() =
        with(paymentActions) { handleLoadAutoPaymentStatus() }

    suspend fun DocumentReviewCtx.handleOpenPaymentSheet() =
        with(paymentActions) { handleOpenPaymentSheet() }

    suspend fun DocumentReviewCtx.handleClosePaymentSheet() =
        with(paymentActions) { handleClosePaymentSheet() }

    suspend fun DocumentReviewCtx.handleLoadPaymentCandidates() =
        with(paymentActions) { handleLoadPaymentCandidates() }

    suspend fun DocumentReviewCtx.handleOpenPaymentTransactionPicker() =
        with(paymentActions) { handleOpenPaymentTransactionPicker() }

    suspend fun DocumentReviewCtx.handleClosePaymentTransactionPicker() =
        with(paymentActions) { handleClosePaymentTransactionPicker() }

    suspend fun DocumentReviewCtx.handleSelectPaymentTransaction(transactionId: tech.dokus.domain.ids.BankTransactionId) =
        with(paymentActions) { handleSelectPaymentTransaction(transactionId) }

    suspend fun DocumentReviewCtx.handleClearPaymentTransactionSelection() =
        with(paymentActions) { handleClearPaymentTransactionSelection() }

    suspend fun DocumentReviewCtx.handleUpdatePaymentAmountText(text: String) =
        with(paymentActions) { handleUpdatePaymentAmountText(text) }

    suspend fun DocumentReviewCtx.handleUpdatePaymentPaidAt(date: kotlinx.datetime.LocalDate) =
        with(paymentActions) { handleUpdatePaymentPaidAt(date) }

    suspend fun DocumentReviewCtx.handleUpdatePaymentNote(note: String) =
        with(paymentActions) { handleUpdatePaymentNote(note) }

    suspend fun DocumentReviewCtx.handleSubmitPayment() =
        with(paymentActions) { handleSubmitPayment() }

    suspend fun DocumentReviewCtx.handleUndoAutoPayment(reason: String?) =
        with(paymentActions) { handleUndoAutoPayment(reason) }

    // Feedback dialog handlers
    suspend fun DocumentReviewCtx.handleShowFeedbackDialog() =
        with(feedbackActions) { handleShowFeedbackDialog() }

    suspend fun DocumentReviewCtx.handleDismissFeedbackDialog() =
        with(feedbackActions) { handleDismissFeedbackDialog() }

    suspend fun DocumentReviewCtx.handleSelectFeedbackCategory(category: FeedbackCategory) =
        with(feedbackActions) { handleSelectFeedbackCategory(category) }

    suspend fun DocumentReviewCtx.handleUpdateFeedbackText(text: String) =
        with(feedbackActions) { handleUpdateFeedbackText(text) }

    suspend fun DocumentReviewCtx.handleSubmitFeedback() =
        with(feedbackActions) { handleSubmitFeedback() }

    suspend fun DocumentReviewCtx.handleRequestAmendment() =
        with(feedbackActions) { handleRequestAmendment() }

    // Failed analysis handlers
    suspend fun DocumentReviewCtx.handleRetryAnalysis() =
        with(feedbackActions) { handleRetryAnalysis() }

    suspend fun DocumentReviewCtx.handleDismissFailureBanner() =
        with(feedbackActions) { handleDismissFailureBanner() }

    suspend fun DocumentReviewCtx.handleResolvePossibleMatchSame() =
        with(feedbackActions) { handleResolvePossibleMatchSame() }

    suspend fun DocumentReviewCtx.handleResolvePossibleMatchDifferent() =
        with(feedbackActions) { handleResolvePossibleMatchDifferent() }
}

private fun applyFieldUpdate(data: DocDto?, field: EditableField, value: String): DocDto? {
    val trimmed = value.trim().ifBlank { null }
    return when (data) {
        is DocDto.Invoice.Draft -> when (field) {
            EditableField.InvoiceNumber -> data.copy(invoiceNumber = trimmed)
            EditableField.IssueDate -> data.copy(issueDate = trimmed?.let { parseDate(it) })
            EditableField.DueDate -> data.copy(dueDate = trimmed?.let { parseDate(it) })
            EditableField.SubtotalAmount -> data.copy(subtotalAmount = trimmed?.let { Money.from(it) })
            EditableField.VatAmount -> data.copy(vatAmount = trimmed?.let { Money.from(it) })
            EditableField.TotalAmount -> data.copy(totalAmount = trimmed?.let { Money.from(it) })
            else -> null
        }
        is DocDto.CreditNote.Draft -> when (field) {
            EditableField.CreditNoteNumber -> data.copy(creditNoteNumber = trimmed)
            EditableField.IssueDate -> data.copy(issueDate = trimmed?.let { parseDate(it) })
            EditableField.OriginalInvoiceNumber -> data.copy(originalInvoiceNumber = trimmed)
            EditableField.SubtotalAmount -> data.copy(subtotalAmount = trimmed?.let { Money.from(it) })
            EditableField.VatAmount -> data.copy(vatAmount = trimmed?.let { Money.from(it) })
            EditableField.TotalAmount -> data.copy(totalAmount = trimmed?.let { Money.from(it) })
            else -> null
        }
        is DocDto.Receipt.Draft -> when (field) {
            EditableField.ReceiptNumber -> data.copy(receiptNumber = trimmed)
            EditableField.ReceiptDate -> data.copy(date = trimmed?.let { parseDate(it) })
            EditableField.VatAmount -> data.copy(vatAmount = trimmed?.let { Money.from(it) })
            EditableField.TotalAmount -> data.copy(totalAmount = trimmed?.let { Money.from(it) })
            else -> null
        }
        else -> null
    }
}

private fun parseDate(text: String): LocalDate? = runCatching { LocalDate.parse(text) }.getOrNull()
