package tech.dokus.peppol.service

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.days
import tech.dokus.database.repository.peppol.PeppolSettingsRepository
import tech.dokus.database.repository.peppol.PeppolTransmissionRepository
import tech.dokus.domain.enums.PeppolDocumentType
import tech.dokus.domain.enums.PeppolStatus
import tech.dokus.domain.enums.PeppolTransmissionDirection
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.InvoiceId
import tech.dokus.domain.ids.PeppolId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.Address
import tech.dokus.domain.model.ExtractedDocumentData
import tech.dokus.domain.model.FinancialDocumentDto
import tech.dokus.domain.model.PeppolInboxPollResponse
import tech.dokus.domain.model.PeppolSettingsDto
import tech.dokus.domain.model.PeppolTransmissionDto
import tech.dokus.domain.model.PeppolValidationResult
import tech.dokus.domain.model.ProcessedPeppolDocument
import tech.dokus.domain.model.SendInvoiceViaPeppolResponse
import tech.dokus.domain.model.Tenant
import tech.dokus.domain.model.TenantSettings
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.domain.utils.json
import tech.dokus.foundation.backend.utils.loggerFor
import tech.dokus.peppol.mapper.PeppolMapper
import tech.dokus.peppol.model.PeppolDirection
import tech.dokus.peppol.model.PeppolDocumentSummary
import tech.dokus.peppol.model.PeppolInboxItem
import tech.dokus.peppol.model.PeppolVerifyResponse
import tech.dokus.peppol.provider.PeppolProvider
import tech.dokus.peppol.provider.PeppolProviderFactory
import tech.dokus.peppol.provider.client.RecommandProvider
import tech.dokus.peppol.provider.client.recommand.model.RecommandDocumentDetail
import tech.dokus.peppol.validator.PeppolValidator

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
    private val validator: PeppolValidator,
    private val credentialResolver: PeppolCredentialResolver
) {
    private val logger = loggerFor()

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
     * NOTE: recipientPeppolId should be resolved via PeppolRecipientResolver before calling this.
     */
    suspend fun validateInvoice(
        invoice: FinancialDocumentDto.InvoiceDto,
        contact: ContactDto,
        tenant: Tenant,
        companyAddress: Address?,
        tenantSettings: TenantSettings,
        tenantId: TenantId,
        recipientPeppolId: String?
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
                peppolSettings,
                recipientPeppolId
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
     * NOTE: recipientPeppolId should be resolved via PeppolRecipientResolver before calling this.
     */
    suspend fun sendInvoice(
        invoice: FinancialDocumentDto.InvoiceDto,
        contact: ContactDto,
        tenant: Tenant,
        companyAddress: Address?,
        tenantSettings: TenantSettings,
        tenantId: TenantId,
        recipientPeppolId: String
    ): Result<SendInvoiceViaPeppolResponse> {
        logger.info("Sending invoice ${invoice.id} via Peppol for tenant: $tenantId")

        return runCatching {
            // Get settings and create provider using centralized credential resolver
            val peppolSettings = settingsRepository.getSettings(tenantId).getOrThrow()
                ?: throw IllegalStateException("Peppol settings not configured for tenant: $tenantId")

            val resolvedCredentials = credentialResolver.resolve(tenantId)
            val provider = providerFactory.createProvider(resolvedCredentials)

            // Validate
            val validationResult = validator.validateForSending(
                invoice,
                contact,
                tenant,
                companyAddress,
                tenantSettings,
                peppolSettings,
                recipientPeppolId
            )

            if (!validationResult.isValid) {
                val errorMessages = validationResult.errors.joinToString("; ") { it.message }
                throw IllegalArgumentException("Invoice validation failed: $errorMessages")
            }

            // Create transmission record (recipientPeppolId passed as parameter)
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
                    companyAddress,
                    recipientPeppolId
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
     *
     * Creates Documents with Drafts for user review (architectural boundary).
     * Bills are created only when the user confirms the draft.
     *
     * Full sync is performed on first connection (lastFullSyncAt is null) or weekly (> 7 days).
     * Full sync fetches ALL documents via /documents endpoint.
     * Normal polling fetches only unread documents via /inbox endpoint.
     *
     * @param tenantId The tenant to poll for
     * @param createDocumentCallback Callback to create document with:
     *   - ExtractedDocumentData: parsed invoice/bill data
     *   - String: sender Peppol ID
     *   - TenantId: tenant ID
     *   - RecommandDocumentDetail?: raw document detail with attachments (Recommand only)
     */
    suspend fun pollInbox(
        tenantId: TenantId,
        createDocumentCallback: suspend (ExtractedDocumentData, String, TenantId, RecommandDocumentDetail?) -> Result<DocumentId>
    ): Result<PeppolInboxPollResponse> {
        logger.info("Polling Peppol inbox for tenant: $tenantId")

        return runCatching {
            val provider = createProviderForTenant(tenantId)
            val settings = settingsRepository.getSettings(tenantId).getOrThrow()
                ?: throw IllegalStateException("Peppol settings not configured for tenant: $tenantId")

            // Check if full sync needed (first time or > 7 days since last)
            val lastFullSync = settings.lastFullSyncAt
            val needsFullSync = lastFullSync == null || isOlderThan(lastFullSync, 7.days)

            // Fetch documents based on sync mode
            val inboxItems = if (needsFullSync) {
                // Full sync: get ALL incoming documents via /documents endpoint
                logger.info("Performing full sync for tenant: $tenantId (lastFullSyncAt: $lastFullSync)")
                fetchAllIncomingDocuments(provider, settings.peppolId)
            } else {
                // Normal polling: only unread via /inbox
                logger.info("Performing normal poll for tenant: $tenantId")
                provider.getInbox().getOrThrow()
            }

            logger.info("Found ${inboxItems.size} documents to process")

            val processedDocuments = mutableListOf<ProcessedPeppolDocument>()

            for (inboxItem in inboxItems) {
                try {
                    // Dedupe: avoid re-importing documents during weekly/full sync.
                    // Use the provider document id as stable externalDocumentId.
                    val alreadyImported = transmissionRepository
                        .existsByExternalDocumentId(tenantId, inboxItem.id)
                        .getOrThrow()
                    if (alreadyImported) {
                        logger.debug("Skipping already-imported Peppol document {}", inboxItem.id)
                        provider.markAsRead(inboxItem.id).getOrNull() // Best-effort
                        continue
                    }

                    // Validate incoming document
                    val validationResult =
                        validator.validateIncoming(inboxItem.id, inboxItem.senderPeppolId)
                    if (!validationResult.isValid) {
                        logger.warn("Skipping invalid incoming document ${inboxItem.id}: ${validationResult.errors}")
                        continue
                    }

                    // Fetch full document content
                    val fullDocument = provider.getDocument(inboxItem.id).getOrThrow()

                    // Fetch raw document detail for attachment extraction (Recommand-specific)
                    val rawDetail = (provider as? RecommandProvider)
                        ?.getDocumentDetail(inboxItem.id)
                        ?.getOrNull()

                    // Create transmission record
                    val peppolDocumentType = PeppolDocumentType.fromApiValue(inboxItem.documentType)
                    val transmission = transmissionRepository.createTransmission(
                        tenantId = tenantId,
                        direction = PeppolTransmissionDirection.Inbound,
                        documentType = peppolDocumentType,
                        externalDocumentId = inboxItem.id,
                        senderPeppolId = PeppolId(inboxItem.senderPeppolId),
                    ).getOrThrow()

                    // Convert to extracted data (for draft)
                    val extractedData = mapper.toExtractedDocumentData(fullDocument, inboxItem.senderPeppolId)

                    // Create document + draft via callback
                    val documentId = createDocumentCallback(
                        extractedData,
                        inboxItem.senderPeppolId,
                        tenantId,
                        rawDetail
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
                        .getOrNull() // Ignore errors on marking as read

                    processedDocuments.add(
                        ProcessedPeppolDocument(
                            transmissionId = transmission.id,
                            documentId = documentId,
                            senderPeppolId = PeppolId(inboxItem.senderPeppolId),
                            invoiceNumber = extractedData.bill?.invoiceNumber,
                            totalAmount = extractedData.bill?.amount,
                            receivedAt = now
                        )
                    )

                    logger.info(
                        "Processed incoming Peppol document ${inboxItem.id} -> Document $documentId (needs review)"
                    )
                } catch (e: Exception) {
                    logger.error("Failed to process incoming document ${inboxItem.id}", e)
                    // Continue processing other documents
                }
            }

            // Update lastFullSyncAt after successful full sync
            if (needsFullSync) {
                settingsRepository.updateLastFullSyncAt(tenantId).getOrThrow()
                logger.info("Updated lastFullSyncAt for tenant: $tenantId")
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

    /**
     * Fetch ALL incoming documents via /documents endpoint with pagination.
     * Used for full sync (first connection and weekly).
     */
    private suspend fun fetchAllIncomingDocuments(
        provider: PeppolProvider,
        receiverPeppolId: PeppolId
    ): List<PeppolInboxItem> {
        val allDocs = mutableListOf<PeppolInboxItem>()
        var offset = 0
        val limit = 100

        do {
            val batch = provider.listDocuments(
                direction = PeppolDirection.INBOUND,
                limit = limit,
                offset = offset,
                isUnread = null // Get ALL documents (both read and unread)
            ).getOrThrow()

            allDocs.addAll(batch.documents.map { it.toPeppolInboxItem(receiverPeppolId) })
            offset += limit
            logger.debug("Full sync: fetched ${allDocs.size} documents so far (hasMore: ${batch.hasMore})")
        } while (batch.hasMore)

        logger.info("Full sync completed: found ${allDocs.size} total incoming documents")
        return allDocs
    }

    /**
     * Check if a datetime is older than the specified duration.
     */
    private fun isOlderThan(dateTime: LocalDateTime, duration: kotlin.time.Duration): Boolean {
        val threshold = Clock.System.now()
            .minus(duration)
            .toLocalDateTime(TimeZone.UTC)
        return dateTime < threshold
    }

    /**
     * Convert a PeppolDocumentSummary (from /documents) to PeppolInboxItem.
     */
    private fun PeppolDocumentSummary.toPeppolInboxItem(receiverPeppolId: PeppolId) = PeppolInboxItem(
        id = id,
        documentType = documentType,
        senderPeppolId = counterpartyPeppolId,
        receiverPeppolId = receiverPeppolId.value,
        receivedAt = createdAt,
        isRead = readAt != null
    )

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
     * Create a provider for a tenant using the centralized credential resolver.
     * Handles both cloud (master creds) and self-hosted (per-tenant creds) automatically.
     */
    private suspend fun createProviderForTenant(tenantId: TenantId): PeppolProvider {
        val resolvedCredentials = credentialResolver.resolve(tenantId)
        return providerFactory.createProvider(resolvedCredentials)
    }
}
