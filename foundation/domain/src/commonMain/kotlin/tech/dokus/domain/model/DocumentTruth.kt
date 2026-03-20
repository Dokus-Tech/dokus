package tech.dokus.domain.model

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import tech.dokus.domain.enums.DocumentMatchReviewStatus
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.enums.DocumentSourceStatus
import tech.dokus.domain.enums.IntakeOutcome
import tech.dokus.domain.enums.ReviewReason
import tech.dokus.domain.enums.SourceMatchKind
import tech.dokus.domain.ids.DocumentBlobId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.DocumentMatchReviewId
import tech.dokus.domain.ids.DocumentSourceId
import tech.dokus.domain.ids.TenantId

@Serializable
data class DocumentSourceDto(
    val id: DocumentSourceId,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val blobId: DocumentBlobId,
    val peppolRawUblBlobId: DocumentBlobId? = null,
    val sourceChannel: DocumentSource,
    val arrivalAt: LocalDateTime,
    val contentHash: String? = null,
    val identityKeyHash: String? = null,
    val status: DocumentSourceStatus = DocumentSourceStatus.Linked,
    val isCorrective: Boolean = false,
    val extractedSnapshotJson: String? = null,
    val peppolStructuredSnapshotJson: String? = null,
    val peppolSnapshotVersion: Int? = null,
    val detachedAt: LocalDateTime? = null,
    val filename: String? = null,
    val contentType: String? = null,
    val sizeBytes: Long? = null,
    val matchType: SourceMatchKind? = null,
) {
    companion object
}

@Serializable
data class DocumentMatchReviewSummaryDto(
    val reviewId: DocumentMatchReviewId,
    val incomingSourceId: DocumentSourceId,
    val reasonType: ReviewReason,
    val status: DocumentMatchReviewStatus,
    val createdAt: LocalDateTime
) {
    companion object
}

@Serializable
data class DocumentIntakeOutcomeDto(
    val outcome: IntakeOutcome,
    val sourceId: DocumentSourceId,
    val documentId: DocumentId,
    val linkedDocumentId: DocumentId? = null,
    val reviewId: DocumentMatchReviewId? = null,
    val sourceCount: Int = 1,
    val matchType: SourceMatchKind? = null
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
