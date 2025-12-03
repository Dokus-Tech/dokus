package ai.dokus.app.cashflow.usecase

import ai.dokus.foundation.domain.enums.InvoiceStatus
import ai.dokus.foundation.domain.model.FinancialDocumentDto

/**
 * Filters financial documents by a free-text query.
 */
internal class SearchCashflowDocumentsUseCase {

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
            } || document.amount.value.contains(needle, ignoreCase = true)
        }
    }

    private fun matchesInvoice(
        invoice: FinancialDocumentDto.InvoiceDto,
        needle: String
    ): Boolean {
        return invoice.invoiceNumber.value.contains(needle, ignoreCase = true) ||
                invoice.notes?.contains(needle, ignoreCase = true) == true ||
                invoice.status.matches(needle)
    }

    private fun matchesExpense(
        expense: FinancialDocumentDto.ExpenseDto,
        needle: String
    ): Boolean {
        return expense.merchant.contains(needle, ignoreCase = true) ||
                expense.description?.contains(needle, ignoreCase = true) == true ||
                expense.category.name.contains(needle, ignoreCase = true) ||
                expense.notes?.contains(needle, ignoreCase = true) == true
    }

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

    private fun InvoiceStatus.matches(needle: String): Boolean {
        return name.contains(needle, ignoreCase = true)
    }
}
