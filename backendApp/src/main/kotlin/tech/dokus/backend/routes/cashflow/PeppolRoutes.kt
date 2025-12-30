package tech.dokus.backend.routes.cashflow

import tech.dokus.database.repository.auth.AddressRepository
import tech.dokus.database.repository.auth.TenantRepository
import tech.dokus.database.repository.contacts.ContactRepository
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.InvoiceId
import tech.dokus.domain.model.PeppolConnectRequest
import tech.dokus.domain.model.SavePeppolSettingsRequest
import tech.dokus.domain.routes.Peppol
import tech.dokus.foundation.backend.security.authenticateJwt
import tech.dokus.foundation.backend.security.dokusPrincipal
import tech.dokus.peppol.service.PeppolConnectionService
import tech.dokus.peppol.service.PeppolService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.resources.delete
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.resources.put
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject
import tech.dokus.backend.services.cashflow.BillService
import tech.dokus.backend.services.cashflow.InvoiceService
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Peppol API Routes using Ktor Type-Safe Routing
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
internal fun Route.peppolRoutes() {
    val peppolService by inject<PeppolService>()
    val peppolConnectionService by inject<PeppolConnectionService>()
    val invoiceService by inject<InvoiceService>()
    val billService by inject<BillService>()
    val contactRepository by inject<ContactRepository>()
    val tenantRepository by inject<TenantRepository>()
    val addressRepository by inject<AddressRepository>()

    authenticateJwt {
        // ================================================================
        // PROVIDER INFO
        // ================================================================

        /**
         * GET /api/v1/peppol/providers
         * List available Peppol providers.
         */
        get<Peppol.Providers> {
            val providers = peppolService.getAvailableProviders()
            call.respond(HttpStatusCode.OK, ProvidersResponse(providers))
        }

        // ================================================================
        // SETTINGS
        // ================================================================

        /**
         * GET /api/v1/peppol/settings
         * Get Peppol settings for current tenant.
         */
        get<Peppol.Settings> {
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
        put<Peppol.Settings> {
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
        delete<Peppol.Settings> {
            val tenantId = dokusPrincipal.requireTenantId()

            peppolService.deleteSettings(tenantId)
                .getOrElse { throw DokusException.InternalError("Failed to delete Peppol settings: ${it.message}") }

            call.respond(HttpStatusCode.NoContent)
        }

        /**
         * POST /api/v1/peppol/settings/connection-tests
         * Test connection with current credentials.
         */
        post<Peppol.Settings.ConnectionTests> {
            val tenantId = dokusPrincipal.requireTenantId()

            val success = peppolService.testConnection(tenantId)
                .getOrElse { throw DokusException.InternalError("Failed to test connection: ${it.message}") }

            call.respond(HttpStatusCode.OK, TestConnectionResponse(success))
        }

        /**
         * POST /api/v1/peppol/settings/connect
         * Matches (and if needed creates) a Recommand company by tenant VAT and saves credentials only after resolution.
         */
        post<Peppol.Settings.Connect> {
            val tenantId = dokusPrincipal.requireTenantId()
            val tenant = tenantRepository.findById(tenantId)
                ?: throw DokusException.NotFound("Tenant not found")
            val companyAddress = addressRepository.getCompanyAddress(tenantId)
            val request = call.receive<PeppolConnectRequest>()

            val result = peppolConnectionService.connectRecommand(tenant, companyAddress, request)
                .getOrElse { throw DokusException.InternalError("Failed to connect Peppol: ${it.message}") }

            call.respond(HttpStatusCode.OK, result)
        }

        // ================================================================
        // VERIFICATION
        // ================================================================

        /**
         * POST /api/v1/peppol/recipient-validations
         * Verify if a recipient is registered on the Peppol network.
         */
        post<Peppol.RecipientValidations> {
            val tenantId = dokusPrincipal.requireTenantId()
            val request = call.receive<VerifyRecipientRequest>()

            val result = peppolService.verifyRecipient(tenantId, request.peppolId)
                .getOrElse { throw DokusException.InternalError("Failed to verify recipient: ${it.message}") }

            call.respond(HttpStatusCode.OK, result)
        }

        // ================================================================
        // OUTBOUND - SENDING INVOICES
        // ================================================================

        /**
         * POST /api/v1/peppol/transmissions
         * Send an invoice via Peppol (creates a transmission).
         *
         * The invoice must have a contactId that matches a contact with a valid peppolId.
         */
        post<Peppol.Transmissions> { route ->
            val tenantId = dokusPrincipal.requireTenantId()
            val invoiceIdStr = route.invoiceId
                ?: throw DokusException.BadRequest("invoiceId query parameter is required")
            val invoiceId = InvoiceId(Uuid.parse(invoiceIdStr))

            // Fetch invoice
            val invoice = invoiceService.getInvoice(invoiceId, tenantId)
                .getOrElse { throw DokusException.InternalError("Failed to fetch invoice: ${it.message}") }
                ?: throw DokusException.NotFound("Invoice not found")

            // Get tenant
            val tenant = tenantRepository.findById(tenantId)
                ?: throw DokusException.InternalError("Tenant not found")
            val companyAddress = addressRepository.getCompanyAddress(tenantId)

            // Get tenant settings
            val tenantSettings = tenantRepository.getSettings(tenantId)
                ?: throw DokusException.InternalError("Tenant settings not found")

            // Get contact (customer)
            val contact = contactRepository.getContact(invoice.contactId, tenantId)
                .getOrElse { throw DokusException.InternalError("Failed to fetch contact: ${it.message}") }
                ?: throw DokusException.NotFound("Contact not found for invoice")

            // Verify contact has Peppol enabled
            if (!contact.peppolEnabled || contact.peppolId.isNullOrBlank()) {
                throw DokusException.BadRequest(
                    "Contact '${contact.name.value}' is not configured for Peppol. " +
                    "Please enable Peppol and set a valid Peppol ID for this contact."
                )
            }

            // Send invoice via Peppol
            val result = peppolService.sendInvoice(invoice, contact, tenant, companyAddress, tenantSettings, tenantId)
                .getOrElse { throw DokusException.InternalError("Failed to send invoice via Peppol: ${it.message}") }

            call.respond(HttpStatusCode.OK, SendInvoiceResponse(
                success = true,
                transmissionId = result.transmissionId.toString(),
                status = result.status.name,
                externalDocumentId = result.externalDocumentId,
                errorMessage = result.errorMessage
            ))
        }

        /**
         * POST /api/v1/peppol/invoice-validations
         * Validate an invoice for Peppol without sending.
         */
        post<Peppol.InvoiceValidations> { route ->
            val tenantId = dokusPrincipal.requireTenantId()
            val invoiceIdStr = route.invoiceId
                ?: throw DokusException.BadRequest("invoiceId query parameter is required")
            val invoiceId = InvoiceId(Uuid.parse(invoiceIdStr))

            // Fetch invoice
            val invoice = invoiceService.getInvoice(invoiceId, tenantId)
                .getOrElse { throw DokusException.InternalError("Failed to fetch invoice: ${it.message}") }
                ?: throw DokusException.NotFound("Invoice not found")

            // Get tenant
            val tenant = tenantRepository.findById(tenantId)
                ?: throw DokusException.InternalError("Tenant not found")
            val companyAddress = addressRepository.getCompanyAddress(tenantId)

            // Get tenant settings
            val tenantSettings = tenantRepository.getSettings(tenantId)
                ?: throw DokusException.InternalError("Tenant settings not found")

            // Get contact (customer)
            val contact = contactRepository.getContact(invoice.contactId, tenantId)
                .getOrElse { throw DokusException.InternalError("Failed to fetch contact: ${it.message}") }
                ?: throw DokusException.NotFound("Contact not found for invoice")

            // Validate invoice for Peppol
            val validationResult =
                peppolService.validateInvoice(invoice, contact, tenant, companyAddress, tenantSettings, tenantId)
                .getOrElse { throw DokusException.InternalError("Failed to validate invoice: ${it.message}") }

            call.respond(HttpStatusCode.OK, validationResult)
        }

        // ================================================================
        // INBOUND - POLLING INBOX
        // ================================================================

        /**
         * POST /api/v1/peppol/inbox/syncs
         * Poll inbox for new documents.
         *
         * Polls the Peppol provider's inbox for new documents and creates
         * corresponding bills in the system.
         */
        post<Peppol.Inbox.Syncs> {
            val tenantId = dokusPrincipal.requireTenantId()

            // Poll inbox with bill creation callback
            val pollResult = peppolService.pollInbox(tenantId) { createBillRequest, tid ->
                billService.createBill(tid, createBillRequest)
            }.getOrElse { throw DokusException.InternalError("Failed to poll Peppol inbox: ${it.message}") }

            call.respond(HttpStatusCode.OK, pollResult)
        }

        // ================================================================
        // TRANSMISSION HISTORY
        // ================================================================

        /**
         * GET /api/v1/peppol/transmissions
         * List transmission history.
         */
        get<Peppol.Transmissions> { route ->
            val tenantId = dokusPrincipal.requireTenantId()

            if (route.limit < 1 || route.limit > 200) {
                throw DokusException.BadRequest("Limit must be between 1 and 200")
            }

            val transmissions = peppolService.listTransmissions(
                tenantId = tenantId,
                direction = route.direction,
                status = route.status,
                limit = route.limit,
                offset = route.offset
            ).getOrElse { throw DokusException.InternalError("Failed to list transmissions: ${it.message}") }

            call.respond(HttpStatusCode.OK, transmissions)
        }

        /**
         * GET /api/v1/peppol/transmissions/{id}
         * Get transmission by ID.
         * Note: Currently uses invoiceId from Transmissions.Id route
         */
        get<Peppol.Transmissions.Id> { route ->
            val tenantId = dokusPrincipal.requireTenantId()
            val invoiceId = InvoiceId(Uuid.parse(route.id))

            val transmission = peppolService.getTransmissionByInvoiceId(invoiceId, tenantId)
                .getOrElse { throw DokusException.InternalError("Failed to get transmission: ${it.message}") }

            if (transmission == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("message" to "Peppol transmission not found"))
            } else {
                call.respond(HttpStatusCode.OK, transmission)
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
