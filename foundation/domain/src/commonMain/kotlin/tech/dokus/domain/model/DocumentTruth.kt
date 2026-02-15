package tech.dokus.domain.model

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import tech.dokus.domain.enums.DocumentIntakeOutcome
import tech.dokus.domain.enums.DocumentMatchReviewReasonType
import tech.dokus.domain.enums.DocumentMatchReviewStatus
import tech.dokus.domain.enums.DocumentMatchType
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.enums.DocumentSourceStatus
import tech.dokus.domain.ids.DocumentBlobId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.DocumentMatchReviewId
import tech.dokus.domain.ids.DocumentSourceId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId

@Serializable
data class DocumentSourceDto(
    val id: DocumentSourceId,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val blobId: DocumentBlobId,
    val sourceChannel: DocumentSource,
    val arrivalAt: LocalDateTime,
    val contentHash: String? = null,
    val identityKeyHash: String? = null,
    val status: DocumentSourceStatus = DocumentSourceStatus.Linked,
    val isCorrective: Boolean = false,
    val extractedSnapshotJson: String? = null,
    val detachedAt: LocalDateTime? = null,
    val filename: String? = null,
    val contentType: String? = null,
    val sizeBytes: Long? = null,
    val matchType: DocumentMatchType? = null,
)

@Serializable
data class DocumentMatchReviewDto(
    val id: DocumentMatchReviewId,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val incomingSourceId: DocumentSourceId,
    val reasonType: DocumentMatchReviewReasonType,
    val aiSummary: String? = null,
    val aiConfidence: Double? = null,
    val status: DocumentMatchReviewStatus,
    val resolvedBy: UserId? = null,
    val resolvedAt: LocalDateTime? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

@Serializable
data class DocumentMatchReviewSummaryDto(
    val reviewId: DocumentMatchReviewId,
    val reasonType: DocumentMatchReviewReasonType,
    val status: DocumentMatchReviewStatus,
    val createdAt: LocalDateTime
)

@Serializable
data class DocumentIntakeOutcomeDto(
    val outcome: DocumentIntakeOutcome,
    val sourceId: DocumentSourceId,
    val documentId: DocumentId,
    val linkedDocumentId: DocumentId? = null,
    val reviewId: DocumentMatchReviewId? = null,
    val sourceCount: Int = 1,
    val matchType: DocumentMatchType? = null
)

@Serializable
data class DocumentIntakeResult(
    val document: DocumentDto,
    val intake: DocumentIntakeOutcomeDto
)

@Serializable
enum class DocumentMatchResolutionDecision {
    SAME,
    DIFFERENT
}

@Serializable
data class ResolveDocumentMatchReviewRequest(
    val decision: DocumentMatchResolutionDecision
)
