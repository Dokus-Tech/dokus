package tech.dokus.backend.services.documents

import tech.dokus.database.repository.cashflow.DocumentIngestionRunRepository
import tech.dokus.database.repository.cashflow.DocumentRepository
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.BulkReprocessResponse
import tech.dokus.domain.model.ProcessingHealthRecommendation
import tech.dokus.domain.model.ReprocessRecommendationReason
import tech.dokus.domain.processing.DocumentProcessingConstants
import tech.dokus.foundation.backend.utils.loggerFor

/**
 * Evaluates workspace processing health and executes bulk reprocessing.
 *
 * Two independent recommendation signals:
 * - **Recovery**: high share of NeedsReview documents (indicates processing quality issues)
 * - **Version**: documents processed with older extraction logic (indicates upgrade opportunity)
 *
 * Bulk reprocess safety contract:
 * - Reuses existing linked source/blob — does not re-upload or re-fetch
 * - Does not mutate the document directly — only enqueues new ingestion runs
 * - Does not overwrite confirmed truth — scoped to NeedsReview only
 * - Idempotent — skips documents with active (Queued/Processing) runs
 */
class ProcessingHealthService(
    private val documentRepository: DocumentRepository,
    private val ingestionRunRepository: DocumentIngestionRunRepository,
) {
    private val logger = loggerFor()

    companion object {
        private const val MIN_TOTAL_FOR_RECOVERY = 20
        private const val MIN_NEEDS_REVIEW_FOR_RECOVERY = 10
        private const val MIN_NEEDS_REVIEW_PERCENT = 0.30
        private const val DEFAULT_BULK_REPROCESS_LIMIT = 500
    }

    suspend fun getRecommendation(tenantId: TenantId): ProcessingHealthRecommendation {
        val currentVersion = DocumentProcessingConstants.PROCESSING_VERSION
        val stats = documentRepository.getProcessingHealthStats(tenantId, currentVersion)

        val needsReviewPercent = if (stats.totalProcessedLast30Days > 0) {
            stats.needsReviewCount.toDouble() / stats.totalProcessedLast30Days
        } else {
            0.0
        }

        val recoverySignal = stats.totalProcessedLast30Days >= MIN_TOTAL_FOR_RECOVERY &&
            stats.needsReviewCount >= MIN_NEEDS_REVIEW_FOR_RECOVERY &&
            needsReviewPercent >= MIN_NEEDS_REVIEW_PERCENT

        val versionSignal = stats.eligibleForReprocessCount > 0

        val reason = when {
            recoverySignal && versionSignal -> ReprocessRecommendationReason.Both
            recoverySignal -> ReprocessRecommendationReason.HighNeedsReview
            versionSignal -> ReprocessRecommendationReason.OutdatedProcessing
            else -> null
        }

        return ProcessingHealthRecommendation(
            recommended = reason != null,
            totalProcessedLast30Days = stats.totalProcessedLast30Days,
            needsReviewCount = stats.needsReviewCount,
            failedCount = stats.failedCount,
            needsReviewPercent = needsReviewPercent,
            eligibleForReprocessCount = stats.eligibleForReprocessCount,
            currentVersion = currentVersion,
            reason = reason,
        )
    }

    /**
     * Bulk reprocess eligible documents.
     *
     * Safety: reuses existing source, does not mutate document directly,
     * does not overwrite confirmed truth, only enqueues new ingestion runs.
     */
    suspend fun executeBulkReprocess(
        tenantId: TenantId,
        maxDocuments: Int = DEFAULT_BULK_REPROCESS_LIMIT,
    ): BulkReprocessResponse {
        val limit = maxDocuments.coerceAtLeast(1)
        val currentVersion = DocumentProcessingConstants.PROCESSING_VERSION

        val candidates = documentRepository.findDocumentsEligibleForReprocess(
            tenantId = tenantId,
            currentProcessingVersion = currentVersion,
            limit = limit,
        )

        var queued = 0
        var skipped = 0

        for (candidate in candidates) {
            try {
                ingestionRunRepository.createRun(
                    documentId = candidate.documentId,
                    tenantId = tenantId,
                    sourceId = candidate.sourceId,
                )
                queued++
            } catch (e: Exception) {
                logger.warn("Failed to queue reprocess for {}: {}", candidate.documentId, e.message)
                skipped++
            }
        }

        logger.info(
            "Bulk reprocess for tenant {}: queued={}, skipped={}, candidates={}",
            tenantId, queued, skipped, candidates.size
        )

        return BulkReprocessResponse(queuedCount = queued, skippedCount = skipped)
    }
}
