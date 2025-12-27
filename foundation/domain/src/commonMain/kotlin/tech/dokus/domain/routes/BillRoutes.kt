package tech.dokus.domain.routes

import tech.dokus.domain.enums.BillStatus
import tech.dokus.domain.enums.ExpenseCategory
import io.ktor.resources.*
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * Type-safe route definitions for Bill API (Cash-Out / Supplier Invoices).
 * Base path: /api/v1/bills
 *
 * SECURITY: All operations are scoped to the authenticated user's tenant via JWT.
 */
@Serializable
@Resource("/api/v1/bills")
class Bills(
    val status: BillStatus? = null,
    val category: ExpenseCategory? = null,
    val fromDate: LocalDate? = null,
    val toDate: LocalDate? = null,
    val limit: Int = 50,
    val offset: Int = 0
) {
    /**
     * GET /api/v1/bills/overdue - List overdue bills
     */
    @Serializable
    @Resource("overdue")
    class Overdue(val parent: Bills = Bills())

    /**
     * /api/v1/bills/{id} - Single bill operations
     * GET - Retrieve bill
     * PUT - Replace bill
     * PATCH - Partial update
     * DELETE - Delete bill
     */
    @Serializable
    @Resource("{id}")
    class Id(val parent: Bills = Bills(), val id: String) {

        /**
         * GET/PATCH /api/v1/bills/{id}/status
         * GET - Get current status
         * PATCH - Update status
         */
        @Serializable
        @Resource("status")
        class Status(val parent: Id)

        /**
         * GET/POST /api/v1/bills/{id}/payments
         * GET - List payments made for this bill
         * POST - Record a payment (marks bill as paid when fully paid)
         */
        @Serializable
        @Resource("payments")
        class Payments(val parent: Id)
    }
}
