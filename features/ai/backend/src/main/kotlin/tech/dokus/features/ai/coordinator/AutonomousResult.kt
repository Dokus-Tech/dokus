package tech.dokus.features.ai.coordinator

import tech.dokus.features.ai.ensemble.ConflictReport
import tech.dokus.features.ai.judgment.JudgmentDecision
import tech.dokus.features.ai.judgment.JudgmentOutcome
import tech.dokus.features.ai.models.ClassifiedDocumentType
import tech.dokus.features.ai.models.DocumentClassification
import tech.dokus.features.ai.retry.RetryResult
import tech.dokus.features.ai.validation.AuditReport

/**
 * Result from the Autonomous Processing Pipeline.
 *
 * This sealed hierarchy captures all possible outcomes:
 * - **Success**: Full pipeline completed, judgment made
 * - **PartialSuccess**: Pipeline completed but with issues
 * - **Rejected**: Pipeline stopped early due to unrecoverable issues
 *
 * ## Design Philosophy
 *
 * The result is designed to support the "Silence" philosophy:
 * - AUTO_APPROVE results can be processed without user intervention
 * - NEEDS_REVIEW results contain enough context for efficient human review
 * - REJECTED results explain why processing failed
 *
 * ## Usage
 *
 * ```kotlin
 * when (val result = coordinator.process(images, context)) {
 *     is AutonomousResult.Success -> {
 *         if (result.isAutoApproved) {
 *             // Silent success - create draft automatically
 *         } else {
 *             // Show to user with result.judgment.issuesForUser
 *         }
 *     }
 *     is AutonomousResult.Rejected -> {
 *         // Log result.reason, notify user
 *     }
 * }
 * ```
 *
 * Note: This class is intentionally not serializable because it contains
 * generic types and is primarily a runtime model. If serialization is needed,
 * convert to a DTO first.
 */
sealed class AutonomousResult {

    /**
     * Successfully processed through all 5 layers.
     *
     * @param T The type of extracted data
     */
    data class Success<T>(
        // =========================================================================
        // Layer 0: Classification
        // =========================================================================

        /** Document classification from Layer 0 */
        val classification: DocumentClassification,

        // =========================================================================
        // Layers 1-2: Perception + Consensus
        // =========================================================================

        /** Merged extraction from consensus engine */
        val extraction: T,

        /** Conflicts between fast and expert models (null if single model or unanimous) */
        val conflictReport: ConflictReport?,

        // =========================================================================
        // Layer 3: Validation
        // =========================================================================

        /** Audit report from legally-aware auditor */
        val auditReport: AuditReport,

        // =========================================================================
        // Layer 4: Self-Correction
        // =========================================================================

        /** Result of self-correction attempts (null if not needed or disabled) */
        val retryResult: RetryResult<T>?,

        // =========================================================================
        // Layer 5: Judgment
        // =========================================================================

        /** Final judgment decision */
        val judgment: JudgmentDecision
    ) : AutonomousResult() {

        // =========================================================================
        // Convenience Properties
        // =========================================================================

        /** Whether this extraction was auto-approved (silent success) */
        val isAutoApproved: Boolean
            get() = judgment.outcome == JudgmentOutcome.AUTO_APPROVE

        /** Whether this extraction needs human review */
        val needsReview: Boolean
            get() = judgment.outcome == JudgmentOutcome.NEEDS_REVIEW

        /** Whether this extraction was rejected by judgment */
        val isRejected: Boolean
            get() = judgment.outcome == JudgmentOutcome.REJECT

        /** Document type from classification */
        val documentType: ClassifiedDocumentType
            get() = classification.documentType

        /** Whether self-correction was performed */
        val wasCorrected: Boolean
            get() = retryResult is RetryResult.CorrectedOnRetry

        /** Fields that were corrected during retry (empty if no correction) */
        val correctedFields: List<String>
            get() = when (val r = retryResult) {
                is RetryResult.CorrectedOnRetry -> r.correctedFields
                else -> emptyList()
            }

        /** Total retry attempts made */
        val retryAttempts: Int
            get() = when (val r = retryResult) {
                is RetryResult.CorrectedOnRetry -> r.attempt
                is RetryResult.StillFailing -> r.attempts
                is RetryResult.NoRetryNeeded, null -> 0
            }

        /** Whether models had any conflicts (even if resolved) */
        val hadConflicts: Boolean
            get() = conflictReport?.hasConflicts == true

        /** Overall processing confidence (from judgment) */
        val confidence: Double
            get() = judgment.confidence
    }

