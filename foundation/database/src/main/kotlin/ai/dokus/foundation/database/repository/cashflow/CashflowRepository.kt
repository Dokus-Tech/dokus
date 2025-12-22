package ai.dokus.foundation.database.repository.cashflow

import ai.dokus.foundation.database.tables.cashflow.BillsTable
import ai.dokus.foundation.database.tables.cashflow.ExpensesTable
import ai.dokus.foundation.database.tables.cashflow.InvoicesTable
import ai.dokus.foundation.domain.ids.ContactId
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.model.ContactActivitySummary
import ai.dokus.foundation.domain.model.FinancialDocumentDto
import ai.dokus.foundation.domain.model.PaginatedResponse
import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.max
import org.jetbrains.exposed.v1.core.sum
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import java.math.BigDecimal
import java.util.UUID

class CashflowRepository(
    private val invoiceRepository: InvoiceRepository,
    private val billRepository: BillRepository,
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

    /**
     * Get activity summary for a specific contact.
     * Returns counts and totals of invoices, bills, and expenses linked to this contact.
     *
     * CRITICAL: MUST filter by tenantId for multi-tenant isolation.
     */
    suspend fun getContactActivitySummary(
        contactId: ContactId,
        tenantId: TenantId
    ): Result<ContactActivitySummary> = runCatching {
        newSuspendedTransaction {
            val contactUuid = UUID.fromString(contactId.toString())
            val tenantUuid = UUID.fromString(tenantId.toString())

            // Get invoice stats
            val invoiceStats = InvoicesTable
                .select(
                    InvoicesTable.id.count(),
                    InvoicesTable.totalAmount.sum(),
                    InvoicesTable.createdAt.max()
                )
                .where {
                    (InvoicesTable.tenantId eq tenantUuid) and
                            (InvoicesTable.contactId eq contactUuid)
                }
                .singleOrNull()

            val invoiceCount = invoiceStats?.get(InvoicesTable.id.count()) ?: 0L
            val invoiceTotal = invoiceStats?.get(InvoicesTable.totalAmount.sum()) ?: BigDecimal.ZERO
            val invoiceLastDate = invoiceStats?.get(InvoicesTable.createdAt.max())

            // Get bill stats
            val billStats = BillsTable
                .select(
                    BillsTable.id.count(),
                    BillsTable.amount.sum(),
                    BillsTable.createdAt.max()
                )
                .where {
                    (BillsTable.tenantId eq tenantUuid) and
                            (BillsTable.contactId eq contactUuid)
                }
                .singleOrNull()

            val billCount = billStats?.get(BillsTable.id.count()) ?: 0L
            val billTotal = billStats?.get(BillsTable.amount.sum()) ?: BigDecimal.ZERO
            val billLastDate = billStats?.get(BillsTable.createdAt.max())

            // Get expense stats
            val expenseStats = ExpensesTable
                .select(
                    ExpensesTable.id.count(),
                    ExpensesTable.amount.sum(),
                    ExpensesTable.createdAt.max()
                )
                .where {
                    (ExpensesTable.tenantId eq tenantUuid) and
                            (ExpensesTable.contactId eq contactUuid)
                }
                .singleOrNull()

            val expenseCount = expenseStats?.get(ExpensesTable.id.count()) ?: 0L
            val expenseTotal = expenseStats?.get(ExpensesTable.amount.sum()) ?: BigDecimal.ZERO
            val expenseLastDate = expenseStats?.get(ExpensesTable.createdAt.max())

            // Find the most recent activity date
            val lastActivityDate = listOfNotNull(invoiceLastDate, billLastDate, expenseLastDate)
                .maxOrNull()

            // TODO: Count pending approval items (documents with this contact as suggested)
            val pendingApprovalCount = 0L

            ContactActivitySummary(
                contactId = contactId,
                invoiceCount = invoiceCount,
                invoiceTotal = invoiceTotal.toPlainString(),
                billCount = billCount,
                billTotal = billTotal.toPlainString(),
                expenseCount = expenseCount,
                expenseTotal = expenseTotal.toPlainString(),
                lastActivityDate = lastActivityDate,
                pendingApprovalCount = pendingApprovalCount
            )
        }
    }
}
