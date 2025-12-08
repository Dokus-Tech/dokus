package ai.dokus.cashflow.backend.routes

import ai.dokus.cashflow.backend.service.BillService
import ai.dokus.cashflow.backend.service.InvoiceService
import ai.dokus.foundation.database.repository.auth.TenantRepository
import ai.dokus.foundation.domain.enums.PeppolStatus
import ai.dokus.foundation.domain.enums.PeppolTransmissionDirection
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.ids.InvoiceId
import ai.dokus.foundation.domain.model.SavePeppolSettingsRequest
import ai.dokus.foundation.ktor.security.authenticateJwt
import ai.dokus.foundation.ktor.security.dokusPrincipal
import ai.dokus.peppol.service.PeppolService
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
 * Provides endpoints for:
 * - Settings management (CRUD)
 * - Recipient verification
 * - Invoice sending via Peppol
 * - Inbox polling for received documents
 * - Transmission history
 */
@OptIn(ExperimentalUuidApi::class)
fun Route.peppolRoutes() {
    val peppolService by inject<PeppolService>()
    val invoiceService by inject<InvoiceService>()
    val billService by inject<BillService>()
    val tenantRepository by inject<TenantRepository>()

    route("/api/v1/peppol") {
        authenticateJwt {

            // ================================================================
            // PROVIDER INFO
            // ================================================================

            /**
             * GET /api/v1/peppol/providers
             * List available Peppol providers.
             */
            get("/providers") {
                val providers = peppolService.getAvailableProviders()
                call.respond(HttpStatusCode.OK, ProvidersResponse(providers))
            }

            // ================================================================
            // SETTINGS
            // ================================================================

            route("/settings") {
                /**
                 * GET /api/v1/peppol/settings
                 * Get Peppol settings for current tenant.
                 */
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

                /**
                 * PUT /api/v1/peppol/settings
                 * Save Peppol settings for current tenant.
                 */
                put {
                    val tenantId = dokusPrincipal.requireTenantId()
                    val request = call.receive<SavePeppolSettingsRequest>()

                    val settings = peppolService.saveSettings(tenantId, request)
                        .getOrElse { throw DokusException.InternalError("Failed to save Peppol settings: ${it.message}") }

                    call.respond(HttpStatusCode.OK, settings)
                }

                /**
                 * DELETE /api/v1/peppol/settings
                 * Delete Peppol settings for current tenant.
                 */
                delete {
                    val tenantId = dokusPrincipal.requireTenantId()

                    peppolService.deleteSettings(tenantId)
                        .getOrElse { throw DokusException.InternalError("Failed to delete Peppol settings: ${it.message}") }

                    call.respond(HttpStatusCode.NoContent)
                }

                /**
                 * POST /api/v1/peppol/settings/test
                 * Test connection with current credentials.
                 */
                post("/test") {
                    val tenantId = dokusPrincipal.requireTenantId()

                    val success = peppolService.testConnection(tenantId)
                        .getOrElse { throw DokusException.InternalError("Failed to test connection: ${it.message}") }

                    call.respond(HttpStatusCode.OK, TestConnectionResponse(success))
                }
            }

            // ================================================================
            // VERIFICATION
            // ================================================================

            /**
             * POST /api/v1/peppol/verify
             * Verify if a recipient is registered on the Peppol network.
             */
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
                /**
                 * POST /api/v1/peppol/send/invoice/{invoiceId}
                 * Send an invoice via Peppol.
                 *
                 * Note: This endpoint requires client data which is not yet fully integrated.
                 * The invoice must have a clientId that matches a client with a valid peppolId.
                 */
                post("/invoice/{invoiceId}") {
                    val tenantId = dokusPrincipal.requireTenantId()
                    val invoiceIdStr = call.parameters["invoiceId"]
                        ?: throw DokusException.BadRequest("Invoice ID is required")

                    val invoiceId = InvoiceId(Uuid.parse(invoiceIdStr))

                    // Fetch invoice
                    val invoice = invoiceService.getInvoice(invoiceId, tenantId)
                        .getOrElse { throw DokusException.InternalError("Failed to fetch invoice: ${it.message}") }
                        ?: throw DokusException.NotFound("Invoice not found")

                    // Get tenant settings
                    val tenantSettings = tenantRepository.getSettings(tenantId)
                        ?: throw DokusException.InternalError("Tenant settings not found")

                    // TODO: Get client from a ClientRepository when it's implemented
                    // For now, this endpoint returns an error indicating the feature is not fully implemented
                    throw DokusException.NotImplemented(
                        "Sending invoices via Peppol requires client data. " +
                        "This feature is pending ClientRepository integration."
                    )
                }

                /**
                 * POST /api/v1/peppol/send/validate/{invoiceId}
                 * Validate an invoice for Peppol without sending.
                 */
                post("/validate/{invoiceId}") {
                    val tenantId = dokusPrincipal.requireTenantId()
                    val invoiceIdStr = call.parameters["invoiceId"]
                        ?: throw DokusException.BadRequest("Invoice ID is required")

                    val invoiceId = InvoiceId(Uuid.parse(invoiceIdStr))

                    // Fetch invoice
                    val invoice = invoiceService.getInvoice(invoiceId, tenantId)
                        .getOrElse { throw DokusException.InternalError("Failed to fetch invoice: ${it.message}") }
                        ?: throw DokusException.NotFound("Invoice not found")

                    // Get tenant settings
                    val tenantSettings = tenantRepository.getSettings(tenantId)
                        ?: throw DokusException.InternalError("Tenant settings not found")

                    // TODO: Get client from a ClientRepository when it's implemented
                    throw DokusException.NotImplemented(
                        "Validating invoices for Peppol requires client data. " +
                        "This feature is pending ClientRepository integration."
                    )
                }
            }

            // ================================================================
            // INBOUND - POLLING INBOX
            // ================================================================

            /**
             * POST /api/v1/peppol/inbox/poll
             * Poll inbox for new documents.
             *
             * Note: This endpoint requires createBillFromPeppol in BillService which is not yet implemented.
             */
            post("/inbox/poll") {
                // TODO: Implement bill creation from Peppol documents
                // This requires a createBillFromPeppol method in BillService
                throw DokusException.NotImplemented(
                    "Polling Peppol inbox requires bill creation from Peppol documents. " +
                    "This feature is pending BillService.createBillFromPeppol integration."
                )
            }

            // ================================================================
            // TRANSMISSION HISTORY
            // ================================================================

            route("/transmissions") {
                /**
                 * GET /api/v1/peppol/transmissions
                 * List transmission history.
                 */
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

                /**
                 * GET /api/v1/peppol/transmissions/invoice/{invoiceId}
                 * Get transmission for a specific invoice.
                 */
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
private data class ProvidersResponse(val providers: List<String>)

@Serializable
private data class VerifyRecipientRequest(val peppolId: String)

@Serializable
private data class TestConnectionResponse(val success: Boolean)

@Serializable
private data class SendInvoiceResponse(
    val success: Boolean,
    val transmissionId: String,
    val status: String,
    val externalDocumentId: String? = null,
    val errorMessage: String? = null
)
