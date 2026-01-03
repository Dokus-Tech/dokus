package tech.dokus.domain.model

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.DraftStatus
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.IngestionRunId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId

/**
 * Document ingestion run DTO - represents a single AI extraction attempt.
 * Multiple runs can exist per document (for reprocessing history).
 */
@Serializable
data class DocumentIngestionDto(
    val id: IngestionRunId,
    val documentId: DocumentId,
    val tenantId: TenantId,
    val status: IngestionStatus,
    val provider: String?,
    val queuedAt: LocalDateTime,
    val startedAt: LocalDateTime?,
    val finishedAt: LocalDateTime?,
    val errorMessage: String?,
    val confidence: Double?
)

/**
 * Document draft DTO - represents the editable extraction state.
 * One draft per document.
 */
@Serializable
data class DocumentDraftDto(
    val documentId: DocumentId,
    val tenantId: TenantId,
    val draftStatus: DraftStatus,
    val documentType: DocumentType?,
    val extractedData: ExtractedDocumentData?,
    val aiDraftData: ExtractedDocumentData?, // Original immutable AI extraction (for diff display)
    val aiDraftSourceRunId: IngestionRunId?, // Which run produced ai_draft_data
    val draftVersion: Int,
    val draftEditedAt: LocalDateTime?,
    val draftEditedBy: UserId?,
    val suggestedContactId: ContactId?,
    val contactSuggestionConfidence: Float?,
    val contactSuggestionReason: String?,
    val lastSuccessfulRunId: IngestionRunId?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

/**
 * Document record DTO - envelope containing full document state.
 * Used as the consistent response type for all document endpoints.
 *
 * - document: File metadata (always present)
 * - draft: Editable extraction state (present if document has been processed)
 * - latestIngestion: Current/last ingestion run (present if any runs exist)
 *   - Selection priority: Processing > latest Succeeded/Failed > latest Queued
 * - confirmedEntity: The created Invoice/Bill/Expense (present if confirmed)
 */
@Serializable
data class DocumentRecordDto(
    val document: DocumentDto,
    val draft: DocumentDraftDto?,
    val latestIngestion: DocumentIngestionDto?,
    val confirmedEntity: FinancialDocumentDto?
)

/**
 * Request to reprocess a document.
 *
 * @property force If true, create new run even if one is already processing
 * @property preferredProvider AI provider to use (future feature)
 * @property maxPages Override max pages to process (null = use default 10)
 * @property dpi Override DPI for PDF rendering (null = use default 150)
 * @property timeoutSeconds Override base timeout in seconds (null = use default 60)
 */
@Serializable
data class ReprocessRequest(
    val force: Boolean = false,
    val preferredProvider: String? = null,
    val maxPages: Int? = null,
    val dpi: Int? = null,
    val timeoutSeconds: Int? = null
)

/**
 * Response from reprocessing a document.
 */
@Serializable
data class ReprocessResponse(
    val runId: IngestionRunId,
    val status: IngestionStatus,
    val message: String,
    val isExistingRun: Boolean = false
)

/**
 * Request to update a document draft.
 */
@Serializable
data class UpdateDraftRequest(
    val extractedData: ExtractedDocumentData,
    val changeDescription: String? = null
)

/**
 * Response from updating a document draft.
 */
@Serializable
data class UpdateDraftResponse(
    val documentId: DocumentId,
    val draftVersion: Int,
    val extractedData: ExtractedDocumentData,
    val updatedAt: LocalDateTime
)

/**
 * Request to confirm a document and create a financial entity.
 */
@Serializable
data class ConfirmDocumentRequest(
    val documentType: DocumentType,
    val extractedData: ExtractedDocumentData? = null // Optional overrides
)

/**
 * Filters for listing documents.
 */
@Serializable
data class DocumentFilters(
    val draftStatus: DraftStatus? = null,
    val documentType: DocumentType? = null,
    val ingestionStatus: IngestionStatus? = null,
    val search: String? = null
)
