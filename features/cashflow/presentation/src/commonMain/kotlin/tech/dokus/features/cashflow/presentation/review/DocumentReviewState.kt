package tech.dokus.features.cashflow.presentation.review

import androidx.compose.runtime.Immutable
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.jetbrains.compose.resources.StringResource
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_confirm_missing_fields
import tech.dokus.aura.resources.cashflow_confirm_select_contact
import tech.dokus.domain.Money
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.DocumentRejectReason
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.DocumentSourceId
import tech.dokus.domain.model.AnnualAccountsDraftData
import tech.dokus.domain.model.AutoPaymentStatusDto
import tech.dokus.domain.model.BankFeeDraftData
import tech.dokus.domain.model.BankStatementDraftData
import tech.dokus.domain.model.BankTransactionDto
import tech.dokus.domain.model.BoardMinutesDraftData
import tech.dokus.domain.model.C4DraftData
import tech.dokus.domain.model.CashflowEntry
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
import tech.dokus.domain.model.DocumentDraftData
import tech.dokus.domain.model.EmploymentContractDraftData
import tech.dokus.domain.model.ExpenseClaimDraftData
import tech.dokus.domain.model.FinancialDocumentDto
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
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.domain.model.toDocumentType
import tech.dokus.features.cashflow.presentation.review.models.DocumentUiData
import tech.dokus.features.cashflow.presentation.review.models.toUiData
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.app.state.isSuccess
import kotlin.time.Clock

/**
 * State for the reject document dialog.
 * Lifted to MVI state for consistency with other dialogs.
 */
@Immutable
data class RejectDialogState(
    val selectedReason: DocumentRejectReason = DocumentRejectReason.NotMyBusiness,
    val otherNote: String = "",
    val isConfirming: Boolean = false,
)

/**
 * Structured correction categories for the feedback dialog.
 * Sent as a prefix in the feedback text to the reprocess pipeline.
 */
enum class FeedbackCategory {
    WrongContact,
    WrongAmount,
    WrongDate,
    WrongType,
    MissingInfo,
    Other,
}

/**
 * State for the "What needs correction?" feedback dialog.
 * Shown when user clicks "Something's wrong" -- correction-first approach.
 */
@Immutable
data class FeedbackDialogState(
    val selectedCategory: FeedbackCategory? = null,
    val feedbackText: String = "",
    val isSubmitting: Boolean = false,
)

@Immutable
data class SourceEvidenceViewerState(
    val sourceId: DocumentSourceId,
    val sourceName: String,
    val sourceType: DocumentSource,
    val sourceReceivedAt: LocalDateTime? = null,
    val previewState: DocumentPreviewState = DocumentPreviewState.Loading,
    val isTechnicalDetailsExpanded: Boolean = false,
    val rawContent: String? = null,
    val isLoadingRawContent: Boolean = false,
    val rawContentError: DokusException? = null,
)

@Immutable
data class PaymentSheetState(
    val amountText: String = "",
    val amount: Money? = null,
    val paidAt: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
    val note: String = "",
    val suggestedTransaction: BankTransactionDto? = null,
    val selectedTransaction: BankTransactionDto? = null,
    val selectableTransactions: List<BankTransactionDto> = emptyList(),
    val showTransactionPicker: Boolean = false,
    val isLoadingTransactions: Boolean = false,
    val transactionsError: DokusException? = null,
    val isSubmitting: Boolean = false,
    val amountError: DokusException? = null,
)

@Immutable
enum class ReviewFinancialStatus {
    Paid,
    Unpaid,
    Overdue,
    Review,
}

/**
 * Bundled network-loaded data for the document review screen.
 * Wrapped in [DokusState] to represent loading/success/error.
 */
@Immutable
data class ReviewDocumentData(
    val documentId: DocumentId,
    val documentRecord: DocumentDetailDto,
    val draftData: DocumentDraftData?,
    val originalData: DocumentDraftData?,
    val previewUrl: String?,
    val contactSuggestions: List<ContactSuggestion>,
)

