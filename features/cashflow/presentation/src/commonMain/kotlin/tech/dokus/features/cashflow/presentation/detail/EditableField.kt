package tech.dokus.features.cashflow.presentation.detail

/**
 * Identifiers for inline-editable fields on the document review screen.
 * Used by [DocumentDetailIntent.UpdateField] to dispatch field updates.
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
