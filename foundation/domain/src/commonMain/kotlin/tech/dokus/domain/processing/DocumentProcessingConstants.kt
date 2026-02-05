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
}
