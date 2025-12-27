package ai.dokus.foundation.database.repository.cashflow

import ai.dokus.foundation.domain.ids.TenantId
import tech.dokus.domain.model.FinancialDocumentDto
import ai.dokus.foundation.domain.model.common.PaginatedResponse
import kotlinx.datetime.LocalDate

class CashflowRepository(
    private val invoiceRepository: InvoiceRepository,
    private val expenseRepository: ExpenseRepository
) {

    suspend fun listDocuments(
        tenantId: TenantId,
        fromDate: LocalDate?,
        toDate: LocalDate?,
        limit: Int,
        offset: Int
    ): Result<PaginatedResponse<FinancialDocumentDto>> = runCatching {
        val fetchSize = limit + offset

        val invoicePage = invoiceRepository.listInvoices(
            tenantId = tenantId,
            status = null,
            fromDate = fromDate,
            toDate = toDate,
            limit = fetchSize,
            offset = 0
        ).getOrThrow()

        val expensePage = expenseRepository.listExpenses(
            tenantId = tenantId,
            category = null,
            fromDate = fromDate,
            toDate = toDate,
            limit = fetchSize,
            offset = 0
        ).getOrThrow()

        val total = invoicePage.total + expensePage.total
        val combined = (invoicePage.items + expensePage.items)
            .sortedByDescending { it.date }
        val items = combined
            .drop(offset)
            .take(limit)

        PaginatedResponse(
            items = items,
            total = total,
            limit = limit,
            offset = offset,
            hasMore = offset + items.size < total
        )
    }
}
