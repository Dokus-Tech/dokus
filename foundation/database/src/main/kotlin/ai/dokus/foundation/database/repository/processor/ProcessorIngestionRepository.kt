package ai.dokus.foundation.database.repository.processor

import ai.dokus.foundation.database.entity.IngestionItemEntity
import ai.dokus.foundation.database.tables.cashflow.DocumentDraftsTable
import ai.dokus.foundation.database.tables.cashflow.DocumentIngestionRunsTable
import ai.dokus.foundation.database.tables.cashflow.DocumentsTable
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.toStdlibInstant
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.DraftStatus
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.model.ExtractedDocumentData
import java.util.UUID
import kotlin.time.ExperimentalTime

/**
 * Repository for ingestion run operations in the processor worker.
 *
 * This repository handles the processing lifecycle:
 * 1. Find queued runs
 * 2. Mark as processing
 * 3. Mark as succeeded (and update draft)
 * 4. Mark as failed
 *
 * Each run is a single processing attempt. Retries are handled via
 * the /reprocess endpoint which creates new runs.
 */
class ProcessorIngestionRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Check if extracted data meets the minimal threshold to create a draft.
     *
     * Thresholds:
     * - Invoice: has totalAmount OR (subtotal + vat) OR (amount + date + clientName)
     * - Bill: has amount OR (amount + date + supplierName)
     * - Expense: has amount AND (merchant OR date)
     * - Unknown: never creates draft
     */
    private fun meetsMinimalThreshold(type: DocumentType, data: ExtractedDocumentData): Boolean {
        return when (type) {
            DocumentType.Invoice -> {
                val inv = data.invoice ?: return false
                inv.totalAmount != null ||
                (inv.subtotalAmount != null && inv.vatAmount != null) ||
                (inv.totalAmount != null && inv.issueDate != null && inv.clientName != null)
            }
            DocumentType.Bill -> {
                val bill = data.bill ?: return false
                bill.amount != null ||
                (bill.amount != null && bill.issueDate != null && bill.supplierName != null)
            }
            DocumentType.Expense -> {
                val exp = data.expense ?: return false
                exp.amount != null && (exp.merchant != null || exp.date != null)
            }
            DocumentType.Unknown -> false
        }
    }

    /**
     * Determine the draft status based on extracted data validation.
     *
     * Returns:
     * - Ready: All required fields present and valid
     * - NeedsReview: Missing required fields or validation issues
     */
    private fun determineDraftStatus(type: DocumentType, data: ExtractedDocumentData): DraftStatus {
        return when (type) {
            DocumentType.Invoice -> {
                val inv = data.invoice ?: return DraftStatus.NeedsReview
                if (inv.totalAmount != null && inv.clientName != null &&
                    inv.issueDate != null && inv.dueDate != null) {
                    DraftStatus.Ready
                } else {
                    DraftStatus.NeedsReview
                }
            }
            DocumentType.Bill -> {
                val bill = data.bill ?: return DraftStatus.NeedsReview
                if (bill.amount != null && bill.supplierName != null &&
                    bill.issueDate != null && bill.dueDate != null &&
                    bill.category != null) {
                    DraftStatus.Ready
                } else {
                    DraftStatus.NeedsReview
                }
            }
            DocumentType.Expense -> {
                val exp = data.expense ?: return DraftStatus.NeedsReview
                if (exp.amount != null && exp.merchant != null &&
                    exp.date != null && exp.category != null) {
                    DraftStatus.Ready
                } else {
                    DraftStatus.NeedsReview
                }
            }
            DocumentType.Unknown -> DraftStatus.NeedsReview
        }
    }

    /**
     * Find pending ingestion runs ready for processing.
     * Only picks up runs with status=Queued, ordered by queue time (FIFO).
     */
    suspend fun findPendingForProcessing(limit: Int = 10): List<IngestionItemEntity> =
        newSuspendedTransaction {
            (DocumentIngestionRunsTable innerJoin DocumentsTable)
                .selectAll()
                .where {
                    DocumentIngestionRunsTable.status eq IngestionStatus.Queued
                }
                .orderBy(DocumentIngestionRunsTable.queuedAt to SortOrder.ASC)
                .limit(limit)
                .map { row ->
                    IngestionItemEntity(
                        runId = row[DocumentIngestionRunsTable.id].value.toString(),
                        documentId = row[DocumentIngestionRunsTable.documentId].toString(),
                        tenantId = row[DocumentIngestionRunsTable.tenantId].toString(),
                        storageKey = row[DocumentsTable.storageKey],
                        filename = row[DocumentsTable.filename],
                        contentType = row[DocumentsTable.contentType]
                    )
                }
        }

    /**
     * Mark an ingestion run as currently being processed.
     * Sets status to Processing and records the AI provider.
     */
    @OptIn(ExperimentalTime::class)
    suspend fun markAsProcessing(runId: String, provider: String): Boolean =
        newSuspendedTransaction {
            val now = Clock.System.now().toStdlibInstant().toLocalDateTime(TimeZone.UTC)
            DocumentIngestionRunsTable.update({
                DocumentIngestionRunsTable.id eq UUID.fromString(runId)
            }) {
                it[status] = IngestionStatus.Processing
                it[startedAt] = now
                it[DocumentIngestionRunsTable.provider] = provider
            } > 0
        }

    /**
     * Mark an ingestion run as successfully completed.
     *
     * This also creates or updates the document draft with the extraction results.
     * Draft update logic:
     * - ai_draft_data: Set ONLY if null (immutable original from first successful run)
     * - extracted_data: Set ONLY if draftVersion == 0 (not user-edited) or force=true
     * - last_successful_run_id: Always updated to this run
     *
     * @param runId The ingestion run ID
     * @param tenantId The tenant ID (required for draft operations)
     * @param documentId The document ID
     * @param documentType Detected document type
     * @param extractedData The extracted data structure
     * @param confidence Overall confidence score (0.0 - 1.0)
     * @param rawText Raw OCR/extracted text
     * @param force If true, overwrite extracted_data even if user has edited
     */
    suspend fun markAsSucceeded(
        runId: String,
        tenantId: String,
        documentId: String,
        documentType: DocumentType,
        extractedData: ExtractedDocumentData,
        confidence: Double,
        rawText: String?,
        force: Boolean = false
    ): Boolean {
        // Defense-in-depth: Validate tenantId is provided
        require(tenantId.isNotBlank()) { "tenantId is required for draft operations" }

        return newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val runUuid = UUID.fromString(runId)
        val documentUuid = UUID.fromString(documentId)
        val tenantUuid = UUID.fromString(tenantId)
        val extractedDataJson = json.encodeToString(extractedData)
        val fieldConfidencesJson = extractedData.fieldConfidences.let { json.encodeToString(it) }

        // Update the ingestion run
        val runUpdated = DocumentIngestionRunsTable.update({
            DocumentIngestionRunsTable.id eq runUuid
        }) {
            it[status] = IngestionStatus.Succeeded
            it[finishedAt] = now
            it[DocumentIngestionRunsTable.rawText] = rawText
            it[rawExtractionJson] = extractedDataJson
            it[DocumentIngestionRunsTable.confidence] = confidence.toBigDecimal()
            it[fieldConfidences] = fieldConfidencesJson
            it[errorMessage] = null
        } > 0

        if (!runUpdated) return@newSuspendedTransaction false

        // Only create/update draft if extraction meets minimal threshold
        if (!meetsMinimalThreshold(documentType, extractedData)) {
            // Extraction didn't produce meaningful data - don't create draft
            // Ingestion run is still marked as succeeded with artifacts
            return@newSuspendedTransaction true
        }

        // Determine draft status based on validation
        val calculatedStatus = determineDraftStatus(documentType, extractedData)

        // Check if draft exists and get current state
        // SECURITY: Always filter by tenantId to prevent cross-tenant access
        val existingDraft = DocumentDraftsTable.selectAll()
            .where {
                (DocumentDraftsTable.documentId eq documentUuid) and
                (DocumentDraftsTable.tenantId eq tenantUuid)
            }
            .singleOrNull()

        if (existingDraft == null) {
            // Create new draft - set both ai_draft_data and extracted_data
            DocumentDraftsTable.insert {
                it[DocumentDraftsTable.documentId] = documentUuid
                it[DocumentDraftsTable.tenantId] = tenantUuid
                it[draftStatus] = calculatedStatus
                it[DocumentDraftsTable.documentType] = documentType
                it[aiDraftData] = extractedDataJson
                it[aiDraftSourceRunId] = runUuid
                it[DocumentDraftsTable.extractedData] = extractedDataJson
                it[draftVersion] = 0
                it[lastSuccessfulRunId] = runUuid
                it[createdAt] = now
                it[updatedAt] = now
            }
        } else {
            // Update existing draft
            val currentAiDraftData = existingDraft[DocumentDraftsTable.aiDraftData]
            val currentVersion = existingDraft[DocumentDraftsTable.draftVersion]

            // ai_draft_data: set ONLY if null (first successful run)
            val shouldSetAiDraft = currentAiDraftData == null

            // extracted_data: set ONLY if draftVersion == 0 (not user-edited) or force
            val shouldSetExtracted = currentVersion == 0 || force

            // SECURITY: Always filter by tenantId to prevent cross-tenant modification
            DocumentDraftsTable.update({
                (DocumentDraftsTable.documentId eq documentUuid) and
                (DocumentDraftsTable.tenantId eq tenantUuid)
            }) {
                it[DocumentDraftsTable.documentType] = documentType
                it[lastSuccessfulRunId] = runUuid
                it[updatedAt] = now

                if (shouldSetAiDraft) {
                    it[aiDraftData] = extractedDataJson
                    it[aiDraftSourceRunId] = runUuid
                }

                if (shouldSetExtracted) {
                    it[DocumentDraftsTable.extractedData] = extractedDataJson
                    // Update status when we update extracted data
                    it[draftStatus] = calculatedStatus
                }
            }
        }

        true
    }
    }

    /**
     * Mark an ingestion run as failed.
     */
    @OptIn(ExperimentalTime::class)
    suspend fun markAsFailed(runId: String, error: String): Boolean =
        newSuspendedTransaction {
            val now = Clock.System.now().toStdlibInstant().toLocalDateTime(TimeZone.UTC)
            DocumentIngestionRunsTable.update({
                DocumentIngestionRunsTable.id eq UUID.fromString(runId)
            }) {
                it[status] = IngestionStatus.Failed
                it[finishedAt] = now
                it[errorMessage] = error
            } > 0
        }
}
