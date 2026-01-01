package tech.dokus.features.cashflow.presentation.review

import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIIntent
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.ExtractedLineItem

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

    data class UpdateExpenseField(
        val field: ExpenseField,
        val value: Any?,
    ) : DocumentReviewIntent

    data class SelectContact(val contactId: ContactId) : DocumentReviewIntent
    data object AcceptSuggestedContact : DocumentReviewIntent
    data object ClearSelectedContact : DocumentReviewIntent
    data class ContactCreated(val contactId: ContactId) : DocumentReviewIntent

    data object OpenPreviewSheet : DocumentReviewIntent
    data object ClosePreviewSheet : DocumentReviewIntent

    data object AddLineItem : DocumentReviewIntent
    data class UpdateLineItem(val index: Int, val item: ExtractedLineItem) : DocumentReviewIntent
    data class RemoveLineItem(val index: Int) : DocumentReviewIntent

    data class SelectFieldForProvenance(val fieldPath: String?) : DocumentReviewIntent

    data object SaveDraft : DocumentReviewIntent
    data object DiscardChanges : DocumentReviewIntent
    data object Confirm : DocumentReviewIntent
    data object Reject : DocumentReviewIntent
    data object OpenChat : DocumentReviewIntent
}

enum class InvoiceField {
    CLIENT_NAME,
    CLIENT_VAT_NUMBER,
    CLIENT_EMAIL,
    CLIENT_ADDRESS,
    INVOICE_NUMBER,
    ISSUE_DATE,
    DUE_DATE,
    SUBTOTAL_AMOUNT,
    VAT_AMOUNT,
    TOTAL_AMOUNT,
    CURRENCY,
    NOTES,
    PAYMENT_TERMS,
    BANK_ACCOUNT,
}

enum class BillField {
    SUPPLIER_NAME,
    SUPPLIER_VAT_NUMBER,
    SUPPLIER_ADDRESS,
    INVOICE_NUMBER,
    ISSUE_DATE,
    DUE_DATE,
    AMOUNT,
    VAT_AMOUNT,
    VAT_RATE,
    CURRENCY,
    CATEGORY,
    DESCRIPTION,
    NOTES,
    PAYMENT_TERMS,
    BANK_ACCOUNT,
}

enum class ExpenseField {
    MERCHANT,
    MERCHANT_ADDRESS,
    MERCHANT_VAT_NUMBER,
    DATE,
    AMOUNT,
    VAT_AMOUNT,
    VAT_RATE,
    CURRENCY,
    CATEGORY,
    DESCRIPTION,
    IS_DEDUCTIBLE,
    DEDUCTIBLE_PERCENTAGE,
    PAYMENT_METHOD,
    NOTES,
    RECEIPT_NUMBER,
}
