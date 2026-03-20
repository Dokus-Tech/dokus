package tech.dokus.database.entity

import kotlinx.datetime.LocalDateTime
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.DocumentMatchReviewStatus
import tech.dokus.domain.enums.DocumentPurposeSource
import tech.dokus.domain.enums.DocumentRejectReason
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.enums.DocumentSourceStatus
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.enums.ProcessingOutcome
import tech.dokus.domain.enums.PurposePeriodMode
import tech.dokus.domain.enums.ReviewReason
import tech.dokus.domain.enums.SourceMatchKind
import tech.dokus.domain.ids.DocumentBlobId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.DocumentMatchReviewId
import tech.dokus.domain.ids.DocumentSourceId
import tech.dokus.domain.ids.IngestionRunId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.contact.CounterpartyInfo

data class DocumentBlobEntity(
    val id: DocumentBlobId,
    val tenantId: TenantId,
    val inputHash: String,
    val storageKey: String,
    val contentType: String,
    val sizeBytes: Long
) {
    companion object
}

data class DocumentSourceEntity(
    val id: DocumentSourceId,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val blobId: DocumentBlobId,
    val peppolRawUblBlobId: DocumentBlobId? = null,
    val sourceChannel: DocumentSource,
    val arrivalAt: LocalDateTime,
    val contentHash: String?,
    val identityKeyHash: String?,
    val status: DocumentSourceStatus,
    val matchType: SourceMatchKind?,
    val isCorrective: Boolean,
    val extractedSnapshotJson: String?,
    val peppolStructuredSnapshotJson: String? = null,
    val peppolSnapshotVersion: Int? = null,
    val detachedAt: LocalDateTime?,
    val normalizedSupplierVat: String?,
    val normalizedDocumentNumber: String?,
    val documentType: DocumentType?,
    val direction: DocumentDirection?,
    val filename: String?,
    val inputHash: String,
    val storageKey: String,
    val contentType: String,
    val sizeBytes: Long
) {
    companion object
}

data class DocumentMatchReviewEntity(
    val id: DocumentMatchReviewId,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val incomingSourceId: DocumentSourceId,
    val reasonType: ReviewReason,
    val aiSummary: String?,
    val aiConfidence: Double?,
    val status: DocumentMatchReviewStatus,
    val resolvedBy: UserId?,
    val resolvedAt: LocalDateTime?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object
}

data class DocumentPurposeTemplateEntity(
    val tenantId: TenantId,
    val counterpartyKey: String,
    val documentType: DocumentType,
    val purposeBase: String,
    val periodMode: PurposePeriodMode,
    val confidence: Double,
    val usageCount: Int
) {
    companion object
}

/**
 * Data class for draft summary.
 */
data class DraftSummaryEntity(
    val documentId: DocumentId,
    val tenantId: TenantId,
    val documentStatus: DocumentStatus,
    val documentType: DocumentType?,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val aiKeywords: List<String> = emptyList(),
    val purposeBase: String? = null,
    val purposePeriodYear: Int? = null,
    val purposePeriodMonth: Int? = null,
    val purposeRendered: String? = null,
    val purposeSource: DocumentPurposeSource? = null,
    val purposeLocked: Boolean = false,
    val purposePeriodMode: PurposePeriodMode = PurposePeriodMode.IssueMonth,
    val counterpartyKey: String? = null,
    val merchantToken: String? = null,
    val aiDraftSourceRunId: IngestionRunId?,
    val draftVersion: Int,
    val draftEditedAt: LocalDateTime?,
    val draftEditedBy: UserId?,
    val counterparty: CounterpartyInfo? = null,
    val counterpartyDisplayName: String? = null,
    val rejectReason: DocumentRejectReason?,
    val lastSuccessfulRunId: IngestionRunId?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object
}

/**
 * Data class for ingestion run summary.
 */
data class IngestionRunSummaryEntity(
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
    val processingOutcome: ProcessingOutcome?,
    val rawExtractionJson: String? = null,
    val processingTrace: String? = null
) {
    companion object
}
