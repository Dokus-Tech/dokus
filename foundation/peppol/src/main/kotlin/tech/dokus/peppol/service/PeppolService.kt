package tech.dokus.peppol.service

import kotlinx.datetime.Clock
import tech.dokus.domain.Money
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import tech.dokus.database.repository.peppol.PeppolSettingsRepository
import tech.dokus.database.repository.peppol.PeppolTransmissionInternal
import tech.dokus.database.repository.peppol.PeppolTransmissionRepository
import tech.dokus.domain.enums.PeppolDocumentType
import tech.dokus.domain.enums.PeppolStatus
import tech.dokus.domain.enums.PeppolTransmissionDirection
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.InvoiceId
import tech.dokus.domain.ids.PeppolId
import tech.dokus.domain.ids.PeppolTransmissionId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.Address
import tech.dokus.domain.model.DocumentDraftData
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
import tech.dokus.peppol.model.PeppolDocumentSummary
import tech.dokus.peppol.model.PeppolInboxItem
import tech.dokus.peppol.model.PeppolSendRequest
import tech.dokus.peppol.model.PeppolVerifyResponse
import tech.dokus.peppol.provider.PeppolProvider
import tech.dokus.peppol.provider.PeppolProviderFactory
import tech.dokus.peppol.provider.client.RecommandApiException
import tech.dokus.peppol.provider.client.RecommandProvider
import tech.dokus.peppol.provider.client.recommand.model.RecommandDocumentDetail
import tech.dokus.peppol.provider.client.recommand.model.RecommandDocumentValidationResult
import tech.dokus.peppol.validator.PeppolValidator
import java.security.MessageDigest
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

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
    private val credentialResolver: PeppolCredentialResolver,
    private val outboundErrorClassifier: PeppolOutboundErrorClassifier,
    private val transmissionStateMachine: PeppolTransmissionStateMachine
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
     * Queue an invoice for outbound PEPPOL sending (enqueue-only).
     * NOTE: recipientPeppolId should be resolved via PeppolRecipientResolver before calling this.
     */
    suspend fun enqueueInvoiceTransmission(
        invoice: FinancialDocumentDto.InvoiceDto,
        contact: ContactDto,
        tenant: Tenant,
        companyAddress: Address?,
        tenantSettings: TenantSettings,
        tenantId: TenantId,
        recipientPeppolId: String
    ): Result<SendInvoiceViaPeppolResponse> {
        logger.info("Queueing invoice {} for outbound PEPPOL send (tenant={})", invoice.id, tenantId)

        return runCatching {
            val peppolSettings = settingsRepository.getSettings(tenantId).getOrThrow()
                ?: throw IllegalStateException("Peppol settings not configured for tenant: $tenantId")

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

            val sendRequest = mapper.toSendRequest(
                invoice = invoice,
                contact = contact,
                tenant = tenant,
                tenantSettings = tenantSettings,
                peppolSettings = peppolSettings,
                companyAddress = companyAddress,
                recipientPeppolId = recipientPeppolId
            )
            val rawRequest = json.encodeToString(sendRequest)
            val idempotencyKey = buildIdempotencyKey(
                tenantId = tenantId,
                invoiceId = invoice.id,
                recipientPeppolId = recipientPeppolId,
                rawRequest = rawRequest
            )

            val transmission = transmissionRepository.upsertOutboundQueued(
                tenantId = tenantId,
                documentType = PeppolDocumentType.Invoice,
                invoiceId = invoice.id,
                recipientPeppolId = PeppolId(recipientPeppolId),
                idempotencyKey = idempotencyKey,
                rawRequest = rawRequest
            ).getOrThrow()

            SendInvoiceViaPeppolResponse(
                transmissionId = transmission.id,
                status = transmission.status,
                externalDocumentId = transmission.externalDocumentId,
                errorMessage = transmission.errorMessage
            )
        }
    }

    /**
     * Process one outbound transmission already claimed by the worker.
     * The transmission must be in SENDING state.
     */
    suspend fun processOutboundTransmission(transmission: PeppolTransmissionInternal): Result<PeppolStatus> {
        return runCatching {
            if (transmission.direction != PeppolTransmissionDirection.Outbound) {
                throw IllegalArgumentException("Transmission ${transmission.id} is not outbound")
            }
            if (transmission.status != PeppolStatus.Sending) {
                throw IllegalStateException("Transmission ${transmission.id} is not in SENDING state")
            }

            val rawRequest = transmission.rawRequest
                ?: throw IllegalStateException("Missing outbound raw request payload for ${transmission.id}")
            val sendRequest = json.decodeFromString<PeppolSendRequest>(rawRequest)

            val provider = createProviderForTenant(transmission.tenantId)
            val sendResponse = provider.sendDocument(sendRequest, transmission.idempotencyKey).getOrThrow()
            val rawResponse = json.encodeToString(sendResponse)

            if (sendResponse.success) {
                val updated = transmissionRepository.markOutboundSent(
                    transmissionId = transmission.id,
                    tenantId = transmission.tenantId,
                    externalDocumentId = sendResponse.externalDocumentId,
                    rawResponse = rawResponse,
                    transmittedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
                ).getOrThrow()
                if (!updated) {
                    throw IllegalStateException("Failed to mark transmission ${transmission.id} as SENT")
                }
                PeppolStatus.Sent
            } else {
                val failure = OutboundFailureClassification(
                    retryable = false,
                    errorCode = "PROVIDER_REJECTED",
                    humanMessage = sendResponse.errorMessage ?: "Provider rejected outbound transmission"
                )
                persistOutboundFailure(
                    transmission = transmission,
                    failure = failure,
                    rawResponse = rawResponse
                )
            }
        }.recoverCatching { throwable ->
            val failure = outboundErrorClassifier.classify(throwable)
            persistOutboundFailure(
                transmission = transmission,
                failure = failure,
                rawResponse = (throwable as? RecommandApiException)?.responseBody
            )
        }
    }

    suspend fun reconcileOutboundByExternalDocumentId(
        tenantId: TenantId,
        externalDocumentId: String
    ): Result<Boolean> = runCatching {
        val transmission = transmissionRepository.getOutboundByExternalDocumentIdInternal(tenantId, externalDocumentId)
            .getOrThrow()
            ?: return@runCatching false

        reconcileOutboundTransmission(transmission)
    }

    suspend fun reconcileStaleOutbound(
        olderThan: LocalDateTime,
        limit: Int = 100
    ): Result<Int> = runCatching {
        val candidates = transmissionRepository.listOutboundForReconciliation(
            olderThan = olderThan,
            limit = limit
        ).getOrThrow()

        var reconciled = 0
        for (candidate in candidates) {
            if (reconcileOutboundTransmission(candidate)) {
                reconciled++
            }
        }
        reconciled
    }

    // ========================================================================
    // INBOUND: POLLING INBOX
    // ========================================================================

    /**
     * Poll the Peppol inbox for new documents.
     *
     * Creates Documents with Drafts for user review (architectural boundary).
     * Inbound Invoices are created only when the user confirms the draft.
     *
     * Full sync is performed on first connection (lastFullSyncAt is null) or weekly (> 7 days).
     * Full sync fetches ALL documents via /documents endpoint.
     * Normal polling fetches only unread documents via /inbox endpoint.
     *
     * @param tenantId The tenant to poll for
     * @param createDocumentCallback Callback to create document with:
     *   - DocumentDraftData: normalized draft data
     *   - String: sender Peppol ID
     *   - TenantId: tenant ID
     *   - RecommandDocumentDetail?: raw document detail with attachments (Recommand only)
     */
    suspend fun pollInbox(
        tenantId: TenantId,
        createDocumentCallback: suspend (DocumentDraftData, String, TenantId, RecommandDocumentDetail?) -> Result<DocumentId>
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
                var transmissionId: PeppolTransmissionId? = null
                try {
                    // Dedupe + retry safety:
                    // - Delivered/Rejected: skip (best-effort mark as read)
                    // - Pending/Failed: retry processing (no poisoning)
                    val existingTransmission = transmissionRepository
                        .getByExternalDocumentId(tenantId, inboxItem.id)
                        .getOrThrow()

                    if (existingTransmission != null &&
                        existingTransmission.status in setOf(PeppolStatus.Delivered, PeppolStatus.Rejected)
                    ) {
                        logger.debug(
                            "Skipping already-processed Peppol document {} (status={})",
                            inboxItem.id,
                            existingTransmission.status
                        )
                        provider.markAsRead(inboxItem.id).getOrNull() // Best-effort
                        continue
                    }

                    // Create/reuse transmission record early so failures are visible + retryable.
                    val peppolDocumentType = inboxItem.documentType
                    val transmission = existingTransmission ?: transmissionRepository.createTransmission(
                        tenantId = tenantId,
                        direction = PeppolTransmissionDirection.Inbound,
                        documentType = peppolDocumentType,
                        externalDocumentId = inboxItem.id,
                        senderPeppolId = PeppolId(inboxItem.senderPeppolId),
                    ).getOrThrow()
                    transmissionId = transmission.id

                    // Validate incoming document
                    val validationResult =
                        validator.validateIncoming(inboxItem.id, inboxItem.senderPeppolId)
                    if (!validationResult.isValid) {
                        val message = "Validation failed: ${validationResult.errors.joinToString("; ")}"
                        logger.warn("Rejecting invalid incoming document ${inboxItem.id}: ${validationResult.errors}")
                        transmissionRepository.updateTransmissionResult(
                            transmissionId = transmission.id,
                            tenantId = tenantId,
                            status = PeppolStatus.Rejected,
                            externalDocumentId = inboxItem.id,
                            errorMessage = message
                        ).getOrThrow()
                        provider.markAsRead(inboxItem.id).getOrNull() // Best-effort
                        continue
                    }

                    // Fetch full document content
                    val fullDocument = provider.getDocument(inboxItem.id).getOrThrow()

                    // Fetch raw document detail for attachment extraction (Recommand-specific)
                    val rawDetail = (provider as? RecommandProvider)
                        ?.getDocumentDetail(inboxItem.id)
                        ?.getOrNull()

                    // Convert to normalized draft data
                    val draftData = mapper.toDraftData(fullDocument, inboxItem.senderPeppolId)

                    // Create document + draft via callback
                    val documentId = createDocumentCallback(
                        draftData,
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
                            invoiceNumber = fullDocument.invoiceNumber,
                            totalAmount = fullDocument.totals?.payableAmount?.let { Money.fromDouble(it) }
                                ?: fullDocument.totals?.taxInclusiveAmount?.let { Money.fromDouble(it) },
                            receivedAt = now
                        )
                    )

                    logger.info(
                        "Processed incoming Peppol document ${inboxItem.id} -> Document $documentId (needs review)"
                    )
                } catch (e: Exception) {
                    logger.error("Failed to process incoming document ${inboxItem.id}", e)
                    // Mark transmission as failed (retryable) and do NOT mark as read.
                    val tid = transmissionId
                    if (tid != null) {
                        transmissionRepository.updateTransmissionResult(
                            transmissionId = tid,
                            tenantId = tenantId,
                            status = PeppolStatus.Failed,
                            externalDocumentId = inboxItem.id,
                            errorMessage = e.message
                        ).getOrNull()
                    }
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
                direction = PeppolTransmissionDirection.Inbound,
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

    private suspend fun persistOutboundFailure(
        transmission: PeppolTransmissionInternal,
        failure: OutboundFailureClassification,
        rawResponse: String?
    ): PeppolStatus {
        return if (failure.retryable) {
            val retryAt = (Clock.System.now() + retryBackoff(transmission.attemptCount)).toLocalDateTime(TimeZone.UTC)
            val updated = transmissionRepository.markOutboundRetryable(
                transmissionId = transmission.id,
                tenantId = transmission.tenantId,
                providerErrorCode = failure.errorCode,
                providerErrorMessage = failure.humanMessage,
                retryAt = retryAt,
                rawResponse = rawResponse
            ).getOrThrow()
            if (!updated) {
                throw IllegalStateException("Failed to mark transmission ${transmission.id} as FAILED_RETRYABLE")
            }
            PeppolStatus.FailedRetryable
        } else {
            val updated = transmissionRepository.markOutboundPermanentFailure(
                transmissionId = transmission.id,
                tenantId = transmission.tenantId,
                providerErrorCode = failure.errorCode,
                providerErrorMessage = failure.humanMessage,
                rawResponse = rawResponse
            ).getOrThrow()
            if (!updated) {
                throw IllegalStateException("Failed to mark transmission ${transmission.id} as FAILED")
            }
            PeppolStatus.Failed
        }
    }

    private suspend fun reconcileOutboundTransmission(transmission: PeppolTransmissionInternal): Boolean {
        if (transmission.direction != PeppolTransmissionDirection.Outbound) {
            return false
        }
        val externalDocumentId = transmission.externalDocumentId ?: return false
        if (transmissionStateMachine.isTerminal(transmission.status)) {
            return false
        }

        val provider = createProviderForTenant(transmission.tenantId) as? RecommandProvider
            ?: return false

        val detail = provider.getDocumentDetail(externalDocumentId).getOrElse {
            logger.warn(
                "Failed outbound PEPPOL reconciliation lookup for transmission {} / doc {}",
                transmission.id,
                externalDocumentId,
                it
            )
            return false
        }

        val mappedStatus = mapRecommandOutboundStatus(detail) ?: return false
        if (!transmissionStateMachine.canTransition(transmission.status, mappedStatus)) {
            return false
        }

        return transmissionRepository.applyProviderStatusMonotonic(
            transmissionId = transmission.id,
            tenantId = transmission.tenantId,
            status = mappedStatus,
            canTransition = transmissionStateMachine::canTransition,
            externalDocumentId = detail.id,
            providerErrorCode = if (mappedStatus == PeppolStatus.Rejected || mappedStatus == PeppolStatus.Failed) {
                "PROVIDER_SIGNAL"
            } else {
                null
            },
            providerErrorMessage = detail.receivedPeppolSignalMessage,
            transmittedAt = if (mappedStatus == PeppolStatus.Sent || mappedStatus == PeppolStatus.Delivered) {
                Clock.System.now().toLocalDateTime(TimeZone.UTC)
            } else {
                null
            }
        ).getOrElse {
            logger.warn(
                "Failed to apply monotonic outbound PEPPOL state for transmission {}",
                transmission.id,
                it
            )
            false
        }
    }

    private fun mapRecommandOutboundStatus(detail: RecommandDocumentDetail): PeppolStatus? {
        val signal = detail.receivedPeppolSignalMessage?.lowercase()
        if (signal != null) {
            if (signal.contains("rejected") || signal.contains("negative")) {
                return PeppolStatus.Rejected
            }
            if (signal.contains("accepted") || signal.contains("acknowledgement") || signal.contains("delivered")) {
                return PeppolStatus.Delivered
            }
        }

        return when {
            detail.validation.result == RecommandDocumentValidationResult.Invalid ||
                detail.validation.result == RecommandDocumentValidationResult.Error -> PeppolStatus.Failed

            detail.sentOverPeppol -> PeppolStatus.Sent
            else -> null
        }
    }

    private fun retryBackoff(attemptCount: Int) = when {
        attemptCount <= 1 -> 1.minutes
        attemptCount == 2 -> 5.minutes
        attemptCount == 3 -> 15.minutes
        attemptCount == 4 -> 1.hours
        else -> 6.hours
    }

    private fun buildIdempotencyKey(
        tenantId: TenantId,
        invoiceId: InvoiceId,
        recipientPeppolId: String,
        rawRequest: String
    ): String {
        val canonical = "${tenantId}:${invoiceId}:${recipientPeppolId}:${rawRequest.sha256Hex()}"
        return canonical.sha256Hex()
    }

    private fun String.sha256Hex(): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
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
