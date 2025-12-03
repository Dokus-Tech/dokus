package ai.dokus.foundation.domain.model

import ai.dokus.foundation.domain.enums.DocumentType
import ai.dokus.foundation.domain.enums.EntityType
import ai.dokus.foundation.domain.enums.ProcessingStatus
import ai.dokus.foundation.domain.ids.DocumentId
import ai.dokus.foundation.domain.ids.DocumentProcessingId
import ai.dokus.foundation.domain.ids.TenantId
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

/**
 * Document processing state and extraction results.
 */
@Serializable
data class DocumentProcessingDto(
    /** Processing record ID */
    val id: DocumentProcessingId,

    /** Reference to the uploaded document */
    val documentId: DocumentId,

    /** Tenant for multi-tenant isolation */
    val tenantId: TenantId,

    /** Current processing status */
    val status: ProcessingStatus,

    /** Detected document type from AI extraction */
    val documentType: DocumentType? = null,

    /** Extracted data (includes invoice/bill/expense fields + raw text) */
    val extractedData: ExtractedDocumentData? = null,

    /** Overall extraction confidence (0.0 - 1.0) */
    val confidence: Double? = null,

    /** Number of processing attempts */
    val processingAttempts: Int = 0,

    /** When processing last completed (success or failure) */
    val lastProcessedAt: LocalDateTime? = null,

    /** When processing started (for timeout tracking) */
    val processingStartedAt: LocalDateTime? = null,

    /** Error message if processing failed */
    val errorMessage: String? = null,

    /** Which AI provider was used (koog_local, openai, anthropic) */
    val aiProvider: String? = null,

    /** When user confirmed the extraction */
    val confirmedAt: LocalDateTime? = null,

    /** Entity type created after confirmation */
    val confirmedEntityType: EntityType? = null,

    /** Entity ID created after confirmation */
    val confirmedEntityId: String? = null,

    /** When processing record was created */
    val createdAt: LocalDateTime,

    /** When processing record was last updated */
    val updatedAt: LocalDateTime,

    /** Embedded document info for convenience (optional, loaded when needed) */
    val document: DocumentDto? = null
)

/**
 * Summary view for document processing lists.
 * Lighter weight than full DocumentProcessingDto.
 */
@Serializable
data class DocumentProcessingSummary(
    val id: DocumentProcessingId,
    val documentId: DocumentId,
    val status: ProcessingStatus,
    val documentType: DocumentType?,
    val confidence: Double?,
    val filename: String,
    val createdAt: LocalDateTime,
    val errorMessage: String? = null
)

/**
 * Paginated response for document processing queries.
 */
@Serializable
data class DocumentProcessingListResponse(
    val items: List<DocumentProcessingDto>,
    val total: Long,
    val page: Int,
    val limit: Int,
    val hasMore: Boolean
)

/**
 * Request to reprocess a document.
 */
@Serializable
data class ReprocessDocumentRequest(
    /** Force reprocessing even if already processed */
    val force: Boolean = false,

    /** Preferred AI provider (optional override) */
    val preferredProvider: String? = null
)

/**
 * Response after reprocessing is queued.
 */
@Serializable
data class ReprocessDocumentResponse(
    val processingId: DocumentProcessingId,
    val status: ProcessingStatus,
    val message: String
)
