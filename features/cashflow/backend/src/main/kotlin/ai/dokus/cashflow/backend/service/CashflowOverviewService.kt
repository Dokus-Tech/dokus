package ai.dokus.cashflow.backend.service

import ai.dokus.cashflow.backend.repository.BillRepository
import ai.dokus.cashflow.backend.repository.ExpenseRepository
import ai.dokus.cashflow.backend.repository.InvoiceRepository
import ai.dokus.foundation.domain.Money
import ai.dokus.foundation.domain.enums.Currency
import ai.dokus.foundation.domain.enums.InvoiceStatus
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.model.CashInSummary
import ai.dokus.foundation.domain.model.CashOutSummary
import ai.dokus.foundation.domain.model.CashflowOverview
import ai.dokus.foundation.domain.model.CashflowPeriod
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import org.slf4j.LoggerFactory
import java.math.BigDecimal

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
    private val logger = LoggerFactory.getLogger(CashflowOverviewService::class.java)

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

        logger.info("Calculating cashflow overview for tenant: $tenantId (from=$effectiveFromDate, to=$effectiveToDate)")

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
        val billStats = billRepository.getBillStatistics(tenantId, effectiveFromDate, effectiveToDate)
            .getOrThrow()

        // Calculate Cash-In (Invoices)
        val invoices = invoicesResult.items
        val cashInTotal = invoices.sumOf { BigDecimal(it.totalAmount.value) }
        val cashInPaid = invoices
            .filter { it.status == InvoiceStatus.Paid }
            .sumOf { BigDecimal(it.totalAmount.value) }
        val cashInPending = invoices
            .filter { it.status in listOf(InvoiceStatus.Draft, InvoiceStatus.Sent, InvoiceStatus.Viewed, InvoiceStatus.PartiallyPaid) }
            .sumOf { BigDecimal(it.totalAmount.value) }
        val cashInOverdue = invoices
            .filter { it.status == InvoiceStatus.Overdue }
            .sumOf { BigDecimal(it.totalAmount.value) }

        val cashIn = CashInSummary(
            total = Money(cashInTotal.toString()),
            paid = Money(cashInPaid.toString()),
            pending = Money(cashInPending.toString()),
            overdue = Money(cashInOverdue.toString()),
            invoiceCount = invoices.size
        )

        // Calculate Cash-Out (Expenses + Bills)
        val expenses = expensesResult.items
        val expenseTotal = expenses.sumOf { BigDecimal(it.amount.value) }

        val cashOutTotal = expenseTotal + BigDecimal(billStats.total.value)
        val cashOutPaid = expenseTotal + BigDecimal(billStats.paid.value)
        val cashOutPending = BigDecimal(billStats.pending.value)

        val cashOut = CashOutSummary(
            total = Money(cashOutTotal.toString()),
            paid = Money(cashOutPaid.toString()),
            pending = Money(cashOutPending.toString()),
            expenseCount = expenses.size,
            billCount = billStats.count
        )

        // Calculate net cashflow
        val netCashflow = cashInPaid - cashOutPaid

        CashflowOverview(
            period = CashflowPeriod(from = effectiveFromDate, to = effectiveToDate),
            cashIn = cashIn,
            cashOut = cashOut,
            netCashflow = Money(netCashflow.toString()),
            currency = Currency.Eur
        )
    }.onFailure {
        logger.error("Failed to calculate cashflow overview for tenant: $tenantId", it)
    }
}
