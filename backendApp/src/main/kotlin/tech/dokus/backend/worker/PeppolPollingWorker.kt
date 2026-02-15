package tech.dokus.backend.worker

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.decodeFromJsonElement
import org.slf4j.LoggerFactory
import tech.dokus.backend.services.documents.AutoConfirmPolicy
import tech.dokus.backend.services.documents.DocumentTruthService
import tech.dokus.backend.services.documents.confirmation.DocumentConfirmationDispatcher
import tech.dokus.backend.services.notifications.NotificationEmission
import tech.dokus.backend.services.notifications.NotificationService
import tech.dokus.database.repository.cashflow.DocumentDraftRepository
import tech.dokus.database.repository.cashflow.DocumentIngestionRunRepository
import tech.dokus.database.repository.cashflow.DocumentRepository
import tech.dokus.database.repository.contacts.ContactRepository
import tech.dokus.database.repository.peppol.PeppolSettingsRepository
import tech.dokus.domain.Money
import tech.dokus.domain.Name
import tech.dokus.domain.enums.ContactSource
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.DocumentIntakeOutcome
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.NotificationReferenceType
import tech.dokus.domain.enums.NotificationType
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.DocumentDraftData
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.ReceiptDraftData
import tech.dokus.domain.model.contact.CreateContactRequest
import tech.dokus.domain.utils.json
import tech.dokus.peppol.provider.client.recommand.model.RecommandAttachment
import tech.dokus.peppol.provider.client.recommand.model.RecommandCreditNote
import tech.dokus.peppol.provider.client.recommand.model.RecommandDocumentDetail
import tech.dokus.peppol.provider.client.recommand.model.RecommandDocumentType
import tech.dokus.peppol.provider.client.recommand.model.RecommandInvoice
import tech.dokus.peppol.provider.client.recommand.model.RecommandSelfBillingCreditNote
import tech.dokus.peppol.provider.client.recommand.model.RecommandSelfBillingInvoice
import tech.dokus.peppol.service.PeppolService
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private val DataUrlBase64Prefix = Regex("^data:[^,]*;base64,", RegexOption.IGNORE_CASE)
private val StandardBase64Pattern = Regex("^[A-Za-z0-9+/]*={0,2}$")
private val UrlSafeBase64Pattern = Regex("^[A-Za-z0-9_-]*={0,2}$")

internal fun decodePeppolAttachmentBase64(encoded: String): ByteArray {
    val stripped = encoded.trim().replace(DataUrlBase64Prefix, "")
    if (stripped.isBlank()) {
        throw IllegalArgumentException("Attachment payload is empty")
    }

    val compact = stripped.filterNot(Char::isWhitespace)
    val isStandard = StandardBase64Pattern.matches(compact)
    val isUrlSafe = UrlSafeBase64Pattern.matches(compact)
    if (!isStandard && !isUrlSafe) {
        throw IllegalArgumentException("Invalid PEPPOL attachment base64 payload")
    }

    val decoders = buildList {
        if (isStandard) add(Base64.getDecoder())
        if (isUrlSafe) add(Base64.getUrlDecoder())
    }
    var lastError: IllegalArgumentException? = null
    for (decoder in decoders) {
        try {
            return decoder.decode(compact)
        } catch (e: IllegalArgumentException) {
            lastError = e
        }
    }

    throw IllegalArgumentException("Invalid PEPPOL attachment base64 payload", lastError)
}

/**
 * Background worker that polls Peppol inbox for each enabled tenant.
 *
 * Features:
 * - Staggered polling: Each tenant is polled on a 15-30 minute interval
 * - pollNow(tenantId): Immediate poll for a specific tenant (for webhook trigger)
 * - Tracks lastPollTime per tenant to avoid hammering the API
 * - Graceful shutdown support
 */
