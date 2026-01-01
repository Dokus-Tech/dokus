package tech.dokus.features.cashflow.presentation.cashflow.model.usecase

import tech.dokus.domain.enums.InvoiceStatus
import tech.dokus.domain.model.FinancialDocumentDto

/**
 * Filters financial documents by a free-text query.
 *
 * Provides client-side search functionality across different types of financial documents
 * (invoices, expenses, and bills). The search is case-insensitive and matches against
 * document-specific fields as well as the common amount field.
 *
 * This use case enables quick filtering of the document list displayed in the cashflow
 * overview without requiring a network request, providing instant feedback as the user types.
 */
internal class SearchCashflowDocumentsUseCase {

    /**
     * Filters the given list of financial documents by matching against the search query.
     *
     * Performs case-insensitive substring matching across document-specific searchable fields.
     * Each document type is searched differently:
     * - **Invoices**: invoice number, notes, and status name
     * - **Expenses**: merchant name, description, category name, and notes
     * - **Bills**: supplier name, invoice number, description, category name, notes, and status name
     *
     * All document types also match against the amount field.
     *
     * @param documents The list of financial documents to filter. Can be a mixed list of
     *                  [FinancialDocumentDto.InvoiceDto], [FinancialDocumentDto.ExpenseDto],
     *                  and [FinancialDocumentDto.BillDto] items.
     * @param query The search query entered by the user. Will be trimmed and normalized
     *              to lowercase for matching. An empty or whitespace-only query returns
     *              all documents unfiltered.
     * @return A filtered list containing only documents where at least one searchable field
     *         contains the query as a substring (case-insensitive). Returns the original
     *         list if the query is empty after trimming. The returned list maintains the
     *         original document order.
     */
    operator fun invoke(
        documents: List<FinancialDocumentDto>,
        query: String
    ): List<FinancialDocumentDto> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) return documents

        val needle = normalizedQuery.lowercase()
        return documents.filter { document ->
            when (document) {
                is FinancialDocumentDto.InvoiceDto -> matchesInvoice(document, needle)
                is FinancialDocumentDto.ExpenseDto -> matchesExpense(document, needle)
                is FinancialDocumentDto.BillDto -> matchesBill(document, needle)
            } || document.amount.toDisplayString().contains(needle, ignoreCase = true)
        }
    }

    /**
     * Checks if an invoice matches the search query.
     *
     * Searches against invoice-specific fields: invoice number, optional notes,
     * and the invoice status name (e.g., "DRAFT", "SENT", "PAID").
     *
     * @param invoice The invoice document to check for a match.
     * @param needle The lowercase search query to match against.
     * @return `true` if any of the invoice's searchable fields contain the needle,
     *         `false` otherwise.
     */
    private fun matchesInvoice(
        invoice: FinancialDocumentDto.InvoiceDto,
        needle: String
    ): Boolean {
        return invoice.invoiceNumber.value.contains(needle, ignoreCase = true) ||
                invoice.notes?.contains(needle, ignoreCase = true) == true ||
                invoice.status.matches(needle)
    }

    /**
     * Checks if an expense matches the search query.
     *
     * Searches against expense-specific fields: merchant name, optional description,
     * category name, and optional notes.
     *
     * @param expense The expense document to check for a match.
     * @param needle The lowercase search query to match against.
     * @return `true` if any of the expense's searchable fields contain the needle,
     *         `false` otherwise.
     */
    private fun matchesExpense(
        expense: FinancialDocumentDto.ExpenseDto,
        needle: String
    ): Boolean {
        return expense.merchant.contains(needle, ignoreCase = true) ||
                expense.description?.contains(needle, ignoreCase = true) == true ||
                expense.category.name.contains(needle, ignoreCase = true) ||
                expense.notes?.contains(needle, ignoreCase = true) == true
    }

    /**
     * Checks if a bill matches the search query.
     *
     * Searches against bill-specific fields: supplier name, optional invoice number,
     * optional description, category name, optional notes, and status name.
     *
     * @param bill The bill document to check for a match.
     * @param needle The lowercase search query to match against.
     * @return `true` if any of the bill's searchable fields contain the needle,
     *         `false` otherwise.
     */
    private fun matchesBill(
        bill: FinancialDocumentDto.BillDto,
        needle: String
    ): Boolean {
        return bill.supplierName.contains(needle, ignoreCase = true) ||
                bill.invoiceNumber?.contains(needle, ignoreCase = true) == true ||
                bill.description?.contains(needle, ignoreCase = true) == true ||
                bill.category.name.contains(needle, ignoreCase = true) ||
                bill.notes?.contains(needle, ignoreCase = true) == true ||
                bill.status.name.contains(needle, ignoreCase = true)
    }

    /**
     * Checks if this invoice status matches the search query.
     *
     * Matches against the status enum name (e.g., "DRAFT", "SENT", "PAID", "OVERDUE").
     *
     * @param needle The lowercase search query to match against.
     * @return `true` if the status name contains the needle (case-insensitive),
     *         `false` otherwise.
     */
    private fun InvoiceStatus.matches(needle: String): Boolean {
        return name.contains(needle, ignoreCase = true)
    }
}
