package ai.dokus.foundation.domain.model

import ai.dokus.foundation.domain.Money
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable


@Serializable
data class FinancialSummary(
    val organizationId: String,
    val period: DateRange,
    val totalRevenue: Money,
    val totalExpenses: Money,
    val netProfit: Money,
    val invoiceCount: Int,
    val expenseCount: Int,
    val paymentCount: Int,
    val outstandingAmount: Money
)

@Serializable
data class InvoiceAnalytics(
    val totalInvoices: Int,
    val totalAmount: Money,
    val paidAmount: Money,
    val outstandingAmount: Money,
    val overdueAmount: Money,
    val statusBreakdown: Map<String, Int>,
    val averageInvoiceValue: Money
)

@Serializable
data class ExpenseAnalytics(
    val totalExpenses: Int,
    val totalAmount: Money,
    val categoryBreakdown: Map<String, Money>,
    val averageExpenseValue: Money,
    val deductibleAmount: Money
)

@Serializable
data class CashFlowReport(
    val period: DateRange,
    val totalInflow: Money,
    val totalOutflow: Money,
    val netCashFlow: Money,
    val monthlyData: List<MonthlyCashFlow>
)

@Serializable
data class MonthlyCashFlow(
    val month: String,
    val inflow: Money,
    val outflow: Money,
    val net: Money
)

@Serializable
data class VatReport(
    val period: DateRange,
    val vatCollected: Money,
    val vatPaid: Money,
    val vatOwed: Money,
    val salesCount: Int,
    val purchaseCount: Int
)

@Serializable
data class DateRange(
    val start: LocalDate?,
    val end: LocalDate?
)
