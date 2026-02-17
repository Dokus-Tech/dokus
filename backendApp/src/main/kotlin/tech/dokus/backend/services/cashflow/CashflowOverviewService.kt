package tech.dokus.backend.services.cashflow

import kotlin.time.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import tech.dokus.database.repository.cashflow.CashflowEntriesRepository
import tech.dokus.database.repository.cashflow.ExpenseRepository
import tech.dokus.database.repository.cashflow.InvoiceRepository
import tech.dokus.domain.Money
import tech.dokus.domain.enums.CashflowDirection
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.enums.CashflowViewMode
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.enums.DocumentDirection
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
 * This service aggregates data from invoices and expenses
 * to provide a comprehensive view of cash flow for a tenant.
 *
 * Cash-In: Money flowing INTO the business (outgoing invoices to clients)
 * Cash-Out: Money flowing OUT of the business (expenses + inbound invoices)
 */
class CashflowOverviewService(
    private val invoiceRepository: InvoiceRepository,
    private val expenseRepository: ExpenseRepository,
    private val cashflowEntriesRepository: CashflowEntriesRepository
) {
    private val logger = loggerFor()

    /**
     * Get cashflow overview for a period.
     *
     * @param tenantId The tenant ID
     * @param viewMode Determines date field filtering:
     *                 - Upcoming: filter by eventDate
     *                 - History: filter by paidAt
     * @param fromDate Start date (defaults to first day of current month)
     * @param toDate End date (defaults to today)
     * @param direction Optional filter: IN or OUT
     * @param statuses Multi-status filter (e.g., [Open, Overdue])
     */
    @Suppress("LongParameterList")
    suspend fun getCashflowOverview(
        tenantId: TenantId,
        viewMode: CashflowViewMode? = null,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null,
        direction: CashflowDirection? = null,
        statuses: List<CashflowEntryStatus>? = null
    ): Result<CashflowOverview> = runCatching {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val effectiveFromDate = fromDate ?: today.minus(DatePeriod(days = today.dayOfMonth - 1))
        val effectiveToDate = toDate ?: today

        logger.info(
            "Calculating cashflow overview for tenant: $tenantId " +
                "(viewMode=$viewMode, from=$effectiveFromDate, to=$effectiveToDate, " +
                "direction=$direction, statuses=$statuses)"
        )

        // If viewMode is specified, use cashflow entries for summary calculation
        if (viewMode != null) {
            return@runCatching calculateFromCashflowEntries(
                tenantId = tenantId,
                viewMode = viewMode,
                fromDate = effectiveFromDate,
                toDate = effectiveToDate,
                direction = direction,
                statuses = statuses
            )
        }

        // Legacy path: aggregate from invoices and expenses directly
        // (used when viewMode is not specified for backward compatibility)

        // Fetch outbound invoices for period (cash-in)
        val invoicesResult = invoiceRepository.listInvoices(
            tenantId = tenantId,
            status = null,
            direction = DocumentDirection.Outbound,
            fromDate = effectiveFromDate,
            toDate = effectiveToDate,
            limit = 1000,
            offset = 0
        ).getOrThrow()

        // Fetch inbound invoices for period (cash-out)
        val inboundInvoicesResult = invoiceRepository.listInvoices(
            tenantId = tenantId,
            status = null,
            direction = DocumentDirection.Inbound,
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

        // Calculate Cash-In (outbound invoices)
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

        // Calculate Cash-Out (Expenses + inbound invoices)
        val expenses = expensesResult.items
        val expenseTotal = expenses.sumOf { it.amount.minor }
        val inboundInvoices = inboundInvoicesResult.items
        val inboundInvoiceTotal = inboundInvoices.sumOf { it.totalAmount.minor }
        val inboundInvoicePaid = inboundInvoices
            .filter { it.status == InvoiceStatus.Paid }
            .sumOf { it.totalAmount.minor }
        val inboundInvoicePending = inboundInvoices
            .filter {
                it.status in listOf(
                    InvoiceStatus.Draft,
                    InvoiceStatus.Sent,
                    InvoiceStatus.Viewed,
                    InvoiceStatus.PartiallyPaid,
                    InvoiceStatus.Overdue
                )
            }
            .sumOf { it.totalAmount.minor }

        val cashOutTotal = expenseTotal + inboundInvoiceTotal
        val cashOutPaid = expenseTotal + inboundInvoicePaid
        val cashOutPending = inboundInvoicePending

        val cashOut = CashOutSummary(
            total = Money(cashOutTotal),
            paid = Money(cashOutPaid),
            pending = Money(cashOutPending),
            expenseCount = expenses.size,
            inboundInvoiceCount = inboundInvoices.size
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

    /**
     * Calculate overview from cashflow entries table.
     * Used when viewMode is specified (modern path).
     */
    @Suppress("LongParameterList")
    private suspend fun calculateFromCashflowEntries(
        tenantId: TenantId,
        viewMode: CashflowViewMode,
        fromDate: LocalDate,
        toDate: LocalDate,
        direction: CashflowDirection?,
        statuses: List<CashflowEntryStatus>?
    ): CashflowOverview {
        // Fetch entries matching the filters
        val entries = cashflowEntriesRepository.listEntries(
            tenantId = tenantId,
            viewMode = viewMode,
            fromDate = fromDate,
            toDate = toDate,
            direction = direction,
            statuses = statuses
        ).getOrThrow()

        // Separate by direction
        val cashInEntries = if (direction == null || direction == CashflowDirection.In) {
            entries.filter { it.direction == CashflowDirection.In }
        } else {
            emptyList()
        }

        val cashOutEntries = if (direction == null || direction == CashflowDirection.Out) {
            entries.filter { it.direction == CashflowDirection.Out }
        } else {
            emptyList()
        }

        // Calculate Cash-In
        val cashInTotal = cashInEntries.sumOf { it.amountGross.minor }
        val cashInPaid = cashInEntries
            .filter { it.status == CashflowEntryStatus.Paid }
            .sumOf { it.amountGross.minor }
        val cashInPending = cashInEntries
            .filter { it.status == CashflowEntryStatus.Open }
            .sumOf { it.amountGross.minor }
        val cashInOverdue = cashInEntries
            .filter { it.status == CashflowEntryStatus.Overdue }
            .sumOf { it.amountGross.minor }

        val cashIn = CashInSummary(
            total = Money(cashInTotal),
            paid = Money(cashInPaid),
            pending = Money(cashInPending),
            overdue = Money(cashInOverdue),
            invoiceCount = cashInEntries.size
        )

        // Calculate Cash-Out
        val cashOutTotal = cashOutEntries.sumOf { it.amountGross.minor }
        val cashOutPaid = cashOutEntries
            .filter { it.status == CashflowEntryStatus.Paid }
            .sumOf { it.amountGross.minor }
        val cashOutPending = cashOutEntries
            .filter { it.status in listOf(CashflowEntryStatus.Open, CashflowEntryStatus.Overdue) }
            .sumOf { it.amountGross.minor }

        val cashOut = CashOutSummary(
            total = Money(cashOutTotal),
            paid = Money(cashOutPaid),
            pending = Money(cashOutPending),
            expenseCount = cashOutEntries.count { it.sourceType == tech.dokus.domain.enums.CashflowSourceType.Expense },
            inboundInvoiceCount = cashOutEntries.count { it.sourceType == tech.dokus.domain.enums.CashflowSourceType.Invoice }
        )

        // Net cashflow = Cash-In total - Cash-Out total
        val netCashflow = cashInTotal - cashOutTotal

        return CashflowOverview(
            period = CashflowPeriod(from = fromDate, to = toDate),
            cashIn = cashIn,
            cashOut = cashOut,
            netCashflow = Money(netCashflow),
            currency = Currency.Eur
        )
    }
}
