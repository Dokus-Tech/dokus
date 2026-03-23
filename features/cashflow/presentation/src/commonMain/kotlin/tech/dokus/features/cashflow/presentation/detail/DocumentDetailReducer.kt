@file:Suppress("TooManyFunctions") // Reducer handles document review state transitions

package tech.dokus.features.cashflow.presentation.detail

import pro.respawn.flowmvi.dsl.withState
import tech.dokus.foundation.app.state.DokusState
import kotlinx.datetime.LocalDate
import tech.dokus.domain.Money
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.DocDto
import tech.dokus.domain.model.DocumentDetailDto
import tech.dokus.domain.model.isContactRequired
import tech.dokus.features.cashflow.usecases.ConfirmDocumentUseCase
import tech.dokus.features.cashflow.usecases.UnconfirmDocumentUseCase
import tech.dokus.features.cashflow.usecases.GetDocumentRecordUseCase
import tech.dokus.features.cashflow.usecases.RejectDocumentUseCase
import tech.dokus.features.cashflow.usecases.ReprocessDocumentUseCase
import tech.dokus.features.cashflow.usecases.ResolveDocumentMatchReviewUseCase
import tech.dokus.features.cashflow.usecases.UpdateDocumentDraftContactUseCase
import tech.dokus.features.cashflow.usecases.UpdateDocumentDraftUseCase
import tech.dokus.features.contacts.usecases.GetContactUseCase
import tech.dokus.foundation.platform.Logger

