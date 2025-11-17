package ai.dokus.app.cashflow.components

import ai.dokus.foundation.domain.enums.InvoiceStatus
import ai.dokus.foundation.domain.model.Invoice

/**
 * Extension functions for converting domain models to component data models.
 */

/**
 * Converts an Invoice to CashflowItemData.
 *
 * @param customStatusText Optional custom status text. If null, will derive from invoice status.
 * @return CashflowItemData representation of the invoice
 */
fun Invoice.toCashflowItemData(customStatusText: String? = null): CashflowItemData {
    return CashflowItemData(
        invoiceNumber = this.invoiceNumber.toString(),
        statusText = customStatusText ?: getStatusText()
    )
}

/**
 * Gets a human-readable status text based on the invoice status.
 *
 * @return Status text suitable for display in the cashflow card
 */
private fun Invoice.getStatusText(): String {
    return when (this.status) {
        InvoiceStatus.Draft -> "Draft"
        InvoiceStatus.Sent -> "Sent"
        InvoiceStatus.Paid -> "Paid"
        InvoiceStatus.Overdue -> "Overdue"
        InvoiceStatus.Cancelled -> "Cancelled"
        InvoiceStatus.PartiallyPaid -> "Partially paid"
        InvoiceStatus.PendingApproval -> "Need confirmation"
        InvoiceStatus.Disputed -> "Disputed"
        else -> "Unknown"
    }
}

/**
 * Filters invoices that need confirmation or approval.
 *
 * @return List of invoices with PendingApproval status
 */
fun List<Invoice>.needingConfirmation(): List<Invoice> {
    return this.filter { it.status == InvoiceStatus.PendingApproval }
}

/**
 * Converts a list of invoices to CashflowItemData list.
 *
 * @param limit Maximum number of items to convert. Default is 4 (matches Figma design)
 * @param customStatusText Optional custom status text for all items
 * @return List of CashflowItemData
 */
fun List<Invoice>.toCashflowItems(
    limit: Int = 4,
    customStatusText: String? = null
): List<CashflowItemData> {
    return this.take(limit).map { it.toCashflowItemData(customStatusText) }
}
