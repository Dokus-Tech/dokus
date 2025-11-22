package ai.dokus.payment.backend.routes

import ai.dokus.foundation.domain.ids.OrganizationId
import ai.dokus.foundation.domain.ids.PaymentId
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import ai.dokus.foundation.ktor.services.PaymentService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory

@OptIn(ExperimentalUuidApi::class)
fun Route.paymentRoutes() {
    val logger = LoggerFactory.getLogger("PaymentRoutes")
    val paymentService by inject<PaymentService>()

    route("/api/payments") {
        // List payments
        get {
            val organizationIdStr = call.request.queryParameters["organizationId"] ?: "00000000-0000-0000-0000-000000000001"
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
            val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0

            try {
                val organizationId = OrganizationId(Uuid.parse(organizationIdStr))
                val payments = paymentService.listByTenant(
                    organizationId = organizationId,
                    limit = limit,
                    offset = offset
                )
                call.respond(HttpStatusCode.OK, payments)
            } catch (e: Exception) {
                logger.error("Error listing payments", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Failed to list payments")))
            }
        }

        // Get payment by ID
        get("/{id}") {
            val paymentIdStr = call.parameters["id"]

            if (paymentIdStr.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Payment ID is required"))
                return@get
            }

            try {
                val paymentId = PaymentId(Uuid.parse(paymentIdStr))
                val payment = paymentService.findById(paymentId)
                if (payment != null) {
                    call.respond(HttpStatusCode.OK, payment)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Payment not found"))
                }
            } catch (e: Exception) {
                logger.error("Error getting payment", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Failed to get payment")))
            }
        }

        // Create payment
        post {
            call.respond(HttpStatusCode.NotImplemented, mapOf("message" to "Create payment endpoint - coming soon"))
        }

        // Update payment
        put("/{id}") {
            call.respond(HttpStatusCode.NotImplemented, mapOf("message" to "Update payment endpoint - coming soon"))
        }

        // Delete payment
        delete("/{id}") {
            call.respond(HttpStatusCode.NotImplemented, mapOf("message" to "Delete payment endpoint - coming soon"))
        }

        // Process payment with Mollie
        post("/{id}/process") {
            call.respond(HttpStatusCode.NotImplemented, mapOf("message" to "Process payment endpoint - coming soon"))
        }
    }
}
