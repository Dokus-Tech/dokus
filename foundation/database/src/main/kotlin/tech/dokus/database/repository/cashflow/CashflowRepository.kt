package tech.dokus.database.repository.cashflow

import kotlinx.datetime.LocalDate
import tech.dokus.database.entity.ExpenseEntity
import tech.dokus.database.entity.InvoiceEntity
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.common.PaginatedResponse
import tech.dokus.foundation.backend.utils.runSuspendCatching

/**
 * Combined financial document for the cashflow list.
 */
sealed interface CashflowDocumentEntity {
    val date: LocalDate

    data class InvoiceItem(val entity: InvoiceEntity) : CashflowDocumentEntity {
        override val date: LocalDate get() = entity.issueDate
    }

    data class ExpenseItem(val entity: ExpenseEntity) : CashflowDocumentEntity {
        override val date: LocalDate get() = entity.date
    }
}

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
    ): Result<PaginatedResponse<CashflowDocumentEntity>> = runSuspendCatching {
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
        val combined = (
            invoicePage.items.map { CashflowDocumentEntity.InvoiceItem(it) } +
                expensePage.items.map { CashflowDocumentEntity.ExpenseItem(it) }
            ).sortedByDescending { it.date }
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