@Immutable
data class DocumentReviewState(
    // === Main network data ===
    val document: DokusState<ReviewDocumentData> = DokusState.idle(),
    val isAwaitingExtraction: Boolean = false,
    val previewState: DocumentPreviewState = DocumentPreviewState.Loading,

    // === UI flags ===
    val hasUnsavedChanges: Boolean = false,
    val isSaving: Boolean = false,
    val isConfirming: Boolean = false,
    val selectedFieldPath: String? = null,
    val selectedContactId: ContactId? = null,
    val selectedContactSnapshot: ContactSnapshot? = null,
    val contactSelectionState: ContactSelectionState = ContactSelectionState.NoContact,
    val isContactRequired: Boolean = false,
    val isPendingCreation: Boolean = false,
    val contactValidationError: DokusException? = null,
    val isBindingContact: Boolean = false,
    val isRejecting: Boolean = false,
    val isResolvingMatchReview: Boolean = false,
    val documentStatus: DocumentStatus? = null,
    val confirmedCashflowEntryId: CashflowEntryId? = null,
    val cashflowEntryState: DokusState<CashflowEntry> = DokusState.idle(),
    val autoPaymentStatus: DokusState<AutoPaymentStatusDto> = DokusState.idle(),
    val isUndoingAutoPayment: Boolean = false,
    val isEditMode: Boolean = false,
    val sourceViewerState: SourceEvidenceViewerState? = null,
    val paymentSheetState: PaymentSheetState? = null,
    val rejectDialogState: RejectDialogState? = null,
    val feedbackDialogState: FeedbackDialogState? = null,
    val failureBannerDismissed: Boolean = false,

    // === Contact sheet state ===
    val showContactSheet: Boolean = false,
    val contactSheetSearchQuery: String = "",
    val contactSheetContacts: DokusState<List<ContactDto>> = DokusState.idle(),

    // === Queue state ===
    val queueState: DocumentReviewQueueState? = null,
    val selectedQueueDocumentId: DocumentId? = null,
    val today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
) : MVIState {

    // === Convenience accessors for ReviewDocumentData fields ===

    /** The active document ID, available once data has loaded at least once. */
    val documentId: DocumentId?
        get() = documentData?.documentId

    val isDocumentConfirmed: Boolean get() = documentStatus == DocumentStatus.Confirmed
    val isDocumentRejected: Boolean get() = documentStatus == DocumentStatus.Rejected
    val isDocumentUnsupported: Boolean get() = documentStatus == DocumentStatus.Unsupported

    /** The document record, available when loaded. */
    val documentRecord: DocumentDetailDto?
        get() = documentData?.documentRecord

    /** The current draft data (store-internal — composables should use [uiData] instead). */
    val draftData: DocumentDraftData?
        get() = documentData?.draftData

    /** Presentation-layer document data for UI rendering. Composables should use this. */
    val uiData: DocumentUiData?
        get() = draftData?.toUiData()

    /** The original AI draft data. */
    val originalData: DocumentDraftData?
        get() = documentData?.originalData

    /** The preview URL. */
    val previewUrl: String?
        get() = documentData?.previewUrl

    /** Contact suggestions from extraction. */
    val contactSuggestions: List<ContactSuggestion>
        get() = documentData?.contactSuggestions.orEmpty()

    /** The underlying data, if loaded. */
    val documentData: ReviewDocumentData?
        get() = (document as? DokusState.Success<ReviewDocumentData>)?.data
            ?: document.lastData

    /** True when document data has loaded successfully. */
    val hasContent: Boolean
        get() = document.isSuccess()

    /** True when AI extraction is still in progress (Queued or Processing). */
    val isProcessing: Boolean
        get() = documentRecord?.latestIngestion?.status in listOf(
            IngestionStatus.Queued,
            IngestionStatus.Processing
        )

    /** True when AI extraction failed. */
    val isFailed: Boolean
        get() = documentRecord?.latestIngestion?.status == IngestionStatus.Failed ||
            !documentRecord?.latestIngestion?.errorMessage.isNullOrBlank()

    /** Error message from failed extraction, if available. */
    val failureReason: String?
        get() = documentRecord?.latestIngestion?.errorMessage

    val canConfirm: Boolean
        get() {
            if (!hasContent) return false
            val baseValid = (
                documentRecord?.draft?.documentStatus == DocumentStatus.NeedsReview
                ) &&
                !isConfirming &&
                !isSaving &&
                !isRejecting &&
                !isBindingContact &&
                !isProcessing &&
                confirmBlockedReason == null
            return baseValid
        }

    val hasUnsyncedLocalChanges: Boolean
        get() = hasUnsavedChanges || isSaving

    /**
     * True when contact is required but neither bound nor suggested.
     * A suggested contact does NOT block — confirm will auto-bind it.
     */
    val hasUnresolvedContact: Boolean
        get() = draftData?.isContactRequired == true &&
            selectedContactId == null &&
            contactSelectionState !is ContactSelectionState.Suggested

    val confirmBlockedReason: StringResource?
        get() {
            if (isDocumentConfirmed || isDocumentRejected || isDocumentUnsupported) return null
            val draft = draftData ?: return Res.string.cashflow_confirm_missing_fields
            return when {
                isPendingCreation -> Res.string.cashflow_confirm_select_contact
                hasUnresolvedContact -> Res.string.cashflow_confirm_select_contact
                !draft.hasKnownDirectionForConfirmation -> Res.string.cashflow_confirm_missing_fields
                !draft.hasRequiredIdentityForConfirmation -> Res.string.cashflow_confirm_missing_fields
                !draft.hasRequiredDates -> Res.string.cashflow_confirm_missing_fields
                !draft.hasRequiredSubtotalForConfirmation -> Res.string.cashflow_confirm_missing_fields
                !draft.hasRequiredTotalForConfirmation -> Res.string.cashflow_confirm_missing_fields
                !draft.hasRequiredVatForConfirmation -> Res.string.cashflow_confirm_missing_fields
                else -> null
            }
        }

    /**
     * Hard gate - Confirm button is disabled when this is true.
     * True when: missing required fields (type, total, issue date).
     */
    val isBlocking: Boolean
        get() = confirmBlockedReason != null

    /**
     * Contact match status - captures uncertainty, not just null.
     * Used for policy-based attention signals.
     */
    val contactMatchStatus: ContactMatchStatus
        get() = when {
            // User explicitly selected
            contactSelectionState is ContactSelectionState.Selected -> ContactMatchStatus.Matched
            // Suggested contact exists for required types, but needs user confirmation
            contactSelectionState is ContactSelectionState.Suggested &&
                draftData.isContactRequired ->
                ContactMatchStatus.Uncertain
            // No contact, but required for this document type (Invoice/CreditNote)
            draftData.isContactRequired -> ContactMatchStatus.MissingButRequired
            // No contact, but acceptable (Receipt)
            else -> ContactMatchStatus.NotRequired
        }

    /**
     * Soft attention signal - policy-based, not field-null checks.
     * SEPARATE from confirmBlockedReason (hard block).
     *
     * Attention rules:
     * 1. Always attention if confirmBlockedReason != null (hard issues are also soft)
     * 2. Attention if contact is Uncertain or MissingButRequired
     * 3. Attention if due date missing AND Invoice AND not yet confirmed
     */
    val hasAttention: Boolean
        get() {
            // Hard block implies attention
            if (confirmBlockedReason != null) return true

            // Contact uncertainty
            if (contactMatchStatus == ContactMatchStatus.Uncertain ||
                contactMatchStatus == ContactMatchStatus.MissingButRequired
            ) {
                return true
            }

            // Due date missing for invoices (only when not confirmed)
            val needsDueDate = (draftData is InvoiceDraftData) &&
                !isDocumentConfirmed
            return needsDueDate && (draftData as? InvoiceDraftData)?.dueDate == null
        }

    /**
     * Resolved description for the header understanding line.
     * Priority: context (notes/description) + counterparty, or fallback to filename.
     */
    val description: String
        get() {
            val counterparty = documentRecord?.draft?.counterpartyDisplayName?.takeIf { it.isNotBlank() }
                ?: selectedContactSnapshot?.name?.takeIf { it.isNotBlank() }
            val context = draftData.displayContextDescription

            return when {
                counterparty != null && context != null -> "$counterparty — $context"
                context != null -> context
                counterparty != null -> counterparty
                isProcessing -> "Processing document..."
                else -> documentRecord?.document?.filename ?: ""
            }
        }

    /**
     * Total amount for the understanding line (currency-formatted).
     */
    val totalAmount: Money?
        get() = when (val draft = draftData) {
            is InvoiceDraftData -> draft.totalAmount
            is ReceiptDraftData -> draft.totalAmount
            is CreditNoteDraftData -> draft.totalAmount
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
            null -> null
        }

    /**
     * Canonical rendering is available for invoice-like documents.
     * If unavailable, UI should fallback to PDF preview.
     */
    val canRenderCanonical: Boolean
        get() = draftData is InvoiceDraftData || draftData is CreditNoteDraftData

    val shouldUsePdfFallback: Boolean
        get() = !canRenderCanonical || isProcessing || isFailed

    val resolvedDueDate: LocalDate?
        get() = when (val data = draftData) {
            is InvoiceDraftData -> data.dueDate
            else -> (documentRecord?.confirmedEntity as? FinancialDocumentDto.InvoiceDto)?.dueDate
        }

    val financialStatus: ReviewFinancialStatus
        get() {
            if (isProcessing || hasAttention) return ReviewFinancialStatus.Review
            val entry = (cashflowEntryState as? DokusState.Success<CashflowEntry>)?.data
            val isOverdueByDueDate = isDocumentConfirmed &&
                (resolvedDueDate?.let { it < today } == true)
            return when (entry?.status) {
                CashflowEntryStatus.Paid -> ReviewFinancialStatus.Paid
                CashflowEntryStatus.Overdue -> ReviewFinancialStatus.Overdue
                CashflowEntryStatus.Open -> if (isOverdueByDueDate) {
                    ReviewFinancialStatus.Overdue
                } else {
                    ReviewFinancialStatus.Unpaid
                }
                CashflowEntryStatus.Cancelled,
                null -> {
                    if (isOverdueByDueDate) {
                        ReviewFinancialStatus.Overdue
                    } else if (isDocumentConfirmed) {
                        ReviewFinancialStatus.Unpaid
                    } else {
                        ReviewFinancialStatus.Review
                    }
                }
            }
        }

    val canRecordPayment: Boolean
        get() {
            val entry = (cashflowEntryState as? DokusState.Success<CashflowEntry>)?.data ?: return false
            return entry.status != CashflowEntryStatus.Paid && !entry.remainingAmount.isZero
        }
}

