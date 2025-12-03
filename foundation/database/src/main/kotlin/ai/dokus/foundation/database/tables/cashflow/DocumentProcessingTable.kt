package ai.dokus.foundation.database.tables.cashflow

import ai.dokus.foundation.domain.enums.DocumentType
import ai.dokus.foundation.domain.enums.EntityType
import ai.dokus.foundation.domain.enums.ProcessingStatus
import ai.dokus.foundation.ktor.database.dbEnumeration
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime

/**
 * Document processing table - tracks AI extraction status and results.
 *
 * OWNER: cashflow service
 * ACCESS: processor service (read-write for processing updates)
 * CRITICAL: All queries MUST filter by tenant_id for multi-tenant security.
 *
 * This table links to DocumentsTable and stores:
 * - Processing status (pending → queued → processing → processed/failed → confirmed/rejected)
 * - Extracted data as JSON
 * - Raw text from document for future AI use
 * - Confidence scores
 * - AI provider used
 * - Confirmation status when user reviews
 */
object DocumentProcessingTable : UUIDTable("document_processing") {

    // Reference to the document being processed
    val documentId = uuid("document_id")
        .references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
        .uniqueIndex()

    // Multi-tenancy (denormalized for query performance)
    val tenantId = uuid("tenant_id")

    // Processing state
    val status = dbEnumeration<ProcessingStatus>("status")
        .default(ProcessingStatus.Pending)

    // Detected document type
    val documentType = dbEnumeration<DocumentType>("document_type")
        .nullable()

    // Extraction results as JSON (ExtractedDocumentData serialized)
    val extractedData = text("extracted_data").nullable()

    // Full OCR/extracted text - stored separately for size
    val rawText = text("raw_text").nullable()

    // Confidence score (0.0000 - 1.0000)
    val confidence = decimal("confidence", 5, 4).nullable()

    // Per-field confidence scores as JSON
    val fieldConfidences = text("field_confidences").nullable()

    // Processing metadata
    val processingAttempts = integer("processing_attempts").default(0)
    val lastProcessedAt = datetime("last_processed_at").nullable()
    val processingStartedAt = datetime("processing_started_at").nullable()
    val errorMessage = text("error_message").nullable()

    // AI provider used (koog_local, openai, anthropic)
    val aiProvider = varchar("ai_provider", 50).nullable()

    // Confirmation - when user reviews and confirms extraction
    val confirmedAt = datetime("confirmed_at").nullable()
    val confirmedEntityType = dbEnumeration<EntityType>("confirmed_entity_type").nullable()
    val confirmedEntityId = uuid("confirmed_entity_id").nullable()

    // Timestamps
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        // CRITICAL: Index tenant_id + status for main query pattern
        index(false, tenantId, status)

        // For background job: find pending/failed documents to process
        index(false, status, processingAttempts)

        // For looking up processing by document
        index(false, documentId)

        // For cleanup: find old unconfirmed documents
        index(false, tenantId, confirmedAt)
    }
}
