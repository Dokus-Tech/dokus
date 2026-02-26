package tech.dokus.backend.routes.cashflow

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject
import tech.dokus.backend.services.cashflow.InvoiceService
import tech.dokus.backend.services.peppol.PeppolRecipientResolver
import tech.dokus.backend.worker.PeppolPollingWorker
import tech.dokus.database.repository.auth.AddressRepository
import tech.dokus.database.repository.auth.TenantRepository
import tech.dokus.database.repository.cashflow.DocumentDraftRepository
import tech.dokus.database.repository.contacts.ContactRepository
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.InvoiceId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.enums.InvoiceStatus
import tech.dokus.domain.enums.Permission
import tech.dokus.domain.model.PeppolConnectStatus
import tech.dokus.domain.routes.Peppol
import tech.dokus.foundation.backend.security.authenticateJwt
import tech.dokus.foundation.backend.security.dokusPrincipal
import tech.dokus.foundation.backend.security.requirePermission
import tech.dokus.peppol.service.PeppolConnectionService
import tech.dokus.peppol.service.PeppolRegistrationService
import tech.dokus.peppol.service.PeppolService
import tech.dokus.peppol.service.PeppolVerificationService
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
    val peppolRecipientResolver by inject<PeppolRecipientResolver>()
    val peppolRegistrationService by inject<PeppolRegistrationService>()
    val peppolVerificationService by inject<PeppolVerificationService>()
    val invoiceService by inject<InvoiceService>()
    val contactRepository by inject<ContactRepository>()
    val tenantRepository by inject<TenantRepository>()
    val addressRepository by inject<AddressRepository>()
    val documentDraftRepository by inject<DocumentDraftRepository>()
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
         * Connect tenant to Peppol using master credentials from environment.
         * Auto-detects or creates Recommand company based on tenant VAT.
         */
        post<Peppol.Settings.Connect> {
            val tenantId = dokusPrincipal.requireTenantId()
            val tenant = tenantRepository.findById(tenantId)
                ?: throw DokusException.NotFound("Tenant not found")
            val companyAddress = addressRepository.getCompanyAddress(tenantId)

            val result = peppolConnectionService.connect(tenant, companyAddress)
                .getOrElse { throw DokusException.InternalError("Failed to connect Peppol: ${it.message}") }

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
            requirePermission(Permission.InvoicesEdit)
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

            // Resolve PEPPOL recipient ID (cache-first lookup)
            val (resolution, _) = peppolRecipientResolver.resolveRecipient(tenantId, invoice.contactId)
                .getOrElse { throw DokusException.InternalError("Failed to resolve PEPPOL recipient: ${it.message}") }

            val recipientPeppolId = resolution?.participantId
            if (recipientPeppolId.isNullOrBlank()) {
                throw DokusException.BadRequest(
                    "Contact '${contact.name.value}' is not registered on the PEPPOL network. " +
                        "Please verify their PEPPOL status before sending."
                )
            }

            val sourceDocumentId = invoice.documentId
            if (sourceDocumentId != null) {
                val isConfirmed = documentDraftRepository.isConfirmed(
                    tenantId = tenantId,
                    documentId = sourceDocumentId
                )
                if (!isConfirmed) {
                    throw DokusException.PeppolSendRequiresConfirmedDocument
                }
            }

            // Enqueue-only outbound flow.
            val result = peppolService.enqueueInvoiceTransmission(
                invoice,
                contact,
                tenant,
                companyAddress,
                tenantSettings,
                tenantId,
                recipientPeppolId
            ).getOrElse { error ->
                when (error) {
                    is IllegalArgumentException -> throw DokusException.BadRequest(error.message ?: "Invalid PEPPOL request")
                    else -> throw DokusException.InternalError("Failed to queue invoice via PEPPOL")
                }
            }

            call.respond(
                HttpStatusCode.Accepted,
                SendInvoiceResponse(
                    success = true,
                    transmissionId = result.transmissionId.toString(),
                    status = result.status.dbValue,
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

            // Resolve PEPPOL recipient ID (for validation)
            val (resolution, _) = peppolRecipientResolver.resolveRecipient(tenantId, invoice.contactId)
                .getOrNull() ?: Pair(null, false)
            val recipientPeppolId = resolution?.participantId

            // Validate invoice for Peppol
            val validationResult =
                peppolService.validateInvoice(
                    invoice,
                    contact,
                    tenant,
                    companyAddress,
                    tenantSettings,
                    tenantId,
                    recipientPeppolId
                )
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
         * - Auto-confirms PEPPOL documents (creates Inbound Invoices immediately)
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

        // ================================================================
        // PEPPOL REGISTRATION (Phase B State Machine)
        // ================================================================

        /**
         * GET /api/v1/peppol/registration
         * Get current PEPPOL registration status.
         */
        get<Peppol.Registration> {
            val tenantId = dokusPrincipal.requireTenantId()

            val result = peppolRegistrationService.getRegistration(tenantId)
                .getOrElse { throw DokusException.InternalError("Failed to get registration: ${it.message}") }

            if (result == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("message" to "No PEPPOL registration found"))
            } else {
                call.respond(HttpStatusCode.OK, result)
            }
        }

        /**
         * POST /api/v1/peppol/verify
         * Verify if a PEPPOL ID is available for registration.
         * Accepts a VAT number - the backend converts it to PEPPOL ID format.
         */
        post<Peppol.Verify> {
            val request = call.receive<VerifyPeppolIdRequest>()

            val result = peppolVerificationService.verify(request.vatNumber)
                .getOrElse { throw DokusException.InternalError("Failed to verify PEPPOL ID: ${it.message}") }

            call.respond(HttpStatusCode.OK, result)
        }

        /**
         * POST /api/v1/peppol/enable
         * Enable PEPPOL for the tenant.
         */
        post<Peppol.Enable> {
            val tenantId = dokusPrincipal.requireTenantId()
            val result = peppolRegistrationService.enablePeppol(tenantId)
                .getOrElse { throw DokusException.InternalError("Failed to enable PEPPOL: ${it.message}") }

            // Trigger immediate poll for initial sync if receiving is enabled
            if (result.registration.canReceive) {
                peppolPollingWorker.pollNow(tenantId)
            }

            call.respond(HttpStatusCode.OK, result)
        }

        /**
         * POST /api/v1/peppol/enable-sending-only
         * Enable PEPPOL sending only (when receiving is blocked elsewhere).
         */
        post<Peppol.EnableSendingOnly> {
            val tenantId = dokusPrincipal.requireTenantId()

            val result = peppolRegistrationService.enableSendingOnly(tenantId)
                .getOrElse { throw DokusException.InternalError("Failed to enable PEPPOL sending-only: ${it.message}") }

            call.respond(HttpStatusCode.OK, result)
        }

        /**
         * POST /api/v1/peppol/wait-for-transfer
         * Opt to wait for PEPPOL ID transfer.
         */
        post<Peppol.WaitForTransfer> {
            val tenantId = dokusPrincipal.requireTenantId()

            val result = peppolRegistrationService.waitForTransfer(tenantId)
                .getOrElse { throw DokusException.InternalError("Failed to set wait for transfer: ${it.message}") }

            call.respond(HttpStatusCode.OK, result)
        }

        /**
         * POST /api/v1/peppol/opt-out
         * Opt out of PEPPOL via Dokus.
         */
        post<Peppol.OptOut> {
            val tenantId = dokusPrincipal.requireTenantId()

            peppolRegistrationService.optOut(tenantId)
                .getOrElse { throw DokusException.InternalError("Failed to opt out: ${it.message}") }

            call.respond(HttpStatusCode.OK, mapOf("success" to true))
        }

        /**
         * POST /api/v1/peppol/poll
         * Manual poll for transfer status.
         */
        post<Peppol.Poll> {
            val tenantId = dokusPrincipal.requireTenantId()

            val result = peppolRegistrationService.pollTransferStatus(tenantId)
                .getOrElse { throw DokusException.InternalError("Failed to poll transfer status: ${it.message}") }

            // If transfer completed and receiving is now enabled, trigger initial sync
            if (result.registration.canReceive) {
                peppolPollingWorker.pollNow(tenantId)
            }

            call.respond(HttpStatusCode.OK, result)
        }
    }
}

// Request/Response DTOs

@Serializable
private data class ProvidersResponse(val providers: List<String>)

@Serializable
private data class VerifyRecipientRequest(val peppolId: String)

@Serializable
private data class VerifyPeppolIdRequest(val vatNumber: VatNumber)

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
