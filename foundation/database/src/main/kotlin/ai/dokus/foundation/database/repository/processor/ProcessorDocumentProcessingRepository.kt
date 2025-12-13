package ai.dokus.foundation.database.repository.processor

import ai.dokus.foundation.database.tables.cashflow.DocumentProcessingTable
import ai.dokus.foundation.database.tables.cashflow.DocumentsTable
import ai.dokus.foundation.domain.enums.DocumentType
import ai.dokus.foundation.domain.enums.ProcessingStatus
import ai.dokus.foundation.domain.model.ExtractedDocumentData
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.plus
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update
import java.util.UUID

/**
 * Item representing a document ready for processing.
 */
data class ProcessingItem(
    val processingId: String,
    val documentId: String,
    val storageKey: String,
    val filename: String,
    val contentType: String,
    val attempts: Int
)

/**
 * Repository for document processing operations in the processor worker.
 * Uses shared tables from foundation/database module.
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
                    ProcessingStatus.Pending,
                    ProcessingStatus.Failed
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
            it[status] = ProcessingStatus.Processing
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
            it[status] = ProcessingStatus.Processed
            it[DocumentProcessingTable.documentType] = documentType
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
            it[status] = ProcessingStatus.Failed
            it[errorMessage] = error
            it[lastProcessedAt] = now
            it[processingAttempts] = DocumentProcessingTable.processingAttempts + 1
            it[updatedAt] = now
        } > 0
    }
}
