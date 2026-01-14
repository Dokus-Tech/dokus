package tech.dokus.features.ai.retry

import kotlinx.serialization.Serializable
import tech.dokus.features.ai.validation.AuditCheck

/**
 * Result of a self-correction retry attempt (Layer 4).
 *
 * @param T The type of extracted data
 */
sealed class RetryResult<out T> {

    /**
     * No retry was needed - the extraction passed all checks on the first attempt.
     */
    data object NoRetryNeeded : RetryResult<Nothing>()

    /**
     * The extraction was corrected after one or more retry attempts.
     *
     * @param data The corrected extracted data
     * @param attempt Which retry attempt succeeded (1-based)
     * @param correctedFields List of field names that were corrected
     * @param originalFailures The failures that triggered the retry
     */
    data class CorrectedOnRetry<T>(
        val data: T,
        val attempt: Int,
        val correctedFields: List<String>,
        val originalFailures: List<AuditCheck>
    ) : RetryResult<T>()

    /**
     * The extraction still has failures after exhausting all retry attempts.
     *
     * @param data The best extraction attempt (may still have errors)
     * @param attempts Number of retry attempts made
     * @param remainingFailures Failures that could not be corrected
     */
    data class StillFailing<T>(
        val data: T,
        val attempts: Int,
        val remainingFailures: List<AuditCheck>
    ) : RetryResult<T>()

    /**
     * Get the data if available (null for NoRetryNeeded).
     */
    fun dataOrNull(): T? = when (this) {
        is NoRetryNeeded -> null
        is CorrectedOnRetry -> data
        is StillFailing -> data
    }

    /**
     * Whether the retry was successful (either no retry needed or corrected).
     */
    val isSuccess: Boolean
        get() = this is NoRetryNeeded || this is CorrectedOnRetry
}

/**
 * Configuration for the self-correction retry agent.
 */
@Serializable
data class RetryConfig(
    /** Maximum number of retry attempts before giving up */
    val maxRetries: Int = 2,

    /** Whether to retry only on critical failures or also warnings */
    val retryOnWarnings: Boolean = false,

    /** Minimum confidence improvement required to accept a retry result */
    val minConfidenceImprovement: Double = 0.0
) {
    companion object {
        val DEFAULT = RetryConfig()

        /** Aggressive retry configuration for high-value documents */
        val AGGRESSIVE = RetryConfig(
            maxRetries = 3,
            retryOnWarnings = true,
            minConfidenceImprovement = 0.05
        )
    }
}
