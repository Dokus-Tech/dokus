package tech.dokus.domain.routes

import io.ktor.resources.*
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * Type-safe route definitions for Reporting API.
 * Base path: /api/v1/reports
 *
 * SECURITY: All operations are scoped to the authenticated user's tenant via JWT.
 */
@Serializable
@Resource("/api/v1/reports")
class Reports {
    /**
     * GET /api/v1/reports/profit-loss - Profit & Loss report
     */
    @Serializable
    @Resource("profit-loss")
    class ProfitLoss(
        val parent: Reports = Reports(),
        val fromDate: LocalDate? = null,
        val toDate: LocalDate? = null
    )

    /**
     * GET /api/v1/reports/vat - VAT report
     */
    @Serializable
    @Resource("vat")
    class Vat(
        val parent: Reports = Reports(),
        val fromDate: LocalDate? = null,
        val toDate: LocalDate? = null
    )

    /**
     * GET /api/v1/reports/tax-summary - Tax summary report
     */
    @Serializable
    @Resource("tax-summary")
    class TaxSummary(
        val parent: Reports = Reports(),
        val year: Int? = null
    )

    /**
     * GET /api/v1/reports/revenue - Revenue report
     */
    @Serializable
    @Resource("revenue")
    class Revenue(
        val parent: Reports = Reports(),
        val fromDate: LocalDate? = null,
        val toDate: LocalDate? = null,
        val groupBy: String? = null
    )

    /**
     * GET /api/v1/reports/expenses - Expenses report
     */
    @Serializable
    @Resource("expenses")
    class ExpensesReport(
        val parent: Reports = Reports(),
        val fromDate: LocalDate? = null,
        val toDate: LocalDate? = null,
        val groupBy: String? = null
    )
}
