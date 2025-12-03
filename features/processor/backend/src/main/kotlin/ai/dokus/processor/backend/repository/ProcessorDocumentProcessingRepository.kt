package ai.dokus.processor.backend.repository

import ai.dokus.foundation.domain.enums.DocumentType
import ai.dokus.foundation.domain.enums.ProcessingStatus
import ai.dokus.foundation.domain.model.ExtractedDocumentData
import ai.dokus.processor.backend.worker.ProcessingItem
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.plus
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import java.util.UUID

/**
 * Repository for document processing operations in the processor worker.
 * Uses direct database access (same database as cashflow service).
 *
 * Note: This duplicates some schema knowledge from cashflow backend,
 * but keeps the processor service self-contained.
 */
class ProcessorDocumentProcessingRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Find pending documents ready for processing.
     */
    suspend fun findPendingForProcessing(
        limit: Int = 10,
        maxAttempts: Int = 3
    ): List<ProcessingItem> = newSuspendedTransaction {
        (DocumentProcessingTable innerJoin DocumentsTable)
            .selectAll()
            .where {
                (DocumentProcessingTable.status inList listOf(
                    ProcessingStatus.Pending.dbValue,
                    ProcessingStatus.Failed.dbValue
                )) and
                        (DocumentProcessingTable.processingAttempts lessEq maxAttempts)
            }
            .orderBy(DocumentProcessingTable.createdAt to SortOrder.ASC)
            .limit(limit)
            .map { row ->
                ProcessingItem(
                    processingId = row[DocumentProcessingTable.id].value.toString(),
                    documentId = row[DocumentProcessingTable.documentId].toString(),
                    storageKey = row[DocumentsTable.storageKey],
                    filename = row[DocumentsTable.filename],
                    contentType = row[DocumentsTable.contentType],
                    attempts = row[DocumentProcessingTable.processingAttempts]
                )
            }
    }

    /**
     * Mark document as currently being processed.
     */
    suspend fun markAsProcessing(
        processingId: String,
        provider: String
    ): Boolean = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        DocumentProcessingTable.update({
            DocumentProcessingTable.id eq UUID.fromString(processingId)
        }) {
            it[status] = ProcessingStatus.Processing.dbValue
            it[processingStartedAt] = now
            it[aiProvider] = provider
            it[updatedAt] = now
        } > 0
    }

    /**
     * Mark document as successfully processed with extracted data.
     */
    suspend fun markAsProcessed(
        processingId: String,
        documentType: DocumentType,
        extractedData: ExtractedDocumentData,
        confidence: Double,
        rawText: String?
    ): Boolean = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        DocumentProcessingTable.update({
            DocumentProcessingTable.id eq UUID.fromString(processingId)
        }) {
            it[status] = ProcessingStatus.Processed.dbValue
            it[DocumentProcessingTable.documentType] = documentType.dbValue
            it[DocumentProcessingTable.extractedData] = json.encodeToString(extractedData)
            it[DocumentProcessingTable.rawText] = rawText
            it[DocumentProcessingTable.confidence] = confidence.toBigDecimal()
            it[fieldConfidences] = json.encodeToString(extractedData.fieldConfidences)
            it[lastProcessedAt] = now
            it[errorMessage] = null
            it[updatedAt] = now
        } > 0
    }

    /**
     * Mark document as failed with error message.
     */
    suspend fun markAsFailed(
        processingId: String,
        error: String
    ): Boolean = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        DocumentProcessingTable.update({
            DocumentProcessingTable.id eq UUID.fromString(processingId)
        }) {
            it[status] = ProcessingStatus.Failed.dbValue
            it[errorMessage] = error
            it[lastProcessedAt] = now
            it[processingAttempts] = DocumentProcessingTable.processingAttempts + 1
            it[updatedAt] = now
        } > 0
    }
}

// Minimal table definitions needed by the processor
// These mirror the tables from cashflow backend

private object DocumentProcessingTable : UUIDTable("document_processing") {
    val documentId = uuid("document_id")
    val tenantId = uuid("tenant_id")
    val status = varchar("status", 20)
    val documentType = varchar("document_type", 20).nullable()
    val extractedData = text("extracted_data").nullable()
    val rawText = text("raw_text").nullable()
    val confidence = decimal("confidence", 5, 4).nullable()
    val fieldConfidences = text("field_confidences").nullable()
    val processingAttempts = integer("processing_attempts").default(0)
    val lastProcessedAt = datetime("last_processed_at").nullable()
    val processingStartedAt = datetime("processing_started_at").nullable()
    val errorMessage = text("error_message").nullable()
    val aiProvider = varchar("ai_provider", 50).nullable()
    val confirmedAt = datetime("confirmed_at").nullable()
    val confirmedEntityType = varchar("confirmed_entity_type", 20).nullable()
    val confirmedEntityId = uuid("confirmed_entity_id").nullable()
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)
}

private object DocumentsTable : UUIDTable("documents") {
    val tenantId = uuid("tenant_id")
    val filename = varchar("filename", 255)
    val contentType = varchar("content_type", 100)
    val sizeBytes = long("size_bytes")
    val storageKey = varchar("storage_key", 512)
    val entityType = varchar("entity_type", 50).nullable()
    val entityId = varchar("entity_id", 100).nullable()
    val uploadedAt = datetime("uploaded_at").defaultExpression(CurrentDateTime)
}
