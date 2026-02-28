package tech.dokus.backend.routes.cashflow

import tech.dokus.backend.security.requireTenantAccess

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.request.receiveNullable
import io.ktor.server.resources.delete
import io.ktor.server.resources.get
import io.ktor.server.resources.patch
import io.ktor.server.resources.post
import io.ktor.server.resources.put
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import org.koin.ktor.ext.inject
import tech.dokus.backend.services.cashflow.InvoiceService
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.InvoiceStatus
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.InvoiceId
import tech.dokus.domain.model.CreateInvoiceRequest
import tech.dokus.domain.model.InvoiceItemDto
import tech.dokus.domain.model.RecordPaymentRequest
import tech.dokus.domain.routes.Invoices
import tech.dokus.foundation.backend.security.authenticateJwt
import tech.dokus.foundation.backend.security.dokusPrincipal
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Invoice API Routes using Ktor Type-Safe Routing
 * Base path: /api/v1/invoices
 *
 * All routes require JWT authentication and tenant context.
 */
@OptIn(ExperimentalUuidApi::class)
internal fun Route.invoiceRoutes() {
    val invoiceService by inject<InvoiceService>()

    authenticateJwt {
        // GET /api/v1/invoices - List invoices with query params
        get<Invoices> { route ->
            val tenantId = requireTenantAccess().tenantId

            if (route.limit < 1 || route.limit > 200) {
                throw DokusException.BadRequest("Limit must be between 1 and 200")
            }
            if (route.offset < 0) {
                throw DokusException.BadRequest("Offset must be non-negative")
            }

            val invoices = invoiceService.listInvoices(
                tenantId = tenantId,
                status = route.status,
                direction = route.direction,
                fromDate = route.fromDate,
                toDate = route.toDate,
                limit = route.limit,
                offset = route.offset
            ).getOrElse {
                throw DokusException.InternalError("Failed to list invoices: ${it.message}")
            }

            call.respond(HttpStatusCode.OK, invoices)
        }

        // POST /api/v1/invoices - Create invoice
        post<Invoices> {
            val tenantId = requireTenantAccess().tenantId
            val request = call.receive<CreateInvoiceRequest>()

            val invoice = invoiceService.createInvoice(tenantId, request)
                .getOrElse { throw DokusException.InternalError("Failed to create invoice: ${it.message}") }

            call.respond(HttpStatusCode.Created, invoice)
        }

        // GET /api/v1/invoices/overdue - List overdue invoices
        get<Invoices.Overdue> { route ->
            val tenantId = requireTenantAccess().tenantId

            val invoices = invoiceService.listOverdueInvoices(
                tenantId = tenantId,
                direction = route.parent.direction ?: DocumentDirection.Outbound
            )
                .getOrElse { throw DokusException.InternalError("Failed to list overdue invoices: ${it.message}") }

            call.respond(HttpStatusCode.OK, invoices)
        }

        // GET /api/v1/invoices/{id} - Get invoice by ID
        get<Invoices.Id> { route ->
            val tenantId = requireTenantAccess().tenantId
            val invoiceId = InvoiceId(Uuid.parse(route.id))

            val invoice = invoiceService.getInvoice(invoiceId, tenantId)
                .getOrElse { throw DokusException.InternalError("Failed to fetch invoice: ${it.message}") }
                ?: throw DokusException.NotFound("Invoice not found")

            call.respond(HttpStatusCode.OK, invoice)
        }

        // PUT /api/v1/invoices/{id} - Update invoice
        put<Invoices.Id> { route ->
            val tenantId = requireTenantAccess().tenantId
            val invoiceId = InvoiceId(Uuid.parse(route.id))
            val request = call.receive<CreateInvoiceRequest>()

            val invoice = invoiceService.updateInvoice(invoiceId, tenantId, request)
                .getOrElse { throw DokusException.InternalError("Failed to update invoice: ${it.message}") }

            call.respond(HttpStatusCode.OK, invoice)
        }

        // DELETE /api/v1/invoices/{id} - Delete invoice
        delete<Invoices.Id> { route ->
            val tenantId = requireTenantAccess().tenantId
            val invoiceId = InvoiceId(Uuid.parse(route.id))

            invoiceService.deleteInvoice(invoiceId, tenantId)
                .getOrElse { throw DokusException.InternalError("Failed to delete invoice: ${it.message}") }

            call.respond(HttpStatusCode.NoContent)
        }

        // PATCH /api/v1/invoices/{id}/status - Update invoice status
        patch<Invoices.Id.Status> { route ->
            val tenantId = requireTenantAccess().tenantId
            val invoiceId = InvoiceId(Uuid.parse(route.parent.id))

            val status = call.receiveNullable<InvoiceStatusRequest>()?.status
                ?: throw DokusException.BadRequest()

            invoiceService.updateInvoiceStatus(invoiceId, tenantId, status)
                .getOrElse { throw DokusException.InternalError("Failed to update invoice status: ${it.message}") }

            call.respond(HttpStatusCode.NoContent)
        }

        // POST /api/v1/invoices/{id}/payments - Record payment
        post<Invoices.Id.Payments> { route ->
            val tenantId = requireTenantAccess().tenantId
            val invoiceId = InvoiceId(Uuid.parse(route.parent.id))
            val request = call.receive<RecordPaymentRequest>()

            if (request.invoiceId != invoiceId) {
                throw DokusException.BadRequest("Invoice ID mismatch")
            }

            invoiceService.recordPayment(invoiceId, tenantId, request)
                .getOrElse { throw DokusException.InternalError("Failed to record payment: ${it.message}") }

            call.respond(HttpStatusCode.NoContent)
        }

        // POST /api/v1/invoices/{id}/emails - Send invoice via email
        post<Invoices.Id.Emails> { route ->
            val tenantId = requireTenantAccess().tenantId
            val invoiceId = InvoiceId(Uuid.parse(route.parent.id))
            val request = call.receiveNullable<SendInvoiceEmailRequest>()

            // TODO: Implement email sending via EmailService when available
            throw DokusException.InternalError("Email sending not yet implemented")
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
