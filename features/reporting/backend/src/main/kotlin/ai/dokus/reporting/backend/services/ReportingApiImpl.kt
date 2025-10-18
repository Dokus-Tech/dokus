package ai.dokus.reporting.backend.services

import ai.dokus.foundation.apispec.*
import ai.dokus.foundation.domain.Money
import ai.dokus.foundation.domain.TenantId
import ai.dokus.foundation.ktor.services.ExpenseService
import ai.dokus.foundation.ktor.services.InvoiceService
import ai.dokus.foundation.ktor.services.PaymentService
import kotlinx.datetime.LocalDate

class ReportingApiImpl(
    private val invoiceService: InvoiceService,
    private val expenseService: ExpenseService,
    private val paymentService: PaymentService
) : ReportingApi {

    override suspend fun getFinancialSummary(
        tenantId: TenantId,
        startDate: LocalDate?,
        endDate: LocalDate?
    ): Result<FinancialSummary> = runCatching {
        val invoices = invoiceService.listByTenant(tenantId, null, null, startDate, endDate)
        val expenses = expenseService.listByTenant(tenantId, 10000, 0)
            .filter { expense ->
                (startDate == null || expense.date >= startDate) &&
                (endDate == null || expense.date <= endDate)
            }
        val payments = paymentService.listByTenant(tenantId, 10000, 0)
            .filter { payment ->
                (startDate == null || payment.paymentDate >= startDate) &&
                (endDate == null || payment.paymentDate <= endDate)
            }

        val totalRevenue = payments.fold(Money.ZERO) { acc, p -> acc + p.amount }
        val totalExpenses = expenses.fold(Money.ZERO) { acc, e -> acc + e.amount }
        val outstandingAmount = invoices
            .filter { it.status.name in listOf("SENT", "OVERDUE", "PARTIALLY_PAID") }
            .fold(Money.ZERO) { acc, inv -> acc + (inv.total - inv.paidAmount) }

        FinancialSummary(
            tenantId = tenantId.value.toString(),
            period = DateRange(startDate, endDate),
            totalRevenue = totalRevenue,
            totalExpenses = totalExpenses,
            netProfit = totalRevenue - totalExpenses,
            invoiceCount = invoices.size,
            expenseCount = expenses.size,
            paymentCount = payments.size,
            outstandingAmount = outstandingAmount
        )
    }

    override suspend fun getInvoiceAnalytics(
        tenantId: TenantId,
        startDate: LocalDate?,
        endDate: LocalDate?
    ): Result<InvoiceAnalytics> = runCatching {
        val invoices = invoiceService.listByTenant(tenantId, null, null, startDate, endDate)

        val totalAmount = invoices.fold(Money.ZERO) { acc, inv -> acc + inv.total }
        val paidAmount = invoices.fold(Money.ZERO) { acc, inv -> acc + inv.paidAmount }
        val outstandingAmount = totalAmount - paidAmount

        val overdueInvoices = invoiceService.listOverdue(tenantId)
        val overdueAmount = overdueInvoices.fold(Money.ZERO) { acc, inv -> acc + (inv.total - inv.paidAmount) }

        val statusBreakdown = invoices.groupBy { it.status.name }
            .mapValues { it.value.size }

        val averageInvoiceValue = if (invoices.isNotEmpty()) {
            totalAmount / invoices.size
        } else {
            Money.ZERO
        }

        InvoiceAnalytics(
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
        tenantId: TenantId,
        startDate: LocalDate?,
        endDate: LocalDate?
    ): Result<ExpenseAnalytics> = runCatching {
        val expenses = expenseService.listByTenant(tenantId, 10000, 0)
            .filter { expense ->
                (startDate == null || expense.date >= startDate) &&
                (endDate == null || expense.date <= endDate)
            }

        val totalAmount = expenses.fold(Money.ZERO) { acc, e -> acc + e.amount }
        val categoryBreakdown = expenses.groupBy { it.category.name }
            .mapValues { it.value.fold(Money.ZERO) { acc, e -> acc + e.amount } }

        val averageExpenseValue = if (expenses.isNotEmpty()) {
            totalAmount / expenses.size
        } else {
            Money.ZERO
        }

        val deductibleAmount = expenses
            .filter { it.isDeductible }
            .fold(Money.ZERO) { acc, e -> acc + e.amount }

        ExpenseAnalytics(
            totalExpenses = expenses.size,
            totalAmount = totalAmount,
            categoryBreakdown = categoryBreakdown,
            averageExpenseValue = averageExpenseValue,
            deductibleAmount = deductibleAmount
        )
    }

    override suspend fun getCashFlow(
        tenantId: TenantId,
        startDate: LocalDate?,
        endDate: LocalDate?
    ): Result<CashFlowReport> = runCatching {
        val payments = paymentService.listByTenant(tenantId, 10000, 0)
            .filter { payment ->
                (startDate == null || payment.paymentDate >= startDate) &&
                (endDate == null || payment.paymentDate <= endDate)
            }

        val expenses = expenseService.listByTenant(tenantId, 10000, 0)
            .filter { expense ->
                (startDate == null || expense.date >= startDate) &&
                (endDate == null || expense.date <= endDate)
            }

        val totalInflow = payments.fold(Money.ZERO) { acc, p -> acc + p.amount }
        val totalOutflow = expenses.fold(Money.ZERO) { acc, e -> acc + e.amount }

        // Group by month (simplified - you might want more sophisticated grouping)
        val monthlyData = emptyList<MonthlyCashFlow>() // TODO: Implement monthly grouping

        CashFlowReport(
            period = DateRange(startDate, endDate),
            totalInflow = totalInflow,
            totalOutflow = totalOutflow,
            netCashFlow = totalInflow - totalOutflow,
            monthlyData = monthlyData
        )
    }

    override suspend fun getVatReport(
        tenantId: TenantId,
        startDate: LocalDate?,
        endDate: LocalDate?
    ): Result<VatReport> = runCatching {
        val invoices = invoiceService.listByTenant(tenantId, null, null, startDate, endDate)
        val expenses = expenseService.listByTenant(tenantId, 10000, 0)
            .filter { expense ->
                (startDate == null || expense.date >= startDate) &&
                (endDate == null || expense.date <= endDate)
            }

        val vatCollected = invoices.fold(Money.ZERO) { acc, inv -> acc + inv.vatAmount }
        val vatPaid = expenses.fold(Money.ZERO) { acc, e -> acc + (e.vatAmount ?: Money.ZERO) }

        VatReport(
            period = DateRange(startDate, endDate),
            vatCollected = vatCollected,
            vatPaid = vatPaid,
            vatOwed = vatCollected - vatPaid,
            salesCount = invoices.size,
            purchaseCount = expenses.size
        )
    }
}