    /**
     * Processing was rejected early - could not complete pipeline.
     *
     * This happens when:
     * - Document type is UNKNOWN and failFastOnUnknownType is true
     * - Classification confidence is below minimum threshold
     * - Both models failed to extract data
     * - Essential structural requirements not met
     */
    data class Rejected(
        /** Human-readable reason for rejection */
        val reason: String,

        /** Classification result (if available) */
        val classification: DocumentClassification?,

        /** Stage at which rejection occurred */
        val stage: RejectionStage,

        /** Additional context for debugging */
        val details: Map<String, String> = emptyMap()
    ) : AutonomousResult() {

        /** Document type if classification succeeded */
        val documentType: ClassifiedDocumentType?
            get() = classification?.documentType

        companion object {
            /**
             * Create rejection for unknown document type.
             */
            fun unknownDocumentType(classification: DocumentClassification) = Rejected(
                reason = "Could not determine document type",
                classification = classification,
                stage = RejectionStage.CLASSIFICATION
            )

            /**
             * Create rejection for low classification confidence.
             */
            fun lowConfidence(classification: DocumentClassification, threshold: Double) = Rejected(
                reason = "Classification confidence ${formatPercent(classification.confidence)} " +
                    "is below threshold ${formatPercent(threshold)}",
                classification = classification,
                stage = RejectionStage.CLASSIFICATION,
                details = mapOf(
                    "confidence" to classification.confidence.toString(),
                    "threshold" to threshold.toString()
                )
            )

            /**
             * Create rejection for extraction failure.
             */
            fun extractionFailed(
                classification: DocumentClassification,
                fastError: Throwable?,
                expertError: Throwable?
            ) = Rejected(
                reason = "Both models failed to extract data",
                classification = classification,
                stage = RejectionStage.EXTRACTION,
                details = buildMap {
                    fastError?.let { put("fastError", it.message ?: "Unknown error") }
                    expertError?.let { put("expertError", it.message ?: "Unknown error") }
                }
            )

            /**
             * Create rejection for no data extracted.
             */
            fun noDataExtracted(classification: DocumentClassification) = Rejected(
                reason = "No data could be extracted from the document",
                classification = classification,
                stage = RejectionStage.EXTRACTION
            )

            private fun formatPercent(value: Double): String = "${(value * 100).toInt()}%"
        }
    }
}

/**
 * Stage at which processing was rejected.
 */
enum class RejectionStage {
    /** Rejected during document classification */
    CLASSIFICATION,

    /** Rejected during extraction (both models failed) */
    EXTRACTION,

    /** Rejected during validation (should rarely happen - usually goes to judgment) */
    VALIDATION
}

/**
 * Summary statistics for a batch of autonomous processing results.
 */
data class AutonomousProcessingStats(
    val totalProcessed: Int,
    val autoApproved: Int,
    val needsReview: Int,
    val rejected: Int,
    val earlyRejected: Int,
    val averageConfidence: Double,
    val averageRetryAttempts: Double
) {
    /** Percentage of documents that were auto-approved */
    val autoApproveRate: Double
        get() = if (totalProcessed > 0) autoApproved.toDouble() / totalProcessed else 0.0

    /** Percentage of documents requiring review */
    val reviewRate: Double
        get() = if (totalProcessed > 0) needsReview.toDouble() / totalProcessed else 0.0

    /** Percentage of documents rejected */
    val rejectionRate: Double
        get() = if (totalProcessed > 0) (rejected + earlyRejected).toDouble() / totalProcessed else 0.0

    /** Whether the "Silence" goal is being met (95%+ auto-approve) */
    val meetsSilenceGoal: Boolean
        get() = autoApproveRate >= 0.95

    companion object {
        /**
         * Calculate statistics from a list of results.
         */
        fun <T> from(results: List<AutonomousResult>): AutonomousProcessingStats {
            val successes = results.filterIsInstance<AutonomousResult.Success<*>>()
            val rejections = results.filterIsInstance<AutonomousResult.Rejected>()

            return AutonomousProcessingStats(
                totalProcessed = results.size,
                autoApproved = successes.count { it.isAutoApproved },
                needsReview = successes.count { it.needsReview },
                rejected = successes.count { it.isRejected },
                earlyRejected = rejections.size,
                averageConfidence = successes
                    .map { it.confidence }
                    .average()
                    .takeIf { !it.isNaN() } ?: 0.0,
                averageRetryAttempts = successes
                    .map { it.retryAttempts.toDouble() }
                    .average()
                    .takeIf { !it.isNaN() } ?: 0.0
            )
        }
    }
}
