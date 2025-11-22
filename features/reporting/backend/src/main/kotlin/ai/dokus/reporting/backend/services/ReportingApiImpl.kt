package ai.dokus.reporting.backend.services

import ai.dokus.foundation.domain.Money
import ai.dokus.foundation.domain.model.CashFlowReport
import ai.dokus.foundation.domain.model.DateRange
import ai.dokus.foundation.domain.model.ExpenseAnalytics
import ai.dokus.foundation.domain.model.FinancialSummary
import ai.dokus.foundation.domain.model.InvoiceAnalytics
import ai.dokus.foundation.domain.model.MonthlyCashFlow
import ai.dokus.foundation.domain.model.VatReport
import ai.dokus.foundation.domain.rpc.ReportingApi
import ai.dokus.foundation.ktor.security.requireAuthenticatedOrganizationId
import ai.dokus.foundation.ktor.services.ExpenseService
import ai.dokus.foundation.ktor.services.InvoiceService
import ai.dokus.foundation.ktor.services.PaymentService
import kotlinx.datetime.LocalDate
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class ReportingApiImpl(
    private val invoiceService: InvoiceService,
    private val expenseService: ExpenseService,
    private val paymentService: PaymentService
) : ReportingApi {

    override suspend fun getFinancialSummary(
        startDate: LocalDate?,
        endDate: LocalDate?
    ): FinancialSummary {
        val organizationId = requireAuthenticatedOrganizationId()
        val invoices = invoiceService.listByTenant(
            organizationId = organizationId,
            status = null,
            clientId = null,
            fromDate = startDate,
            toDate = endDate,
            limit = null,
            offset = null
        )
        val expenses = expenseService.listByTenant(
            organizationId = organizationId,
            category = null,
            fromDate = startDate,
            toDate = endDate,
            merchant = null,
            limit = null,
            offset = null
        )
        val payments = paymentService.listByTenant(
            organizationId = organizationId,
            fromDate = startDate,
            toDate = endDate,
            paymentMethod = null,
            limit = null,
            offset = null
        )

        val totalRevenue = Money(
            payments.sumOf { java.math.BigDecimal(it.amount.value) }.toString()
        )
        val totalExpenses = Money(
            expenses.sumOf { java.math.BigDecimal(it.amount.value) }.toString()
        )
        val outstandingAmount = Money(
            invoices
                .filter { it.status.name in listOf("SENT", "OVERDUE", "PARTIALLY_PAID") }
                .sumOf {
                    java.math.BigDecimal(it.totalAmount.value) - java.math.BigDecimal(it.paidAmount.value)
                }.toString()
        )

        return FinancialSummary(
            organizationId = organizationId.value.toString(),
            period = DateRange(startDate, endDate),
            totalRevenue = totalRevenue,
            totalExpenses = totalExpenses,
            netProfit = Money(
                (java.math.BigDecimal(totalRevenue.value) - java.math.BigDecimal(totalExpenses.value)).toString()
            ),
            invoiceCount = invoices.size,
            expenseCount = expenses.size,
            paymentCount = payments.size,
            outstandingAmount = outstandingAmount
        )
    }

    override suspend fun getInvoiceAnalytics(
        startDate: LocalDate?,
        endDate: LocalDate?
    ): InvoiceAnalytics {
        val organizationId = requireAuthenticatedOrganizationId()
        val invoices = invoiceService.listByTenant(
            organizationId = organizationId,
            status = null,
            clientId = null,
            fromDate = startDate,
            toDate = endDate,
            limit = null,
            offset = null
        )

        val totalAmount = Money(
            invoices.sumOf { java.math.BigDecimal(it.totalAmount.value) }.toString()
        )
        val paidAmount = Money(
            invoices.sumOf { java.math.BigDecimal(it.paidAmount.value) }.toString()
        )
        val outstandingAmount = Money(
            (java.math.BigDecimal(totalAmount.value) - java.math.BigDecimal(paidAmount.value)).toString()
        )

        val overdueInvoices = invoiceService.listOverdue(organizationId)
        val overdueAmount = Money(
            overdueInvoices.sumOf {
                java.math.BigDecimal(it.totalAmount.value) - java.math.BigDecimal(it.paidAmount.value)
            }.toString()
        )

        val statusBreakdown = invoices.groupBy { it.status.name }
            .mapValues { it.value.size }

        val averageInvoiceValue = if (invoices.isNotEmpty()) {
            Money(
                (java.math.BigDecimal(totalAmount.value) / java.math.BigDecimal(invoices.size.toString())).toString()
            )
        } else {
            Money.ZERO
        }

        return InvoiceAnalytics(
            totalInvoices = invoices.size,
            totalAmount = totalAmount,
            paidAmount = paidAmount,
            outstandingAmount = outstandingAmount,
            overdueAmount = overdueAmount,
            statusBreakdown = statusBreakdown,
            averageInvoiceValue = averageInvoiceValue
        )
    }

    override suspend fun getExpenseAnalytics(
        startDate: LocalDate?,
        endDate: LocalDate?
    ): ExpenseAnalytics {
        val organizationId = requireAuthenticatedOrganizationId()
        val expenses = expenseService.listByTenant(
            organizationId = organizationId,
            category = null,
            fromDate = startDate,
            toDate = endDate,
            merchant = null,
            limit = null,
            offset = null
        )

        val totalAmount = Money(
            expenses.sumOf { java.math.BigDecimal(it.amount.value) }.toString()
        )
        val categoryBreakdown = expenses.groupBy { it.category.name }
            .mapValues { entry ->
                Money(entry.value.sumOf { java.math.BigDecimal(it.amount.value) }.toString())
            }

        val averageExpenseValue = if (expenses.isNotEmpty()) {
            Money(
                (java.math.BigDecimal(totalAmount.value) / java.math.BigDecimal(expenses.size.toString())).toString()
            )
        } else {
            Money.ZERO
        }

        val deductibleAmount = Money(
            expenses
                .filter { it.isDeductible }
                .sumOf { java.math.BigDecimal(it.amount.value) }
                .toString()
        )

        return ExpenseAnalytics(
            totalExpenses = expenses.size,
            totalAmount = totalAmount,
            categoryBreakdown = categoryBreakdown,
            averageExpenseValue = averageExpenseValue,
            deductibleAmount = deductibleAmount
        )
    }

    override suspend fun getCashFlow(
        startDate: LocalDate?,
        endDate: LocalDate?
    ): CashFlowReport {
        val organizationId = requireAuthenticatedOrganizationId()
        val payments = paymentService.listByTenant(
            organizationId = organizationId,
            fromDate = startDate,
            toDate = endDate,
            paymentMethod = null,
            limit = null,
            offset = null
        )

        val expenses = expenseService.listByTenant(
            organizationId = organizationId,
            category = null,
            fromDate = startDate,
            toDate = endDate,
            merchant = null,
            limit = null,
            offset = null
        )

        val totalInflow = Money(
            payments.sumOf { java.math.BigDecimal(it.amount.value) }.toString()
        )
        val totalOutflow = Money(
            expenses.sumOf { java.math.BigDecimal(it.amount.value) }.toString()
        )

        // Group by month and calculate monthly cash flow
        val monthlyData = buildMonthlyData(payments, expenses)

        return CashFlowReport(
            period = DateRange(startDate, endDate),
            totalInflow = totalInflow,
            totalOutflow = totalOutflow,
            netCashFlow = Money(
                (java.math.BigDecimal(totalInflow.value) - java.math.BigDecimal(totalOutflow.value)).toString()
            ),
            monthlyData = monthlyData
        )
    }

    override suspend fun getVatReport(
        startDate: LocalDate?,
        endDate: LocalDate?
    ): VatReport {
        val organizationId = requireAuthenticatedOrganizationId()
        val invoices = invoiceService.listByTenant(
            organizationId = organizationId,
            status = null,
            clientId = null,
            fromDate = startDate,
            toDate = endDate,
            limit = null,
            offset = null
        )
        val expenses = expenseService.listByTenant(
            organizationId = organizationId,
            category = null,
            fromDate = startDate,
            toDate = endDate,
            merchant = null,
            limit = null,
            offset = null
        )

        val vatCollected = Money(
            invoices.sumOf { java.math.BigDecimal(it.vatAmount.value) }.toString()
        )
        val vatPaid = Money(
            expenses.sumOf { java.math.BigDecimal(it.vatAmount?.value ?: "0") }.toString()
        )

        return VatReport(
            period = DateRange(startDate, endDate),
            vatCollected = vatCollected,
            vatPaid = vatPaid,
            vatOwed = Money(
                (java.math.BigDecimal(vatCollected.value) - java.math.BigDecimal(vatPaid.value)).toString()
            ),
            salesCount = invoices.size,
            purchaseCount = expenses.size
        )
    }

    private fun buildMonthlyData(
        payments: List<ai.dokus.foundation.domain.model.Payment>,
        expenses: List<ai.dokus.foundation.domain.model.Expense>
    ): List<MonthlyCashFlow> {
        // Group payments and expenses by month
        @Suppress("DEPRECATION")
        val paymentsByMonth = payments.groupBy { "${it.paymentDate.year}-${it.paymentDate.monthNumber.toString().padStart(2, '0')}" }
        @Suppress("DEPRECATION")
        val expensesByMonth = expenses.groupBy { "${it.date.year}-${it.date.monthNumber.toString().padStart(2, '0')}" }

        // Get all unique months
        val allMonths = (paymentsByMonth.keys + expensesByMonth.keys).distinct().sorted()

        return allMonths.map { month ->
            val monthPayments = paymentsByMonth[month] ?: emptyList()
            val monthExpenses = expensesByMonth[month] ?: emptyList()

            val inflow = Money(
                monthPayments.sumOf { java.math.BigDecimal(it.amount.value) }.toString()
            )
            val outflow = Money(
                monthExpenses.sumOf { java.math.BigDecimal(it.amount.value) }.toString()
            )

            MonthlyCashFlow(
                month = month,
                inflow = inflow,
                outflow = outflow,
                net = Money(
                    (java.math.BigDecimal(inflow.value) - java.math.BigDecimal(outflow.value)).toString()
                )
            )
        }
    }
}
