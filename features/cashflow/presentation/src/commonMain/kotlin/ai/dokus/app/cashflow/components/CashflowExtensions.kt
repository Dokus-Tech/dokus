package ai.dokus.app.cashflow.components

import ai.dokus.foundation.domain.enums.InvoiceStatus
import ai.dokus.foundation.domain.model.FinancialDocumentDto

/**
 * Extension functions for working with FinancialDocumentDto types in the cashflow presentation layer.
 */

/**
 * Filters financial documents that need confirmation or approval.
 * For invoices: Sent or Overdue status
 * For expenses: Currently none (could be extended for approval workflows)
 *
 * @return List of documents requiring user attention
 */
fun List<FinancialDocumentDto>.needingConfirmation(): List<FinancialDocumentDto> {
    return this.filter { doc ->
        when (doc) {
            is FinancialDocumentDto.InvoiceDto -> doc.status == InvoiceStatus.Sent || doc.status == InvoiceStatus.Overdue
            is FinancialDocumentDto.ExpenseDto -> false // Expenses don't have a confirmation workflow by default
        }
    }
}

/**
 * Filters invoices that need confirmation.
 *
 * @return List of invoices with Sent or Overdue status
 */
fun List<FinancialDocumentDto.InvoiceDto>.invoicesNeedingConfirmation(): List<FinancialDocumentDto.InvoiceDto> {
    return this.filter { it.status == InvoiceStatus.Sent || it.status == InvoiceStatus.Overdue }
}

/**
 * Combines invoices and expenses into a unified FinancialDocumentDto list, sorted by date.
 *
 * @param invoices List of invoices to include
 * @param expenses List of expenses to include
 * @param limit Maximum number of items to return
 * @return Combined and sorted list of FinancialDocumentDto
 */
fun combineFinancialDocuments(
    invoices: List<FinancialDocumentDto.InvoiceDto>,
    expenses: List<FinancialDocumentDto.ExpenseDto>,
    limit: Int = 4
): List<FinancialDocumentDto> {
    val allDocs = mutableListOf<FinancialDocumentDto>()
    allDocs.addAll(invoices)
    allDocs.addAll(expenses)

    return allDocs
        .sortedByDescending { it.date }
        .take(limit)
}

/**
 * Gets a human-readable status text for an invoice status.
 *
 * @return Status text suitable for display
 */
fun InvoiceStatus.toDisplayText(): String {
    return when (this) {
        InvoiceStatus.Draft -> "Draft"
        InvoiceStatus.Sent -> "Sent"
        InvoiceStatus.Viewed -> "Viewed"
        InvoiceStatus.PartiallyPaid -> "Partially Paid"
        InvoiceStatus.Paid -> "Paid"
        InvoiceStatus.Overdue -> "Overdue"
        InvoiceStatus.Cancelled -> "Cancelled"
        InvoiceStatus.Refunded -> "Refunded"
    }
}

/**
 * Checks if an invoice requires user attention (confirmation/payment).
 *
 * @return True if the invoice needs attention
 */
fun FinancialDocumentDto.InvoiceDto.needsAttention(): Boolean {
    return status == InvoiceStatus.Sent || status == InvoiceStatus.Overdue
}

/**
 * Checks if an invoice is in a completed state (paid or refunded).
 *
 * @return True if the invoice is completed
 */
fun FinancialDocumentDto.InvoiceDto.isCompleted(): Boolean {
    return status == InvoiceStatus.Paid || status == InvoiceStatus.Refunded
}

/**
 * Checks if an invoice can be edited (only draft invoices).
 *
 * @return True if the invoice can be edited
 */
fun FinancialDocumentDto.InvoiceDto.canEdit(): Boolean {
    return status == InvoiceStatus.Draft
}
