package ai.dokus.cashflow.backend.routes

import ai.dokus.cashflow.backend.service.ExpenseService
import ai.dokus.foundation.domain.enums.ExpenseCategory
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.model.CreateExpenseRequest
import ai.dokus.foundation.ktor.security.authenticateJwt
import ai.dokus.foundation.ktor.security.dokusPrincipal
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

/**
 * Expense API Routes
 * Base path: /api/v1/expenses
 *
 * All routes require JWT authentication and tenant context.
 */
fun Route.expenseRoutes() {
    val expenseService by inject<ExpenseService>()

    route("/api/v1/expenses") {
        authenticateJwt {

            // POST /api/v1/expenses - Create expense
            post {
                val tenantId = dokusPrincipal.requireTenantId()
                val request = call.receive<CreateExpenseRequest>()

                val expense = expenseService.createExpense(tenantId, request)
                    .getOrElse { throw DokusException.InternalError("Failed to create expense: ${it.message}") }

                call.respond(HttpStatusCode.Created, expense)
            }

            // GET /api/v1/expenses/{id} - Get expense by ID
            get("/{expenseId}") {
                val tenantId = dokusPrincipal.requireTenantId()
                val expenseId = call.parameters.expenseId
                    ?: throw DokusException.BadRequest()

                val expense = expenseService.getExpense(expenseId, tenantId)
                    .getOrElse { throw DokusException.InternalError("Failed to fetch expense: ${it.message}") }
                    ?: throw DokusException.NotFound("Expense not found")

                call.respond(HttpStatusCode.OK, expense)
            }

            // GET /api/v1/expenses - List expenses with query params
            get {
                val tenantId = dokusPrincipal.requireTenantId()
                val category = call.parameters.expenseCategory
                val fromDate = call.parameters.fromDate
                val toDate = call.parameters.toDate
                val limit = call.parameters.limit
                val offset = call.parameters.offset

                if (limit < 1 || limit > 200) {
                    throw DokusException.BadRequest("Limit must be between 1 and 200")
                }
                if (offset < 0) {
                    throw DokusException.BadRequest("Offset must be non-negative")
                }

                val expenses = expenseService.listExpenses(
                    tenantId = tenantId,
                    category = category,
                    fromDate = fromDate,
                    toDate = toDate,
                    limit = limit,
                    offset = offset
                ).getOrElse { throw DokusException.InternalError("Failed to list expenses: ${it.message}") }

                call.respond(HttpStatusCode.OK, expenses)
            }

            // PUT /api/v1/expenses/{id} - Update expense
            put("/{expenseId}") {
                val tenantId = dokusPrincipal.requireTenantId()
                val expenseId = call.parameters.expenseId
                    ?: throw DokusException.BadRequest()

                val request = call.receive<CreateExpenseRequest>()

                val expense = expenseService.updateExpense(expenseId, tenantId, request)
                    .getOrElse { throw DokusException.InternalError("Failed to update expense: ${it.message}") }

                call.respond(HttpStatusCode.OK, expense)
            }

            // DELETE /api/v1/expenses/{id} - Delete expense
            delete("/{expenseId}") {
                val tenantId = dokusPrincipal.requireTenantId()
                val expenseId = call.parameters.expenseId
                    ?: throw DokusException.BadRequest()

                expenseService.deleteExpense(expenseId, tenantId)
                    .getOrElse { throw DokusException.InternalError("Failed to delete expense: ${it.message}") }

                call.respond(HttpStatusCode.NoContent)
            }

            // POST /api/v1/expenses/categorize - Categorize expense
            post("/categorize") {
                val tenantId = dokusPrincipal.requireTenantId()
                val request = call.receive<CategorizeExpenseRequest>()

                val category = expenseService.categorizeExpense(request.merchant, request.description)

                call.respond(HttpStatusCode.OK, CategorizeExpenseResponse(category))
            }
        }
    }
}

// Request/Response DTOs
@kotlinx.serialization.Serializable
private data class CategorizeExpenseRequest(
    val merchant: String,
    val description: String? = null
)

@kotlinx.serialization.Serializable
private data class CategorizeExpenseResponse(
    val category: ExpenseCategory
)
