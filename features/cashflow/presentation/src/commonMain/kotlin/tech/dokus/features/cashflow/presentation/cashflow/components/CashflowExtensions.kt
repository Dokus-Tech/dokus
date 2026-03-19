package tech.dokus.features.cashflow.presentation.cashflow.components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.invoice_status_cancelled
import tech.dokus.aura.resources.invoice_status_draft
import tech.dokus.aura.resources.invoice_status_overdue
import tech.dokus.aura.resources.invoice_status_paid
import tech.dokus.aura.resources.invoice_status_partial
import tech.dokus.aura.resources.invoice_status_refunded
import tech.dokus.aura.resources.invoice_status_sent
import tech.dokus.aura.resources.invoice_status_viewed
import tech.dokus.domain.enums.InvoiceStatus
import tech.dokus.domain.model.DocDto
import tech.dokus.domain.model.sortDate

/**
 * Extension functions for working with DocDto types in the cashflow presentation layer.
 */

/**
 * Filters documents that need confirmation or approval.
 * For invoices: Sent or Overdue status
 * For other types: Currently none (could be extended for approval workflows)
 *
 * @return List of documents requiring user attention
 */
fun List<DocDto>.needingConfirmation(): List<DocDto> {
    return this.filter { doc ->
        when (doc) {
            is DocDto.Invoice.Confirmed ->
                doc.status == InvoiceStatus.Sent || doc.status == InvoiceStatus.Overdue
            is DocDto.Invoice.Draft -> false
            is DocDto.Receipt -> false
            is DocDto.CreditNote -> false
            is DocDto.BankStatement -> false
            is DocDto.ClassifiedDoc -> false
        }
    }
}

/**
 * Filters confirmed invoices that need confirmation.
 *
 * @return List of invoices with Sent or Overdue status
 */
fun List<DocDto.Invoice.Confirmed>.invoicesNeedingConfirmation(): List<DocDto.Invoice.Confirmed> {
    return this.filter { it.status == InvoiceStatus.Sent || it.status == InvoiceStatus.Overdue }
}

/**
 * Combines invoices and receipts into a unified DocDto list, sorted by date.
 *
 * @param invoices List of invoices to include
 * @param receipts List of receipts to include
 * @param limit Maximum number of items to return
 * @return Combined and sorted list of DocDto
 */
fun combineFinancialDocuments(
    invoices: List<DocDto.Invoice.Confirmed>,
    receipts: List<DocDto.Receipt.Confirmed>,
    limit: Int = 4
): List<DocDto> {
    val allDocs = mutableListOf<DocDto>()
    allDocs.addAll(invoices)
    allDocs.addAll(receipts)

    return allDocs
        .sortedByDescending { it.sortDate }
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
fun DocDto.Invoice.Confirmed.needsAttention(): Boolean {
    return status == InvoiceStatus.Sent || status == InvoiceStatus.Overdue
}

/**
 * Checks if an invoice is in a completed state (paid or refunded).
 *
 * @return True if the invoice is completed
 */
fun DocDto.Invoice.Confirmed.isCompleted(): Boolean {
    return status == InvoiceStatus.Paid || status == InvoiceStatus.Refunded
}

/**
 * Checks if an invoice can be edited (only draft invoices).
 *
 * @return True if the invoice can be edited
 */
fun DocDto.Invoice.Confirmed.canEdit(): Boolean {
    return status == InvoiceStatus.Draft
}
