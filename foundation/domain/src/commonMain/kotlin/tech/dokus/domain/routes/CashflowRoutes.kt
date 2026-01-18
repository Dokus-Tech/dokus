package tech.dokus.domain.routes

import io.ktor.resources.*
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * Type-safe route definitions for Cashflow API.
 * Base path: /api/v1/cashflow
 *
 * SECURITY: All operations are scoped to the authenticated user's tenant via JWT.
 */
@Serializable
@Resource("/api/v1/cashflow")
class Cashflow {
    /**
     * GET /api/v1/cashflow/overview - Get cashflow overview with projections
     *
     * @param viewMode Required. "upcoming" uses eventDate range, "history" uses paidAt range.
     * @param fromDate Start of date range (interpreted based on viewMode)
     * @param toDate End of date range (interpreted based on viewMode)
     * @param direction Optional filter: IN, OUT
     * @param status Optional filter: comma-separated statuses (e.g., "Open,Overdue")
     */
    @Serializable
    @Resource("overview")
    class Overview(
        val parent: Cashflow = Cashflow(),
        val viewMode: String? = null,
        val fromDate: LocalDate? = null,
        val toDate: LocalDate? = null,
        val direction: String? = null,
        val status: String? = null
    )

    /**
     * GET /api/v1/cashflow/entries - List cashflow entries (projection ledger)
     *
     * Supports filtering by:
     * - View mode (viewMode): "upcoming" uses eventDate, "history" uses paidAt
     * - Date range (fromDate, toDate): interpreted based on viewMode
     * - Direction (IN/OUT)
     * - Status: comma-separated (e.g., "Open,Overdue")
     * - Source type (INVOICE/BILL/EXPENSE)
     * - Exact entry ID (entryId) for deep linking
     *
     * Sorting:
     * - viewMode=upcoming: eventDate ASC (soonest first)
     * - viewMode=history: paidAt DESC (most recent first)
     *
     * Cancelled entries are excluded by default.
     */
    @Serializable
    @Resource("entries")
    class Entries(
        val parent: Cashflow = Cashflow(),
        val viewMode: String? = null,
        val fromDate: LocalDate? = null,
        val toDate: LocalDate? = null,
        val direction: String? = null,
        val status: String? = null,
        val sourceType: String? = null,
        val entryId: String? = null,
        val limit: Int = 50,
        val offset: Int = 0
    ) {
        /**
         * GET /api/v1/cashflow/entries/{id} - Get single entry
         * POST /api/v1/cashflow/entries/{id}/payments - Record payment
         * POST /api/v1/cashflow/entries/{id}/cancel - Cancel entry
         */
        @Serializable
        @Resource("{id}")
        class Id(val parent: Entries = Entries(), val id: String) {
            @Serializable
            @Resource("payments")
            class Payments(val parent: Id)

            @Serializable
            @Resource("cancel")
            class Cancel(val parent: Id)
        }
    }
}
