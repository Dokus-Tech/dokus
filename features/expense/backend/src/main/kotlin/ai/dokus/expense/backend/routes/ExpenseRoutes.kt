package ai.dokus.expense.backend.routes

import ai.dokus.foundation.domain.TenantId
import ai.dokus.foundation.domain.ExpenseId
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import ai.dokus.foundation.ktor.services.ExpenseService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory

@OptIn(ExperimentalUuidApi::class)
fun Route.expenseRoutes() {
    val logger = LoggerFactory.getLogger("ExpenseRoutes")
    val expenseService by inject<ExpenseService>()

    route("/api/expenses") {
        // List expenses
        get {
            val tenantIdStr = call.request.queryParameters["tenantId"] ?: "00000000-0000-0000-0000-000000000001"
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
            val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0

            try {
                val tenantId = TenantId(Uuid.parse(tenantIdStr))
                val expenses = expenseService.listByTenant(
                    tenantId = tenantId,
                    limit = limit,
                    offset = offset
                )
                call.respond(HttpStatusCode.OK, expenses)
            } catch (e: Exception) {
                logger.error("Error listing expenses", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Failed to list expenses")))
            }
        }

        // Get expense by ID
        get("/{id}") {
            val expenseIdStr = call.parameters["id"]

            if (expenseIdStr.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Expense ID is required"))
                return@get
            }

            try {
                val expenseId = ExpenseId(Uuid.parse(expenseIdStr))
                val expense = expenseService.findById(expenseId)
                if (expense != null) {
                    call.respond(HttpStatusCode.OK, expense)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Expense not found"))
                }
            } catch (e: Exception) {
                logger.error("Error getting expense", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Failed to get expense")))
            }
        }

        // Create expense
        post {
            call.respond(HttpStatusCode.NotImplemented, mapOf("message" to "Create expense endpoint - coming soon"))
        }

        // Update expense
        put("/{id}") {
            call.respond(HttpStatusCode.NotImplemented, mapOf("message" to "Update expense endpoint - coming soon"))
        }

        // Delete expense
        delete("/{id}") {
            call.respond(HttpStatusCode.NotImplemented, mapOf("message" to "Delete expense endpoint - coming soon"))
        }

        // Upload receipt
        post("/{id}/receipt") {
            call.respond(HttpStatusCode.NotImplemented, mapOf("message" to "Upload receipt endpoint - coming soon"))
        }
    }
}
