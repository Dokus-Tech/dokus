package tech.dokus.features.cashflow.presentation.review

import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIIntent
import tech.dokus.domain.enums.CounterpartyIntent
import tech.dokus.domain.enums.DocumentRejectReason
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.FinancialLineItem

@Immutable
sealed interface DocumentReviewIntent : MVIIntent {

    data class LoadDocument(val documentId: DocumentId) : DocumentReviewIntent
    data object Refresh : DocumentReviewIntent

    data object LoadPreviewPages : DocumentReviewIntent
    data class LoadMorePages(val maxPages: Int) : DocumentReviewIntent
    data object RetryLoadPreview : DocumentReviewIntent

    data class UpdateInvoiceField(
        val field: InvoiceField,
        val value: Any?,
    ) : DocumentReviewIntent

    data class UpdateBillField(
        val field: BillField,
        val value: Any?,
    ) : DocumentReviewIntent

    data class UpdateReceiptField(
        val field: ReceiptField,
        val value: Any?,
    ) : DocumentReviewIntent

    data class UpdateCreditNoteField(
        val field: CreditNoteField,
        val value: Any?,
    ) : DocumentReviewIntent

    data class SelectContact(val contactId: ContactId) : DocumentReviewIntent
    data object AcceptSuggestedContact : DocumentReviewIntent
    data object ClearSelectedContact : DocumentReviewIntent
    data class ContactCreated(val contactId: ContactId) : DocumentReviewIntent
    data class SetCounterpartyIntent(val intent: CounterpartyIntent) : DocumentReviewIntent

    data object OpenPreviewSheet : DocumentReviewIntent
    data object ClosePreviewSheet : DocumentReviewIntent

    // Contact sheet intents
    data object OpenContactSheet : DocumentReviewIntent
    data object CloseContactSheet : DocumentReviewIntent
    data class UpdateContactSheetSearch(val query: String) : DocumentReviewIntent

    data object AddLineItem : DocumentReviewIntent
    data class UpdateLineItem(val index: Int, val item: FinancialLineItem) : DocumentReviewIntent
    data class RemoveLineItem(val index: Int) : DocumentReviewIntent

    data class SelectFieldForProvenance(val fieldPath: String?) : DocumentReviewIntent

    data object SaveDraft : DocumentReviewIntent
    data object DiscardChanges : DocumentReviewIntent
    data object ConfirmDiscardChanges : DocumentReviewIntent
    data object Confirm : DocumentReviewIntent
    data object OpenChat : DocumentReviewIntent
    data object ViewCashflowEntry : DocumentReviewIntent
    data object ViewEntity : DocumentReviewIntent

    // Reject dialog intents
    data object ShowRejectDialog : DocumentReviewIntent
    data object DismissRejectDialog : DocumentReviewIntent
    data class SelectRejectReason(val reason: DocumentRejectReason) : DocumentReviewIntent
    data class UpdateRejectNote(val note: String) : DocumentReviewIntent
    data object ConfirmReject : DocumentReviewIntent

    // Failed analysis intents
    data object RetryAnalysis : DocumentReviewIntent
    data object DismissFailureBanner : DocumentReviewIntent

    // Manual document type selection (when AI fails or type is unknown)
    data class SelectDocumentType(val type: DocumentType) : DocumentReviewIntent
}

enum class InvoiceField {
    CUSTOMER_NAME,
    CUSTOMER_VAT_NUMBER,
    CUSTOMER_EMAIL,
    INVOICE_NUMBER,
    ISSUE_DATE,
    DUE_DATE,
    SUBTOTAL_AMOUNT,
    VAT_AMOUNT,
    TOTAL_AMOUNT,
    CURRENCY,
    NOTES,
    IBAN,
    PAYMENT_REFERENCE,
}

enum class BillField {
    SUPPLIER_NAME,
    SUPPLIER_VAT_NUMBER,
    INVOICE_NUMBER,
    ISSUE_DATE,
    DUE_DATE,
    TOTAL_AMOUNT,
    VAT_AMOUNT,
    CURRENCY,
    NOTES,
    IBAN,
    PAYMENT_REFERENCE,
}

enum class ReceiptField {
    MERCHANT_NAME,
    MERCHANT_VAT_NUMBER,
    DATE,
    TOTAL_AMOUNT,
    VAT_AMOUNT,
    CURRENCY,
    RECEIPT_NUMBER,
    PAYMENT_METHOD,
    NOTES,
}

/**
 * CreditNote fields - no cashflow on confirm, only on refund recording.
 */
enum class CreditNoteField {
    COUNTERPARTY_NAME,
    COUNTERPARTY_VAT_NUMBER,
    CREDIT_NOTE_NUMBER,
    ORIGINAL_INVOICE_NUMBER,
    ISSUE_DATE,
    SUBTOTAL_AMOUNT,
    VAT_AMOUNT,
    TOTAL_AMOUNT,
    CURRENCY,
    REASON,
    NOTES,
}
