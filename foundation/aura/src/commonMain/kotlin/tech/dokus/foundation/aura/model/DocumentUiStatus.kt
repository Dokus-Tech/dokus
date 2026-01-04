package tech.dokus.foundation.aura.model

/**
 * UI-specific status for document processing display.
 * Maps from domain models (IngestionStatus, DraftStatus) to user-facing states.
 *
 * Status priority (first matching wins):
 * 1. Failed - ingestion failed or has error message
 * 2. Queued - ingestion pending or not started
 * 3. Processing - ingestion actively running
 * 4. Ready - succeeded with linked contact and high confidence
 * 5. Review - succeeded but needs user attention
 */
enum class DocumentUiStatus {
    /** Document is queued for processing */
    Queued,

    /** Document is actively being processed */
    Processing,

    /** Document needs user review (low confidence or missing data) */
    Review,

    /** Document is ready for confirmation */
    Ready,

    /** Processing failed */
    Failed
}

/**
 * Constants for document processing status determination.
 */
object DocumentProcessingConstants {
    /**
     * Confidence threshold for "Ready" status eligibility.
     * Documents with extraction confidence >= this value AND a linked contact
     * are marked as "Ready to confirm" rather than "Needs attention".
     * Note: Documents still require explicit user confirmation - this only
     * affects the status badge display.
     */
    const val READY_STATUS_CONFIDENCE_THRESHOLD = 0.90
}
