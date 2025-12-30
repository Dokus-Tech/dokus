package ai.dokus.app.cashflow.components

import ai.dokus.app.resources.generated.Res
import tech.dokus.domain.enums.InvoiceStatus
import tech.dokus.domain.model.FinancialDocumentDto
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

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
            is FinancialDocumentDto.BillDto -> false // Bills don't have a confirmation workflow by default
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
fun InvoiceStatus.labelRes(): StringResource {
    return when (this) {
        InvoiceStatus.Draft -> Res.string.invoice_status_draft
        InvoiceStatus.Sent -> Res.string.invoice_status_sent
        InvoiceStatus.Viewed -> Res.string.invoice_status_viewed
        InvoiceStatus.PartiallyPaid -> Res.string.invoice_status_partial
        InvoiceStatus.Paid -> Res.string.invoice_status_paid
        InvoiceStatus.Overdue -> Res.string.invoice_status_overdue
        InvoiceStatus.Cancelled -> Res.string.invoice_status_cancelled
        InvoiceStatus.Refunded -> Res.string.invoice_status_refunded
    }
}

@Composable
fun InvoiceStatus.toDisplayText(): String = stringResource(labelRes())

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
