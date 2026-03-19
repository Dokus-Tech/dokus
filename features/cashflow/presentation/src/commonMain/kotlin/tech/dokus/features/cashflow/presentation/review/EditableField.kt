package tech.dokus.features.cashflow.presentation.review

/**
 * Identifiers for inline-editable fields on the document review screen.
 * Used by [DocumentReviewIntent.UpdateField] to dispatch field updates.
 */
enum class EditableField {
    InvoiceNumber,
    IssueDate,
    DueDate,
    CreditNoteNumber,
    OriginalInvoiceNumber,
    ReceiptNumber,
    ReceiptDate,
    SubtotalAmount,
    VatAmount,
    TotalAmount,
}
