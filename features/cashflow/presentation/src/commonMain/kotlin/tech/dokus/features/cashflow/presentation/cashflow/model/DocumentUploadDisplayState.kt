package tech.dokus.features.cashflow.presentation.cashflow.model

import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.model.DocumentDto

private const val PercentageMultiplier = 100
private const val MinPercentage = 0
private const val MaxPercentage = 100
private const val DeleteCountdownSeconds = 5

/**
 * Display state for a document upload item in the UI.
 *
 * This sealed hierarchy represents all possible visual states a document upload
 * can be in, enabling exhaustive handling in composables and type-safe
 * state transitions.
 *
 * State transitions:
 * - Pending -> Uploading -> Uploaded
 * - Uploading -> Failed -> (retry) -> Uploading
 * - Uploaded -> Deleting -> (timeout) -> removed
 * - Deleting -> (undo) -> Uploaded
 */
sealed interface DocumentUploadDisplayState {
    /**
     * Stable identifier for this item across state transitions.
     */
    val id: String

    /**
     * File name for display.
     */
    val fileName: String

    /**
     * File size in bytes.
     */
    val fileSize: Long

    /**
     * Upload is pending, waiting in queue.
     */
    data class Pending(
        override val id: String,
        override val fileName: String,
        override val fileSize: Long,
        val task: DocumentUploadTask
    ) : DocumentUploadDisplayState

    /**
     * Upload is in progress with current progress.
     */
    data class Uploading(
        override val id: String,
        override val fileName: String,
        override val fileSize: Long,
        val task: DocumentUploadTask,
        val progress: Float
    ) : DocumentUploadDisplayState {
        val progressPercent: Int get() = (progress * PercentageMultiplier).toInt().coerceIn(
            MinPercentage,
            MaxPercentage
        )
    }

    /**
     * Upload failed with error message.
     */
    data class Failed(
        override val id: String,
        override val fileName: String,
        override val fileSize: Long,
        val task: DocumentUploadTask,
        val error: DokusException
    ) : DocumentUploadDisplayState {
        val canRetry: Boolean get() = task.canManualRetry
    }

    /**
     * Upload completed, document is available.
     */
    data class Uploaded(
        override val id: String,
        override val fileName: String,
        override val fileSize: Long,
        val document: DocumentDto
    ) : DocumentUploadDisplayState

    /**
     * Upload was linked to an existing canonical document.
     */
    data class Linked(
        override val id: String,
        override val fileName: String,
        override val fileSize: Long,
        val document: DocumentDto,
        val otherSources: Int
    ) : DocumentUploadDisplayState

    /**
     * Upload requires a possible-match review decision.
     */
    data class NeedsReview(
        override val id: String,
        override val fileName: String,
        override val fileSize: Long,
        val document: DocumentDto
    ) : DocumentUploadDisplayState

    /**
     * Document is being deleted with countdown for undo.
     */
    data class Deleting(
        override val id: String,
        override val fileName: String,
        override val fileSize: Long,
        val document: DocumentDto,
        val progress: Float
    ) : DocumentUploadDisplayState {
        /** Remaining seconds (approximate, based on 5-second total) */
        val remainingSeconds: Int get() = (progress * DeleteCountdownSeconds).toInt()
    }
}
