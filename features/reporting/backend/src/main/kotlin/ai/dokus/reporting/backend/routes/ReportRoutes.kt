package ai.dokus.reporting.backend.routes

import ai.dokus.foundation.domain.ids.TenantId
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import ai.dokus.foundation.ktor.services.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory

@OptIn(ExperimentalUuidApi::class)
fun Route.reportRoutes() {
    val logger = LoggerFactory.getLogger("ReportRoutes")
    val invoiceService by inject<InvoiceService>()
    val expenseService by inject<ExpenseService>()
    val paymentService by inject<PaymentService>()

    route("/api/reports") {
        // Financial summary report
        get("/financial-summary") {
            val tenantIdStr = call.request.queryParameters["tenantId"] ?: "00000000-0000-0000-0000-000000000001"
            val startDate = call.request.queryParameters["startDate"]
            val endDate = call.request.queryParameters["endDate"]

            try {
                val tenantId = TenantId(Uuid.parse(tenantIdStr))

                // Aggregate data from multiple services
                val invoices = invoiceService.listByTenant(tenantId, limit = 1000, offset = 0)
                val expenses = expenseService.listByTenant(tenantId, limit = 1000, offset = 0)
                val payments = paymentService.listByTenant(tenantId, limit = 1000, offset = 0)

                val summary = mapOf(
                    "tenantId" to tenantIdStr,
                    "period" to mapOf("start" to startDate, "end" to endDate),
                    "invoiceCount" to invoices.size,
                    "expenseCount" to expenses.size,
                    "paymentCount" to payments.size,
                    "note" to "Full aggregation logic - coming soon"
                )

                call.respond(HttpStatusCode.OK, summary)
            } catch (e: Exception) {
                logger.error("Error generating financial summary", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Failed to generate report")))
            }
        }

        // Invoice analytics
        get("/invoice-analytics") {
            call.respond(HttpStatusCode.NotImplemented, mapOf("message" to "Invoice analytics endpoint - coming soon"))
        }

        // Expense analytics
        get("/expense-analytics") {
            call.respond(HttpStatusCode.NotImplemented, mapOf("message" to "Expense analytics endpoint - coming soon"))
        }

        // Cash flow report
        get("/cash-flow") {
            call.respond(HttpStatusCode.NotImplemented, mapOf("message" to "Cash flow report endpoint - coming soon"))
        }

        // VAT report
        get("/vat-report") {
            call.respond(HttpStatusCode.NotImplemented, mapOf("message" to "VAT report endpoint - coming soon"))
        }
    }
}
