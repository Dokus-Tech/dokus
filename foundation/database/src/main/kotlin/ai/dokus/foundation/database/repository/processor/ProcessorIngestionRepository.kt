package ai.dokus.foundation.database.repository.processor

import ai.dokus.foundation.database.tables.cashflow.DocumentDraftsTable
import ai.dokus.foundation.database.tables.cashflow.DocumentIngestionRunsTable
import ai.dokus.foundation.database.tables.cashflow.DocumentsTable
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.DraftStatus
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.model.ExtractedDocumentData
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.upsert
import java.util.UUID

/**
 * Item representing a document ready for processing.
 * Contains all info needed by the worker to process a document.
 */
data class IngestionItem(
    val runId: String,
    val documentId: String,
    val tenantId: String,
    val storageKey: String,
    val filename: String,
    val contentType: String
)

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
     * Find pending ingestion runs ready for processing.
     * Only picks up runs with status=Queued, ordered by queue time (FIFO).
     */
    suspend fun findPendingForProcessing(limit: Int = 10): List<IngestionItem> =
        newSuspendedTransaction {
            (DocumentIngestionRunsTable innerJoin DocumentsTable)
                .selectAll()
                .where {
                    DocumentIngestionRunsTable.status eq IngestionStatus.Queued
                }
                .orderBy(DocumentIngestionRunsTable.queuedAt to SortOrder.ASC)
                .limit(limit)
                .map { row ->
                    IngestionItem(
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
    suspend fun markAsProcessing(runId: String, provider: String): Boolean =
        newSuspendedTransaction {
            val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
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
    ): Boolean = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val runUuid = UUID.fromString(runId)
        val documentUuid = UUID.fromString(documentId)
        val tenantUuid = UUID.fromString(tenantId)
        val extractedDataJson = json.encodeToString(extractedData)
        val fieldConfidencesJson = extractedData.fieldConfidences?.let { json.encodeToString(it) }

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

        // Check if draft exists and get current state
        val existingDraft = DocumentDraftsTable.selectAll()
            .where { DocumentDraftsTable.documentId eq documentUuid }
            .singleOrNull()

        if (existingDraft == null) {
            // Create new draft - set both ai_draft_data and extracted_data
            DocumentDraftsTable.insert {
                it[DocumentDraftsTable.documentId] = documentUuid
                it[DocumentDraftsTable.tenantId] = tenantUuid
                it[draftStatus] = DraftStatus.NeedsReview
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

            DocumentDraftsTable.update({
                DocumentDraftsTable.documentId eq documentUuid
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
                }
            }
        }

        true
    }

    /**
     * Mark an ingestion run as failed.
     */
    suspend fun markAsFailed(runId: String, error: String): Boolean =
        newSuspendedTransaction {
            val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
            DocumentIngestionRunsTable.update({
                DocumentIngestionRunsTable.id eq UUID.fromString(runId)
            }) {
                it[status] = IngestionStatus.Failed
                it[finishedAt] = now
                it[errorMessage] = error
            } > 0
        }
}
