package tech.dokus.database.tables.documents

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.domain.enums.IndexingStatus
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.foundation.backend.database.dbEnumeration

/**
 * Document ingestion runs table - append-only history of AI extraction attempts.
 *
 * OWNER: documents service
 * ACCESS: processor service (read-write for processing updates)
 * CRITICAL: All queries MUST filter by tenant_id for multi-tenant security.
 *
 * Each document can have multiple ingestion runs (for reprocessing).
 * This table stores:
 * - Run lifecycle: Queued -> Processing -> Succeeded/Failed
 * - Raw extraction outputs (immutable after finished)
 * - Confidence scores
 * - AI provider used
 * - Error messages for failed runs
 *
 * Note: queuedAt (createdAt) is when the run was queued.
 * startedAt is when processing actually began.
 * finishedAt is when processing completed (success or failure).
 */
object DocumentIngestionRunsTable : UUIDTable("document_ingestion_runs") {

    // Reference to the document being processed
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)

    // Multi-tenancy (denormalized for query performance)
    val tenantId = uuid("tenant_id").references(
        TenantTable.id,
        onDelete = ReferenceOption.CASCADE
    )

    // Run lifecycle status
    val status = dbEnumeration<IngestionStatus>("status")
        .default(IngestionStatus.Queued)

    // AI provider used (koog_local, openai, anthropic)
    val provider = varchar("ai_provider", 50).nullable()

    // Timestamps for run lifecycle
    // queuedAt = createdAt (when run was created/queued)
    val queuedAt = datetime("queued_at").defaultExpression(CurrentDateTime)
    val startedAt = datetime("started_at").nullable()
    val finishedAt = datetime("finished_at").nullable()

    // Error message (for failed runs)
    val errorMessage = text("error_message").nullable()

    // Raw OCR/extracted text (immutable after finished)
    val rawText = text("raw_text").nullable()

    // Raw extraction JSON output from AI (immutable after finished)
    val rawExtractionJson = text("raw_extraction_json").nullable()

    // Processing trace JSON (tool calls + orchestration steps)
    val processingTrace = text("processing_trace").nullable()

    // Confidence score (0.0000 - 1.0000)
    val confidence = decimal("confidence", 5, 4).nullable()

    // Per-field confidence scores as JSON
    val fieldConfidences = text("field_confidences").nullable()

    // Chunk indexing status (tracked separately from ingestion status)
    // Allows RAG indexing to be retried independently
    val indexingStatus = dbEnumeration<IndexingStatus>("indexing_status")
        .default(IndexingStatus.Pending)

    // Error message if chunk indexing failed
    val indexingErrorMessage = text("indexing_error_message").nullable()

    // When chunk indexing completed
    val indexedAt = datetime("indexed_at").nullable()

    // Number of chunks created (for diagnostics)
    val chunksCount = integer("chunks_count").nullable()

    init {
        // For processor: find runs to process by status
        index(false, tenantId, status, queuedAt)

        // For fetching latest run for a document
        index(false, documentId, finishedAt)
    }
}
