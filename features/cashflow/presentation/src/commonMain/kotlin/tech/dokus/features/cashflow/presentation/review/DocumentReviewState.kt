package tech.dokus.features.cashflow.presentation.review

import androidx.compose.runtime.Immutable
import org.jetbrains.compose.resources.StringResource
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_confirm_missing_fields
import tech.dokus.aura.resources.cashflow_confirm_select_contact
import tech.dokus.domain.Money
import tech.dokus.domain.asbtractions.RetryHandler
import tech.dokus.domain.enums.CounterpartyIntent
import tech.dokus.domain.enums.DocumentRejectReason
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.DraftStatus
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.model.ExtractedDocumentData
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.foundation.app.state.DokusState

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

private const val MinConfidenceThreshold = 0.0
private const val PercentageMultiplier = 100

@Immutable
sealed interface DocumentReviewState : MVIState, DokusState<Nothing> {

    data object Loading : DocumentReviewState

    data class Content(
        val documentId: DocumentId,
        val document: DocumentRecordDto,
        val editableData: EditableExtractedData,
        val originalData: ExtractedDocumentData?,
        val hasUnsavedChanges: Boolean = false,
        val isSaving: Boolean = false,
        val isConfirming: Boolean = false,
        val selectedFieldPath: String? = null,
        val previewUrl: String? = null,
        val contactSuggestions: List<ContactSuggestion> = emptyList(),
        val previewState: DocumentPreviewState = DocumentPreviewState.Loading,
        val selectedContactId: ContactId? = null,
        val selectedContactSnapshot: ContactSnapshot? = null,
        val contactSelectionState: ContactSelectionState = ContactSelectionState.NoContact,
        val isContactRequired: Boolean = false,
        val counterpartyIntent: CounterpartyIntent = CounterpartyIntent.None,
        val contactValidationError: DokusException? = null,
        val isBindingContact: Boolean = false,
        val isRejecting: Boolean = false,
        val isDocumentConfirmed: Boolean = false,
        val isDocumentRejected: Boolean = false,
        val confirmedCashflowEntryId: CashflowEntryId? = null,
        val showPreviewSheet: Boolean = false,
        val rejectDialogState: RejectDialogState? = null,
        val failureBannerDismissed: Boolean = false,
        // Contact sheet state
        val showContactSheet: Boolean = false,
        val contactSheetSearchQuery: String = "",
        val contactSheetContacts: DokusState<List<ContactDto>> = DokusState.idle(),
    ) : DocumentReviewState {

        /** True when AI extraction is still in progress (Queued or Processing). */
        val isProcessing: Boolean
            get() = document.latestIngestion?.status in listOf(
                IngestionStatus.Queued,
                IngestionStatus.Processing
            )

        /** True when AI extraction failed. */
        val isFailed: Boolean
            get() = document.latestIngestion?.status == IngestionStatus.Failed ||
                !document.latestIngestion?.errorMessage.isNullOrBlank()

        /** Error message from failed extraction, if available. */
        val failureReason: String?
            get() = document.latestIngestion?.errorMessage

        val canConfirm: Boolean
            get() {
                val baseValid = (
                    document.draft?.draftStatus == DraftStatus.NeedsReview ||
                        document.draft?.draftStatus == DraftStatus.Ready
                    ) &&
                    !isConfirming &&
                    !isSaving &&
                    !isRejecting &&
                    !isBindingContact &&
                    !isProcessing &&
                    confirmBlockedReason == null
                return baseValid
            }

        val confirmBlockedReason: StringResource?
            get() = when {
                isDocumentConfirmed || isDocumentRejected -> null
                editableData.documentType.isUnknown -> Res.string.cashflow_confirm_missing_fields
                !editableData.hasRequiredDates -> Res.string.cashflow_confirm_missing_fields
                !editableData.hasCoherentAmounts -> Res.string.cashflow_confirm_missing_fields
                counterpartyIntent == CounterpartyIntent.Pending -> Res.string.cashflow_confirm_select_contact
                isContactRequired && selectedContactId == null -> Res.string.cashflow_confirm_select_contact
                !editableData.isValid -> Res.string.cashflow_confirm_missing_fields
                else -> null
            }

        val showConfidence: Boolean
            get() {
                val conf = document.latestIngestion?.confidence
                val status = document.draft?.draftStatus
                val statusAllowsConfidence = status != DraftStatus.NeedsReview && status != DraftStatus.Rejected
                return conf != null && conf > MinConfidenceThreshold && statusAllowsConfidence
            }

        val confidencePercent: Int
            get() = ((document.latestIngestion?.confidence ?: MinConfidenceThreshold) * PercentageMultiplier).toInt()

        /**
         * Hard gate - Confirm button is disabled when this is true.
         * True when: missing required fields (type, total, issue date).
         */
        val isBlocking: Boolean
            get() = confirmBlockedReason != null

        /**
         * Soft attention signal - shows amber indicator but doesn't block confirm.
         * True when: contact uncertain, due date missing, etc.
         */
        val hasAttention: Boolean
            get() = isBlocking ||
                selectedContactSnapshot == null ||
                editableData.dueDate == null

        /**
         * Resolved description for the header understanding line.
         * Priority: context (notes/description) + counterparty, or fallback to filename.
         */
        val description: String
            get() {
                val counterparty = editableData.counterpartyName
                val context = editableData.contextDescription

                return when {
                    counterparty != null && context != null -> "$counterparty — $context"
                    context != null -> context
                    counterparty != null -> counterparty
                    isProcessing -> "Processing document…"
                    else -> document.document.filename ?: "Document"
                }
            }

        /**
         * Total amount for the understanding line (currency-formatted).
         */
        val totalAmount: Money?
            get() = when (editableData.documentType) {
                DocumentType.Invoice -> Money.parse(editableData.invoice?.totalAmount ?: "")
                DocumentType.Bill -> Money.parse(editableData.bill?.amount ?: "")
                DocumentType.Expense -> Money.parse(editableData.expense?.amount ?: "")
                DocumentType.Receipt -> Money.parse(editableData.receipt?.amount ?: "")
                DocumentType.ProForma -> Money.parse(editableData.proForma?.totalAmount ?: "")
                DocumentType.CreditNote -> Money.parse(editableData.creditNote?.totalAmount ?: "")
                else -> null
            }
    }

