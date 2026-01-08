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
     */
    @Serializable
    @Resource("overview")
    class Overview(
        val parent: Cashflow = Cashflow(),
        val fromDate: LocalDate? = null,
        val toDate: LocalDate? = null
    )

    /**
     * GET /api/v1/cashflow/documents - List combined financial documents
     *
     * @deprecated This endpoint is being replaced. Use /api/v1/documents for the inbox
     * and /api/v1/cashflow/entries for the projection ledger.
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

    /**
     * GET /api/v1/cashflow/entries - List cashflow entries (projection ledger)
     *
     * Supports filtering by:
     * - Date range (fromDate, toDate)
     * - Direction (IN/OUT)
     * - Status (OPEN/PAID/OVERDUE/CANCELLED)
     * - Source type (INVOICE/BILL/EXPENSE)
     * - Exact entry ID (entryId) for deep linking
     */
    @Serializable
    @Resource("entries")
    class Entries(
        val parent: Cashflow = Cashflow(),
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
