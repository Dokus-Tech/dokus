package ai.dokus.backend.routes.cashflow

import ai.dokus.backend.services.cashflow.ExpenseService
import ai.dokus.foundation.domain.enums.ExpenseCategory
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.ids.ExpenseId
import ai.dokus.foundation.domain.model.CreateExpenseRequest
import ai.dokus.foundation.domain.routes.Expenses
import ai.dokus.foundation.ktor.security.authenticateJwt
import ai.dokus.foundation.ktor.security.dokusPrincipal
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.resources.delete
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.resources.put
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import org.koin.ktor.ext.inject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Expense API Routes using Ktor Type-Safe Routing
 * Base path: /api/v1/expenses
 *
 * All routes require JWT authentication and tenant context.
 */
@OptIn(ExperimentalUuidApi::class)
fun Route.expenseRoutes() {
    val expenseService by inject<ExpenseService>()

    authenticateJwt {
        // GET /api/v1/expenses - List expenses with query params
        get<Expenses> { route ->
            val tenantId = dokusPrincipal.requireTenantId()

            if (route.limit < 1 || route.limit > 200) {
                throw DokusException.BadRequest("Limit must be between 1 and 200")
            }
            if (route.offset < 0) {
                throw DokusException.BadRequest("Offset must be non-negative")
            }

            val expenses = expenseService.listExpenses(
                tenantId = tenantId,
                category = route.category,
                fromDate = route.fromDate,
                toDate = route.toDate,
                limit = route.limit,
                offset = route.offset
            ).getOrElse { throw DokusException.InternalError("Failed to list expenses: ${it.message}") }

            call.respond(HttpStatusCode.OK, expenses)
        }

        // POST /api/v1/expenses - Create expense
        post<Expenses> {
            val tenantId = dokusPrincipal.requireTenantId()
            val request = call.receive<CreateExpenseRequest>()

            val expense = expenseService.createExpense(tenantId, request)
                .getOrElse { throw DokusException.InternalError("Failed to create expense: ${it.message}") }

            call.respond(HttpStatusCode.Created, expense)
        }

        // GET /api/v1/expenses/{id} - Get expense by ID
        get<Expenses.Id> { route ->
            val tenantId = dokusPrincipal.requireTenantId()
            val expenseId = ExpenseId(Uuid.parse(route.id))

            val expense = expenseService.getExpense(expenseId, tenantId)
                .getOrElse { throw DokusException.InternalError("Failed to fetch expense: ${it.message}") }
                ?: throw DokusException.NotFound("Expense not found")

            call.respond(HttpStatusCode.OK, expense)
        }

        // PUT /api/v1/expenses/{id} - Update expense
        put<Expenses.Id> { route ->
            val tenantId = dokusPrincipal.requireTenantId()
            val expenseId = ExpenseId(Uuid.parse(route.id))
            val request = call.receive<CreateExpenseRequest>()

            val expense = expenseService.updateExpense(expenseId, tenantId, request)
                .getOrElse { throw DokusException.InternalError("Failed to update expense: ${it.message}") }

            call.respond(HttpStatusCode.OK, expense)
        }

        // DELETE /api/v1/expenses/{id} - Delete expense
        delete<Expenses.Id> { route ->
            val tenantId = dokusPrincipal.requireTenantId()
            val expenseId = ExpenseId(Uuid.parse(route.id))

            expenseService.deleteExpense(expenseId, tenantId)
                .getOrElse { throw DokusException.InternalError("Failed to delete expense: ${it.message}") }

            call.respond(HttpStatusCode.NoContent)
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
