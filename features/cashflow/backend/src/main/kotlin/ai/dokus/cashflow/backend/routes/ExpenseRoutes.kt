package ai.dokus.cashflow.backend.routes

import ai.dokus.cashflow.backend.repository.ExpenseRepository
import ai.dokus.foundation.domain.enums.ExpenseCategory
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.model.CreateExpenseRequest
import ai.dokus.foundation.ktor.security.authenticateJwt
import ai.dokus.foundation.ktor.security.dokusPrincipal
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory

/**
 * Expense API Routes
 * Base path: /api/v1/expenses
 *
 * All routes require JWT authentication and tenant context.
 */
fun Route.expenseRoutes() {
    val expenseRepository by inject<ExpenseRepository>()
    val logger = LoggerFactory.getLogger("ExpenseRoutes")

    route("/api/v1/expenses") {
        authenticateJwt {

            // POST /api/v1/expenses - Create expense
            post {
                val tenantId = dokusPrincipal.requireTenantId()
                val request = call.receive<CreateExpenseRequest>()
                logger.info("Creating expense for tenant: $tenantId")

                val expense = expenseRepository.createExpense(tenantId, request)
                    .onSuccess { logger.info("Expense created: ${it.id}") }
                    .onFailure {
                        logger.error("Failed to create expense for tenant: $tenantId", it)
                        throw DokusException.InternalError("Failed to create expense: ${it.message}")
                    }
                    .getOrThrow()

                call.respond(HttpStatusCode.Created, expense)
            }

            // GET /api/v1/expenses/{id} - Get expense by ID
            get("/{expenseId}") {
                val tenantId = dokusPrincipal.requireTenantId()
                val expenseId = call.parameters.expenseId
                    ?: throw DokusException.BadRequest()

                logger.info("Fetching expense: $expenseId for tenant: $tenantId")

                val expense = expenseRepository.getExpense(expenseId, tenantId)
                    .onFailure {
                        logger.error("Failed to fetch expense: $expenseId", it)
                        throw DokusException.InternalError("Failed to fetch expense: ${it.message}")
                    }
                    .getOrThrow()
                    ?: throw DokusException.BadRequest()

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

                logger.info("Listing expenses for tenant: $tenantId (category=$category, limit=$limit, offset=$offset)")

                val expenses = expenseRepository.listExpenses(
                    tenantId = tenantId,
                    category = category,
                    fromDate = fromDate,
                    toDate = toDate,
                    limit = limit,
                    offset = offset
                )
                    .onSuccess { logger.info("Retrieved ${it.items.size} expenses (total=${it.total})") }
                    .onFailure {
                        logger.error("Failed to list expenses for tenant: $tenantId", it)
                        throw DokusException.InternalError("Failed to list expenses: ${it.message}")
                    }
                    .getOrThrow()

                call.respond(HttpStatusCode.OK, expenses)
            }

            // PUT /api/v1/expenses/{id} - Update expense
            put("/{expenseId}") {
                val principal = dokusPrincipal
                val tenantId = principal.requireTenantId()
                val expenseId = call.parameters.expenseId
                    ?: throw DokusException.BadRequest()

                val request = call.receive<CreateExpenseRequest>()
                logger.info("Updating expense: $expenseId")

                val expense = expenseRepository.updateExpense(expenseId, tenantId, request)
                    .onSuccess { logger.info("Expense updated: $expenseId") }
                    .onFailure {
                        logger.error("Failed to update expense: $expenseId", it)
                        throw DokusException.InternalError("Failed to update expense: ${it.message}")
                    }
                    .getOrThrow()

                call.respond(HttpStatusCode.OK, expense)
            }

            // DELETE /api/v1/expenses/{id} - Delete expense
            delete("/{expenseId}") {
                val principal = dokusPrincipal
                val tenantId = principal.requireTenantId()
                val expenseId = call.parameters.expenseId
                    ?: throw DokusException.BadRequest()

                logger.info("Deleting expense: $expenseId")

                expenseRepository.deleteExpense(expenseId, tenantId)
                    .onSuccess { logger.info("Expense deleted: $expenseId") }
                    .onFailure {
                        logger.error("Failed to delete expense: $expenseId", it)
                        throw DokusException.InternalError("Failed to delete expense: ${it.message}")
                    }
                    .getOrThrow()

                call.respond(HttpStatusCode.NoContent)
            }

            // POST /api/v1/expenses/categorize - Categorize expense
            post("/categorize") {
                val principal = dokusPrincipal
                val tenantId = principal.requireTenantId()
                val request = call.receive<CategorizeExpenseRequest>()
                logger.info("Categorizing expense for merchant: ${request.merchant}")

                // TODO: Implement auto-categorization when service is available
                val category = ExpenseCategory.Other

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
