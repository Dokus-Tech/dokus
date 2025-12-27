package tech.dokus.domain.routes

import ai.dokus.foundation.domain.enums.ExpenseCategory
import io.ktor.resources.*
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * Type-safe route definitions for Expense API.
 * Base path: /api/v1/expenses
 *
 * SECURITY: All operations are scoped to the authenticated user's tenant via JWT.
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
     * /api/v1/expenses/{id} - Single expense operations
     * GET - Retrieve expense
     * PUT - Replace expense
     * PATCH - Partial update (including category)
     * DELETE - Delete expense
     */
    @Serializable
    @Resource("{id}")
    class Id(val parent: Expenses = Expenses(), val id: String) {

        /**
         * GET/POST /api/v1/expenses/{id}/attachments
         * GET - List receipts/attachments
         * POST - Upload receipt
         */
        @Serializable
        @Resource("attachments")
        class Attachments(val parent: Id)
    }
}
