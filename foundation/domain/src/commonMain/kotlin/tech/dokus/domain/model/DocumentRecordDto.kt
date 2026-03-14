package tech.dokus.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import tech.dokus.domain.Money
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.DocumentPurposeSource
import tech.dokus.domain.enums.DocumentRejectReason
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.enums.ProcessingOutcome
import tech.dokus.domain.enums.PurposePeriodMode
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.IngestionRunId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.contact.CounterpartyInfo
import kotlin.time.Instant

/**
 * Processing trace step for document ingestion.
 */
@Serializable
data class DocumentProcessingStepDto(
    val step: Int,
    val action: String,
    val tool: String? = null,
    val timestamp: Instant,
    val durationMs: Long,
    val input: JsonElement? = null,
    val output: JsonElement? = null,
    val notes: String? = null
)

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
    val confidence: Double?,
    val processingOutcome: ProcessingOutcome? = null,
    val rawExtraction: JsonElement? = null,
    val processingTrace: List<DocumentProcessingStepDto>? = null
)

/**
 * Document draft DTO - represents the editable extraction state.
 * One draft per document.
 */
@Serializable
data class DocumentDraftDto(
    val documentId: DocumentId,
    val tenantId: TenantId,
    val documentStatus: DocumentStatus,
    val documentType: DocumentType?,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val extractedData: DocumentDraftData?,
    val aiKeywords: List<String> = emptyList(),
    val purposeBase: String? = null,
    val purposePeriodYear: Int? = null,
    val purposePeriodMonth: Int? = null,
    val purposeRendered: String? = null,
    val purposeSource: DocumentPurposeSource? = null,
    val purposeLocked: Boolean = false,
    val purposePeriodMode: PurposePeriodMode = PurposePeriodMode.IssueMonth,
    val aiDraftSourceRunId: IngestionRunId?, // Which run first produced the AI draft
    val draftVersion: Int,
    val draftEditedAt: LocalDateTime?,
    val draftEditedBy: UserId?,
    val counterparty: CounterpartyInfo? = null,
    val counterpartyDisplayName: String? = null,
    val rejectReason: DocumentRejectReason? = null,
    val lastSuccessfulRunId: IngestionRunId?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

/**
 * Flattened DTO for document list endpoints.
 * Contains only the fields needed for rendering a document row in a list/table.
 * No nested sub-objects — all fields are top-level for efficient serialization.
 */
@Serializable
data class DocumentListItemDto(
    val documentId: DocumentId,
    val tenantId: TenantId,
    val filename: String,
    val documentType: DocumentType?,
    val direction: DocumentDirection?,
    val documentStatus: DocumentStatus?,
    val ingestionStatus: IngestionStatus?,
    val effectiveOrigin: DocumentSource,
    val uploadedAt: LocalDateTime,
    val counterpartyDisplayName: String?,
    val purposeRendered: String?,
    val totalAmount: Money?,
    val currency: Currency?,
    val sortDate: LocalDate,
    val downloadUrl: String? = null,
    val hasPendingMatchReview: Boolean = false,
    val sourceCount: Int = 1,
    val cashflowEntryId: CashflowEntryId? = null,
)

/**
 * Full document detail DTO - envelope containing complete document state.
 * Used for single-document detail/review endpoints.
 *
 * - document: Canonical document metadata (always present)
 * - draft: Editable extraction state (present if document has been processed)
 * - latestIngestion: Current/last ingestion run (present if any runs exist)
 *   - Selection priority: Processing > latest Succeeded/Failed > latest Queued
 * - confirmedEntity: The created Invoice/Expense (present if confirmed)
 * - cashflowEntryId: The created cashflow entry ID (present if confirmed)
 */
@Serializable
data class DocumentDetailDto(
    val document: DocumentDto,
    val draft: DocumentDraftDto?,
    val latestIngestion: DocumentIngestionDto?,
    val confirmedEntity: FinancialDocumentDto?,
    val cashflowEntryId: CashflowEntryId? = null,
    val pendingMatchReview: DocumentMatchReviewSummaryDto? = null,
    val sources: List<DocumentSourceDto> = emptyList()
)

@Serializable
data class DocumentCountsResponse(
    val total: Long,
    val needsAttention: Long,
    val confirmed: Long
)

/**
 * Request to reprocess a document.
 *
 * @property force If true, create new run even if one is already processing
 * @property preferredProvider AI provider to use (future feature)
 * @property maxPages Override max pages to process (null = use default 10)
 * @property dpi Override DPI for PDF rendering (null = use default 150)
 * @property timeoutSeconds Reserved for compatibility. Ignored while global timeout policy is active.
 */
@Serializable
data class ReprocessRequest(
    val force: Boolean = false,
    val preferredProvider: String? = null,
    val maxPages: Int? = null,
    val dpi: Int? = null,
    val timeoutSeconds: Int? = null,
    val userFeedback: String? = null
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
    val extractedData: DocumentDraftData? = null,
    val contactId: String? = null,
    val pendingCreation: Boolean? = null,
    val purpose: String? = null,
    val purposePeriodMode: PurposePeriodMode? = null,
    val changeDescription: String? = null
)

/**
 * Response from updating a document draft.
 */
@Serializable
data class UpdateDraftResponse(
    val documentId: DocumentId,
    val draftVersion: Int,
    val extractedData: DocumentDraftData,
    val updatedAt: LocalDateTime
)

/**
 * Request to reject a document draft with a reason.
 */
@Serializable
data class RejectDocumentRequest(
    val reason: DocumentRejectReason
)

/**
 * Filters for listing documents.
 */
@Serializable
data class DocumentFilters(
    val documentStatus: DocumentStatus? = null,
    val documentType: DocumentType? = null,
    val ingestionStatus: IngestionStatus? = null,
    val search: String? = null
)
