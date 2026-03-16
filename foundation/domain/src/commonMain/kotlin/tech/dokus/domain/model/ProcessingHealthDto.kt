package tech.dokus.domain.model

import kotlinx.serialization.Serializable

/**
 * Backend recommendation for whether bulk reprocessing is warranted.
 * Driven by two independent signals:
 * - Recovery: high share of NeedsReview documents
 * - Version: documents processed with older extraction logic
 */
@Serializable
data class ProcessingHealthRecommendation(
    val recommended: Boolean,
    val totalProcessedLast30Days: Int,
    val needsReviewCount: Int,
    val failedCount: Int,
    val needsReviewPercent: Double,
    val eligibleForReprocessCount: Int,
    val currentVersion: Int,
    val reason: ReprocessRecommendationReason?,
)

@Serializable
enum class ReprocessRecommendationReason {
    HighNeedsReview,
    OutdatedProcessing,
    Both,
}

@Serializable
data class BulkReprocessRequest(
    val maxDocuments: Int = 500,
)

@Serializable
data class BulkReprocessResponse(
    val queuedCount: Int,
    val skippedCount: Int,
)
