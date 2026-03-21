package tech.dokus.features.cashflow.presentation.detail

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
import tech.dokus.domain.enums.DocumentRejectReason
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.DocumentSourceId
import tech.dokus.domain.model.AutoPaymentStatus
import tech.dokus.domain.model.BankTransactionDto
import tech.dokus.domain.model.CashflowEntryDto
import tech.dokus.domain.model.DocDto
import tech.dokus.domain.model.DocumentDetailDto
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.domain.model.contact.ContactSuggestionDto
import tech.dokus.domain.model.contact.ResolvedContact
import tech.dokus.domain.model.hasKnownDirectionForConfirmation
import tech.dokus.domain.model.hasRequiredDates
import tech.dokus.domain.model.hasRequiredIdentityForConfirmation
import tech.dokus.domain.model.hasRequiredSubtotalForConfirmation
import tech.dokus.domain.model.hasRequiredTotalForConfirmation
import tech.dokus.domain.model.hasRequiredVatForConfirmation
import tech.dokus.domain.model.isContactRequired
import tech.dokus.domain.model.toDocumentType
import tech.dokus.domain.model.totalAmount
import tech.dokus.features.cashflow.presentation.detail.models.DocumentUiData
import tech.dokus.features.cashflow.presentation.detail.models.toUiData
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
    val draftData: DocDto?,
    val originalData: DocDto?,
    val previewUrl: String?,
    val contactSuggestions: List<ContactSuggestionDto>,
)

