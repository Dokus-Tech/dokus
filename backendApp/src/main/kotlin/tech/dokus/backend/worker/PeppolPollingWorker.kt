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
import kotlinx.serialization.json.decodeFromJsonElement
import org.slf4j.LoggerFactory
import tech.dokus.backend.services.documents.confirmation.BillConfirmationService
import tech.dokus.database.repository.cashflow.DocumentCreatePayload
import tech.dokus.database.repository.cashflow.DocumentDraftRepository
import tech.dokus.database.repository.cashflow.DocumentIngestionRunRepository
import tech.dokus.database.repository.cashflow.DocumentRepository
import tech.dokus.database.repository.contacts.ContactRepository
import tech.dokus.database.repository.peppol.PeppolSettingsRepository
import tech.dokus.domain.Name
import tech.dokus.domain.enums.ContactSource
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.BillDraftData
import tech.dokus.domain.model.contact.CreateContactRequest
import tech.dokus.domain.utils.json
import tech.dokus.foundation.backend.storage.DocumentStorageService
import tech.dokus.peppol.policy.DocumentConfirmationPolicy
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
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

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
    private val confirmationPolicy: DocumentConfirmationPolicy,
    private val billConfirmationService: BillConfirmationService,
    private val documentStorageService: DocumentStorageService,
    private val contactRepository: ContactRepository
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
                pollTenant(tenantId)
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
    private suspend fun pollTenant(tenantId: TenantId) {
        // Get or create mutex for this tenant
        val mutex = pollMutexes.computeIfAbsent(tenantId) { Mutex() }

        // Skip if already polling this tenant
        if (!mutex.tryLock()) {
            logger.debug("Already polling tenant {}, skipping", tenantId)
            return
        }

        try {
            logger.debug("Polling Peppol inbox for tenant: {}", tenantId)

            val result = peppolService.pollInbox(tenantId) { draftData, senderPeppolId, tid, documentDetail ->
                runCatching {
                    // Find PDF attachment (if any)
                    val pdfAttachment = documentDetail
                        ?.let(::extractAttachments)
                        ?.firstOrNull { it.mimeCode == "application/pdf" && !it.embeddedDocument.isNullOrEmpty() }

                    val uploadResult = when {
                        pdfAttachment != null -> {
                            val pdfBytes = Base64.getDecoder().decode(requireNotNull(pdfAttachment.embeddedDocument))
                            documentStorageService.uploadDocument(
                                tenantId = tid,
                                prefix = "peppol",
                                filename = pdfAttachment.filename.ifBlank {
                                    "peppol-${(draftData as? BillDraftData)?.invoiceNumber ?: "unknown"}.pdf"
                                },
                                data = pdfBytes,
                                contentType = "application/pdf"
                            )
                        }

                        !documentDetail?.xml.isNullOrBlank() -> {
                            val xmlBytes = requireNotNull(documentDetail).xml.encodeToByteArray()
                            val fallbackId = documentDetail.envelopeId ?: documentDetail.id
                            documentStorageService.uploadDocument(
                                tenantId = tid,
                                prefix = "peppol",
                                filename = "peppol-$fallbackId.xml",
                                data = xmlBytes,
                                contentType = "application/xml"
                            )
                        }

                        else -> {
                            // We must store an actual artifact to keep downloads functional.
                            error("Peppol document has no PDF attachment and no XML payload")
                        }
                    }

                    val documentId = documentRepository.create(
                        tenantId = tid,
                        payload = DocumentCreatePayload(
                            filename = uploadResult.filename,
                            contentType = uploadResult.contentType,
                            sizeBytes = uploadResult.sizeBytes,
                            storageKey = uploadResult.key,
                            contentHash = null,
                            source = DocumentSource.Peppol
                        )
                    )

                    // Create ingestion run first (satisfies FK constraint on document_drafts)
                    val runId = ingestionRunRepository.createRun(
                        documentId = documentId,
                        tenantId = tid
                    )
                    ingestionRunRepository.markAsProcessing(runId, "peppol")
                    ingestionRunRepository.markAsSucceeded(
                        runId = runId,
                        rawText = null,
                        rawExtractionJson = json.encodeToString(draftData),
                        confidence = 1.0 // Peppol data is authoritative
                    )

                    // Create draft with extracted data
                    draftRepository.createOrUpdateFromIngestion(
                        documentId = documentId,
                        tenantId = tid,
                        runId = runId,
                        extractedData = draftData,
                        documentType = DocumentType.Bill,
                        force = true
                    )

                    // Auto-confirm if policy allows (PEPPOL documents are always auto-confirmed)
                    val billDraftData = draftData as? BillDraftData
                    if (billDraftData != null && confirmationPolicy.canAutoConfirm(DocumentSource.Peppol, draftData, tid)) {
                        // Find or create contact from Peppol data
                        val linkedContactId = findOrCreateContactForPeppol(
                            tenantId = tid,
                            supplierName = billDraftData.supplierName,
                            supplierVatNumber = billDraftData.supplierVat?.value
                        )

                        billConfirmationService.confirm(
                            tenantId = tid,
                            documentId = documentId,
                            draftData = billDraftData,
                            linkedContactId = linkedContactId
                        ).getOrThrow()
                    } else if (billDraftData == null) {
                        logger.warn("Peppol document is not a bill, skipping auto-confirm: documentId=$documentId")
                    }

                    documentId
                }
            }

            result.onSuccess { response ->
                logger.info(
                    "Peppol poll completed for tenant $tenantId: " +
                        "${response.processedDocuments.size} documents processed"
                )
            }.onFailure { e ->
                logger.error("Peppol poll failed for tenant $tenantId", e)
            }

            // Update last poll time
            lastPollTimes[tenantId] = Clock.System.now()
        } finally {
            mutex.unlock()
        }
    }

    /**
     * Find or create a contact for a Peppol document.
     * Matching priority:
     * 1. VAT number (most reliable)
     * 2. Peppol ID
     * 3. Auto-create if we have enough data
     */
    private suspend fun findOrCreateContactForPeppol(
        tenantId: TenantId,
        supplierName: String?,
        supplierVatNumber: String?
    ): ContactId? {
        // 1. Try VAT number (most reliable) - already normalized in repository
        if (!supplierVatNumber.isNullOrBlank()) {
            contactRepository.findByVatNumber(tenantId, supplierVatNumber)
                .getOrNull()?.let { return it.id }
        }

        // NOTE: PEPPOL ID matching removed - peppolId is now discovery data in PeppolDirectoryCacheTable
        // We find contacts by VAT/company number instead

        // 2. Auto-create if we have enough data
        if (!supplierName.isNullOrBlank()) {
            val request = CreateContactRequest(
                name = Name(supplierName),
                vatNumber = supplierVatNumber?.let { VatNumber(it) },
                source = ContactSource.Peppol
            )
            val newContact = contactRepository.createContact(tenantId, request).getOrNull()
            return newContact?.id
        }

        return null
    }
}
