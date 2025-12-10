package ai.dokus.peppol.backend.service

import ai.dokus.foundation.database.repository.auth.TenantRepository
import ai.dokus.foundation.database.repository.cashflow.BillRepository
import ai.dokus.foundation.database.repository.cashflow.ClientRepository
import ai.dokus.foundation.database.repository.cashflow.InvoiceRepository
import ai.dokus.foundation.database.repository.peppol.PeppolSettingsRepository
import ai.dokus.foundation.database.repository.peppol.PeppolTransmissionRepository
import ai.dokus.foundation.domain.enums.PeppolDocumentType
import ai.dokus.foundation.domain.enums.PeppolStatus
import ai.dokus.foundation.domain.enums.PeppolTransmissionDirection
import ai.dokus.foundation.domain.ids.ClientId
import ai.dokus.foundation.domain.ids.InvoiceId
import ai.dokus.foundation.domain.ids.PeppolId
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.model.ClientDto
import ai.dokus.foundation.domain.model.CreateBillRequest
import ai.dokus.foundation.domain.model.FinancialDocumentDto
import ai.dokus.foundation.domain.model.PeppolInboxPollResponse
import ai.dokus.foundation.domain.model.PeppolSettingsDto
import ai.dokus.foundation.domain.model.PeppolTransmissionDto
import ai.dokus.foundation.domain.model.PeppolValidationResult
import ai.dokus.foundation.domain.model.ProcessedPeppolDocument
import ai.dokus.foundation.domain.model.RecommandVerifyResponse
import ai.dokus.foundation.domain.model.SavePeppolSettingsRequest
import ai.dokus.foundation.domain.model.SendInvoiceViaPeppolResponse
import ai.dokus.foundation.domain.model.TenantSettings
import ai.dokus.peppol.backend.client.RecommandClient
import ai.dokus.peppol.backend.mapper.PeppolMapper
import ai.dokus.peppol.backend.validator.PeppolValidator
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

/**
 * Service for Peppol e-invoicing operations.
 *
 * Handles:
 * - Outbound: Sending invoices via Peppol network
 * - Inbound: Polling and processing received documents
 * - Settings: Managing tenant Peppol credentials
 * - Validation: Validating documents against Peppol standards
 *
 * Now uses repositories directly instead of inter-service HTTP calls.
 */
