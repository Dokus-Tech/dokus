package ai.dokus.app.cashflow.components

import ai.dokus.foundation.domain.enums.Currency
import ai.dokus.foundation.domain.enums.InvoiceStatus
import ai.dokus.foundation.domain.model.Expense
import ai.dokus.foundation.domain.model.FinancialDocument
import ai.dokus.foundation.domain.model.FinancialDocumentStatus
import ai.dokus.foundation.domain.model.Invoice

/**
 * Extension functions for converting domain models to FinancialDocument types.
 */

/**
 * Converts an Invoice to FinancialDocument.InvoiceDocument.
 *
 * @param status Optional override status. If null, will map from invoice status.
 * @return FinancialDocument.InvoiceDocument representation
 */
@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
fun Invoice.toFinancialDocument(
    status: FinancialDocumentStatus? = null
): FinancialDocument.InvoiceDocument {
    return FinancialDocument.InvoiceDocument(
        documentId = id.value.toString(),
        tenantId = tenantId,
        documentNumber = invoiceNumber.toString(),
        date = issueDate,
        amount = totalAmount,
        currency = currency,
        status = status ?: mapInvoiceStatus(this.status),
        description = notes,
        createdAt = createdAt,
        updatedAt = updatedAt,
        invoiceId = id,
        clientId = clientId,
        invoiceNumber = invoiceNumber,
        dueDate = dueDate,
        subtotalAmount = subtotalAmount,
        vatAmount = vatAmount,
        paidAmount = paidAmount,
        items = items
    )
}

/**
 * Converts an Expense to FinancialDocument.ExpenseDocument.
 *
 * @param status Optional override status. Defaults to PendingApproval for uploaded expenses.
 * @return FinancialDocument.ExpenseDocument representation
 */
@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
fun Expense.toFinancialDocument(
    status: FinancialDocumentStatus = FinancialDocumentStatus.PendingApproval
): FinancialDocument.ExpenseDocument {
    return FinancialDocument.ExpenseDocument(
        documentId = id.value.toString(),
        tenantId = tenantId,
        documentNumber = "EXP-${id.value}",
        date = date,
        amount = amount,
        currency = Currency.Eur, // Default to EUR, adjust as needed
        status = status,
        description = description,
        createdAt = createdAt,
        updatedAt = updatedAt,
        expenseId = id,
        merchant = merchant,
        category = category,
        receiptUrl = receiptUrl,
        vatAmount = vatAmount,
        isDeductible = isDeductible
    )
}

/**
 * Maps InvoiceStatus to FinancialDocumentStatus.
 */
private fun mapInvoiceStatus(invoiceStatus: InvoiceStatus): FinancialDocumentStatus {
    return when (invoiceStatus) {
        InvoiceStatus.Draft -> FinancialDocumentStatus.Draft
        InvoiceStatus.Sent, InvoiceStatus.Viewed -> FinancialDocumentStatus.PendingApproval
        InvoiceStatus.Paid -> FinancialDocumentStatus.Completed
        InvoiceStatus.Overdue -> FinancialDocumentStatus.PendingApproval
        InvoiceStatus.Cancelled -> FinancialDocumentStatus.Cancelled
        InvoiceStatus.Refunded -> FinancialDocumentStatus.Completed
        InvoiceStatus.PartiallyPaid -> FinancialDocumentStatus.Approved
    }
}

/**
 * Filters financial documents that need confirmation or approval.
 *
 * @return List of documents with PendingApproval status
 */
fun List<FinancialDocument>.financialDocumentsNeedingConfirmation(): List<FinancialDocument> {
    return this.filter { it.status == FinancialDocumentStatus.PendingApproval }
}

/**
 * Filters invoices that need confirmation.
 *
 * @return List of invoices with Sent or Overdue status
 */
fun List<Invoice>.needingConfirmation(): List<Invoice> {
    return this.filter { it.status == InvoiceStatus.Sent || it.status == InvoiceStatus.Overdue }
}

/**
 * Converts a list of invoices to FinancialDocument list.
 *
 * @param limit Maximum number of items to convert. Default is 4 (matches Figma design)
 * @param status Optional override status for all items
 * @return List of FinancialDocument.InvoiceDocument
 */
fun List<Invoice>.invoicesToFinancialDocuments(
    limit: Int = 4,
    status: FinancialDocumentStatus? = null
): List<FinancialDocument.InvoiceDocument> {
    return this.take(limit).map { it.toFinancialDocument(status) }
}

/**
 * Converts a list of expenses to FinancialDocument list.
 *
 * @param limit Maximum number of items to convert. Default is 4
 * @param status Optional override status for all items
 * @return List of FinancialDocument.ExpenseDocument
 */
fun List<Expense>.expensesToFinancialDocuments(
    limit: Int = 4,
    status: FinancialDocumentStatus = FinancialDocumentStatus.PendingApproval
): List<FinancialDocument.ExpenseDocument> {
    return this.take(limit).map { it.toFinancialDocument(status) }
}

/**
 * Combines invoices and expenses into a unified FinancialDocument list, sorted by date.
 *
 * @param invoices List of invoices to include
 * @param expenses List of expenses to include
 * @param limit Maximum number of items to return
 * @return Combined and sorted list of FinancialDocuments
 */
fun combineFinancialDocuments(
    invoices: List<Invoice>,
    expenses: List<Expense>,
    limit: Int = 4
): List<FinancialDocument> {
    val allDocs = mutableListOf<FinancialDocument>()
    allDocs.addAll(invoices.map { it.toFinancialDocument() })
    allDocs.addAll(expenses.map { it.toFinancialDocument() })

    return allDocs
        .sortedByDescending { it.date }
        .take(limit)
}

/**
 * Gets a human-readable status text based on the financial document status.
 *
 * @return Status text suitable for display
 */
fun FinancialDocumentStatus.toDisplayText(): String {
    return when (this) {
        FinancialDocumentStatus.PendingApproval -> "Need confirmation"
        FinancialDocumentStatus.Approved -> "Approved"
        FinancialDocumentStatus.Rejected -> "Rejected"
        FinancialDocumentStatus.Draft -> "Draft"
        FinancialDocumentStatus.Completed -> "Completed"
        FinancialDocumentStatus.Cancelled -> "Cancelled"
    }
}
