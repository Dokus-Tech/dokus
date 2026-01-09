package tech.dokus.backend.routes.cashflow

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
import tech.dokus.backend.services.cashflow.InvoiceService
import tech.dokus.backend.services.documents.DocumentConfirmationService
import tech.dokus.backend.worker.PeppolPollingWorker
import tech.dokus.database.repository.auth.AddressRepository
import tech.dokus.database.repository.auth.TenantRepository
import tech.dokus.database.repository.cashflow.DocumentCreatePayload
import tech.dokus.database.repository.cashflow.DocumentDraftRepository
import tech.dokus.database.repository.cashflow.DocumentRepository
import tech.dokus.database.repository.contacts.ContactRepository
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.IngestionRunId
import tech.dokus.domain.ids.InvoiceId
import tech.dokus.domain.model.PeppolConnectRequest
import tech.dokus.domain.model.PeppolConnectStatus
import tech.dokus.domain.model.SavePeppolSettingsRequest
import tech.dokus.domain.routes.Peppol
import tech.dokus.foundation.backend.security.authenticateJwt
import tech.dokus.foundation.backend.security.dokusPrincipal
import tech.dokus.peppol.policy.DocumentConfirmationPolicy
import tech.dokus.peppol.service.PeppolConnectionService
import tech.dokus.peppol.service.PeppolCredentialResolver
import tech.dokus.peppol.service.PeppolService
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
    val peppolCredentialResolver by inject<PeppolCredentialResolver>()
    val invoiceService by inject<InvoiceService>()
    val documentRepository by inject<DocumentRepository>()
    val draftRepository by inject<DocumentDraftRepository>()
    val contactRepository by inject<ContactRepository>()
    val tenantRepository by inject<TenantRepository>()
    val addressRepository by inject<AddressRepository>()
    val confirmationPolicy by inject<DocumentConfirmationPolicy>()
    val confirmationService by inject<DocumentConfirmationService>()
    val peppolPollingWorker by inject<PeppolPollingWorker>()

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
         *
         * For cloud deployments: Returns 403 Forbidden.
         * Disconnecting from Peppol is a support-only operation for cloud users
         * to avoid generating support debt from users accidentally disconnecting.
         *
         * For self-hosted deployments: Deletes settings (current behavior).
         */
        delete<Peppol.Settings> {
            val tenantId = dokusPrincipal.requireTenantId()

            // Cloud deployments: users cannot disconnect themselves
            if (peppolCredentialResolver.isManagedCredentials()) {
                throw DokusException.NotAuthorized(
                    "Disconnecting from Peppol requires support assistance for cloud-hosted accounts."
                )
            }

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
         *
         * For cloud deployments:
         * - Credentials must NOT be provided (400 Bad Request if present)
         * - Uses Dokus master credentials automatically
         *
         * For self-hosted deployments:
         * - Credentials are REQUIRED (current behavior)
         * - Matches/creates Recommand company by VAT and saves encrypted credentials
         */
        post<Peppol.Settings.Connect> {
            val tenantId = dokusPrincipal.requireTenantId()
            val tenant = tenantRepository.findById(tenantId)
                ?: throw DokusException.NotFound("Tenant not found")
            val companyAddress = addressRepository.getCompanyAddress(tenantId)
            val request = call.receive<PeppolConnectRequest>()

            val result = if (peppolCredentialResolver.isManagedCredentials()) {
                // Cloud deployment: Reject if credentials are provided
                if (request.apiKey.isNotBlank() || request.apiSecret.isNotBlank()) {
                    throw DokusException.BadRequest(
                        "Cloud deployments cannot provide API credentials. " +
                            "Peppol is managed automatically by Dokus."
                    )
                }
                peppolConnectionService.connectCloud(tenant, companyAddress)
                    .getOrElse { throw DokusException.InternalError("Failed to connect Peppol: ${it.message}") }
            } else {
                // Self-hosted deployment: Use provided credentials
                peppolConnectionService.connectRecommand(tenant, companyAddress, request)
                    .getOrElse { throw DokusException.InternalError("Failed to connect Peppol: ${it.message}") }
            }

            // Trigger immediate poll after successful connection
            if (result.status == PeppolConnectStatus.Connected) {
                peppolPollingWorker.pollNow(tenantId)
            }

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

            call.respond(
                HttpStatusCode.OK,
                SendInvoiceResponse(
                    success = true,
                    transmissionId = result.transmissionId.toString(),
                    status = result.status.name,
                    externalDocumentId = result.externalDocumentId,
                    errorMessage = result.errorMessage
                )
            )
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
         * Polls the Peppol provider's inbox for new documents and:
         * - Creates Document+Draft records
         * - Auto-confirms PEPPOL documents (creates Bills immediately)
         */
        post<Peppol.Inbox.Syncs> {
            val tenantId = dokusPrincipal.requireTenantId()

            // Use PeppolPollingWorker for consistent PDF handling
            val polled = peppolPollingWorker.pollNow(tenantId)
            if (!polled) {
                throw DokusException.BadRequest("Poll skipped - last poll was too recent")
            }
            // Return empty response - actual results come from worker
            val pollResult = tech.dokus.domain.model.PeppolInboxPollResponse(
                newDocuments = 0,
                processedDocuments = emptyList()
            )

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
