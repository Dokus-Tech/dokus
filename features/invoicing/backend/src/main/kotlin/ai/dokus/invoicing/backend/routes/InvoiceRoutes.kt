package ai.dokus.invoicing.backend.routes

import ai.dokus.foundation.domain.TenantId
import ai.dokus.foundation.domain.InvoiceId
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import ai.dokus.foundation.ktor.services.InvoiceService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory

@OptIn(ExperimentalUuidApi::class)
fun Route.invoiceRoutes() {
    val logger = LoggerFactory.getLogger("InvoiceRoutes")
    val invoiceService by inject<InvoiceService>()

    route("/api/invoices") {
        // List invoices
        get {
            // Get tenantId from query parameter (in production, this would come from JWT token)
            val tenantIdStr = call.request.queryParameters["tenantId"] ?: "00000000-0000-0000-0000-000000000001"
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
            val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0

            try {
                val tenantId = TenantId(Uuid.parse(tenantIdStr))
                val invoices = invoiceService.listByTenant(
                    tenantId = tenantId,
                    limit = limit,
                    offset = offset
                )
                call.respond(HttpStatusCode.OK, invoices)
            } catch (e: Exception) {
                logger.error("Error listing invoices", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Failed to list invoices")))
            }
        }

        // Get invoice by ID
        get("/{id}") {
            val invoiceIdStr = call.parameters["id"]

            if (invoiceIdStr.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invoice ID is required"))
                return@get
            }

            try {
                val invoiceId = InvoiceId(Uuid.parse(invoiceIdStr))
                val invoice = invoiceService.findById(invoiceId)
                if (invoice != null) {
                    call.respond(HttpStatusCode.OK, invoice)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Invoice not found"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Failed to get invoice")))
            }
        }

        // Create invoice
        post {
            call.respond(HttpStatusCode.NotImplemented, mapOf("message" to "Create invoice endpoint - coming soon"))
        }

        // Update invoice
        put("/{id}") {
            call.respond(HttpStatusCode.NotImplemented, mapOf("message" to "Update invoice endpoint - coming soon"))
        }

        // Delete invoice
        delete("/{id}") {
            call.respond(HttpStatusCode.NotImplemented, mapOf("message" to "Delete invoice endpoint - coming soon"))
        }

        // Send invoice via Peppol
        post("/{id}/send") {
            call.respond(HttpStatusCode.NotImplemented, mapOf("message" to "Send invoice via Peppol - coming soon"))
        }

        // Generate PDF
        get("/{id}/pdf") {
            call.respond(HttpStatusCode.NotImplemented, mapOf("message" to "Generate invoice PDF - coming soon"))
        }
    }
}
