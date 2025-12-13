package ai.dokus.foundation.domain.routes

import ai.dokus.foundation.domain.enums.BillStatus
import ai.dokus.foundation.domain.enums.ExpenseCategory
import io.ktor.resources.*
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * Type-safe route definitions for Bill API (Cash-Out / Supplier Invoices).
 * Base path: /api/v1/cashflow/cash-out/bills
 */
@Serializable
@Resource("/api/v1/cashflow/cash-out/bills")
class Bills(
    val status: BillStatus? = null,
    val category: ExpenseCategory? = null,
    val fromDate: LocalDate? = null,
    val toDate: LocalDate? = null,
    val limit: Int = 50,
    val offset: Int = 0
) {
    /**
     * GET /api/v1/cashflow/cash-out/bills/overdue - List overdue bills
     */
    @Serializable
    @Resource("overdue")
    class Overdue(val parent: Bills = Bills())

    /**
     * /api/v1/cashflow/cash-out/bills/{id} - Single bill operations
     */
    @Serializable
    @Resource("{id}")
    class Id(val parent: Bills = Bills(), val id: String) {

        /**
         * PATCH /api/v1/cashflow/cash-out/bills/{id}/status
         */
        @Serializable
        @Resource("status")
        class Status(val parent: Id)

        /**
         * POST /api/v1/cashflow/cash-out/bills/{id}/pay - Mark bill as paid
         */
        @Serializable
        @Resource("pay")
        class Pay(val parent: Id)
    }
}
