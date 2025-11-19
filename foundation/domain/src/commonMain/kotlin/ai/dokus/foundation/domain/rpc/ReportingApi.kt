package ai.dokus.foundation.domain.rpc

import ai.dokus.foundation.domain.model.CashFlowReport
import ai.dokus.foundation.domain.model.ExpenseAnalytics
import ai.dokus.foundation.domain.model.FinancialSummary
import ai.dokus.foundation.domain.model.InvoiceAnalytics
import ai.dokus.foundation.domain.model.VatReport
import kotlinx.datetime.LocalDate
import kotlinx.rpc.annotations.Rpc

@Rpc
interface ReportingApi {

    /**
     * Get financial summary report for a tenant
     * Aggregates invoice, expense, and payment data
     */
    suspend fun getFinancialSummary(
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): FinancialSummary

    /**
     * Get invoice analytics for dashboard
     * Shows invoice trends, status breakdown, etc.
     */
    suspend fun getInvoiceAnalytics(
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): InvoiceAnalytics

    /**
     * Get expense analytics
     * Shows spending by category, trends, etc.
     */
    suspend fun getExpenseAnalytics(
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): ExpenseAnalytics

    /**
     * Get cash flow report
     * Shows money in vs money out over time
     */
    suspend fun getCashFlow(
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): CashFlowReport

    /**
     * Get VAT report for tax filing
     * Calculates VAT collected and VAT paid
     */
    suspend fun getVatReport(
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): VatReport
}