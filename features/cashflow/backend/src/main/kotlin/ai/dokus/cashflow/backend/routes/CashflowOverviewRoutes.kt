package ai.dokus.cashflow.backend.routes

import ai.dokus.cashflow.backend.repository.BillRepository
import ai.dokus.cashflow.backend.repository.ExpenseRepository
import ai.dokus.cashflow.backend.repository.InvoiceRepository
import ai.dokus.foundation.domain.Money
import ai.dokus.foundation.domain.enums.BillStatus
import ai.dokus.foundation.domain.enums.Currency
import ai.dokus.foundation.domain.enums.InvoiceStatus
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.model.CashInSummary
import ai.dokus.foundation.domain.model.CashOutSummary
import ai.dokus.foundation.domain.model.CashflowOverview
import ai.dokus.foundation.domain.model.CashflowPeriod
import ai.dokus.foundation.ktor.security.authenticateJwt
import ai.dokus.foundation.ktor.security.dokusPrincipal
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory
import java.math.BigDecimal

/**
 * Cashflow Overview API Routes
 * Base path: /api/v1/cashflow
 *
 * All routes require JWT authentication and tenant context.
 */
fun Route.cashflowOverviewRoutes() {
    val invoiceRepository by inject<InvoiceRepository>()
    val expenseRepository by inject<ExpenseRepository>()
    val billRepository by inject<BillRepository>()
    val logger = LoggerFactory.getLogger("CashflowOverviewRoutes")

    route("/api/v1/cashflow") {
        authenticateJwt {

            // GET /api/v1/cashflow/overview - Get cashflow overview
            get("/overview") {
                val tenantId = dokusPrincipal.requireTenantId()
                val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

                // Parse period or default to current month
                val fromDate = call.parameters.fromDate
                    ?: today.minus(DatePeriod(days = today.dayOfMonth - 1))
                val toDate = call.parameters.toDate
                    ?: today

                logger.info("Getting cashflow overview for tenant: $tenantId (from=$fromDate, to=$toDate)")

                // Fetch invoices for period
                val invoicesResult = invoiceRepository.listInvoices(
                    tenantId = tenantId,
                    status = null,
                    fromDate = fromDate,
                    toDate = toDate,
                    limit = 1000,
                    offset = 0
                ).getOrElse {
                    logger.error("Failed to fetch invoices for overview", it)
                    throw DokusException.InternalError("Failed to calculate overview: ${it.message}")
                }

                // Fetch expenses for period
                val expensesResult = expenseRepository.listExpenses(
                    tenantId = tenantId,
                    category = null,
                    fromDate = fromDate,
                    toDate = toDate,
                    limit = 1000,
                    offset = 0
                ).getOrElse {
                    logger.error("Failed to fetch expenses for overview", it)
                    throw DokusException.InternalError("Failed to calculate overview: ${it.message}")
                }

                // Fetch bill statistics for period
                val billStats = billRepository.getBillStatistics(tenantId, fromDate, toDate)
                    .getOrElse {
                        logger.error("Failed to fetch bill statistics for overview", it)
                        throw DokusException.InternalError("Failed to calculate overview: ${it.message}")
                    }

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

                val overview = CashflowOverview(
                    period = CashflowPeriod(from = fromDate, to = toDate),
                    cashIn = cashIn,
                    cashOut = cashOut,
                    netCashflow = Money(netCashflow.toString()),
                    currency = Currency.Eur
                )

                call.respond(HttpStatusCode.OK, overview)
            }
        }
    }
}
