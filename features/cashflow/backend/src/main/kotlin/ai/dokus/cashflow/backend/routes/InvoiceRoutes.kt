package ai.dokus.cashflow.backend.routes

import ai.dokus.cashflow.backend.service.InvoiceService
import ai.dokus.foundation.domain.enums.InvoiceStatus
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.model.CreateInvoiceRequest
import ai.dokus.foundation.domain.model.InvoiceItemDto
import ai.dokus.foundation.domain.model.InvoiceTotals
import ai.dokus.foundation.domain.model.RecordPaymentRequest
import ai.dokus.foundation.ktor.security.authenticateJwt
import ai.dokus.foundation.ktor.security.dokusPrincipal
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.request.receiveNullable
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

/**
 * Invoice API Routes
 * Base path: /api/v1/invoices
 *
 * All routes require JWT authentication and tenant context.
 */
fun Route.invoiceRoutes() {
    val invoiceService by inject<InvoiceService>()

    route("/api/v1/invoices") {
        authenticateJwt {

            // POST /api/v1/invoices - Create invoice
            post {
                val tenantId = dokusPrincipal.requireTenantId()
                val request = call.receive<CreateInvoiceRequest>()

                val invoice = invoiceService.createInvoice(tenantId, request)
                    .getOrElse { throw DokusException.InternalError("Failed to create invoice: ${it.message}") }

                call.respond(HttpStatusCode.Created, invoice)
            }

            // GET /api/v1/invoices/{id} - Get invoice by ID
            get("/{invoiceId}") {
                val tenantId = dokusPrincipal.requireTenantId()
                val invoiceId = call.parameters.invoiceId
                    ?: throw DokusException.BadRequest()

                val invoice = invoiceService.getInvoice(invoiceId, tenantId)
                    .getOrElse { throw DokusException.InternalError("Failed to fetch invoice: ${it.message}") }
                    ?: throw DokusException.NotFound("Invoice not found")

                call.respond(HttpStatusCode.OK, invoice)
            }

            // GET /api/v1/invoices - List invoices with query params
            get {
                val tenantId = dokusPrincipal.requireTenantId()
                val status = call.parameters.invoiceStatus
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

                val invoices = invoiceService.listInvoices(
                    tenantId = tenantId,
                    status = status,
                    fromDate = fromDate,
                    toDate = toDate,
                    limit = limit,
                    offset = offset
                ).getOrElse { throw DokusException.InternalError("Failed to list invoices: ${it.message}") }

                call.respond(HttpStatusCode.OK, invoices)
            }

            // GET /api/v1/invoices/overdue - List overdue invoices
            get("/overdue") {
                val tenantId = dokusPrincipal.requireTenantId()

                val invoices = invoiceService.listOverdueInvoices(tenantId)
                    .getOrElse { throw DokusException.InternalError("Failed to list overdue invoices: ${it.message}") }

                call.respond(HttpStatusCode.OK, invoices)
            }

            // PATCH /api/v1/invoices/{id}/status - Update invoice status
            patch("/{invoiceId}/status") {
                val tenantId = dokusPrincipal.requireTenantId()
                val invoiceId = call.parameters.invoiceId
                    ?: throw DokusException.BadRequest()

                val status = call.receiveNullable<InvoiceStatusRequest>()?.status
                    ?: throw DokusException.BadRequest()

                invoiceService.updateInvoiceStatus(invoiceId, tenantId, status)
                    .getOrElse { throw DokusException.InternalError("Failed to update invoice status: ${it.message}") }

                call.respond(HttpStatusCode.NoContent)
            }

            // PUT /api/v1/invoices/{id} - Update invoice
            put("/{invoiceId}") {
                val tenantId = dokusPrincipal.requireTenantId()
                val invoiceId = call.parameters.invoiceId
                    ?: throw DokusException.BadRequest()

                val request = call.receive<CreateInvoiceRequest>()

                val invoice = invoiceService.updateInvoice(invoiceId, tenantId, request)
                    .getOrElse { throw DokusException.InternalError("Failed to update invoice: ${it.message}") }

                call.respond(HttpStatusCode.OK, invoice)
            }

            // DELETE /api/v1/invoices/{id} - Delete invoice
            delete("/{invoiceId}") {
                val tenantId = dokusPrincipal.requireTenantId()
                val invoiceId = call.parameters.invoiceId
                    ?: throw DokusException.BadRequest()

                invoiceService.deleteInvoice(invoiceId, tenantId)
                    .getOrElse { throw DokusException.InternalError("Failed to delete invoice: ${it.message}") }

                call.respond(HttpStatusCode.NoContent)
            }

            // POST /api/v1/invoices/{id}/payments - Record payment
            post("/{invoiceId}/payments") {
                val tenantId = dokusPrincipal.requireTenantId()
                val invoiceId = call.parameters.invoiceId
                    ?: throw DokusException.BadRequest()

                val request = call.receive<RecordPaymentRequest>()

                // TODO: Implement payment recording via PaymentService when available
                throw DokusException.InternalError("Payment recording not yet implemented")
            }

            // POST /api/v1/invoices/{id}/send-email - Send invoice via email
            post("/{invoiceId}/send-email") {
                val tenantId = dokusPrincipal.requireTenantId()
                val invoiceId = call.parameters.invoiceId
                    ?: throw DokusException.BadRequest()

                val request = call.receiveNullable<SendInvoiceEmailRequest>()

                // TODO: Implement email sending via EmailService when available
                throw DokusException.InternalError("Email sending not yet implemented")
            }

            // POST /api/v1/invoices/{id}/mark-sent - Mark invoice as sent
            post("/{invoiceId}/mark-sent") {
                val tenantId = dokusPrincipal.requireTenantId()
                val invoiceId = call.parameters.invoiceId
                    ?: throw DokusException.BadRequest()

                // TODO: Implement mark as sent via InvoiceService when available
                throw DokusException.InternalError("Mark as sent not yet implemented")
            }

            // POST /api/v1/invoices/calculate-totals - Calculate invoice totals
            post("/calculate-totals") {
                val tenantId = dokusPrincipal.requireTenantId()
                val items = call.receive<CalculateTotalsRequest>().items

                // TODO: Implement totals calculation via InvoiceService
                val totals = InvoiceTotals(
                    subtotal = ai.dokus.foundation.domain.Money.ZERO,
                    vatAmount = ai.dokus.foundation.domain.Money.ZERO,
                    total = ai.dokus.foundation.domain.Money.ZERO
                )

                call.respond(HttpStatusCode.OK, totals)
            }
        }
    }
}

// Request DTOs
@kotlinx.serialization.Serializable
private data class InvoiceStatusRequest(val status: InvoiceStatus)

@kotlinx.serialization.Serializable
private data class SendInvoiceEmailRequest(
    val recipientEmail: String? = null,
    val message: String? = null
)

@kotlinx.serialization.Serializable
private data class CalculateTotalsRequest(val items: List<InvoiceItemDto>)
