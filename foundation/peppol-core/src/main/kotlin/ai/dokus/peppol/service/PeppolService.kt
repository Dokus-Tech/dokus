package ai.dokus.peppol.service

import ai.dokus.foundation.database.repository.peppol.PeppolSettingsRepository
import ai.dokus.foundation.database.repository.peppol.PeppolSettingsWithCredentials
import ai.dokus.foundation.database.repository.peppol.PeppolTransmissionRepository
import ai.dokus.foundation.domain.enums.PeppolDocumentType
import ai.dokus.foundation.domain.enums.PeppolStatus
import ai.dokus.foundation.domain.enums.PeppolTransmissionDirection
import ai.dokus.foundation.domain.ids.InvoiceId
import ai.dokus.foundation.domain.ids.PeppolId
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.model.Address
import ai.dokus.foundation.domain.model.ContactDto
import ai.dokus.foundation.domain.model.CreateBillRequest
import ai.dokus.foundation.domain.model.FinancialDocumentDto
import ai.dokus.foundation.domain.model.PeppolInboxPollResponse
import ai.dokus.foundation.domain.model.PeppolSettingsDto
import ai.dokus.foundation.domain.model.PeppolTransmissionDto
import ai.dokus.foundation.domain.model.PeppolValidationResult
import ai.dokus.foundation.domain.model.ProcessedPeppolDocument
import ai.dokus.foundation.domain.model.SavePeppolSettingsRequest
import ai.dokus.foundation.domain.model.SendInvoiceViaPeppolResponse
import ai.dokus.foundation.domain.model.Tenant
import ai.dokus.foundation.domain.model.TenantSettings
import ai.dokus.foundation.ktor.utils.loggerFor
import ai.dokus.peppol.mapper.PeppolMapper
import ai.dokus.peppol.model.PeppolVerifyResponse
import ai.dokus.peppol.provider.PeppolProvider
import ai.dokus.peppol.provider.PeppolProviderFactory
import ai.dokus.peppol.providers.recommand.RecommandCredentials
import ai.dokus.peppol.validator.PeppolValidator
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json

/**
 * Provider-agnostic Peppol service.
 *
 * Handles all Peppol operations:
 * - Settings management (CRUD)
 * - Outbound: Sending invoices via Peppol
 * - Inbound: Polling inbox for received documents
 * - Validation: Checking documents before sending
 */