// =========================================================================
// DocumentDraftData extension properties for review state
// =========================================================================

/** Whether the draft has the minimum required dates for its type. */
private val DocumentDraftData.hasRequiredDates: Boolean
    get() = when (this) {
        is InvoiceDraftData -> issueDate != null
        is ReceiptDraftData -> date != null
        is CreditNoteDraftData -> issueDate != null
        is BankStatementDraftData -> transactions.all { it.transactionDate != null }
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
        is OtherDraftData -> true
    }

/** Direction must be known for document types that map to invoice/credit-note entities. */
private val DocumentDraftData.hasKnownDirectionForConfirmation: Boolean
    get() = when (this) {
        is InvoiceDraftData -> direction != DocumentDirection.Unknown
        is CreditNoteDraftData -> direction != DocumentDirection.Unknown
        is ReceiptDraftData -> true
        is BankStatementDraftData -> direction == DocumentDirection.Neutral
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
        is OtherDraftData -> true
    }

/** Identity fields required by backend confirmation services. */
private val DocumentDraftData.hasRequiredIdentityForConfirmation: Boolean
    get() = when (this) {
        is InvoiceDraftData -> true
        is ReceiptDraftData -> !merchantName.isNullOrBlank()
        is CreditNoteDraftData -> !creditNoteNumber.isNullOrBlank()
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
        is OtherDraftData -> true
    }