@Suppress("TooGenericExceptionCaught", "LoopWithTooManyJumpStatements", "LongParameterList")
class PeppolPollingWorker(
    private val peppolSettingsRepository: PeppolSettingsRepository,
    private val peppolService: PeppolService,
    private val documentRepository: DocumentRepository,
    private val draftRepository: DocumentDraftRepository,
    private val ingestionRunRepository: DocumentIngestionRunRepository,
    private val documentTruthService: DocumentTruthService,
    private val autoConfirmPolicy: AutoConfirmPolicy,
    private val confirmationDispatcher: DocumentConfirmationDispatcher,
    private val contactRepository: ContactRepository,
    private val notificationService: NotificationService
) {
    private val logger = LoggerFactory.getLogger(PeppolPollingWorker::class.java)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var pollingJob: Job? = null
    private val isRunning = AtomicBoolean(false)

    // Track last poll time per tenant
    private val lastPollTimes = ConcurrentHashMap<TenantId, Instant>()

    // Mutex to prevent concurrent polls for the same tenant
    private val pollMutexes = ConcurrentHashMap<TenantId, Mutex>()

    // Polling configuration
    private val pollInterval = 20.minutes // Base interval between checks
    private val minTimeBetweenPolls = 5.minutes // Minimum time between polls per tenant
    private val tenantPollDelay = 30.seconds // Stagger delay between tenants

    /**
     * Start the polling worker.
     */
    fun start() {
        if (!isRunning.compareAndSet(false, true)) {
            logger.warn("Peppol polling worker already running")
            return
        }

        logger.info("Starting Peppol polling worker (interval=${pollInterval.inWholeMinutes}min)")

        pollingJob = scope.launch {
            while (isActive && isRunning.get()) {
                try {
                    pollAllEnabledTenants()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.error("Error in Peppol polling loop", e)
                }

                delay(pollInterval)
            }
        }
    }

    /**
     * Stop the polling worker gracefully.
     */
    fun stop() {
        if (!isRunning.compareAndSet(true, false)) {
            logger.warn("Peppol polling worker not running")
            return
        }

        logger.info("Stopping Peppol polling worker...")
        pollingJob?.cancel()
        pollingJob = null
        logger.info("Peppol polling worker stopped")
    }

    /**
     * Trigger an immediate poll for a specific tenant.
     * Used by webhook endpoint to trigger polling when notified.
     *
     * @param tenantId The tenant to poll
     * @return true if poll was triggered, false if skipped (too recent)
     */
    suspend fun pollNow(tenantId: TenantId): Boolean {
        val lastPoll = lastPollTimes[tenantId]
        if (lastPoll != null) {
            val elapsed = Clock.System.now() - lastPoll
            if (elapsed < minTimeBetweenPolls) {
                logger.debug(
                    "Skipping poll for tenant $tenantId - polled ${elapsed.inWholeSeconds}s ago"
                )
                return false
            }
        }

        logger.info("Triggering immediate poll for tenant: $tenantId")

        scope.launch {
            pollTenant(tenantId)
        }

        return true
    }

    /**
     * Poll all enabled tenants with staggered timing.
     */
    private suspend fun pollAllEnabledTenants() {
        val enabledSettings = peppolSettingsRepository.getAllEnabled()
            .getOrElse {
                logger.error("Failed to get enabled Peppol settings", it)
                return
            }

        if (enabledSettings.isEmpty()) {
            logger.debug("No enabled Peppol tenants to poll")
            return
        }

        logger.info("Starting Peppol inbox poll for ${enabledSettings.size} tenants")

        for (settings in enabledSettings) {
            if (!isRunning.get()) break

            val tenantId = settings.tenantId

            // Check if enough time has passed since last poll
            val lastPoll = lastPollTimes[tenantId]
            if (lastPoll != null) {
                val elapsed = Clock.System.now() - lastPoll
                if (elapsed < minTimeBetweenPolls) {
                    logger.debug(
                        "Skipping tenant {} - polled {}min ago",
                        tenantId,
                        elapsed.inWholeMinutes
                    )
                    continue
                }
            }

            try {
                pollTenant(
                    tenantId = tenantId,
                    isFullSyncPoll = needsFullSync(settings.lastFullSyncAt)
                )
            } catch (e: Exception) {
                logger.error("Failed to poll tenant $tenantId", e)
            }

            // Stagger polls between tenants
            delay(tenantPollDelay)
        }
    }

    private fun extractAttachments(documentDetail: RecommandDocumentDetail): List<RecommandAttachment>? {
        val parsed = documentDetail.parsed ?: return null

        return when (documentDetail.type) {
            RecommandDocumentType.Invoice ->
                json.decodeFromJsonElement<RecommandInvoice>(parsed).attachments
            RecommandDocumentType.CreditNote ->
                json.decodeFromJsonElement<RecommandCreditNote>(parsed).attachments
            RecommandDocumentType.SelfBillingInvoice ->
                json.decodeFromJsonElement<RecommandSelfBillingInvoice>(parsed).attachments
            RecommandDocumentType.SelfBillingCreditNote ->
                json.decodeFromJsonElement<RecommandSelfBillingCreditNote>(parsed).attachments
            RecommandDocumentType.MessageLevelResponse, RecommandDocumentType.Xml -> null
        }
    }

    /**
     * Poll a single tenant's Peppol inbox.
     */
    @Suppress("LongMethod") // Complex inbox polling with document creation and confirmation
    private suspend fun pollTenant(tenantId: TenantId, isFullSyncPoll: Boolean? = null) {
        // Get or create mutex for this tenant
        val mutex = pollMutexes.computeIfAbsent(tenantId) { Mutex() }

        // Skip if already polling this tenant
        if (!mutex.tryLock()) {
            logger.debug("Already polling tenant {}, skipping", tenantId)
            return
        }

        try {
            logger.debug("Polling Peppol inbox for tenant: {}", tenantId)
            val fullSyncRun = isFullSyncPoll ?: resolveFullSyncForTenant(tenantId)

            val result = peppolService.pollInbox(tenantId) { draftData, senderPeppolId, tid, documentDetail ->
                runCatching {
                    processIncomingPeppolDocument(draftData, senderPeppolId, tid, documentDetail)
                }
            }

            result.onSuccess { response ->
                logger.info(
                    "Peppol poll completed for tenant $tenantId: " +
                        "${response.processedDocuments.size} documents processed"
                )

                if (fullSyncRun) {
                    logger.info(
                        "Skipping PEPPOL received notifications for tenant {} because this poll was a full sync ({} documents)",
                        tenantId,
                        response.processedDocuments.size
                    )
                    return@onSuccess
                }

                response.processedDocuments.forEach { processed ->
                    val title = processed.invoiceNumber?.let { invoiceNumber ->
                        "New PEPPOL document received - Inv #$invoiceNumber"
                    } ?: "New PEPPOL document received"

                    notificationService.emit(
                        NotificationEmission(
                            tenantId = tenantId,
                            type = NotificationType.PeppolReceived,
                            title = title,
                            referenceType = NotificationReferenceType.Document,
                            referenceId = processed.documentId.toString(),
                            openPath = "/cashflow/document_review/${processed.documentId}",
                            emailDetails = listOf(
                                "A new document was received via PEPPOL.",
                                "Sender: ${processed.senderPeppolId.value}"
                            )
                        )
                    ).onFailure { error ->
                        logger.warn(
                            "Failed to emit PEPPOL received notification for document ${processed.documentId}",
                            error
                        )
                    }
                }
            }.onFailure { e ->
                logger.error("Peppol poll failed for tenant $tenantId", e)
            }

            // Update last poll time
            lastPollTimes[tenantId] = Clock.System.now()
        } finally {
            mutex.unlock()
        }
    }

    private suspend fun processIncomingPeppolDocument(
        draftData: DocumentDraftData,
        senderPeppolId: String,
        tenantId: TenantId,
        documentDetail: RecommandDocumentDetail?
    ): DocumentId {
        val pdfAttachment = documentDetail
            ?.let(::extractAttachments)
            ?.firstOrNull { it.mimeCode == "application/pdf" && !it.embeddedDocument.isNullOrEmpty() }

        val artifact = when {
            pdfAttachment != null -> {
                val encodedAttachment = requireNotNull(pdfAttachment.embeddedDocument)
                val pdfBytes = try {
                    decodePeppolAttachmentBase64(encodedAttachment)
                } catch (e: IllegalArgumentException) {
                    throw IllegalArgumentException(
                        "Invalid PEPPOL PDF attachment base64 (attachmentId=${pdfAttachment.id}, filename=${pdfAttachment.filename})",
                        e
                    )
                }
                Triple(
                    pdfBytes,
                    pdfAttachment.filename.ifBlank {
                        "peppol-${documentNumberOf(draftData) ?: "unknown"}.pdf"
                    },
                    "application/pdf"
                )
            }

            !documentDetail?.xml.isNullOrBlank() -> {
                val xmlBytes = requireNotNull(documentDetail).xml.encodeToByteArray()
                val fallbackId = documentDetail.envelopeId ?: documentDetail.id
                Triple(
                    xmlBytes,
                    "peppol-$fallbackId.xml",
                    "application/xml"
                )
            }

            else -> {
                error("Peppol document has no PDF attachment and no XML payload")
            }
        }

        val intake = documentTruthService.intakeBytes(
            tenantId = tenantId,
            filename = artifact.second,
            contentType = artifact.third,
            prefix = "peppol",
            fileBytes = artifact.first,
            sourceChannel = DocumentSource.Peppol
        )
        val documentId = intake.documentId

        if (intake.outcome == DocumentIntakeOutcome.LinkedToExisting) {
            logger.info(
                "PEPPOL artifact linked to existing document {} via exact-byte match",
                documentId
            )
            return documentId
        }

        val runId = intake.runId
            ?: ingestionRunRepository.createRun(
                documentId = documentId,
                tenantId = tenantId,
                sourceId = intake.sourceId
            )
        ingestionRunRepository.markAsProcessing(runId, "peppol")
        ingestionRunRepository.markAsSucceeded(
            runId = runId,
            rawText = null,
            rawExtractionJson = json.encodeToString(draftData),
            confidence = 1.0
        )

        val documentType = documentTypeFor(draftData)
        draftRepository.createOrUpdateFromIngestion(
            documentId = documentId,
            tenantId = tenantId,
            runId = runId,
            extractedData = draftData,
            documentType = documentType,
            force = true
        )

        val matchOutcome = documentTruthService.applyPostExtractionMatching(
            tenantId = tenantId,
            documentId = documentId,
            sourceId = intake.sourceId,
            draftData = draftData,
            extractedSnapshotJson = json.encodeToString(draftData)
        )
        if (matchOutcome.documentId != documentId ||
            matchOutcome.outcome == DocumentIntakeOutcome.PendingMatchReview
        ) {
            logger.info(
                "PEPPOL source {} resolved by truth matcher with outcome {} (target={})",
                intake.sourceId,
                matchOutcome.outcome,
                matchOutcome.documentId
            )
            return matchOutcome.documentId
        }

        val counterparty = extractCounterparty(draftData)
        val linkedContactId = findOrCreateContactForPeppol(
            tenantId = tenantId,
            counterpartyName = counterparty.name,
            counterpartyVatNumber = counterparty.vatNumber
        )

        val canAutoConfirm = autoConfirmPolicy.canAutoConfirm(
            tenantId = tenantId,
            documentId = documentId,
            source = DocumentSource.Peppol,
            documentType = documentType,
            draftData = draftData,
            auditPassed = peppolAuditPassed(draftData),
            confidence = 1.0,
            linkedContactId = linkedContactId,
            directionResolvedFromAiHintOnly = false
        )

        if (canAutoConfirm) {
            confirmationDispatcher.confirm(
                tenantId = tenantId,
                documentId = documentId,
                draftData = draftData,
                linkedContactId = linkedContactId
            ).getOrThrow()
        }

        return documentId
    }

    private suspend fun resolveFullSyncForTenant(tenantId: TenantId): Boolean {
        val settings = peppolSettingsRepository.getSettings(tenantId).getOrElse { error ->
            logger.warn("Failed to resolve Peppol sync mode for tenant {}. Defaulting to non-full-sync notifications.", tenantId, error)
            return false
        } ?: return false
        return needsFullSync(settings.lastFullSyncAt)
    }

    private fun needsFullSync(lastFullSyncAt: LocalDateTime?): Boolean {
        if (lastFullSyncAt == null) return true
        val threshold = Clock.System.now()
            .minus(7.days)
            .toLocalDateTime(TimeZone.UTC)
        return lastFullSyncAt < threshold
    }

    /**
     * Counterparty info extracted from any draft data type.
     */
    private data class CounterpartyInfo(val name: String?, val vatNumber: VatNumber?)

    private fun extractCounterparty(draftData: DocumentDraftData): CounterpartyInfo = when (draftData) {
        is CreditNoteDraftData -> CounterpartyInfo(draftData.counterpartyName, draftData.counterpartyVat)
        is InvoiceDraftData -> when (draftData.direction) {
            DocumentDirection.Inbound -> CounterpartyInfo(
                draftData.seller.name,
                draftData.seller.vat
            )
            DocumentDirection.Outbound -> CounterpartyInfo(
                draftData.buyer.name,
                draftData.buyer.vat
            )
            DocumentDirection.Unknown -> CounterpartyInfo(
                draftData.buyer.name ?: draftData.seller.name,
                draftData.buyer.vat ?: draftData.seller.vat
            )
        }
        is ReceiptDraftData -> CounterpartyInfo(draftData.merchantName, draftData.merchantVat)
    }

    private fun documentTypeFor(draftData: DocumentDraftData): DocumentType = when (draftData) {
        is CreditNoteDraftData -> DocumentType.CreditNote
        is InvoiceDraftData -> DocumentType.Invoice
        is ReceiptDraftData -> DocumentType.Receipt
    }

    private fun documentNumberOf(draftData: DocumentDraftData): String? = when (draftData) {
        is CreditNoteDraftData -> draftData.creditNoteNumber
        is InvoiceDraftData -> draftData.invoiceNumber
        is ReceiptDraftData -> draftData.receiptNumber
    }

    private fun peppolAuditPassed(draftData: DocumentDraftData): Boolean = when (draftData) {
        is InvoiceDraftData -> vatMathOk(draftData.subtotalAmount, draftData.vatAmount, draftData.totalAmount)
        is CreditNoteDraftData -> vatMathOk(draftData.subtotalAmount, draftData.vatAmount, draftData.totalAmount)
        is ReceiptDraftData -> true // Receipts typically don't provide a subtotal; keep audit deterministic and minimal
    }

    private fun vatMathOk(subtotal: Money?, vat: Money?, total: Money?): Boolean {
        if (subtotal == null || vat == null || total == null) return true
        val diffMinor = (subtotal + vat - total).minor
        return kotlin.math.abs(diffMinor) <= 1L // tolerate rounding differences up to 1 cent
    }

    /**
     * Find or create a contact for a Peppol document.
     * Matching priority:
     * 1. VAT number (most reliable)
     * 2. Auto-create if we have enough data
     */
    private suspend fun findOrCreateContactForPeppol(
        tenantId: TenantId,
        counterpartyName: String?,
        counterpartyVatNumber: VatNumber?
    ): ContactId? {
        // 1. Try VAT number (most reliable) - already normalized in repository
        val vatValue = counterpartyVatNumber?.value
        if (!vatValue.isNullOrBlank()) {
            contactRepository.findByVatNumber(tenantId, vatValue)
                .getOrNull()?.let { return it.id }
        }

        // 2. Auto-create if we have enough data
        if (!counterpartyName.isNullOrBlank()) {
            val request = CreateContactRequest(
                name = Name(counterpartyName),
                vatNumber = counterpartyVatNumber,
                source = ContactSource.Peppol
            )
            val newContact = contactRepository.createContact(tenantId, request).getOrNull()
            return newContact?.id
        }

        return null
    }
}
