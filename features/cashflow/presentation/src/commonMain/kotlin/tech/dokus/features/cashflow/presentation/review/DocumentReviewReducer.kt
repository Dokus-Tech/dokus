@file:Suppress("TooManyFunctions") // Reducer handles document review state transitions

package tech.dokus.features.cashflow.presentation.review

import pro.respawn.flowmvi.dsl.withState
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.DocumentSourceId
import tech.dokus.domain.model.AnnualAccountsDraftData
import tech.dokus.domain.model.BankFeeDraftData
import tech.dokus.domain.model.BankStatementDraftData
import tech.dokus.domain.model.BoardMinutesDraftData
import tech.dokus.domain.model.C4DraftData
import tech.dokus.domain.model.CompanyExtractDraftData
import tech.dokus.domain.model.ContractDraftData
import tech.dokus.domain.model.CorporateTaxAdvanceDraftData
import tech.dokus.domain.model.CorporateTaxDraftData
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.CustomsDeclarationDraftData
import tech.dokus.domain.model.DeliveryNoteDraftData
import tech.dokus.domain.model.DepreciationScheduleDraftData
import tech.dokus.domain.model.DimonaDraftData
import tech.dokus.domain.model.DividendDraftData
import tech.dokus.domain.model.DocumentDetailDto
import tech.dokus.domain.model.EmploymentContractDraftData
import tech.dokus.domain.model.ExpenseClaimDraftData
import tech.dokus.domain.model.FineDraftData
import tech.dokus.domain.model.HolidayPayDraftData
import tech.dokus.domain.model.IcListingDraftData
import tech.dokus.domain.model.InsuranceDraftData
import tech.dokus.domain.model.InterestStatementDraftData
import tech.dokus.domain.model.IntrastatDraftData
import tech.dokus.domain.model.InventoryDraftData
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.LeaseDraftData
import tech.dokus.domain.model.LoanDraftData
import tech.dokus.domain.model.OrderConfirmationDraftData
import tech.dokus.domain.model.OssReturnDraftData
import tech.dokus.domain.model.OtherDraftData
import tech.dokus.domain.model.PaymentConfirmationDraftData
import tech.dokus.domain.model.PayrollSummaryDraftData
import tech.dokus.domain.model.PermitDraftData
import tech.dokus.domain.model.PersonalTaxDraftData
import tech.dokus.domain.model.ProFormaDraftData
import tech.dokus.domain.model.PurchaseOrderDraftData
import tech.dokus.domain.model.QuoteDraftData
import tech.dokus.domain.model.ReceiptDraftData
import tech.dokus.domain.model.ReminderDraftData
import tech.dokus.domain.model.SalarySlipDraftData
import tech.dokus.domain.model.SelfEmployedContributionDraftData
import tech.dokus.domain.model.ShareholderRegisterDraftData
import tech.dokus.domain.model.SocialContributionDraftData
import tech.dokus.domain.model.SocialFundDraftData
import tech.dokus.domain.model.StatementOfAccountDraftData
import tech.dokus.domain.model.SubsidyDraftData
import tech.dokus.domain.model.TaxAssessmentDraftData
import tech.dokus.domain.model.VapzDraftData
import tech.dokus.domain.model.VatAssessmentDraftData
import tech.dokus.domain.model.VatListingDraftData
import tech.dokus.domain.model.VatReturnDraftData
import tech.dokus.domain.model.WithholdingTaxDraftData
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

            val newDraftData = when (type) {
                DocumentType.Invoice -> InvoiceDraftData()
                DocumentType.Receipt -> ReceiptDraftData()
                DocumentType.CreditNote -> CreditNoteDraftData()
                else -> return@withState
            }

            val currentData = documentData ?: return@withState
            updateState {
                copy(
                    document = DokusState.success(currentData.copy(draftData = newDraftData)),
                    hasUnsavedChanges = true,
                    isContactRequired = newDraftData.isContactRequired,
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

            val updatedDraftData = when (val data = draftData) {
                is InvoiceDraftData -> {
                    if (data.direction == direction) return@withState
                    data.copy(direction = direction)
                }
                is CreditNoteDraftData -> {
                    if (data.direction == direction) return@withState
                    data.copy(direction = direction)
                }
                is ReceiptDraftData,
                is BankStatementDraftData,
                is ProFormaDraftData,
                is QuoteDraftData,
                is OrderConfirmationDraftData,
                is DeliveryNoteDraftData,
                is ReminderDraftData,
                is StatementOfAccountDraftData,
                is PurchaseOrderDraftData,
                is ExpenseClaimDraftData,
                is BankFeeDraftData,
                is InterestStatementDraftData,
                is PaymentConfirmationDraftData,
                is VatReturnDraftData,
                is VatListingDraftData,
                is VatAssessmentDraftData,
                is IcListingDraftData,
                is OssReturnDraftData,
                is CorporateTaxDraftData,
                is CorporateTaxAdvanceDraftData,
                is TaxAssessmentDraftData,
                is PersonalTaxDraftData,
                is WithholdingTaxDraftData,
                is SocialContributionDraftData,
                is SocialFundDraftData,
                is SelfEmployedContributionDraftData,
                is VapzDraftData,
                is SalarySlipDraftData,
                is PayrollSummaryDraftData,
                is EmploymentContractDraftData,
                is DimonaDraftData,
                is C4DraftData,
                is HolidayPayDraftData,
                is ContractDraftData,
                is LeaseDraftData,
                is LoanDraftData,
                is InsuranceDraftData,
                is DividendDraftData,
                is ShareholderRegisterDraftData,
                is CompanyExtractDraftData,
                is AnnualAccountsDraftData,
                is BoardMinutesDraftData,
                is SubsidyDraftData,
                is FineDraftData,
                is PermitDraftData,
                is CustomsDeclarationDraftData,
                is IntrastatDraftData,
                is DepreciationScheduleDraftData,
                is InventoryDraftData,
                is OtherDraftData,
                null -> return@withState
            }

            val currentData = documentData ?: return@withState
            updateState {
                copy(
                    document = DokusState.success(currentData.copy(draftData = updatedDraftData)),
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
