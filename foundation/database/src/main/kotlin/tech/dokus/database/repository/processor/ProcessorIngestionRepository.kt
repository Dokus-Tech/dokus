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
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.IndexingStatus
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.enums.ProcessingOutcome
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.IngestionRunId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.ExtractedDocumentData
import tech.dokus.domain.processing.DocumentProcessingConstants
import tech.dokus.domain.utils.json
import java.util.*
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

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

    /**
     * Find pending ingestion runs ready for processing.
     * Only picks up runs with status=Queued, ordered by queue time (FIFO).
     */
    @OptIn(ExperimentalUuidApi::class)
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
                        runId = IngestionRunId(row[DocumentIngestionRunsTable.id].value.toKotlinUuid()),
                        documentId = DocumentId(row[DocumentIngestionRunsTable.documentId].toKotlinUuid()),
                        tenantId = TenantId(row[DocumentIngestionRunsTable.tenantId].toKotlinUuid()),
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
     * - Always create a draft (including Unknown type)
     * - Unknown type → DocumentStatus.NeedsReview
     * - confidence < threshold → DocumentStatus.NeedsReview
     * - confidence >= threshold → DocumentStatus.Confirmed
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
            val outcome = if (documentType == DocumentType.Unknown) {
                ProcessingOutcome.ManualReviewRequired
            } else if (confidence >= DocumentProcessingConstants.AUTO_CONFIRM_CONFIDENCE_THRESHOLD) {
                ProcessingOutcome.AutoConfirmEligible
            } else {
                ProcessingOutcome.ManualReviewRequired
            }

            // Update the ingestion run (always, regardless of draft creation)
            val runUpdated = DocumentIngestionRunsTable.update({
                DocumentIngestionRunsTable.id eq runUuid
            }) {
                it[status] = IngestionStatus.Succeeded
                it[finishedAt] = now
                it[DocumentIngestionRunsTable.rawText] = rawText
                it[rawExtractionJson] = extractedDataJson
                it[DocumentIngestionRunsTable.confidence] = confidence.toBigDecimal()
                it[processingOutcome] = outcome
                it[fieldConfidences] = fieldConfidencesJson
                it[errorMessage] = null
            } > 0

            if (!runUpdated) return@newSuspendedTransaction false

            // Determine draft status based on confidence threshold.
            val calculatedStatus = when {
                documentType == DocumentType.Unknown -> {
                    // Unknown classification requires manual review.
                    DocumentStatus.NeedsReview
                }
                confidence >= DocumentProcessingConstants.AUTO_CONFIRM_CONFIDENCE_THRESHOLD -> {
                    // High confidence - auto-confirm.
                    DocumentStatus.Confirmed
                }
                else -> {
                    // Low confidence - user review required.
                    DocumentStatus.NeedsReview
                }
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
                    it[documentStatus] = calculatedStatus
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
                        it[documentStatus] = calculatedStatus
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
