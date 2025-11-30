package ai.dokus.cashflow.backend.routes

import ai.dokus.cashflow.backend.repository.InvoiceRepository
import ai.dokus.foundation.domain.enums.InvoiceStatus
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.model.CreateInvoiceRequest
import ai.dokus.foundation.domain.model.FinancialDocumentDto
import ai.dokus.foundation.domain.model.InvoiceItemDto
import ai.dokus.foundation.domain.model.InvoiceTotals
import ai.dokus.foundation.domain.model.RecordPaymentRequest
import ai.dokus.foundation.ktor.security.dokusPrincipal
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory

/**
 * Invoice API Routes
 * Base path: /api/v1/invoices
 *
 * All routes require JWT authentication and tenant context.
 */
fun Route.invoiceRoutes() {
    val invoiceRepository by inject<InvoiceRepository>()
    val logger = LoggerFactory.getLogger("InvoiceRoutes")

    route("/api/v1/invoices") {

        // POST /api/v1/invoices - Create invoice
        post {
            val principal = dokusPrincipal
            val tenantId = principal.requireTenantId()
            val request = call.receive<CreateInvoiceRequest>()
            logger.info("Creating invoice for tenant: $tenantId")

            val invoice = invoiceRepository.createInvoice(tenantId, request)
                .onSuccess { logger.info("Invoice created: ${it.id}") }
                .onFailure {
                    logger.error("Failed to create invoice for tenant: $tenantId", it)
                    throw DokusException.InternalError("Failed to create invoice: ${it.message}")
                }
                .getOrThrow()

            call.respond(HttpStatusCode.Created, invoice)
        }

        // GET /api/v1/invoices/{id} - Get invoice by ID
        get("/{id}") {
            val principal = dokusPrincipal
            val tenantId = principal.requireTenantId()
            val invoiceId = call.parameters.invoiceId
                ?: throw DokusException.Validation.Other

            logger.info("Fetching invoice: $invoiceId for tenant: $tenantId")

            val invoice = invoiceRepository.getInvoice(invoiceId, tenantId)
                .onFailure {
                    logger.error("Failed to fetch invoice: $invoiceId", it)
                    throw DokusException.InternalError("Failed to fetch invoice: ${it.message}")
                }
                .getOrThrow()
                ?: throw DokusException.Validation.Other

            call.respond(HttpStatusCode.OK, invoice)
        }

        // GET /api/v1/invoices - List invoices with query params
        get {
            val principal = dokusPrincipal
            val tenantId = principal.requireTenantId()
            val status = call.parameters.invoiceStatus
            val fromDate = call.parameters.fromDate
            val toDate = call.parameters.toDate
            val limit = call.parameters.limit
            val offset = call.parameters.offset

            logger.info("Listing invoices for tenant: $tenantId (status=$status, limit=$limit, offset=$offset)")

            val invoices = invoiceRepository.listInvoices(
                tenantId = tenantId,
                status = status,
                fromDate = fromDate,
                toDate = toDate,
                limit = limit,
                offset = offset
            )
                .onSuccess { logger.info("Retrieved ${it.size} invoices") }
                .onFailure {
                    logger.error("Failed to list invoices for tenant: $tenantId", it)
                    throw DokusException.InternalError("Failed to list invoices: ${it.message}")
                }
                .getOrThrow()

            call.respond(HttpStatusCode.OK, invoices)
        }

        // GET /api/v1/invoices/overdue - List overdue invoices
        get("/overdue") {
            val principal = dokusPrincipal
            val tenantId = principal.requireTenantId()
            logger.info("Listing overdue invoices for tenant: $tenantId")

            val invoices = invoiceRepository.listOverdueInvoices(tenantId)
                .onSuccess { logger.info("Retrieved ${it.size} overdue invoices") }
                .onFailure {
                    logger.error("Failed to list overdue invoices for tenant: $tenantId", it)
                    throw DokusException.InternalError("Failed to list overdue invoices: ${it.message}")
                }
                .getOrThrow()

            call.respond(HttpStatusCode.OK, invoices)
        }

        // PUT /api/v1/invoices/{id}/status - Update invoice status
        put("/{id}/status") {
            val principal = dokusPrincipal
            val tenantId = principal.requireTenantId()
            val invoiceId = call.parameters.invoiceId
                ?: throw DokusException.Validation.Other

            val status = call.receiveNullable<InvoiceStatusRequest>()?.status
                ?: throw DokusException.Validation.Other

            logger.info("Updating invoice status: $invoiceId to $status")

            invoiceRepository.updateInvoiceStatus(invoiceId, tenantId, status)
                .onSuccess { logger.info("Invoice status updated: $invoiceId -> $status") }
                .onFailure {
                    logger.error("Failed to update invoice status: $invoiceId", it)
                    throw DokusException.InternalError("Failed to update invoice status: ${it.message}")
                }
                .getOrThrow()

            call.respond(HttpStatusCode.NoContent)
        }

        // PUT /api/v1/invoices/{id} - Update invoice
        put("/{id}") {
            val principal = dokusPrincipal
            val tenantId = principal.requireTenantId()
            val invoiceId = call.parameters.invoiceId
                ?: throw DokusException.Validation.Other

            val request = call.receive<CreateInvoiceRequest>()
            logger.info("Updating invoice: $invoiceId")

            val invoice = invoiceRepository.updateInvoice(invoiceId, tenantId, request)
                .onSuccess { logger.info("Invoice updated: $invoiceId") }
                .onFailure {
                    logger.error("Failed to update invoice: $invoiceId", it)
                    throw DokusException.InternalError("Failed to update invoice: ${it.message}")
                }
                .getOrThrow()

            call.respond(HttpStatusCode.OK, invoice)
        }

        // DELETE /api/v1/invoices/{id} - Delete invoice
        delete("/{id}") {
            val principal = dokusPrincipal
            val tenantId = principal.requireTenantId()
            val invoiceId = call.parameters.invoiceId
                ?: throw DokusException.Validation.Other

            logger.info("Deleting invoice: $invoiceId")

            invoiceRepository.deleteInvoice(invoiceId, tenantId)
                .onSuccess { logger.info("Invoice deleted: $invoiceId") }
                .onFailure {
                    logger.error("Failed to delete invoice: $invoiceId", it)
                    throw DokusException.InternalError("Failed to delete invoice: ${it.message}")
                }
                .getOrThrow()

            call.respond(HttpStatusCode.NoContent)
        }

        // POST /api/v1/invoices/{id}/payment - Record payment
        post("/{id}/payment") {
            val principal = dokusPrincipal
            val tenantId = principal.requireTenantId()
            val invoiceId = call.parameters.invoiceId
                ?: throw DokusException.Validation.Other

            val request = call.receive<RecordPaymentRequest>()
            logger.info("Recording payment for invoice: $invoiceId")

            // TODO: Implement payment recording when service is available
            throw DokusException.InternalError("Payment recording not yet implemented")
        }

        // POST /api/v1/invoices/{id}/send-email - Send invoice via email
        post("/{id}/send-email") {
            val principal = dokusPrincipal
            val tenantId = principal.requireTenantId()
            val invoiceId = call.parameters.invoiceId
                ?: throw DokusException.Validation.Other

            val request = call.receiveNullable<SendInvoiceEmailRequest>()
            logger.info("Sending invoice email: $invoiceId to ${request?.recipientEmail}")

            // TODO: Implement email sending when service is available
            throw DokusException.InternalError("Email sending not yet implemented")
        }

        // POST /api/v1/invoices/{id}/mark-sent - Mark invoice as sent
        post("/{id}/mark-sent") {
            val principal = dokusPrincipal
            val tenantId = principal.requireTenantId()
            val invoiceId = call.parameters.invoiceId
                ?: throw DokusException.Validation.Other

            logger.info("Marking invoice as sent: $invoiceId")

            // TODO: Implement mark as sent when service is available
            throw DokusException.InternalError("Mark as sent not yet implemented")
        }

        // POST /api/v1/invoices/calculate-totals - Calculate invoice totals
        post("/calculate-totals") {
            val principal = dokusPrincipal
            val tenantId = principal.requireTenantId()
            val items = call.receive<CalculateTotalsRequest>().items
            logger.info("Calculating totals for ${items.size} items")

            // TODO: Implement totals calculation
            val totals = InvoiceTotals(
                subtotal = ai.dokus.foundation.domain.Money.ZERO,
                vatAmount = ai.dokus.foundation.domain.Money.ZERO,
                total = ai.dokus.foundation.domain.Money.ZERO
            )

            call.respond(HttpStatusCode.OK, totals)
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
