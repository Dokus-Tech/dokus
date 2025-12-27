package ai.dokus.app.cashflow.model

import tech.dokus.domain.model.DocumentDto

/**
 * Display state for a document upload item in the UI.
 *
 * This sealed hierarchy represents all possible visual states a document upload
 * can be in, enabling exhaustive handling in composables and type-safe
 * state transitions.
 *
 * State transitions:
 * - Pending → Uploading → Uploaded
 * - Uploading → Failed → (retry) → Uploading
 * - Uploaded → Deleting → (timeout) → removed
 * - Deleting → (undo) → Uploaded
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
     * Formatted file size for display (e.g., "1.5 MB").
     */
    val formattedSize: String
        get() = when {
            fileSize < 1024 -> "$fileSize B"
            fileSize < 1024 * 1024 -> {
                val kb = fileSize / 1024.0
                "${((kb * 10).toInt() / 10.0)} KB"
            }
            else -> {
                val mb = fileSize / (1024.0 * 1024.0)
                "${((mb * 10).toInt() / 10.0)} MB"
            }
        }

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
        val progressPercent: Int get() = (progress * 100).toInt().coerceIn(0, 100)
    }

    /**
     * Upload failed with error message.
     */
    data class Failed(
        override val id: String,
        override val fileName: String,
        override val fileSize: Long,
        val task: DocumentUploadTask,
        val error: String
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
        val remainingSeconds: Int get() = (progress * 5).toInt()
    }
}
