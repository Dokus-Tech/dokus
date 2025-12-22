package ai.dokus.app.cashflow.usecase

import ai.dokus.app.cashflow.components.DocumentSortOption
import ai.dokus.foundation.domain.model.FinancialDocumentDto

/**
 * Sorts and filters financial documents based on the selected sort option.
 *
 * This use case handles pure in-memory transformations of document lists,
 * applying sorting criteria based on the user's preference.
 */
internal class FilterDocumentsUseCase {

    /**
     * Sort documents according to the specified sort option.
     *
     * @param documents The list of documents to sort
     * @param sortOption The sorting criteria to apply
     * @return Sorted list of documents
     */
    operator fun invoke(
        documents: List<FinancialDocumentDto>,
        sortOption: DocumentSortOption
    ): List<FinancialDocumentDto> {
        return when (sortOption) {
            DocumentSortOption.Default -> documents
            DocumentSortOption.DateNewest -> sortByDateDescending(documents)
            DocumentSortOption.DateOldest -> sortByDateAscending(documents)
            DocumentSortOption.AmountHighest -> sortByAmountDescending(documents)
            DocumentSortOption.AmountLowest -> sortByAmountAscending(documents)
            DocumentSortOption.Type -> sortByType(documents)
        }
    }

    private fun sortByDateDescending(
        documents: List<FinancialDocumentDto>
    ): List<FinancialDocumentDto> {
        return documents.sortedByDescending { it.date }
    }

    private fun sortByDateAscending(
        documents: List<FinancialDocumentDto>
    ): List<FinancialDocumentDto> {
        return documents.sortedBy { it.date }
    }

    private fun sortByAmountDescending(
        documents: List<FinancialDocumentDto>
    ): List<FinancialDocumentDto> {
        return documents.sortedByDescending { extractAmount(it) }
    }

    private fun sortByAmountAscending(
        documents: List<FinancialDocumentDto>
    ): List<FinancialDocumentDto> {
        return documents.sortedBy { extractAmount(it) }
    }

    private fun sortByType(
        documents: List<FinancialDocumentDto>
    ): List<FinancialDocumentDto> {
        return documents.sortedBy { document ->
            when (document) {
                is FinancialDocumentDto.InvoiceDto -> TYPE_ORDER_INVOICE
                is FinancialDocumentDto.ExpenseDto -> TYPE_ORDER_EXPENSE
                is FinancialDocumentDto.BillDto -> TYPE_ORDER_BILL
            }
        }
    }

    private fun extractAmount(document: FinancialDocumentDto): Double {
        return document.amount.value.toDoubleOrNull() ?: 0.0
    }

    companion object {
        private const val TYPE_ORDER_INVOICE = 0
        private const val TYPE_ORDER_EXPENSE = 1
        private const val TYPE_ORDER_BILL = 2
    }
}
