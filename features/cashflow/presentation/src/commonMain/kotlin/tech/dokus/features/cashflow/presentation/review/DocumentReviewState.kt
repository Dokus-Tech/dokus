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
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.BillDraftData
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.DocumentDraftData
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.ReceiptDraftData
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

@Immutable
sealed interface DocumentReviewState : MVIState, DokusState<Nothing> {

    data object Loading : DocumentReviewState

    data class AwaitingExtraction(
        val documentId: DocumentId,
        val document: DocumentRecordDto,
        val previewUrl: String? = null,
    ) : DocumentReviewState

    data class Content(
        val documentId: DocumentId,
        val document: DocumentRecordDto,
        val draftData: DocumentDraftData?,
        val originalData: DocumentDraftData?,
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
                    document.draft?.documentStatus == DocumentStatus.NeedsReview
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
                draftData == null -> Res.string.cashflow_confirm_missing_fields
                !draftData.hasRequiredDates -> Res.string.cashflow_confirm_missing_fields
                !draftData.hasCoherentAmounts -> Res.string.cashflow_confirm_missing_fields
                counterpartyIntent == CounterpartyIntent.Pending -> Res.string.cashflow_confirm_select_contact
                isContactRequired && selectedContactId == null -> Res.string.cashflow_confirm_select_contact
                !draftData.isReviewValid -> Res.string.cashflow_confirm_missing_fields
                else -> null
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
                contactSelectionState is ContactSelectionState.Selected ->
                    ContactMatchStatus.Matched
                // Suggested contact exists for required types, but needs user confirmation
                contactSelectionState is ContactSelectionState.Suggested &&
                    draftData.isContactRequired ->
                    ContactMatchStatus.Uncertain
                // No contact, but required for this document type (Invoice/Bill/CreditNote)
                draftData.isContactRequired ->
                    ContactMatchStatus.MissingButRequired
                // No contact, but acceptable (Receipt)
                else ->
                    ContactMatchStatus.NotRequired
            }

        /**
         * Soft attention signal - policy-based, not field-null checks.
         * SEPARATE from confirmBlockedReason (hard block).
         *
         * Attention rules:
         * 1. Always attention if confirmBlockedReason != null (hard issues are also soft)
         * 2. Attention if contact is Uncertain or MissingButRequired
         * 3. Attention if due date missing AND (Invoice OR Bill) AND not yet confirmed
         */
        val hasAttention: Boolean
            get() {
                // Hard block implies attention
                if (confirmBlockedReason != null) return true

                // Contact uncertainty
                if (contactMatchStatus == ContactMatchStatus.Uncertain ||
                    contactMatchStatus == ContactMatchStatus.MissingButRequired) return true

                // Due date missing for invoices/bills (only when not confirmed)
                val needsDueDate = (draftData is InvoiceDraftData || draftData is BillDraftData) &&
                    !isDocumentConfirmed
                if (needsDueDate && draftData.dueDate == null) return true

                return false
            }

        /**
         * Resolved description for the header understanding line.
         * Priority: context (notes/description) + counterparty, or fallback to filename.
         */
        val description: String
            get() {
                val counterparty = draftData.displayCounterpartyName
                val context = draftData.displayContextDescription

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
            get() = when (draftData) {
                is InvoiceDraftData -> draftData.totalAmount
                is BillDraftData -> draftData.totalAmount
                is ReceiptDraftData -> draftData.totalAmount
                is CreditNoteDraftData -> draftData.totalAmount
                null -> null
            }
    }

    data class Error(
        override val exception: DokusException,
        override val retryHandler: RetryHandler,
    ) : DocumentReviewState, DokusState.Error<Nothing>
}

// =========================================================================
// DocumentDraftData extension properties for review state
// =========================================================================

/** Whether the draft has the minimum required dates for its type. */
private val DocumentDraftData.hasRequiredDates: Boolean
    get() = when (this) {
        is InvoiceDraftData -> issueDate != null
        is BillDraftData -> issueDate != null
        is ReceiptDraftData -> date != null
        is CreditNoteDraftData -> issueDate != null
    }

/** Whether amounts are coherent (subtotal + vat ≈ total where applicable). */
private val DocumentDraftData.hasCoherentAmounts: Boolean
    get() {
        return when (this) {
            is InvoiceDraftData -> {
                val subtotal = subtotalAmount ?: return false
                if (totalAmount == null || vatAmount == null) return true
                val expected = subtotal + vatAmount!!
                kotlin.math.abs(expected.minor - totalAmount!!.minor) <= 1L
            }
            is BillDraftData -> totalAmount != null
            is ReceiptDraftData -> totalAmount != null
            is CreditNoteDraftData -> {
                val subtotal = subtotalAmount ?: return false
                if (totalAmount == null || vatAmount == null) return true
                val expected = subtotal + vatAmount!!
                kotlin.math.abs(expected.minor - totalAmount!!.minor) <= 1L
            }
        }
    }

/** Whether the draft passes basic review validation. */
val DocumentDraftData.isReviewValid: Boolean
    get() = when (this) {
        is InvoiceDraftData -> issueDate != null && subtotalAmount != null
        is BillDraftData -> supplierName != null && issueDate != null && totalAmount != null
        is ReceiptDraftData -> merchantName != null && date != null && totalAmount != null
        is CreditNoteDraftData -> counterpartyName != null && issueDate != null && subtotalAmount != null
    }

/** Whether a contact is required for this document type. */
internal val DocumentDraftData?.isContactRequired: Boolean
    get() = this is InvoiceDraftData || this is BillDraftData || this is CreditNoteDraftData

/** Derive DocumentType from sealed subtype. */
internal val DocumentDraftData?.documentType: DocumentType
    get() = when (this) {
        is InvoiceDraftData -> DocumentType.Invoice
        is BillDraftData -> DocumentType.Bill
        is CreditNoteDraftData -> DocumentType.CreditNote
        is ReceiptDraftData -> DocumentType.Receipt
        null -> DocumentType.Unknown
    }

/** Counterparty name for description resolution. */
private val DocumentDraftData?.displayCounterpartyName: String?
    get() = when (this) {
        is InvoiceDraftData -> customerName?.takeIf { it.isNotBlank() }
        is BillDraftData -> supplierName?.takeIf { it.isNotBlank() }
        is ReceiptDraftData -> merchantName?.takeIf { it.isNotBlank() }
        is CreditNoteDraftData -> counterpartyName?.takeIf { it.isNotBlank() }
        null -> null
    }

/** Context/description text for understanding line. */
private val DocumentDraftData?.displayContextDescription: String?
    get() = when (this) {
        is InvoiceDraftData -> notes?.takeIf { it.isNotBlank() }
        is BillDraftData -> notes?.takeIf { it.isNotBlank() }
        is ReceiptDraftData -> notes?.takeIf { it.isNotBlank() }
        is CreditNoteDraftData -> reason?.takeIf { it.isNotBlank() }
        null -> null
    }

/** Due date for attention signal. */
private val DocumentDraftData?.dueDate: kotlinx.datetime.LocalDate?
    get() = when (this) {
        is InvoiceDraftData -> dueDate
        is BillDraftData -> dueDate
        is ReceiptDraftData -> null
        is CreditNoteDraftData -> null
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
    /** No contact, and document type requires one (Invoice/Bill/CreditNote). */
    MissingButRequired,
    /** No contact, but acceptable for this document type (Receipt). */
    NotRequired
}
