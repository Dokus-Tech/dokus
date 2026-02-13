package tech.dokus.domain.processing

/**
 * Shared document processing thresholds.
 *
 * Single source of truth for confidence-based decisions.
 */
object DocumentProcessingConstants {
    /**
     * Confidence threshold for auto-confirm eligibility.
     * Values below this require manual confirmation/review.
     */
    const val AUTO_CONFIRM_CONFIDENCE_THRESHOLD = 0.90

    /**
     * Maximum allowed runtime for a single ingestion run before it is considered stuck.
     */
    const val INGESTION_RUN_TIMEOUT_MINUTES = 15L
    const val INGESTION_RUN_TIMEOUT_MS = INGESTION_RUN_TIMEOUT_MINUTES * 60_000L

    /**
     * Canonical error shown when a run exceeded [INGESTION_RUN_TIMEOUT_MINUTES].
     */
    fun ingestionTimeoutErrorMessage(): String =
        "Processing timed out after $INGESTION_RUN_TIMEOUT_MINUTES minutes"
}
