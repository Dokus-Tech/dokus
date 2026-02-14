package tech.dokus.domain.processing

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

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
    val INGESTION_RUN_TIMEOUT: Duration = 15.minutes

    /**
     * Canonical error shown when a run exceeded [INGESTION_RUN_TIMEOUT].
     */
    val INGESTION_TIMEOUT_ERROR_MESSAGE: String =
        "Processing timed out after ${INGESTION_RUN_TIMEOUT.inWholeMinutes} minutes"
}