class PeppolService(
    private val settingsRepository: PeppolSettingsRepository,
    private val transmissionRepository: PeppolTransmissionRepository,
    private val providerFactory: PeppolProviderFactory,
    private val mapper: PeppolMapper,
    private val validator: PeppolValidator
) {
    private val logger = loggerFor()
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // ========================================================================
    // SETTINGS MANAGEMENT
    // ========================================================================

    /**
     * Get Peppol settings for a tenant.
     */
    suspend fun getSettings(tenantId: TenantId): Result<PeppolSettingsDto?> {
        logger.debug("Getting Peppol settings for tenant: {}", tenantId)
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
            .onFailure {
                logger.error(
                    "Failed to delete Peppol settings for tenant: $tenantId",
                    it
                )
            }
    }

    /**
     * Test connection with current credentials.
     */
    suspend fun testConnection(tenantId: TenantId): Result<Boolean> {
        logger.debug("Testing Peppol connection for tenant: $tenantId")
        return runCatching {
            val provider = createProviderForTenant(tenantId)
            provider.testConnection().getOrThrow()
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
        contact: ContactDto,
        tenant: Tenant,
        companyAddress: Address?,
        tenantSettings: TenantSettings,
        tenantId: TenantId
    ): Result<PeppolValidationResult> {
        logger.debug("Validating invoice {} for Peppol sending", invoice.id)

        return runCatching {
            val peppolSettings = settingsRepository.getSettings(tenantId).getOrThrow()
                ?: throw IllegalStateException("Peppol settings not configured for tenant: $tenantId")

            validator.validateForSending(
                invoice,
                contact,
                tenant,
                companyAddress,
                tenantSettings,
                peppolSettings
            )
        }
    }

    /**
     * Verify if a recipient is registered on the Peppol network.
     */
    suspend fun verifyRecipient(
        tenantId: TenantId,
        peppolId: String
    ): Result<PeppolVerifyResponse> {
        logger.debug("Verifying Peppol recipient: $peppolId")

        return runCatching {
            val provider = createProviderForTenant(tenantId)
            provider.verifyRecipient(peppolId).getOrThrow()
        }
    }

    // ========================================================================
    // OUTBOUND: SENDING INVOICES
    // ========================================================================

    /**
     * Send an invoice via Peppol.
     */
    suspend fun sendInvoice(
        invoice: FinancialDocumentDto.InvoiceDto,
        contact: ContactDto,
        tenant: Tenant,
        companyAddress: Address?,
        tenantSettings: TenantSettings,
        tenantId: TenantId
    ): Result<SendInvoiceViaPeppolResponse> {
        logger.info("Sending invoice ${invoice.id} via Peppol for tenant: $tenantId")

        return runCatching {
            // Get settings and create provider
            val credentials = settingsRepository.getSettingsWithCredentials(tenantId).getOrThrow()
                ?: throw IllegalStateException("Peppol settings not configured for tenant: $tenantId")

            val peppolSettings = credentials.settings
            val provider = createProvider(credentials)

            // Validate
            val validationResult = validator.validateForSending(
                invoice, contact, tenant, companyAddress, tenantSettings, peppolSettings
            )

            if (!validationResult.isValid) {
                val errorMessages = validationResult.errors.joinToString("; ") { it.message }
                throw IllegalArgumentException("Invoice validation failed: $errorMessages")
            }

            // Create transmission record
            val recipientPeppolId = contact.peppolId
                ?: throw IllegalArgumentException("Contact must have a Peppol ID")

            val transmission = transmissionRepository.createTransmission(
                tenantId = tenantId,
                direction = PeppolTransmissionDirection.Outbound,
                documentType = PeppolDocumentType.Invoice,
                invoiceId = invoice.id,
                recipientPeppolId = PeppolId(recipientPeppolId)
            ).getOrThrow()

            // Build and send request
            val sendRequest =
                mapper.toSendRequest(
                    invoice,
                    contact,
                    tenant,
                    tenantSettings,
                    peppolSettings,
                    companyAddress
                )
            val rawRequest = provider.serializeRequest(sendRequest)

            val response = provider.sendDocument(sendRequest).getOrThrow()
            val rawResponse = json.encodeToString(response)
            val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

            // Update transmission with result
            val status = if (response.success) PeppolStatus.Sent else PeppolStatus.Failed
            val errorMessage = response.errorMessage

            transmissionRepository.updateTransmissionResult(
                transmissionId = transmission.id,
                tenantId = tenantId,
                status = status,
                externalDocumentId = response.externalDocumentId,
                errorMessage = errorMessage,
                rawRequest = rawRequest,
                rawResponse = rawResponse,
                transmittedAt = if (response.success) now else null
            ).getOrThrow()

            SendInvoiceViaPeppolResponse(
                transmissionId = transmission.id,
                status = status,
                externalDocumentId = response.externalDocumentId,
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
     */
    suspend fun pollInbox(
        tenantId: TenantId,
        createBillCallback: suspend (CreateBillRequest, TenantId) -> Result<FinancialDocumentDto.BillDto>
    ): Result<PeppolInboxPollResponse> {
        logger.info("Polling Peppol inbox for tenant: $tenantId")

        return runCatching {
            val provider = createProviderForTenant(tenantId)

            // Fetch inbox
            val inboxItems = provider.getInbox().getOrThrow()
            logger.info("Found ${inboxItems.size} documents in Peppol inbox")

            val processedDocuments = mutableListOf<ProcessedPeppolDocument>()

            for (inboxItem in inboxItems) {
                try {
                    // Validate incoming document
                    val validationResult =
                        validator.validateIncoming(inboxItem.id, inboxItem.senderPeppolId)
                    if (!validationResult.isValid) {
                        logger.warn("Skipping invalid incoming document ${inboxItem.id}: ${validationResult.errors}")
                        continue
                    }

                    // Fetch full document content
                    val fullDocument = provider.getDocument(inboxItem.id).getOrThrow()

                    // Create transmission record
                    val transmission = transmissionRepository.createTransmission(
                        tenantId = tenantId,
                        direction = PeppolTransmissionDirection.Inbound,
                        documentType = PeppolDocumentType.Invoice,
                        senderPeppolId = PeppolId(inboxItem.senderPeppolId)
                    ).getOrThrow()

                    // Convert to bill request
                    val createBillRequest =
                        mapper.toCreateBillRequest(fullDocument, inboxItem.senderPeppolId)

                    // Create bill via callback
                    val bill = createBillCallback(createBillRequest, tenantId).getOrThrow()

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
                        externalDocumentId = inboxItem.id,
                        transmittedAt = now
                    ).getOrThrow()

                    // Mark as read in provider
                    provider.markAsRead(inboxItem.id)
                        .getOrNull()  // Ignore errors on marking as read

                    processedDocuments.add(
                        ProcessedPeppolDocument(
                            transmissionId = transmission.id,
                            billId = bill.id,
                            senderPeppolId = PeppolId(inboxItem.senderPeppolId),
                            invoiceNumber = createBillRequest.invoiceNumber,
                            totalAmount = createBillRequest.amount,
                            receivedAt = now
                        )
                    )

                    logger.info("Processed incoming Peppol document ${inboxItem.id} -> Bill ${bill.id}")

                } catch (e: Exception) {
                    logger.error("Failed to process incoming document ${inboxItem.id}", e)
                    // Continue processing other documents
                }
            }

            PeppolInboxPollResponse(
                newDocuments = inboxItems.size,
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
        logger.debug("Listing Peppol transmissions for tenant: {}", tenantId)
        return transmissionRepository.listTransmissions(tenantId, direction, status, limit, offset)
    }

    /**
     * Get transmission for an invoice.
     */
    suspend fun getTransmissionByInvoiceId(
        invoiceId: InvoiceId,
        tenantId: TenantId
    ): Result<PeppolTransmissionDto?> {
        logger.debug("Getting Peppol transmission for invoice: {}", invoiceId)
        return transmissionRepository.getTransmissionByInvoiceId(invoiceId, tenantId)
    }

    // ========================================================================
    // PROVIDER MANAGEMENT
    // ========================================================================

    /**
     * Get list of available providers.
     */
    fun getAvailableProviders(): List<String> {
        return providerFactory.getAvailableProviders()
    }

    /**
     * Create a provider for a tenant using their saved credentials.
     */
    private suspend fun createProviderForTenant(tenantId: TenantId): PeppolProvider {
        val credentials = settingsRepository.getSettingsWithCredentials(tenantId).getOrThrow()
            ?: throw IllegalStateException("Peppol settings not configured for tenant: $tenantId")

        return createProvider(credentials)
    }

    /**
     * Create a provider from decrypted credentials.
     */
    private fun createProvider(credentials: PeppolSettingsWithCredentials): PeppolProvider {
        // For now, we only support Recommand
        // In the future, we can read provider_id from settings
        val recommandCredentials = RecommandCredentials(
            companyId = credentials.settings.companyId,
            apiKey = credentials.apiKey,
            apiSecret = credentials.apiSecret,
            peppolId = credentials.settings.peppolId.value,
            testMode = credentials.settings.testMode
        )

        return providerFactory.createProvider(recommandCredentials)
    }
}