/** Subtotal requirement enforced by backend (credit notes only). */
private val DocumentDraftData.hasRequiredSubtotalForConfirmation: Boolean
    get() = when (this) {
        is InvoiceDraftData -> true
        is ReceiptDraftData -> true
        is CreditNoteDraftData -> subtotalAmount != null
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
        is OtherDraftData -> true
    }

/** Whether totals required for confirmation are present. */
private val DocumentDraftData.hasRequiredTotalForConfirmation: Boolean
    get() = when (this) {
        is InvoiceDraftData -> totalAmount != null
        is ReceiptDraftData -> totalAmount != null
        is CreditNoteDraftData -> totalAmount != null
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
        is OtherDraftData -> true
    }

/** Whether VAT required for confirmation is present (0-value VAT is valid). */
private val DocumentDraftData.hasRequiredVatForConfirmation: Boolean
    get() = when (this) {
        is InvoiceDraftData -> vatAmount != null
        is CreditNoteDraftData -> vatAmount != null
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
        is OtherDraftData -> true
    }

/** Whether a contact is required for this document type. */
internal val DocumentDraftData?.isContactRequired: Boolean
    get() = this is InvoiceDraftData || this is CreditNoteDraftData

/** Derive DocumentType from sealed subtype. */
internal val DocumentDraftData?.documentType: DocumentType
    get() = this?.toDocumentType() ?: DocumentType.Unknown

/** Context/description text for understanding line. */
private val DocumentDraftData?.displayContextDescription: String?
    get() = when (this) {
        is InvoiceDraftData -> notes?.takeIf { it.isNotBlank() }
        is ReceiptDraftData -> notes?.takeIf { it.isNotBlank() }
        is CreditNoteDraftData -> reason?.takeIf { it.isNotBlank() }
        is BankStatementDraftData -> notes?.takeIf { it.isNotBlank() }
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
        null -> null
    }

/**
 * Contact match status - captures uncertainty, not just null.
 * Used for policy-based attention signals in the UI.
 */
enum class ContactMatchStatus {
    /** Bound via explicit user selection. */
    Matched,

    /** Suggested but not yet confirmed. */
    Uncertain,

    /** No contact, and document type requires one (Invoice/CreditNote). */
    MissingButRequired,

    /** No contact, but acceptable for this document type (Receipt). */
    NotRequired
}
