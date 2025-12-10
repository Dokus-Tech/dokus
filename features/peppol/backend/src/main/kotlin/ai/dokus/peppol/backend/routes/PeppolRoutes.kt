package ai.dokus.peppol.backend.routes

import ai.dokus.foundation.domain.enums.PeppolStatus
import ai.dokus.foundation.domain.enums.PeppolTransmissionDirection
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.ids.InvoiceId
import ai.dokus.foundation.domain.model.SavePeppolSettingsRequest
import ai.dokus.foundation.ktor.security.authenticateJwt
import ai.dokus.foundation.ktor.security.dokusPrincipal
import ai.dokus.peppol.backend.service.PeppolService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Peppol API Routes
 * Base path: /api/v1/peppol
 *
 * All routes require JWT authentication and tenant context.
 */
@OptIn(ExperimentalUuidApi::class)
fun Route.peppolRoutes() {
    val peppolService by inject<PeppolService>()

    route("/api/v1/peppol") {
        authenticateJwt {

            // ================================================================
            // SETTINGS
            // ================================================================

            route("/settings") {
                // GET /api/v1/peppol/settings - Get Peppol settings
                get {
                    val tenantId = dokusPrincipal.requireTenantId()

                    val settings = peppolService.getSettings(tenantId)
                        .getOrElse { throw DokusException.InternalError("Failed to get Peppol settings: ${it.message}") }

                    if (settings == null) {
                        call.respond(HttpStatusCode.NotFound, mapOf("message" to "Peppol settings not configured"))
                    } else {
                        call.respond(HttpStatusCode.OK, settings)
                    }
                }

                // PUT /api/v1/peppol/settings - Save Peppol settings
                put {
                    val tenantId = dokusPrincipal.requireTenantId()
                    val request = call.receive<SavePeppolSettingsRequest>()

                    val settings = peppolService.saveSettings(tenantId, request)
                        .getOrElse { throw DokusException.InternalError("Failed to save Peppol settings: ${it.message}") }

                    call.respond(HttpStatusCode.OK, settings)
                }

                // DELETE /api/v1/peppol/settings - Delete Peppol settings
                delete {
                    val tenantId = dokusPrincipal.requireTenantId()

                    peppolService.deleteSettings(tenantId)
                        .getOrElse { throw DokusException.InternalError("Failed to delete Peppol settings: ${it.message}") }

                    call.respond(HttpStatusCode.NoContent)
                }
            }

            // ================================================================
            // VERIFICATION
            // ================================================================

            // POST /api/v1/peppol/verify - Verify if a recipient is on Peppol network
            post("/verify") {
                val tenantId = dokusPrincipal.requireTenantId()
                val request = call.receive<VerifyRecipientRequest>()

                val result = peppolService.verifyRecipient(tenantId, request.peppolId)
                    .getOrElse { throw DokusException.InternalError("Failed to verify recipient: ${it.message}") }

                call.respond(HttpStatusCode.OK, result)
            }

            // ================================================================
            // OUTBOUND - SENDING INVOICES
            // ================================================================

            route("/send") {
                // POST /api/v1/peppol/send/invoice/{invoiceId} - Send invoice via Peppol
                post("/invoice/{invoiceId}") {
                    val tenantId = dokusPrincipal.requireTenantId()
                    val invoiceIdStr = call.parameters["invoiceId"]
                        ?: throw DokusException.BadRequest("Invoice ID is required")

                    val invoiceId = InvoiceId(Uuid.parse(invoiceIdStr))

                    // Fetch invoice and client via peppolService (uses repositories directly)
                    val invoice = peppolService.getInvoice(invoiceId, tenantId)
                        .getOrElse { throw DokusException.InternalError("Failed to fetch invoice: ${it.message}") }
                        ?: throw DokusException.NotFound("Invoice not found")

                    val client = peppolService.getClient(invoice.clientId, tenantId)
                        .getOrElse { throw DokusException.InternalError("Failed to fetch client: ${it.message}") }
                        ?: throw DokusException.NotFound("Client not found")

                    val tenantSettings = peppolService.getTenantSettings(tenantId)
                        ?: throw DokusException.NotFound("Tenant settings not found")

                    // Send via Peppol
                    val result = peppolService.sendInvoice(invoice, client, tenantSettings, tenantId)
                        .getOrElse { throw DokusException.InternalError("Failed to send invoice via Peppol: ${it.message}") }

                    if (result.status == PeppolStatus.Failed) {
                        call.respond(
                            HttpStatusCode.UnprocessableEntity,
                            SendInvoiceResponse(
                                success = false,
                                transmissionId = result.transmissionId.value.toString(),
                                status = result.status.name,
                                errorMessage = result.errorMessage
                            )
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.OK,
                            SendInvoiceResponse(
                                success = true,
                                transmissionId = result.transmissionId.value.toString(),
                                status = result.status.name,
                                externalDocumentId = result.externalDocumentId
                            )
                        )
                    }
                }

                // POST /api/v1/peppol/send/validate/{invoiceId} - Validate invoice without sending
                post("/validate/{invoiceId}") {
                    val tenantId = dokusPrincipal.requireTenantId()
                    val invoiceIdStr = call.parameters["invoiceId"]
                        ?: throw DokusException.BadRequest("Invoice ID is required")

                    val invoiceId = InvoiceId(Uuid.parse(invoiceIdStr))

                    // Fetch invoice and client via peppolService (uses repositories directly)
                    val invoice = peppolService.getInvoice(invoiceId, tenantId)
                        .getOrElse { throw DokusException.InternalError("Failed to fetch invoice: ${it.message}") }
                        ?: throw DokusException.NotFound("Invoice not found")

                    val client = peppolService.getClient(invoice.clientId, tenantId)
                        .getOrElse { throw DokusException.InternalError("Failed to fetch client: ${it.message}") }
                        ?: throw DokusException.NotFound("Client not found")

                    val tenantSettings = peppolService.getTenantSettings(tenantId)
                        ?: throw DokusException.NotFound("Tenant settings not found")

                    // Validate
                    val result = peppolService.validateInvoice(invoice, client, tenantSettings, tenantId)
                        .getOrElse { throw DokusException.InternalError("Failed to validate invoice: ${it.message}") }

                    call.respond(HttpStatusCode.OK, result)
                }
            }

            // ================================================================
            // INBOUND - POLLING INBOX
            // ================================================================

            // POST /api/v1/peppol/inbox/poll - Poll inbox for new documents
            post("/inbox/poll") {
                val tenantId = dokusPrincipal.requireTenantId()

                val result = peppolService.pollInbox(tenantId)
                    .getOrElse { throw DokusException.InternalError("Failed to poll inbox: ${it.message}") }

                call.respond(HttpStatusCode.OK, result)
            }

            // ================================================================
            // TRANSMISSION HISTORY
            // ================================================================

            route("/transmissions") {
                // GET /api/v1/peppol/transmissions - List transmissions
                get {
                    val tenantId = dokusPrincipal.requireTenantId()
                    val direction = call.parameters["direction"]?.let {
                        PeppolTransmissionDirection.valueOf(it)
                    }
                    val status = call.parameters["status"]?.let {
                        PeppolStatus.valueOf(it)
                    }
                    val limit = call.parameters["limit"]?.toIntOrNull() ?: 50
                    val offset = call.parameters["offset"]?.toIntOrNull() ?: 0

                    if (limit < 1 || limit > 200) {
                        throw DokusException.BadRequest("Limit must be between 1 and 200")
                    }

                    val transmissions = peppolService.listTransmissions(
                        tenantId = tenantId,
                        direction = direction,
                        status = status,
                        limit = limit,
                        offset = offset
                    ).getOrElse { throw DokusException.InternalError("Failed to list transmissions: ${it.message}") }

                    call.respond(HttpStatusCode.OK, transmissions)
                }

                // GET /api/v1/peppol/transmissions/invoice/{invoiceId} - Get transmission for invoice
                get("/invoice/{invoiceId}") {
                    val tenantId = dokusPrincipal.requireTenantId()
                    val invoiceIdStr = call.parameters["invoiceId"]
                        ?: throw DokusException.BadRequest("Invoice ID is required")

                    val invoiceId = InvoiceId(Uuid.parse(invoiceIdStr))

                    val transmission = peppolService.getTransmissionByInvoiceId(invoiceId, tenantId)
                        .getOrElse { throw DokusException.InternalError("Failed to get transmission: ${it.message}") }

                    if (transmission == null) {
                        call.respond(HttpStatusCode.NotFound, mapOf("message" to "No Peppol transmission found for this invoice"))
                    } else {
                        call.respond(HttpStatusCode.OK, transmission)
                    }
                }
            }
        }
    }
}

// Request/Response DTOs
@Serializable
private data class VerifyRecipientRequest(val peppolId: String)

@Serializable
private data class SendInvoiceResponse(
    val success: Boolean,
    val transmissionId: String,
    val status: String,
    val externalDocumentId: String? = null,
    val errorMessage: String? = null
)
