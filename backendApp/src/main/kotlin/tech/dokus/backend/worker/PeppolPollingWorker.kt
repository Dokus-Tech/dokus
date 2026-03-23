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
import tech.dokus.backend.services.documents.DocumentTruthService
import tech.dokus.backend.services.documents.IntakeResolution
import tech.dokus.backend.services.documents.sse.DocumentSsePublisher
import tech.dokus.backend.services.notifications.NotificationEmission
import tech.dokus.backend.services.notifications.NotificationService
import tech.dokus.database.repository.peppol.PeppolSettingsRepository
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.enums.NotificationReferenceType
import tech.dokus.domain.enums.NotificationType
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.AnnualAccountsDraftData
import tech.dokus.domain.model.BankFeeDraftData
import tech.dokus.domain.model.BankStatementDraftData
import tech.dokus.domain.model.BoardMinutesDraftData
import tech.dokus.domain.model.C4DraftData
import tech.dokus.domain.model.CompanyExtractDraftData
import tech.dokus.domain.model.ContractDraftData
import tech.dokus.domain.model.CorporateTaxAdvanceDraftData
import tech.dokus.domain.model.CorporateTaxDraftData
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.CustomsDeclarationDraftData
import tech.dokus.domain.model.DeliveryNoteDraftData
import tech.dokus.domain.model.DepreciationScheduleDraftData
import tech.dokus.domain.model.DimonaDraftData
import tech.dokus.domain.model.DividendDraftData
import tech.dokus.domain.model.DocumentDraftData
import tech.dokus.domain.model.EmploymentContractDraftData
import tech.dokus.domain.model.ExpenseClaimDraftData
import tech.dokus.domain.model.FineDraftData
import tech.dokus.domain.model.HolidayPayDraftData
import tech.dokus.domain.model.IcListingDraftData
import tech.dokus.domain.model.InsuranceDraftData
import tech.dokus.domain.model.InterestStatementDraftData
import tech.dokus.domain.model.IntrastatDraftData
import tech.dokus.domain.model.InventoryDraftData
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.LeaseDraftData
import tech.dokus.domain.model.LoanDraftData
import tech.dokus.domain.model.OrderConfirmationDraftData
import tech.dokus.domain.model.OssReturnDraftData
import tech.dokus.domain.model.OtherDraftData
import tech.dokus.domain.model.PaymentConfirmationDraftData
import tech.dokus.domain.model.PayrollSummaryDraftData
import tech.dokus.domain.model.PermitDraftData
import tech.dokus.domain.model.PersonalTaxDraftData
import tech.dokus.domain.model.ProFormaDraftData
import tech.dokus.domain.model.PurchaseOrderDraftData
import tech.dokus.domain.model.QuoteDraftData
import tech.dokus.domain.model.ReceiptDraftData
import tech.dokus.domain.model.ReminderDraftData
import tech.dokus.domain.model.SalarySlipDraftData
import tech.dokus.domain.model.SelfEmployedContributionDraftData
import tech.dokus.domain.model.ShareholderRegisterDraftData
import tech.dokus.domain.model.SocialContributionDraftData
import tech.dokus.domain.model.SocialFundDraftData
import tech.dokus.domain.model.StatementOfAccountDraftData
import tech.dokus.domain.model.SubsidyDraftData
import tech.dokus.domain.model.TaxAssessmentDraftData
import tech.dokus.domain.model.VapzDraftData
import tech.dokus.domain.model.VatAssessmentDraftData
import tech.dokus.domain.model.VatListingDraftData
import tech.dokus.domain.model.VatReturnDraftData
import tech.dokus.domain.model.WithholdingTaxDraftData
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
internal class PeppolPollingWorker(
    private val peppolSettingsRepository: PeppolSettingsRepository,
    private val peppolService: PeppolService,
    private val documentTruthService: DocumentTruthService,
    private val notificationService: NotificationService,
    private val documentSsePublisher: DocumentSsePublisher,
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
    private val minTimeBetweenPolls = 60.seconds // Secondary in-memory guard for webhook bursts
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
    @Suppress("LongMethod") // Complex inbox polling with queue-only durable ingestion boundary
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
                            openPath = "/cashflow/document_detail/${processed.documentId}",
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
        documentSsePublisher.publishDocumentChanged(tenantId, intake.documentId)
        val documentId = intake.documentId

        val structuredSnapshotJson = json.encodeToString(draftData)
        val envelopePersisted = documentTruthService.persistPeppolSourceEnvelope(
            tenantId = tenantId,
            sourceId = intake.sourceId,
            structuredSnapshotJson = structuredSnapshotJson,
            snapshotVersion = 1,
            rawUblXml = documentDetail?.xml
        )
        require(envelopePersisted) {
            "Failed to persist PEPPOL envelope for source ${intake.sourceId}"
        }

        if (intake.resolution is IntakeResolution.Linked) {
            logger.info("PEPPOL artifact linked to existing document {} via exact-byte match", documentId)
        } else {
            require(intake.runId != null) {
                "PEPPOL intake did not persist a queued run for new source ${intake.sourceId}"
            }
            logger.info(
                "Queued PEPPOL document {} for unified ingestion worker: documentId={}, runId={}",
                intake.sourceId,
                documentId,
                intake.runId
            )
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

    private fun documentNumberOf(draftData: DocumentDraftData): String? = when (draftData) {
        is CreditNoteDraftData -> draftData.creditNoteNumber
        is InvoiceDraftData -> draftData.invoiceNumber
        is ReceiptDraftData -> draftData.receiptNumber
        is BankStatementDraftData,
        is ProFormaDraftData,
        is QuoteDraftData,
        is OrderConfirmationDraftData,
        is DeliveryNoteDraftData,
        is ReminderDraftData,
        is StatementOfAccountDraftData,
        is PurchaseOrderDraftData,
        is ExpenseClaimDraftData,
        is BankFeeDraftData,
        is InterestStatementDraftData,
        is PaymentConfirmationDraftData,
        is VatReturnDraftData,
        is VatListingDraftData,
        is VatAssessmentDraftData,
        is IcListingDraftData,
        is OssReturnDraftData,
        is CorporateTaxDraftData,
        is CorporateTaxAdvanceDraftData,
        is TaxAssessmentDraftData,
        is PersonalTaxDraftData,
        is WithholdingTaxDraftData,
        is SocialContributionDraftData,
        is SocialFundDraftData,
        is SelfEmployedContributionDraftData,
        is VapzDraftData,
        is SalarySlipDraftData,
        is PayrollSummaryDraftData,
        is EmploymentContractDraftData,
        is DimonaDraftData,
        is C4DraftData,
        is HolidayPayDraftData,
        is ContractDraftData,
        is LeaseDraftData,
        is LoanDraftData,
        is InsuranceDraftData,
        is DividendDraftData,
        is ShareholderRegisterDraftData,
        is CompanyExtractDraftData,
        is AnnualAccountsDraftData,
        is BoardMinutesDraftData,
        is SubsidyDraftData,
        is FineDraftData,
        is PermitDraftData,
        is CustomsDeclarationDraftData,
        is IntrastatDraftData,
        is DepreciationScheduleDraftData,
        is InventoryDraftData,
        is OtherDraftData -> null
    }
}
