package ai.dokus.foundation.database.repository.cashflow

import ai.dokus.foundation.database.tables.cashflow.DocumentProcessingTable
import ai.dokus.foundation.database.tables.cashflow.DocumentsTable
import ai.dokus.foundation.domain.enums.DocumentType
import ai.dokus.foundation.domain.enums.EntityType
import ai.dokus.foundation.domain.enums.ProcessingStatus
import ai.dokus.foundation.domain.ids.ContactId
import ai.dokus.foundation.domain.ids.DocumentId
import ai.dokus.foundation.domain.ids.DocumentProcessingId
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.ids.UserId
import tech.dokus.domain.model.DocumentDto
import tech.dokus.domain.model.DocumentProcessingDto
import tech.dokus.domain.model.DocumentProcessingSummary
import tech.dokus.domain.model.ExtractedDocumentData
import tech.dokus.domain.model.TrackedCorrection
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.plus
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi

/**
 * Repository for document processing operations.
 *
 * CRITICAL SECURITY: All queries MUST filter by tenantId for multi-tenant isolation.
 */
@OptIn(ExperimentalUuidApi::class)
class DocumentProcessingRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Create a new processing record for an uploaded document.
     */
    suspend fun create(
        documentId: DocumentId,
        tenantId: TenantId
    ): DocumentProcessingDto = newSuspendedTransaction {
        val id = UUID.randomUUID()
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        DocumentProcessingTable.insert {
            it[DocumentProcessingTable.id] = id
            it[DocumentProcessingTable.documentId] = UUID.fromString(documentId.toString())
            it[DocumentProcessingTable.tenantId] = UUID.fromString(tenantId.toString())
            it[status] = ProcessingStatus.Pending
            it[processingAttempts] = 0
            it[createdAt] = now
            it[updatedAt] = now
        }

        DocumentProcessingDto(
            id = DocumentProcessingId.parse(id.toString()),
            documentId = documentId,
            tenantId = tenantId,
            status = ProcessingStatus.Pending,
            processingAttempts = 0,
            createdAt = now,
            updatedAt = now
        )
    }

    /**
     * Get processing record by ID.
     * CRITICAL: MUST filter by tenantId.
     */
    suspend fun getById(
        processingId: DocumentProcessingId,
        tenantId: TenantId,
        includeDocument: Boolean = false
    ): DocumentProcessingDto? = newSuspendedTransaction {
        val query = if (includeDocument) {
            (DocumentProcessingTable innerJoin DocumentsTable)
                .selectAll()
                .where {
                    (DocumentProcessingTable.id eq UUID.fromString(processingId.toString())) and
                            (DocumentProcessingTable.tenantId eq UUID.fromString(tenantId.toString()))
                }
        } else {
            DocumentProcessingTable.selectAll().where {
                (DocumentProcessingTable.id eq UUID.fromString(processingId.toString())) and
                        (DocumentProcessingTable.tenantId eq UUID.fromString(tenantId.toString()))
            }
        }

        query.singleOrNull()?.toProcessingDto(includeDocument)
    }

    /**
     * Get processing record by document ID.
     * CRITICAL: MUST filter by tenantId.
     */
    suspend fun getByDocumentId(
        documentId: DocumentId,
        tenantId: TenantId,
        includeDocument: Boolean = false
    ): DocumentProcessingDto? = newSuspendedTransaction {
        val query = if (includeDocument) {
            (DocumentProcessingTable innerJoin DocumentsTable)
                .selectAll()
                .where {
                    (DocumentProcessingTable.documentId eq UUID.fromString(documentId.toString())) and
                            (DocumentProcessingTable.tenantId eq UUID.fromString(tenantId.toString()))
                }
        } else {
            DocumentProcessingTable.selectAll().where {
                (DocumentProcessingTable.documentId eq UUID.fromString(documentId.toString())) and
                        (DocumentProcessingTable.tenantId eq UUID.fromString(tenantId.toString()))
            }
        }

        query.singleOrNull()?.toProcessingDto(includeDocument)
    }

    /**
     * List processing records by status with pagination.
     * CRITICAL: MUST filter by tenantId.
     */
    suspend fun listByStatus(
        tenantId: TenantId,
        statuses: List<ProcessingStatus>,
        limit: Int = 20,
        offset: Int = 0,
        includeDocument: Boolean = true
    ): Pair<List<DocumentProcessingDto>, Long> = newSuspendedTransaction {
        val baseQuery = if (includeDocument) {
            (DocumentProcessingTable innerJoin DocumentsTable)
                .selectAll()
                .where {
                    (DocumentProcessingTable.tenantId eq UUID.fromString(tenantId.toString())) and
                            (DocumentProcessingTable.status inList statuses)
                }
        } else {
            DocumentProcessingTable.selectAll().where {
                (DocumentProcessingTable.tenantId eq UUID.fromString(tenantId.toString())) and
                        (DocumentProcessingTable.status inList statuses)
            }
        }

        val total = baseQuery.count()

        val items = baseQuery
            .orderBy(DocumentProcessingTable.createdAt to SortOrder.DESC)
            .limit(limit)
            .offset(offset.toLong())
            .map { it.toProcessingDto(includeDocument) }

        items to total
    }

    /**
     * Find pending documents ready for processing.
     * Used by background job to pick up work.
     */
    suspend fun findPendingForProcessing(
        limit: Int = 10,
        maxAttempts: Int = 3
    ): List<DocumentProcessingDto> = newSuspendedTransaction {
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
            .map { it.toProcessingDto(includeDocument = true) }
    }

    /**
     * Update status to Queued when picked up by background job.
     */
    suspend fun markAsQueued(
        processingId: DocumentProcessingId
    ): Boolean = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        DocumentProcessingTable.update({
            DocumentProcessingTable.id eq UUID.fromString(processingId.toString())
        }) {
            it[status] = ProcessingStatus.Queued
            it[updatedAt] = now
        } > 0
    }

    /**
     * Update status to Processing when AI extraction starts.
     */
    suspend fun markAsProcessing(
        processingId: DocumentProcessingId,
        provider: String
    ): Boolean = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        DocumentProcessingTable.update({
            DocumentProcessingTable.id eq UUID.fromString(processingId.toString())
        }) {
            it[status] = ProcessingStatus.Processing
            it[processingStartedAt] = now
            it[aiProvider] = provider
            it[updatedAt] = now
        } > 0
    }

    /**
     * Update with successful extraction results.
     */
    suspend fun markAsProcessed(
        processingId: DocumentProcessingId,
        documentType: DocumentType,
        extractedData: ExtractedDocumentData,
        confidence: Double,
        rawText: String?
    ): Boolean = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        DocumentProcessingTable.update({
            DocumentProcessingTable.id eq UUID.fromString(processingId.toString())
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
     * Update with failed extraction.
     */
    suspend fun markAsFailed(
        processingId: DocumentProcessingId,
        error: String
    ): Boolean = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        DocumentProcessingTable.update({
            DocumentProcessingTable.id eq UUID.fromString(processingId.toString())
        }) {
            it[status] = ProcessingStatus.Failed
            it[errorMessage] = error
            it[lastProcessedAt] = now
            it[processingAttempts] = DocumentProcessingTable.processingAttempts + 1
            it[updatedAt] = now
        } > 0
    }

    /**
     * Mark as confirmed after user reviews and creates entity.
     */
    suspend fun markAsConfirmed(
        processingId: DocumentProcessingId,
        tenantId: TenantId,
        entityType: EntityType,
        entityId: UUID
    ): Boolean = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        DocumentProcessingTable.update({
            (DocumentProcessingTable.id eq UUID.fromString(processingId.toString())) and
                    (DocumentProcessingTable.tenantId eq UUID.fromString(tenantId.toString()))
        }) {
            it[status] = ProcessingStatus.Confirmed
            it[confirmedAt] = now
            it[confirmedEntityType] = entityType
            it[confirmedEntityId] = entityId
            it[updatedAt] = now
        } > 0
    }

    /**
     * Mark as rejected when user chooses manual entry.
     */
    suspend fun markAsRejected(
        processingId: DocumentProcessingId,
        tenantId: TenantId
    ): Boolean = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        DocumentProcessingTable.update({
            (DocumentProcessingTable.id eq UUID.fromString(processingId.toString())) and
                    (DocumentProcessingTable.tenantId eq UUID.fromString(tenantId.toString()))
        }) {
            it[status] = ProcessingStatus.Rejected
            it[updatedAt] = now
        } > 0
    }

    /**
     * Update the extracted data draft with user corrections.
     * Preserves the original AI draft for audit trail.
     *
     * @param processingId The processing record ID
     * @param tenantId The tenant ID (for security)
     * @param userId The user making the edit
     * @param updatedData The new extracted data with user corrections
     * @param corrections List of tracked corrections for audit trail
     * @return The new draft version number, or null if update failed
     */
    suspend fun updateDraft(
        processingId: DocumentProcessingId,
        tenantId: TenantId,
        userId: UserId,
        updatedData: ExtractedDocumentData,
        corrections: List<TrackedCorrection>
    ): Int? = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        // First, get the current record to check if AI draft needs to be preserved
        val current = DocumentProcessingTable.selectAll().where {
            (DocumentProcessingTable.id eq UUID.fromString(processingId.toString())) and
                    (DocumentProcessingTable.tenantId eq UUID.fromString(tenantId.toString()))
        }.singleOrNull() ?: return@newSuspendedTransaction null

        val currentVersion = current[DocumentProcessingTable.draftVersion]
        val newVersion = currentVersion + 1

        // If this is the first edit, preserve the original AI draft
        val currentAiDraft = current[DocumentProcessingTable.aiDraftData]
        val aiDraftToStore = if (currentAiDraft == null) {
            // First edit - save current extractedData as aiDraftData
            current[DocumentProcessingTable.extractedData]
        } else {
            // Already have AI draft preserved - keep it
            currentAiDraft
        }

        // Merge new corrections with existing ones
        val existingCorrections = current[DocumentProcessingTable.userCorrections]?.let {
            try {
                json.decodeFromString<List<TrackedCorrection>>(it)
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()

        val allCorrections = existingCorrections + corrections

        val updated = DocumentProcessingTable.update({
            (DocumentProcessingTable.id eq UUID.fromString(processingId.toString())) and
                    (DocumentProcessingTable.tenantId eq UUID.fromString(tenantId.toString()))
        }) {
            it[extractedData] = json.encodeToString(updatedData)
            it[aiDraftData] = aiDraftToStore
            it[userCorrections] = json.encodeToString(allCorrections)
            it[draftVersion] = newVersion
            it[draftEditedAt] = now
            it[draftEditedBy] = UUID.fromString(userId.toString())
            it[updatedAt] = now
        }

        if (updated > 0) newVersion else null
    }

    /**
     * Reset to pending for reprocessing.
     */
    suspend fun resetForReprocessing(
        processingId: DocumentProcessingId,
        tenantId: TenantId
    ): Boolean = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        DocumentProcessingTable.update({
            (DocumentProcessingTable.id eq UUID.fromString(processingId.toString())) and
                    (DocumentProcessingTable.tenantId eq UUID.fromString(tenantId.toString()))
        }) {
            it[status] = ProcessingStatus.Pending
            it[extractedData] = null
            it[rawText] = null
            it[confidence] = null
            it[fieldConfidences] = null
            it[documentType] = null
            it[errorMessage] = null
            it[processingStartedAt] = null
            it[lastProcessedAt] = null
            it[aiProvider] = null
            // Reset contact suggestion
            it[suggestedContactId] = null
            it[contactSuggestionConfidence] = null
            it[contactSuggestionReason] = null
            it[updatedAt] = now
        } > 0
    }

    /**
     * Update the suggested contact for a processing record.
     * Called after AI extraction to populate contact suggestion.
     */
    suspend fun updateContactSuggestion(
        processingId: DocumentProcessingId,
        tenantId: TenantId,
        suggestedContactId: ContactId?,
        confidence: Float?,
        reason: String?
    ): Boolean = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        DocumentProcessingTable.update({
            (DocumentProcessingTable.id eq UUID.fromString(processingId.toString())) and
                    (DocumentProcessingTable.tenantId eq UUID.fromString(tenantId.toString()))
        }) {
            it[DocumentProcessingTable.suggestedContactId] = suggestedContactId?.let { id ->
                UUID.fromString(id.toString())
            }
            it[contactSuggestionConfidence] = confidence
            it[contactSuggestionReason] = reason
            it[updatedAt] = now
        } > 0
    }

    /**
     * Get summary list for quick display.
     */
    suspend fun listSummaries(
        tenantId: TenantId,
        statuses: List<ProcessingStatus>? = null,
        limit: Int = 50
    ): List<DocumentProcessingSummary> = newSuspendedTransaction {
        val query = (DocumentProcessingTable innerJoin DocumentsTable)
            .selectAll()
            .where {
                DocumentProcessingTable.tenantId eq UUID.fromString(tenantId.toString())
            }

        val filtered = if (statuses != null) {
            query.andWhere { DocumentProcessingTable.status inList statuses }
        } else {
            query
        }

        filtered
            .orderBy(DocumentProcessingTable.createdAt to SortOrder.DESC)
            .limit(limit)
            .map { row ->
                DocumentProcessingSummary(
                    id = DocumentProcessingId.parse(row[DocumentProcessingTable.id].value.toString()),
                    documentId = DocumentId.parse(row[DocumentProcessingTable.documentId].toString()),
                    status = row[DocumentProcessingTable.status],
                    documentType = row[DocumentProcessingTable.documentType],
                    confidence = row[DocumentProcessingTable.confidence]?.toDouble(),
                    filename = row[DocumentsTable.filename],
                    createdAt = row[DocumentProcessingTable.createdAt],
                    errorMessage = row[DocumentProcessingTable.errorMessage]
                )
            }
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private fun ResultRow.toProcessingDto(includeDocument: Boolean): DocumentProcessingDto {
        val extractedDataJson = this[DocumentProcessingTable.extractedData]
        val extractedData = extractedDataJson?.let {
            try {
                json.decodeFromString<ExtractedDocumentData>(it)
            } catch (e: Exception) {
                null
            }
        }

        return DocumentProcessingDto(
            id = DocumentProcessingId.parse(this[DocumentProcessingTable.id].value.toString()),
            documentId = DocumentId.parse(this[DocumentProcessingTable.documentId].toString()),
            tenantId = TenantId.parse(this[DocumentProcessingTable.tenantId].toString()),
            status = this[DocumentProcessingTable.status],
            documentType = this[DocumentProcessingTable.documentType],
            extractedData = extractedData,
            confidence = this[DocumentProcessingTable.confidence]?.toDouble(),
            processingAttempts = this[DocumentProcessingTable.processingAttempts],
            lastProcessedAt = this[DocumentProcessingTable.lastProcessedAt],
            processingStartedAt = this[DocumentProcessingTable.processingStartedAt],
            errorMessage = this[DocumentProcessingTable.errorMessage],
            aiProvider = this[DocumentProcessingTable.aiProvider],
            // Contact suggestion fields
            suggestedContactId = this[DocumentProcessingTable.suggestedContactId]?.let {
                ContactId.parse(it.toString())
            },
            contactSuggestionConfidence = this[DocumentProcessingTable.contactSuggestionConfidence],
            contactSuggestionReason = this[DocumentProcessingTable.contactSuggestionReason],
            confirmedAt = this[DocumentProcessingTable.confirmedAt],
            confirmedEntityType = this[DocumentProcessingTable.confirmedEntityType],
            confirmedEntityId = this[DocumentProcessingTable.confirmedEntityId]?.toString(),
            createdAt = this[DocumentProcessingTable.createdAt],
            updatedAt = this[DocumentProcessingTable.updatedAt],
            document = if (includeDocument) toDocumentDto() else null
        )
    }

    private fun ResultRow.toDocumentDto(): DocumentDto {
        return DocumentDto(
            id = DocumentId.parse(this[DocumentsTable.id].value.toString()),
            tenantId = TenantId.parse(this[DocumentsTable.tenantId].toString()),
            filename = this[DocumentsTable.filename],
            contentType = this[DocumentsTable.contentType],
            sizeBytes = this[DocumentsTable.sizeBytes],
            storageKey = this[DocumentsTable.storageKey],
            entityType = this[DocumentsTable.entityType],
            entityId = this[DocumentsTable.entityId],
            uploadedAt = this[DocumentsTable.uploadedAt]
        )
    }
}