class PeppolService(
    private val settingsRepository: PeppolSettingsRepository,
    private val transmissionRepository: PeppolTransmissionRepository,
    private val clientRepository: ClientRepository,
    private val invoiceRepository: InvoiceRepository,
    private val billRepository: BillRepository,
    private val tenantRepository: TenantRepository,
    private val recommandClient: RecommandClient,
    private val mapper: PeppolMapper,
    private val validator: PeppolValidator
) {
    private val logger = LoggerFactory.getLogger(PeppolService::class.java)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // ========================================================================
    // DATA ACCESS - Direct repository access
    // ========================================================================

    /**
     * Get an invoice by ID.
     */
    suspend fun getInvoice(invoiceId: InvoiceId, tenantId: TenantId): Result<FinancialDocumentDto.InvoiceDto?> {
        return invoiceRepository.getInvoice(invoiceId, tenantId)
    }

    /**
     * Get a client by ID.
     */
    suspend fun getClient(clientId: ClientId, tenantId: TenantId): Result<ClientDto?> {
        return clientRepository.getClient(clientId, tenantId)
    }

    /**
     * Get tenant settings.
     */
    suspend fun getTenantSettings(tenantId: TenantId): TenantSettings? {
        return tenantRepository.getSettings(tenantId)
    }

    /**
     * Create a bill from a Peppol document.
     */
    suspend fun createBill(request: CreateBillRequest, tenantId: TenantId): Result<FinancialDocumentDto.BillDto> {
        return billRepository.createBill(tenantId, request)
    }

    // ========================================================================
    // SETTINGS MANAGEMENT
    // ========================================================================

    /**
     * Get Peppol settings for a tenant.
     */
    suspend fun getSettings(tenantId: TenantId): Result<PeppolSettingsDto?> {
        logger.debug("Getting Peppol settings for tenant: $tenantId")
        return settingsRepository.getSettings(tenantId)
    }

    /**
     * Save Peppol settings for a tenant.
     */
    suspend fun saveSettings(
        tenantId: TenantId,
        request: SavePeppolSettingsRequest
    ): Result<PeppolSettingsDto> {
        logger.info("Saving Peppol settings for tenant: $tenantId")
        return settingsRepository.saveSettings(tenantId, request)
            .onSuccess { logger.info("Peppol settings saved for tenant: $tenantId") }
            .onFailure { logger.error("Failed to save Peppol settings for tenant: $tenantId", it) }
    }

    /**
     * Delete Peppol settings for a tenant.
     */
    suspend fun deleteSettings(tenantId: TenantId): Result<Boolean> {
        logger.info("Deleting Peppol settings for tenant: $tenantId")
        return settingsRepository.deleteSettings(tenantId)
            .onSuccess { logger.info("Peppol settings deleted for tenant: $tenantId") }
            .onFailure { logger.error("Failed to delete Peppol settings for tenant: $tenantId", it) }
    }

    /**
     * Test connection with current credentials.
     */
    suspend fun testConnection(tenantId: TenantId): Result<Boolean> {
        logger.debug("Testing Peppol connection for tenant: $tenantId")
        return runCatching {
            val credentials = settingsRepository.getSettingsWithCredentials(tenantId).getOrThrow()
                ?: throw IllegalStateException("Peppol settings not configured for tenant: $tenantId")

            // Test by verifying our own Peppol ID
            recommandClient.verifyRecipient(
                companyId = credentials.settings.companyId,
                apiKey = credentials.apiKey,
                apiSecret = credentials.apiSecret,
                participantId = credentials.settings.peppolId.value
            ).isSuccess
        }
    }

    // ========================================================================
    // VALIDATION
    // ========================================================================

    /**
     * Validate an invoice for Peppol sending without actually sending it.
     */
    suspend fun validateInvoice(
        invoice: FinancialDocumentDto.InvoiceDto,
        client: ClientDto,
        tenantSettings: TenantSettings,
        tenantId: TenantId
    ): Result<PeppolValidationResult> {
        logger.debug("Validating invoice ${invoice.id} for Peppol sending")

        return runCatching {
            val peppolSettings = settingsRepository.getSettings(tenantId).getOrThrow()
                ?: throw IllegalStateException("Peppol settings not configured for tenant: $tenantId")

            validator.validateForSending(invoice, client, tenantSettings, peppolSettings)
        }
    }

    /**
     * Verify if a recipient is registered on the Peppol network.
     */
    suspend fun verifyRecipient(
        tenantId: TenantId,
        peppolId: String
    ): Result<RecommandVerifyResponse> {
        logger.debug("Verifying Peppol recipient: $peppolId")

        return runCatching {
            val credentials = settingsRepository.getSettingsWithCredentials(tenantId).getOrThrow()
                ?: throw IllegalStateException("Peppol settings not configured for tenant: $tenantId")

            recommandClient.verifyRecipient(
                companyId = credentials.settings.companyId,
                apiKey = credentials.apiKey,
                apiSecret = credentials.apiSecret,
                participantId = peppolId
            ).getOrThrow()
        }
    }

    // ========================================================================
    // OUTBOUND: SENDING INVOICES
    // ========================================================================

    /**
     * Send an invoice via Peppol.
     *
     * @param invoice The invoice to send
     * @param client The client (buyer) information
     * @param tenantSettings The tenant settings (seller information)
     * @param tenantId The tenant ID
     * @return The send response with transmission ID and status
     */
    suspend fun sendInvoice(
        invoice: FinancialDocumentDto.InvoiceDto,
        client: ClientDto,
        tenantSettings: TenantSettings,
        tenantId: TenantId
    ): Result<SendInvoiceViaPeppolResponse> {
        logger.info("Sending invoice ${invoice.id} via Peppol for tenant: $tenantId")

        return runCatching {
            // Get credentials
            val credentials = settingsRepository.getSettingsWithCredentials(tenantId).getOrThrow()
                ?: throw IllegalStateException("Peppol settings not configured for tenant: $tenantId")

            // Validate
            val validationResult = validator.validateForSending(
                invoice, client, tenantSettings, credentials.settings
            )

            if (!validationResult.isValid) {
                val errorMessages = validationResult.errors.joinToString("; ") { it.message }
                throw IllegalArgumentException("Invoice validation failed: $errorMessages")
            }

            // Create transmission record
            val recipientPeppolId = client.peppolId
                ?: throw IllegalArgumentException("Client must have a Peppol ID")

            val transmission = transmissionRepository.createTransmission(
                tenantId = tenantId,
                direction = PeppolTransmissionDirection.Outbound,
                documentType = PeppolDocumentType.Invoice,
                invoiceId = invoice.id,
                recipientPeppolId = PeppolId(recipientPeppolId)
            ).getOrThrow()

            // Build request
            val request = mapper.toRecommandSendRequest(
                invoice, client, tenantSettings, credentials.settings
            )

            val rawRequest = recommandClient.serializeRequest(request)

            // Send to Recommand
            val response = recommandClient.sendDocument(
                companyId = credentials.settings.companyId,
                apiKey = credentials.apiKey,
                apiSecret = credentials.apiSecret,
                request = request
            ).getOrThrow()

            val rawResponse = json.encodeToString(response)
            val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

            // Update transmission with result
            val status = if (response.success) PeppolStatus.Sent else PeppolStatus.Failed
            val errorMessage = if (!response.success) {
                response.errors?.joinToString("; ") { it.message } ?: response.message
            } else null

            transmissionRepository.updateTransmissionResult(
                transmissionId = transmission.id,
                tenantId = tenantId,
                status = status,
                externalDocumentId = response.documentId,
                errorMessage = errorMessage,
                rawRequest = rawRequest,
                rawResponse = rawResponse,
                transmittedAt = if (response.success) now else null
            ).getOrThrow()

            SendInvoiceViaPeppolResponse(
                transmissionId = transmission.id,
                status = status,
                externalDocumentId = response.documentId,
                errorMessage = errorMessage
            )
        }.onSuccess {
            logger.info("Invoice ${invoice.id} sent via Peppol. Status: ${it.status}")
        }.onFailure {
            logger.error("Failed to send invoice via Peppol", it)
        }
    }

    // ========================================================================
    // INBOUND: POLLING INBOX
    // ========================================================================

    /**
     * Poll the Peppol inbox for new documents.
     * Creates bills directly using billRepository instead of callback.
     *
     * @param tenantId The tenant ID
     * @return Poll response with processed documents
     */
    suspend fun pollInbox(tenantId: TenantId): Result<PeppolInboxPollResponse> {
        logger.info("Polling Peppol inbox for tenant: $tenantId")

        return runCatching {
            val credentials = settingsRepository.getSettingsWithCredentials(tenantId).getOrThrow()
                ?: throw IllegalStateException("Peppol settings not configured for tenant: $tenantId")

            // Fetch inbox
            val inboxDocuments = recommandClient.getInbox(
                companyId = credentials.settings.companyId,
                apiKey = credentials.apiKey,
                apiSecret = credentials.apiSecret
            ).getOrThrow()

            logger.info("Found ${inboxDocuments.size} documents in Peppol inbox")

            val processedDocuments = mutableListOf<ProcessedPeppolDocument>()

            for (inboxDoc in inboxDocuments) {
                try {
                    // Validate incoming document
                    val validationResult = validator.validateIncoming(inboxDoc.id, inboxDoc.sender)
                    if (!validationResult.isValid) {
                        logger.warn("Skipping invalid incoming document ${inboxDoc.id}: ${validationResult.errors}")
                        continue
                    }

                    // Fetch full document content
                    val fullDocument = recommandClient.getDocument(
                        companyId = credentials.settings.companyId,
                        apiKey = credentials.apiKey,
                        apiSecret = credentials.apiSecret,
                        documentId = inboxDoc.id
                    ).getOrThrow()

                    // Create transmission record
                    val transmission = transmissionRepository.createTransmission(
                        tenantId = tenantId,
                        direction = PeppolTransmissionDirection.Inbound,
                        documentType = PeppolDocumentType.Invoice,
                        senderPeppolId = PeppolId(inboxDoc.sender)
                    ).getOrThrow()

                    // Convert to bill request
                    val createBillRequest = fullDocument.document?.let {
                        mapper.toCreateBillRequest(it, inboxDoc.sender)
                    } ?: throw IllegalStateException("Document content is missing")

                    // Create bill directly using repository
                    val bill = billRepository.createBill(tenantId, createBillRequest).getOrThrow()

                    // Link bill to transmission
                    transmissionRepository.linkBillToTransmission(
                        transmissionId = transmission.id,
                        tenantId = tenantId,
                        billId = bill.id
                    ).getOrThrow()

                    // Update transmission status
                    val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
                    transmissionRepository.updateTransmissionResult(
                        transmissionId = transmission.id,
                        tenantId = tenantId,
                        status = PeppolStatus.Delivered,
                        externalDocumentId = inboxDoc.id,
                        transmittedAt = now
                    ).getOrThrow()

                    // Mark as read in Recommand
                    recommandClient.markAsRead(
                        companyId = credentials.settings.companyId,
                        apiKey = credentials.apiKey,
                        apiSecret = credentials.apiSecret,
                        documentId = inboxDoc.id
                    ).getOrNull()  // Ignore errors on marking as read

                    processedDocuments.add(
                        ProcessedPeppolDocument(
                            transmissionId = transmission.id,
                            billId = bill.id,
                            senderPeppolId = PeppolId(inboxDoc.sender),
                            invoiceNumber = createBillRequest.invoiceNumber,
                            totalAmount = createBillRequest.amount,
                            receivedAt = now
                        )
                    )

                    logger.info("Processed incoming Peppol document ${inboxDoc.id} -> Bill ${bill.id}")

                } catch (e: Exception) {
                    logger.error("Failed to process incoming document ${inboxDoc.id}", e)
                    // Continue processing other documents
                }
            }

            PeppolInboxPollResponse(
                newDocuments = inboxDocuments.size,
                processedDocuments = processedDocuments
            )
        }.onSuccess {
            logger.info("Inbox poll completed. Processed ${it.processedDocuments.size} documents")
        }.onFailure {
            logger.error("Failed to poll Peppol inbox", it)
        }
    }

    // ========================================================================
    // TRANSMISSION HISTORY
    // ========================================================================

    /**
     * Get transmission history for a tenant.
     */
    suspend fun listTransmissions(
        tenantId: TenantId,
        direction: PeppolTransmissionDirection? = null,
        status: PeppolStatus? = null,
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<PeppolTransmissionDto>> {
        logger.debug("Listing Peppol transmissions for tenant: $tenantId")
        return transmissionRepository.listTransmissions(tenantId, direction, status, limit, offset)
    }

    /**
     * Get transmission for an invoice.
     */
    suspend fun getTransmissionByInvoiceId(
        invoiceId: InvoiceId,
        tenantId: TenantId
    ): Result<PeppolTransmissionDto?> {
        logger.debug("Getting Peppol transmission for invoice: $invoiceId")
        return transmissionRepository.getTransmissionByInvoiceId(invoiceId, tenantId)
    }

    /**
     * Get list of available providers.
     */
    fun getAvailableProviders(): List<String> {
        return listOf("recommand")
    }
}
