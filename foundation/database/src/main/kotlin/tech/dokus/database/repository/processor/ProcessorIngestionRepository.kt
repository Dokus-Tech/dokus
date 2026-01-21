package tech.dokus.database.repository.processor

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.toStdlibInstant
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update
import tech.dokus.database.entity.IngestionItemEntity
import tech.dokus.database.tables.documents.DocumentDraftsTable
import tech.dokus.database.tables.documents.DocumentIngestionRunsTable
import tech.dokus.database.tables.documents.DocumentsTable
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.DraftStatus
import tech.dokus.domain.enums.IndexingStatus
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.model.ExtractedDocumentData
import tech.dokus.domain.utils.json
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

    // NOTE: meetsMinimalThreshold() was REMOVED from this file.
    // The ONLY threshold check is DocumentAIResult.meetsMinimalThreshold() in the AI module.
    // The worker passes the threshold result to markAsSucceeded() via the meetsThreshold parameter.

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
                    inv.issueDate != null && inv.dueDate != null
                ) {
                    DraftStatus.Ready
                } else {
                    DraftStatus.NeedsReview
                }
            }
            DocumentType.Bill -> {
                val bill = data.bill ?: return DraftStatus.NeedsReview
                if (bill.amount != null && bill.supplierName != null &&
                    bill.issueDate != null && bill.dueDate != null &&
                    bill.category != null
                ) {
                    DraftStatus.Ready
                } else {
                    DraftStatus.NeedsReview
                }
            }
            DocumentType.Expense -> {
                val exp = data.expense ?: return DraftStatus.NeedsReview
                if (exp.amount != null && exp.merchant != null &&
                    exp.date != null && exp.category != null
                ) {
                    DraftStatus.Ready
                } else {
                    DraftStatus.NeedsReview
                }
            }
            DocumentType.Receipt -> {
                // Receipt uses same required fields as Expense (confirms into Expense)
                val receipt = data.receipt ?: return DraftStatus.NeedsReview
                if (receipt.amount != null && receipt.merchant != null &&
                    receipt.date != null && receipt.category != null
                ) {
                    DraftStatus.Ready
                } else {
                    DraftStatus.NeedsReview
                }
            }
            DocumentType.ProForma -> {
                // ProForma is informational - client/totals needed for conversion
                val proForma = data.proForma ?: return DraftStatus.NeedsReview
                if (proForma.totalAmount != null && proForma.clientName != null &&
                    proForma.issueDate != null
                ) {
                    DraftStatus.Ready
                } else {
                    DraftStatus.NeedsReview
                }
            }
            DocumentType.CreditNote -> {
                // CreditNote needs counterparty, amount, and issue date
                val creditNote = data.creditNote ?: return DraftStatus.NeedsReview
                if (creditNote.totalAmount != null && creditNote.counterpartyName != null &&
                    creditNote.issueDate != null
                ) {
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
            val now = Clock.System.now().toStdlibInstant().toLocalDateTime(TimeZone.Companion.UTC)
            DocumentIngestionRunsTable.update({
                DocumentIngestionRunsTable.id eq UUID.fromString(runId)
            }) {
                it[status] = IngestionStatus.Processing
                it[startedAt] = now
                it[DocumentIngestionRunsTable.provider] = provider
            } > 0
        }

    /**
     * Find the latest processing run ID for a document.
     *
     * Used as a fallback when a tool call doesn't include runId.
     * Returns the most recent run in Processing status for the given document and tenant.
     */
    suspend fun findProcessingRunId(
        tenantId: String,
        documentId: String
    ): String? = newSuspendedTransaction {
        DocumentIngestionRunsTable
            .selectAll()
            .where {
                (DocumentIngestionRunsTable.tenantId eq UUID.fromString(tenantId)) and
                    (DocumentIngestionRunsTable.documentId eq UUID.fromString(documentId)) and
                    (DocumentIngestionRunsTable.status eq IngestionStatus.Processing)
            }
            .orderBy(DocumentIngestionRunsTable.startedAt to SortOrder.DESC)
            .limit(1)
            .singleOrNull()
            ?.get(DocumentIngestionRunsTable.id)
            ?.value
            ?.toString()
    }

    /**
     * Get the current ingestion status for a run ID.
     */
    suspend fun getRunStatus(runId: String): IngestionStatus? = newSuspendedTransaction {
        DocumentIngestionRunsTable
            .selectAll()
            .where { DocumentIngestionRunsTable.id eq UUID.fromString(runId) }
            .singleOrNull()
            ?.get(DocumentIngestionRunsTable.status)
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
     * Draft creation rules:
     * - Unknown type: NO draft created (ingestion still SUCCEEDED)
     * - Classifiable types (Invoice/Bill/Expense): ALWAYS create draft
     *   - meetsThreshold=false → DraftStatus.NeedsInput
     *   - meetsThreshold=true → DraftStatus.Ready or NeedsReview based on completeness
     *
     * @param runId The ingestion run ID
     * @param tenantId The tenant ID (required for draft operations)
     * @param documentId The document ID
     * @param documentType Detected document type
     * @param extractedData The extracted data structure
     * @param confidence Overall confidence score (0.0 - 1.0)
     * @param rawText Raw OCR/extracted text
     * @param description AI-generated short description (optional)
     * @param keywords AI-generated keywords (optional)
     * @param meetsThreshold Result of AI-layer threshold check (from DocumentAIResult.meetsMinimalThreshold())
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
        description: String? = null,
        keywords: List<String> = emptyList(),
        meetsThreshold: Boolean,
        force: Boolean = false
    ): Boolean {
        // Defense-in-depth: Validate tenantId is provided
        require(tenantId.isNotBlank()) { "tenantId is required for draft operations" }

        return newSuspendedTransaction {
            val now = Clock.System.now().toLocalDateTime(TimeZone.Companion.UTC)
            val runUuid = UUID.fromString(runId)
            val documentUuid = UUID.fromString(documentId)
            val tenantUuid = UUID.fromString(tenantId)
            val extractedDataJson = json.encodeToString(extractedData)
            val fieldConfidencesJson =
                extractedData.fieldConfidences.let { json.encodeToString(it) }
            val keywordsJson = keywords.takeIf { it.isNotEmpty() }?.let { json.encodeToString(it) }

            // Update the ingestion run (always, regardless of draft creation)
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

            // Skip draft creation ONLY for Unknown type
            // For all classifiable types, we ALWAYS create a draft
            if (documentType == DocumentType.Unknown) {
                // Unknown type - no draft, but ingestion is still SUCCEEDED
                // Artifacts (raw_text, raw_extraction_json) are saved for debugging
                return@newSuspendedTransaction true
            }

            // Determine draft status based on threshold and field completeness
            val calculatedStatus = if (!meetsThreshold) {
                // AI ran but threshold not met - user must fill fields manually
                DraftStatus.NeedsInput
            } else {
                // Threshold met - determine Ready vs NeedsReview based on completeness
                determineDraftStatus(documentType, extractedData)
            }

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
                    it[DocumentDraftsTable.aiDescription] = description?.takeIf { value -> value.isNotBlank() }
                    it[DocumentDraftsTable.aiKeywords] = keywordsJson
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
                    it[DocumentDraftsTable.aiDescription] = description?.takeIf { value -> value.isNotBlank() }
                    it[DocumentDraftsTable.aiKeywords] = keywordsJson
                    it[aiDraftSourceRunId] = runUuid
                }

                if (shouldSetExtracted) {
                    it[DocumentDraftsTable.extractedData] = extractedDataJson
                    // Update status when we update extracted data
                    it[draftStatus] = calculatedStatus
                    if (!description.isNullOrBlank()) {
                        it[DocumentDraftsTable.aiDescription] = description
                    }
                    if (keywordsJson != null) {
                        it[DocumentDraftsTable.aiKeywords] = keywordsJson
                    }
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
            val now = Clock.System.now().toStdlibInstant().toLocalDateTime(TimeZone.Companion.UTC)
            DocumentIngestionRunsTable.update({
                DocumentIngestionRunsTable.id eq UUID.fromString(runId)
            }) {
                it[status] = IngestionStatus.Failed
                it[finishedAt] = now
                it[errorMessage] = error
            } > 0
        }

    /**
     * Update the indexing status for an ingestion run.
     *
     * This is called after chunk indexing completes (success or failure).
     * Indexing status is tracked separately from ingestion status to allow
     * retry of RAG indexing independently.
     *
     * @param runId The ingestion run ID
     * @param status The new indexing status
     * @param chunksCount Number of chunks created (for SUCCEEDED)
     * @param errorMessage Error message (for FAILED)
     */
    @OptIn(ExperimentalTime::class)
    suspend fun updateIndexingStatus(
        runId: String,
        status: IndexingStatus,
        chunksCount: Int? = null,
        errorMessage: String? = null
    ): Boolean = newSuspendedTransaction {
        val now = Clock.System.now().toStdlibInstant().toLocalDateTime(TimeZone.Companion.UTC)
        DocumentIngestionRunsTable.update({
            DocumentIngestionRunsTable.id eq UUID.fromString(runId)
        }) {
            it[indexingStatus] = status
            it[indexedAt] = now
            if (chunksCount != null) {
                it[DocumentIngestionRunsTable.chunksCount] = chunksCount
            }
            if (errorMessage != null) {
                it[indexingErrorMessage] = errorMessage
            }
        } > 0
    }

    /**
     * Store processing trace JSON for a run.
     */
    suspend fun updateProcessingTrace(
        runId: String,
        traceJson: String?
    ): Boolean = newSuspendedTransaction {
        DocumentIngestionRunsTable.update({
            DocumentIngestionRunsTable.id eq UUID.fromString(runId)
        }) {
            it[processingTrace] = traceJson
        } > 0
    }
}
