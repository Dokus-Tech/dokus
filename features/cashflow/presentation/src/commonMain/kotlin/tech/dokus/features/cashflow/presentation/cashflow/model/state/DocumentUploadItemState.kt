package tech.dokus.features.cashflow.presentation.cashflow.model.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import tech.dokus.domain.enums.DocumentIntakeOutcome
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.model.DocumentDto
import tech.dokus.features.cashflow.presentation.cashflow.model.DocumentDeletionHandle
import tech.dokus.features.cashflow.presentation.cashflow.model.DocumentUploadDisplayState
import tech.dokus.features.cashflow.presentation.cashflow.model.DocumentUploadTask
import tech.dokus.features.cashflow.presentation.cashflow.model.UploadStatus
import tech.dokus.features.cashflow.presentation.cashflow.model.manager.DocumentUploadManager
import kotlin.time.Duration.Companion.milliseconds

/**
 * Component-level state holder for a single document upload item.
 *
 * Manages the display state computation from task + document + deletion handle,
 * and provides action methods for the UI to invoke.
 *
 * Each item is independent from the ViewModel and manages its own:
 * - Display state computation
 * - Deletion countdown animation
 * - Action dispatching to the upload manager
 */
@Stable
class DocumentUploadItemState(
    val taskId: String,
    private val uploadManager: DocumentUploadManager,
    private val scope: CoroutineScope
) {
    private val _displayState = MutableStateFlow<DocumentUploadDisplayState?>(null)
    val displayState: StateFlow<DocumentUploadDisplayState?> = _displayState.asStateFlow()

    private var deletionCountdownJob: Job? = null
    private var currentTask: DocumentUploadTask? = null
    private var currentDocument: DocumentDto? = null
    private var currentDeletionHandle: DocumentDeletionHandle? = null
    private var fallbackError: DokusException = DokusException.DocumentUploadFailed

    /**
     * Updates the state with new data from the manager.
     */
    fun update(
        task: DocumentUploadTask?,
        document: DocumentDto?,
        deletionHandle: DocumentDeletionHandle?,
        fallbackError: DokusException
    ) {
        currentTask = task
        currentDocument = document
        this.fallbackError = fallbackError

        // Handle deletion countdown changes
        if (deletionHandle != currentDeletionHandle) {
            deletionCountdownJob?.cancel()
            currentDeletionHandle = deletionHandle

            if (deletionHandle != null) {
                startDeletionCountdown(deletionHandle)
            }
        }

        // Compute display state (unless we're in deletion countdown mode)
        if (deletionHandle == null) {
            _displayState.value = computeDisplayState(task, document, null)
        }
    }

    /**
     * Cancels the current upload (for pending/uploading states).
     */
    fun cancelUpload() {
        uploadManager.cancelUpload(taskId)
    }

    /**
     * Retries a failed upload.
     */
    fun retry() {
        uploadManager.retryUpload(taskId)
    }

    /**
     * Initiates deletion with undo countdown.
     */
    fun initiateDelete() {
        uploadManager.deleteDocument(taskId)
    }

    /**
     * Cancels a pending deletion (undo).
     */
    fun cancelDelete() {
        deletionCountdownJob?.cancel()
        uploadManager.cancelDeletion(taskId)
        // Restore to uploaded state
        _displayState.value = computeDisplayState(currentTask, currentDocument, null)
    }

    private fun startDeletionCountdown(handle: DocumentDeletionHandle) {
        deletionCountdownJob = scope.launch {
            while (isActive) {
                val progress = handle.progressFraction()

                if (progress <= 0f) {
                    break
                }

                _displayState.value = computeDisplayState(
                    currentTask,
                    currentDocument,
                    progress
                )

                delay(50.milliseconds) // ~20fps
            }
        }
    }

    private fun computeDisplayState(
        task: DocumentUploadTask?,
        document: DocumentDto?,
        deletionProgress: Float?
    ): DocumentUploadDisplayState? {
        if (task == null) return null

        // If we have deletion progress, show deleting state
        if (deletionProgress != null && document != null) {
            return DocumentUploadDisplayState.Deleting(
                id = task.id,
                fileName = task.fileName,
                fileSize = task.fileSize,
                document = document,
                progress = deletionProgress
            )
        }

        return when (task.status) {
            UploadStatus.PENDING -> DocumentUploadDisplayState.Pending(
                id = task.id,
                fileName = task.fileName,
                fileSize = task.fileSize,
                task = task
            )

            UploadStatus.UPLOADING -> DocumentUploadDisplayState.Uploading(
                id = task.id,
                fileName = task.fileName,
                fileSize = task.fileSize,
                task = task,
                progress = task.progress
            )

            UploadStatus.FAILED -> DocumentUploadDisplayState.Failed(
                id = task.id,
                fileName = task.fileName,
                fileSize = task.fileSize,
                task = task,
                error = task.error ?: fallbackError
            )

            UploadStatus.COMPLETED -> {
                if (document != null) {
                    when (task.intakeOutcome) {
                        DocumentIntakeOutcome.LinkedToExisting -> {
                            DocumentUploadDisplayState.Linked(
                                id = task.id,
                                fileName = task.fileName,
                                fileSize = task.fileSize,
                                document = document,
                                otherSources = (task.sourceCount - 1).coerceAtLeast(0)
                            )
                        }

                        DocumentIntakeOutcome.PendingMatchReview -> {
                            DocumentUploadDisplayState.NeedsReview(
                                id = task.id,
                                fileName = task.fileName,
                                fileSize = task.fileSize,
                                document = document
                            )
                        }

                        else -> {
                            DocumentUploadDisplayState.Uploaded(
                                id = task.id,
                                fileName = task.fileName,
                                fileSize = task.fileSize,
                                document = document
                            )
                        }
                    }
                } else {
                    // Document not yet available, show as uploading at 100%
                    DocumentUploadDisplayState.Uploading(
                        id = task.id,
                        fileName = task.fileName,
                        fileSize = task.fileSize,
                        task = task,
                        progress = 1f
                    )
                }
            }
        }
    }
}

/**
 * Remembers a [DocumentUploadItemState] for the given task ID.
 */
@Composable
fun rememberDocumentUploadItemState(
    taskId: String,
    task: DocumentUploadTask?,
    document: DocumentDto?,
    deletionHandle: DocumentDeletionHandle?,
    uploadManager: DocumentUploadManager
): DocumentUploadItemState {
    val scope = rememberCoroutineScope()

    val state = remember(taskId) {
        DocumentUploadItemState(
            taskId = taskId,
            uploadManager = uploadManager,
            scope = scope
        )
    }

    // Update with latest data
    state.update(
        task,
        document,
        deletionHandle,
        DokusException.DocumentUploadFailed
    )

    return state
}
