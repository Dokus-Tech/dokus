package tech.dokus.features.ai.judgment

import kotlinx.serialization.Serializable
import tech.dokus.features.ai.ensemble.ConflictReport
import tech.dokus.features.ai.retry.RetryResult
import tech.dokus.features.ai.validation.AuditReport

/**
 * Layer 5: Judgment Agent Models
 *
 * The Judgment Agent is the final gatekeeper that decides whether a document
 * extraction can be auto-approved or needs human review.
 *
 * Philosophy: "Silence"
 * The goal is for 95%+ of documents to be processed silently (AUTO_APPROVE)
 * without user intervention. Users only see documents when the system cannot
 * prove correctness.
 */

/**
 * Final judgment outcome for a document.
 */
enum class JudgmentOutcome {
    /**
     * Document processed successfully - user never sees it.
     * Target: 95%+ of documents.
     *
     * Criteria:
     * - All critical audits passed
     * - No unresolved critical conflicts
     * - Confidence >= 0.8
     */
    AUTO_APPROVE,

    /**
     * Document needs human review before processing.
     * User sees specific issues highlighted.
     *
     * Criteria:
     * - Warning-level issues present
     * - Moderate confidence (0.5-0.8)
     * - Some fields missing but document is processable
     */
    NEEDS_REVIEW,

    /**
     * Document cannot be processed automatically.
     * Requires manual handling.
     *
     * Criteria:
     * - Critical audit failures remain after retries
     * - Essential fields missing (amount, vendor, date)
     * - Confidence < 0.5
     * - Document type unknown
     */
    REJECT
}

/**
 * Complete judgment decision with reasoning.
 */
@Serializable
data class JudgmentDecision(
    /** The final outcome */
    val outcome: JudgmentOutcome,

    /** Confidence in this decision (0.0 to 1.0) */
    val confidence: Double,

    /** Brief explanation of why this decision was made */
    val reasoning: String,

    /** Specific issues for user to review (only for NEEDS_REVIEW) */
    val issuesForUser: List<String> = emptyList(),

    /** Fields that were auto-corrected during retry (informational) */
    val correctedFields: List<String> = emptyList(),

    /** Whether all critical validations passed */
    val allCriticalChecksPassed: Boolean,

    /** Whether both models agreed (consensus) */
    val hasModelConsensus: Boolean,

    /** Number of retry attempts made */
    val retryAttempts: Int = 0
) {
    companion object {
        /**
         * Create an AUTO_APPROVE decision.
         */
        fun autoApprove(
            confidence: Double,
            reasoning: String,
            correctedFields: List<String> = emptyList(),
            retryAttempts: Int = 0
        ) = JudgmentDecision(
            outcome = JudgmentOutcome.AUTO_APPROVE,
            confidence = confidence,
            reasoning = reasoning,
            issuesForUser = emptyList(),
            correctedFields = correctedFields,
            allCriticalChecksPassed = true,
            hasModelConsensus = true,
            retryAttempts = retryAttempts
        )

        /**
         * Create a NEEDS_REVIEW decision.
         */
        fun needsReview(
            confidence: Double,
            reasoning: String,
            issues: List<String>,
            allCriticalChecksPassed: Boolean = true,
            hasModelConsensus: Boolean = true,
            retryAttempts: Int = 0
        ) = JudgmentDecision(
            outcome = JudgmentOutcome.NEEDS_REVIEW,
            confidence = confidence,
            reasoning = reasoning,
            issuesForUser = issues,
            correctedFields = emptyList(),
            allCriticalChecksPassed = allCriticalChecksPassed,
            hasModelConsensus = hasModelConsensus,
            retryAttempts = retryAttempts
        )

        /**
         * Create a REJECT decision.
         */
        fun reject(
            confidence: Double,
            reasoning: String,
            issues: List<String> = emptyList(),
            retryAttempts: Int = 0
        ) = JudgmentDecision(
            outcome = JudgmentOutcome.REJECT,
            confidence = confidence,
            reasoning = reasoning,
            issuesForUser = issues,
            correctedFields = emptyList(),
            allCriticalChecksPassed = false,
            hasModelConsensus = false,
            retryAttempts = retryAttempts
        )
    }
}

/**
 * Input context for the Judgment Agent.
 * Aggregates all information from previous layers.
 */
data class JudgmentContext(
    /** Extraction confidence (from Layer 1/2) */
    val extractionConfidence: Double,

    /** Consensus report from Layer 2 (null if single source) */
    val consensusReport: ConflictReport?,

    /** Audit report from Layer 3 */
    val auditReport: AuditReport,

    /** Retry result from Layer 4 (null if no retry attempted) */
    val retryResult: RetryResult<*>?,

    /** Document type that was classified */
    val documentType: String,

    /** Whether essential fields are present */
    val hasEssentialFields: Boolean,

    /** List of missing essential fields */
    val missingEssentialFields: List<String> = emptyList()
)

/**
 * Configuration for the Judgment Agent.
 */
@Serializable
data class JudgmentConfig(
    /** Minimum confidence for AUTO_APPROVE */
    val autoApproveMinConfidence: Double = 0.8,

    /** Minimum confidence for NEEDS_REVIEW (below this = REJECT) */
    val needsReviewMinConfidence: Double = 0.5,

    /** Whether to auto-approve with warnings (default: yes, if no critical) */
    val autoApproveWithWarnings: Boolean = true,

    /** Maximum number of warning-level issues for auto-approve */
    val maxWarningsForAutoApprove: Int = 3,

    /** Whether model consensus is required for auto-approve */
    val requireConsensusForAutoApprove: Boolean = false
) {
    companion object {
        val DEFAULT = JudgmentConfig()

        /** Strict configuration - requires higher confidence */
        val STRICT = JudgmentConfig(
            autoApproveMinConfidence = 0.9,
            needsReviewMinConfidence = 0.6,
            autoApproveWithWarnings = false,
            requireConsensusForAutoApprove = true
        )

        /** Lenient configuration - auto-approves more documents */
        val LENIENT = JudgmentConfig(
            autoApproveMinConfidence = 0.7,
            needsReviewMinConfidence = 0.4,
            autoApproveWithWarnings = true,
            maxWarningsForAutoApprove = 5
        )
    }
}