    data class Error(
        override val exception: DokusException,
        override val retryHandler: RetryHandler,
    ) : DocumentReviewState, DokusState.Error<Nothing>
}

private val DocumentType.isUnknown: Boolean
    get() = this == DocumentType.Unknown

private val EditableExtractedData.hasRequiredDates: Boolean
    get() = when (documentType) {
        DocumentType.Invoice -> invoice?.issueDate != null
        DocumentType.Bill -> bill?.issueDate != null
        DocumentType.Expense -> expense?.date != null
        else -> false
    }

private val EditableExtractedData.hasCoherentAmounts: Boolean
    get() {
        return when (documentType) {
            DocumentType.Invoice -> {
                val invoiceData = invoice ?: return false
                val subtotal = Money.parse(invoiceData.subtotalAmount)
                val vat = Money.parse(invoiceData.vatAmount)
                val total = Money.parse(invoiceData.totalAmount)
                if (subtotal == null) return false
                if (total == null || vat == null) return true
                val expected = subtotal + vat
                kotlin.math.abs(expected.minor - total.minor) <= 1L
            }
            DocumentType.Bill -> {
                val billData = bill ?: return false
                Money.parse(billData.amount) != null
            }
            DocumentType.Expense -> {
                val expenseData = expense ?: return false
                Money.parse(expenseData.amount) != null
            }
            else -> false
        }
    }

/** Counterparty name for description resolution. */
private val EditableExtractedData.counterpartyName: String?
    get() = when (documentType) {
        DocumentType.Invoice -> invoice?.clientName?.takeIf { it.isNotBlank() }
        DocumentType.Bill -> bill?.supplierName?.takeIf { it.isNotBlank() }
        DocumentType.Expense -> expense?.merchant?.takeIf { it.isNotBlank() }
        DocumentType.Receipt -> receipt?.merchant?.takeIf { it.isNotBlank() }
        DocumentType.ProForma -> proForma?.clientName?.takeIf { it.isNotBlank() }
        DocumentType.CreditNote -> creditNote?.counterpartyName?.takeIf { it.isNotBlank() }
        else -> null
    }

/** Context/description text for understanding line. */
private val EditableExtractedData.contextDescription: String?
    get() = when (documentType) {
        DocumentType.Invoice -> invoice?.notes?.takeIf { it.isNotBlank() }
        DocumentType.Bill -> bill?.description?.takeIf { it.isNotBlank() }
        DocumentType.Expense -> expense?.description?.takeIf { it.isNotBlank() }
        DocumentType.Receipt -> receipt?.description?.takeIf { it.isNotBlank() }
        DocumentType.ProForma -> proForma?.notes?.takeIf { it.isNotBlank() }
        DocumentType.CreditNote -> creditNote?.reason?.takeIf { it.isNotBlank() }
        else -> null
    }

/** Due date for attention signal. */
private val EditableExtractedData.dueDate: kotlinx.datetime.LocalDate?
    get() = when (documentType) {
        DocumentType.Invoice -> invoice?.dueDate
        DocumentType.Bill -> bill?.dueDate
        DocumentType.ProForma -> proForma?.validUntil
        else -> null
    }
