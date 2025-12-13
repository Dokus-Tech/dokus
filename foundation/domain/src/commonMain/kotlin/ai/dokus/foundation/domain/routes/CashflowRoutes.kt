package ai.dokus.foundation.domain.routes

import io.ktor.resources.*
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * Type-safe route definitions for Cashflow Overview API.
 * Base path: /api/v1/cashflow
 *
 * SECURITY: All operations are scoped to the authenticated user's tenant via JWT.
 */
@Serializable
@Resource("/api/v1/cashflow")
class Cashflow {
    /**
     * GET /api/v1/cashflow/overview - Get cashflow overview with projections
     */
    @Serializable
    @Resource("overview")
    class Overview(
        val parent: Cashflow = Cashflow(),
        val fromDate: LocalDate? = null,
        val toDate: LocalDate? = null
    )

    /**
     * GET /api/v1/cashflow/documents - List cashflow documents (invoices, bills, expenses)
     */
    @Serializable
    @Resource("documents")
    class CashflowDocuments(
        val parent: Cashflow = Cashflow(),
        val fromDate: LocalDate? = null,
        val toDate: LocalDate? = null,
        val limit: Int = 50,
        val offset: Int = 0
    )
}