@Immutable
data class DocumentDetailState(
    // === Main network data ===
    val document: DokusState<ReviewDocumentData> = DokusState.idle(),
    val isAwaitingExtraction: Boolean = false,
    val previewState: DocumentPreviewState = DocumentPreviewState.Loading,
    val incomingPreviewState: DocumentPreviewState? = null,

    // === UI flags ===
    val hasUnsavedChanges: Boolean = false,
    val isSaving: Boolean = false,
    val isConfirming: Boolean = false,
    val selectedFieldPath: String? = null,
    val selectedContactOverride: ResolvedContact.Linked? = null,
    val isContactRequired: Boolean = false,
    val contactValidationError: DokusException? = null,
    val isBindingContact: Boolean = false,
    val isRejecting: Boolean = false,
    val isResolvingMatchReview: Boolean = false,
    val documentStatus: DocumentStatus? = null,
    val confirmedCashflowEntryId: CashflowEntryId? = null,
    val cashflowEntryState: DokusState<CashflowEntryDto> = DokusState.idle(),
    val autoPaymentStatus: DokusState<AutoPaymentStatus> = DokusState.idle(),
    val isUndoingAutoPayment: Boolean = false,
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
    val queueState: DocumentDetailQueueState? = null,
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
    val draftData: DocDto?
        get() = documentData?.draftData

    /** Presentation-layer document data for UI rendering. Composables should use this. */
    val uiData: DocumentUiData?
        get() = draftData?.toUiData()

    /** The original AI draft data. */
    val originalData: DocDto?
        get() = documentData?.originalData

    /** The preview URL. */
    val previewUrl: String?
        get() = documentData?.previewUrl

    /** Contact suggestions from extraction. */
    val contactSuggestions: List<ContactSuggestionDto>
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
     * The effective contact for display and confirm logic.
     * User override (from contact sheet) takes priority over backend resolution.
     */
    val effectiveContact: ResolvedContact
        get() = selectedContactOverride
            ?: documentRecord?.draft?.resolvedContact
            ?: ResolvedContact.Unknown

    /**
     * True when contact is required but no contact data is available.
     * Linked, Suggested, and Detected all provide enough data — only Unknown blocks.
     */
    val hasUnresolvedContact: Boolean
        get() {
            if (draftData?.isContactRequired != true) return false
            return effectiveContact is ResolvedContact.Unknown
        }

    val confirmBlockedReason: StringResource?
        get() {
            if (isDocumentConfirmed || isDocumentRejected || isDocumentUnsupported) return null
            val content = draftData ?: return Res.string.cashflow_confirm_missing_fields
            return when {
                hasUnresolvedContact -> Res.string.cashflow_confirm_select_contact
                !content.hasKnownDirectionForConfirmation -> Res.string.cashflow_confirm_missing_fields
                !content.hasRequiredIdentityForConfirmation -> Res.string.cashflow_confirm_missing_fields
                !content.hasRequiredDates -> Res.string.cashflow_confirm_missing_fields
                !content.hasRequiredSubtotalForConfirmation -> Res.string.cashflow_confirm_missing_fields
                !content.hasRequiredTotalForConfirmation -> Res.string.cashflow_confirm_missing_fields
                !content.hasRequiredVatForConfirmation -> Res.string.cashflow_confirm_missing_fields
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
            effectiveContact is ResolvedContact.Linked -> ContactMatchStatus.Matched
            effectiveContact is ResolvedContact.Suggested && draftData.isContactRequired ->
                ContactMatchStatus.Uncertain
            effectiveContact is ResolvedContact.Detected && draftData.isContactRequired ->
                ContactMatchStatus.Uncertain
            draftData.isContactRequired -> ContactMatchStatus.MissingButRequired
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
            val needsDueDate = (draftData is DocDto.Invoice) &&
                !isDocumentConfirmed
            return needsDueDate && (draftData as? DocDto.Invoice)?.dueDate == null
        }

    /**
     * Resolved description for the header understanding line.
     * Priority: context (notes/description) + counterparty, or fallback to filename.
     */
    val description: String
        get() {
            val contactName = when (val c = effectiveContact) {
                is ResolvedContact.Linked -> c.name
                is ResolvedContact.Suggested -> c.name
                is ResolvedContact.Detected -> c.name
                is ResolvedContact.Unknown -> null
            }?.takeIf { it.isNotBlank() }
            val context = draftData.displayContextDescription

            return when {
                contactName != null && context != null -> "$contactName — $context"
                context != null -> context
                contactName != null -> contactName
                isProcessing -> "Processing document..."
                else -> documentRecord?.document?.filename ?: ""
            }
        }

    /**
     * Total amount for the understanding line (currency-formatted).
     */
    val totalAmount: Money?
        get() = draftData?.totalAmount

    /**
     * Canonical rendering is available for document types with structured views.
     * Bank statements always use canonical (transaction table).
     * Invoices/credit notes use canonical only when confirmed — show PDF while reviewing.
     */
    val canRenderCanonical: Boolean
        get() = when {
            draftData is DocDto.BankStatement -> true
            documentStatus == DocumentStatus.Confirmed -> draftData is DocDto.Invoice || draftData is DocDto.CreditNote
            else -> false
        }

    val hasPendingMatchReview: Boolean
        get() = documentRecord?.pendingMatchReview != null

    val shouldShowPendingMatchComparison: Boolean
        get() = hasPendingMatchReview && !isProcessing && !isFailed && sourceViewerState == null

    val shouldUsePdfFallback: Boolean
        get() = when {
            hasPendingMatchReview && !isProcessing && !isFailed -> false
            else -> !canRenderCanonical || isProcessing || isFailed
        }

    val resolvedDueDate: LocalDate?
        get() = (draftData as? DocDto.Invoice)?.dueDate

    val financialStatus: ReviewFinancialStatus
        get() {
            if (isProcessing || hasAttention) return ReviewFinancialStatus.Review
            val entry = (cashflowEntryState as? DokusState.Success<CashflowEntryDto>)?.data
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
            val entry = (cashflowEntryState as? DokusState.Success<CashflowEntryDto>)?.data ?: return false
            return entry.status != CashflowEntryStatus.Paid && !entry.remainingAmount.isZero
        }
}

// =========================================================================
// DocDto extension properties for review state
// =========================================================================
// hasRequiredDates, hasKnownDirectionForConfirmation, hasRequiredIdentityForConfirmation,
// hasRequiredSubtotalForConfirmation, hasRequiredTotalForConfirmation, hasRequiredVatForConfirmation,
// isContactRequired, totalAmount — all imported from DocDtoConversions.kt

/** Derive DocumentType from sealed subtype. */
internal val DocDto?.documentType: DocumentType
    get() = this?.toDocumentType() ?: DocumentType.Unknown

/** Context/description text for understanding line. */
private val DocDto?.displayContextDescription: String?
    get() = when (this) {
        is DocDto.Invoice -> notes?.takeIf { it.isNotBlank() }
        is DocDto.Receipt -> notes?.takeIf { it.isNotBlank() }
        is DocDto.CreditNote -> reason?.takeIf { it.isNotBlank() }
        is DocDto.BankStatement -> notes?.takeIf { it.isNotBlank() }
        is DocDto.ClassifiedDoc,
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
