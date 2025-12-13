package ai.dokus.foundation.domain.routes

import ai.dokus.foundation.domain.enums.ExpenseCategory
import io.ktor.resources.*
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * Type-safe route definitions for Expense API.
 * Base path: /api/v1/expenses
 */
@Serializable
@Resource("/api/v1/expenses")
class Expenses(
    val category: ExpenseCategory? = null,
    val fromDate: LocalDate? = null,
    val toDate: LocalDate? = null,
    val limit: Int = 50,
    val offset: Int = 0
) {
    /**
     * POST /api/v1/expenses/categorize - Auto-categorize expense
     */
    @Serializable
    @Resource("categorize")
    class Categorize(val parent: Expenses = Expenses())

    /**
     * /api/v1/expenses/{id} - Single expense operations
     */
    @Serializable
    @Resource("{id}")
    class Id(val parent: Expenses = Expenses(), val id: String) {

        /**
         * GET/POST /api/v1/expenses/{id}/attachments
         */
        @Serializable
        @Resource("attachments")
        class Attachments(val parent: Id)
    }
}