@Suppress("LongParameterList") // Keeps use case dependencies explicit in the reducer.
internal class DocumentDetailReducer(
    private val getDocumentRecord: GetDocumentRecordUseCase,
    private val updateDocumentDraft: UpdateDocumentDraftUseCase,
    private val updateDocumentDraftContact: UpdateDocumentDraftContactUseCase,
    private val confirmDocument: ConfirmDocumentUseCase,
    private val unconfirmDocument: UnconfirmDocumentUseCase,
    private val rejectDocument: RejectDocumentUseCase,
    private val reprocessDocument: ReprocessDocumentUseCase,
    private val resolveDocumentMatchReview: ResolveDocumentMatchReviewUseCase,
    private val getContact: GetContactUseCase,
    private val logger: Logger,
) {
    private val loader = DocumentDetailLoader(getDocumentRecord, logger)
    private val contactBinder = DocumentDetailContactBinder(updateDocumentDraftContact, getContact, logger)
    private val provenance = DocumentDetailProvenance()
    private val actions = DocumentDetailActions(
        updateDocumentDraft,
        updateDocumentDraftContact,
        confirmDocument,
        unconfirmDocument,
        rejectDocument,
        getDocumentRecord,
        logger
    )
    private val feedbackActions = DocumentDetailFeedbackActions(
        reprocessDocument = reprocessDocument,
        resolveDocumentMatchReview = resolveDocumentMatchReview,
        refreshAfterDraftUpdate = { documentId ->
            with(actions) { refreshAfterDraftUpdate(documentId) }
        },
        logger = logger,
    )

    suspend fun DocumentDetailCtx.handleLoadDocument(documentId: DocumentId) =
        with(loader) { handleLoadDocument(documentId) }

    suspend fun DocumentDetailCtx.handleRefresh() =
        with(loader) { handleRefresh() }

    suspend fun DocumentDetailCtx.handleApplyRemoteSnapshot(record: DocumentDetailDto) =
        with(loader) { handleApplyRemoteSnapshot(record) }

    suspend fun DocumentDetailCtx.handleSelectDocumentType(type: DocumentType) {
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

    suspend fun DocumentDetailCtx.handleSelectDirection(direction: DocumentDirection) {
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

    suspend fun DocumentDetailCtx.handleUpdateField(field: EditableField, value: String) {
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

    suspend fun DocumentDetailCtx.handleSelectContact(contactId: ContactId) =
        with(contactBinder) { handleSelectContact(contactId) }

    suspend fun DocumentDetailCtx.handleAcceptSuggestedContact() =
        with(contactBinder) { handleAcceptSuggestedContact() }

    suspend fun DocumentDetailCtx.handleClearSelectedContact() =
        with(contactBinder) { handleClearSelectedContact() }

    suspend fun DocumentDetailCtx.handleContactCreated(contactId: ContactId) =
        with(contactBinder) { handleContactCreated(contactId) }

    suspend fun DocumentDetailCtx.handleSetPendingCreation() =
        with(contactBinder) { handleSetPendingCreation() }

    // Contact sheet handlers
    suspend fun DocumentDetailCtx.handleOpenContactSheet() =
        with(contactBinder) { handleOpenContactSheet() }

    suspend fun DocumentDetailCtx.handleCloseContactSheet() =
        with(contactBinder) { handleCloseContactSheet() }

    suspend fun DocumentDetailCtx.handleUpdateContactSheetSearch(query: String) =
        with(contactBinder) { handleUpdateContactSheetSearch(query) }

    suspend fun DocumentDetailCtx.handleSelectFieldForProvenance(fieldPath: String?) =
        with(provenance) { handleSelectFieldForProvenance(fieldPath) }

    suspend fun DocumentDetailCtx.handleConfirm() =
        with(actions) { handleConfirm() }

    suspend fun DocumentDetailCtx.handleUnconfirm() =
        with(actions) { handleUnconfirm() }

    // Reject dialog handlers
    suspend fun DocumentDetailCtx.handleShowRejectDialog() =
        with(actions) { handleShowRejectDialog() }

    suspend fun DocumentDetailCtx.handleDismissRejectDialog() =
        with(actions) { handleDismissRejectDialog() }

    suspend fun DocumentDetailCtx.handleSelectRejectReason(reason: tech.dokus.domain.enums.DocumentRejectReason) =
        with(actions) { handleSelectRejectReason(reason) }

    suspend fun DocumentDetailCtx.handleUpdateRejectNote(note: String) =
        with(actions) { handleUpdateRejectNote(note) }

    suspend fun DocumentDetailCtx.handleConfirmReject() =
        with(actions) { handleConfirmReject() }

    suspend fun DocumentDetailCtx.handleViewCashflowEntry() =
        with(actions) { handleViewCashflowEntry() }

    suspend fun DocumentDetailCtx.handleViewEntity() =
        with(actions) { handleViewEntity() }

    // Feedback dialog handlers
    suspend fun DocumentDetailCtx.handleShowFeedbackDialog() =
        with(feedbackActions) { handleShowFeedbackDialog() }

    suspend fun DocumentDetailCtx.handleDismissFeedbackDialog() =
        with(feedbackActions) { handleDismissFeedbackDialog() }

    suspend fun DocumentDetailCtx.handleSelectFeedbackCategory(category: FeedbackCategory) =
        with(feedbackActions) { handleSelectFeedbackCategory(category) }

    suspend fun DocumentDetailCtx.handleUpdateFeedbackText(text: String) =
        with(feedbackActions) { handleUpdateFeedbackText(text) }

    suspend fun DocumentDetailCtx.handleSubmitFeedback() =
        with(feedbackActions) { handleSubmitFeedback() }

    suspend fun DocumentDetailCtx.handleRequestAmendment() =
        with(feedbackActions) { handleRequestAmendment() }

    // Failed analysis handlers
    suspend fun DocumentDetailCtx.handleRetryAnalysis() =
        with(feedbackActions) { handleRetryAnalysis() }

    suspend fun DocumentDetailCtx.handleDismissFailureBanner() =
        with(feedbackActions) { handleDismissFailureBanner() }

    suspend fun DocumentDetailCtx.handleResolvePossibleMatchSame() =
        with(feedbackActions) { handleResolvePossibleMatchSame() }

    suspend fun DocumentDetailCtx.handleResolvePossibleMatchDifferent() =
        with(feedbackActions) { handleResolvePossibleMatchDifferent() }

    suspend fun DocumentDetailCtx.handleToggleBankStatementTransaction(index: Int) {
        var shouldPersist = false
        withState {
            val currentData = documentData ?: return@withState
            val draft = draftData as? DocDto.BankStatement.Draft ?: return@withState
            val updatedTransactions = draft.transactions.toMutableList().also {
                it[index] = it[index].copy(excluded = !it[index].excluded)
            }
            val updatedDraft = draft.copy(transactions = updatedTransactions)
            updateState {
                copy(
                    document = DokusState.success(currentData.copy(draftData = updatedDraft)),
                    hasUnsavedChanges = true,
                )
            }
            shouldPersist = true
        }
        if (shouldPersist) {
            with(actions) { syncDraftImmediately() }
        }
    }
}

private fun applyFieldUpdate(data: DocDto?, field: EditableField, value: String): DocDto? {
    val trimmed = value.trim().ifBlank { null }
    return when (data) {
        is DocDto.Invoice.Draft -> when (field) {
            EditableField.InvoiceNumber -> data.copy(invoiceNumber = trimmed)
            EditableField.IssueDate -> data.copy(issueDate = trimmed?.let { parseDate(it) })
            EditableField.DueDate -> data.copy(dueDate = trimmed?.let { parseDate(it) })
            EditableField.SubtotalAmount -> data.copy(subtotalAmount = trimmed?.let { Money.from(it, data.currency) })
            EditableField.VatAmount -> data.copy(vatAmount = trimmed?.let { Money.from(it, data.currency) })
            EditableField.TotalAmount -> data.copy(totalAmount = trimmed?.let { Money.from(it, data.currency) })
            else -> null
        }
        is DocDto.CreditNote.Draft -> when (field) {
            EditableField.CreditNoteNumber -> data.copy(creditNoteNumber = trimmed)
            EditableField.IssueDate -> data.copy(issueDate = trimmed?.let { parseDate(it) })
            EditableField.OriginalInvoiceNumber -> data.copy(originalInvoiceNumber = trimmed)
            EditableField.SubtotalAmount -> data.copy(subtotalAmount = trimmed?.let { Money.from(it, data.currency) })
            EditableField.VatAmount -> data.copy(vatAmount = trimmed?.let { Money.from(it, data.currency) })
            EditableField.TotalAmount -> data.copy(totalAmount = trimmed?.let { Money.from(it, data.currency) })
            else -> null
        }
        is DocDto.Receipt.Draft -> when (field) {
            EditableField.ReceiptNumber -> data.copy(receiptNumber = trimmed)
            EditableField.ReceiptDate -> data.copy(date = trimmed?.let { parseDate(it) })
            EditableField.VatAmount -> data.copy(vatAmount = trimmed?.let { Money.from(it, data.currency) })
            EditableField.TotalAmount -> data.copy(totalAmount = trimmed?.let { Money.from(it, data.currency) })
            else -> null
        }
        else -> null
    }
}

private fun parseDate(text: String): LocalDate? = runCatching { LocalDate.parse(text) }.getOrNull()
