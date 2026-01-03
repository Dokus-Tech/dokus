package tech.dokus.backend.services.cashflow

import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import tech.dokus.database.repository.cashflow.BillRepository
import tech.dokus.database.repository.cashflow.ExpenseRepository
import tech.dokus.database.repository.cashflow.InvoiceRepository
import tech.dokus.domain.Money
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.enums.InvoiceStatus
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.CashInSummary
import tech.dokus.domain.model.CashOutSummary
import tech.dokus.domain.model.CashflowOverview
import tech.dokus.domain.model.CashflowPeriod
import tech.dokus.foundation.backend.utils.loggerFor

/**
 * Service for cashflow overview calculations.
 *
 * This service aggregates data from invoices, expenses, and bills
 * to provide a comprehensive view of cash flow for a tenant.
 *
 * Cash-In: Money flowing INTO the business (outgoing invoices to clients)
 * Cash-Out: Money flowing OUT of the business (expenses + bills to pay)
 */
class CashflowOverviewService(
    private val invoiceRepository: InvoiceRepository,
    private val expenseRepository: ExpenseRepository,
    private val billRepository: BillRepository
) {
    private val logger = loggerFor()

    /**
     * Get cashflow overview for a period.
     *
     * @param tenantId The tenant ID
     * @param fromDate Start date (defaults to first day of current month)
     * @param toDate End date (defaults to today)
     */
    suspend fun getCashflowOverview(
        tenantId: TenantId,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null
    ): Result<CashflowOverview> = runCatching {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val effectiveFromDate = fromDate ?: today.minus(DatePeriod(days = today.dayOfMonth - 1))
        val effectiveToDate = toDate ?: today

        logger.info(
            "Calculating cashflow overview for tenant: $tenantId (from=$effectiveFromDate, to=$effectiveToDate)"
        )

        // Fetch invoices for period
        val invoicesResult = invoiceRepository.listInvoices(
            tenantId = tenantId,
            status = null,
            fromDate = effectiveFromDate,
            toDate = effectiveToDate,
            limit = 1000,
            offset = 0
        ).getOrThrow()

        // Fetch expenses for period
        val expensesResult = expenseRepository.listExpenses(
            tenantId = tenantId,
            category = null,
            fromDate = effectiveFromDate,
            toDate = effectiveToDate,
            limit = 1000,
            offset = 0
        ).getOrThrow()

        // Fetch bill statistics for period
        val billStats =
            billRepository.getBillStatistics(tenantId, effectiveFromDate, effectiveToDate)
                .getOrThrow()

        // Calculate Cash-In (Invoices)
        val invoices = invoicesResult.items
        val cashInTotal = invoices.sumOf { it.totalAmount.minor }
        val cashInPaid = invoices
            .filter { it.status == InvoiceStatus.Paid }
            .sumOf { it.totalAmount.minor }
        val cashInPending = invoices
            .filter {
                it.status in listOf(
                    InvoiceStatus.Draft,
                    InvoiceStatus.Sent,
                    InvoiceStatus.Viewed,
                    InvoiceStatus.PartiallyPaid
                )
            }
            .sumOf { it.totalAmount.minor }
        val cashInOverdue = invoices
            .filter { it.status == InvoiceStatus.Overdue }
            .sumOf { it.totalAmount.minor }

        val cashIn = CashInSummary(
            total = Money(cashInTotal),
            paid = Money(cashInPaid),
            pending = Money(cashInPending),
            overdue = Money(cashInOverdue),
            invoiceCount = invoices.size
        )

        // Calculate Cash-Out (Expenses + Bills)
        val expenses = expensesResult.items
        val expenseTotal = expenses.sumOf { it.amount.minor }

        val cashOutTotal = expenseTotal + billStats.total.minor
        val cashOutPaid = expenseTotal + billStats.paid.minor
        val cashOutPending = billStats.pending.minor

        val cashOut = CashOutSummary(
            total = Money(cashOutTotal),
            paid = Money(cashOutPaid),
            pending = Money(cashOutPending),
            expenseCount = expenses.size,
            billCount = billStats.count
        )

        // Calculate net cashflow
        val netCashflow = cashInPaid - cashOutPaid

        CashflowOverview(
            period = CashflowPeriod(from = effectiveFromDate, to = effectiveToDate),
            cashIn = cashIn,
            cashOut = cashOut,
            netCashflow = Money(netCashflow),
            currency = Currency.Eur
        )
    }.onFailure {
        logger.error("Failed to calculate cashflow overview for tenant: $tenantId", it)
    }
}
