package ai.dokus.foundation.domain.rpc

import ai.dokus.foundation.domain.Money
import ai.dokus.foundation.domain.TenantId
import kotlinx.datetime.LocalDate
import kotlinx.rpc.annotations.Rpc
import kotlinx.serialization.Serializable

@Rpc
interface ReportingApi {

    /**
     * Get financial summary report for a tenant
     * Aggregates invoice, expense, and payment data
     */
    suspend fun getFinancialSummary(
        tenantId: TenantId,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): Result<FinancialSummary>

    /**
     * Get invoice analytics for dashboard
     * Shows invoice trends, status breakdown, etc.
     */
    suspend fun getInvoiceAnalytics(
        tenantId: TenantId,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): Result<InvoiceAnalytics>

    /**
     * Get expense analytics
     * Shows spending by category, trends, etc.
     */
    suspend fun getExpenseAnalytics(
        tenantId: TenantId,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): Result<ExpenseAnalytics>

    /**
     * Get cash flow report
     * Shows money in vs money out over time
     */
    suspend fun getCashFlow(
        tenantId: TenantId,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): Result<CashFlowReport>

    /**
     * Get VAT report for tax filing
     * Calculates VAT collected and VAT paid
     */
    suspend fun getVatReport(
        tenantId: TenantId,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): Result<VatReport>
}

@Serializable
data class FinancialSummary(
    val tenantId: String,
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
