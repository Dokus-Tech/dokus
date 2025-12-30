package ai.dokus.app.cashflow.state

import ai.dokus.app.cashflow.manager.DocumentUploadManager
import ai.dokus.app.cashflow.model.DocumentDeletionHandle
import ai.dokus.app.cashflow.model.DocumentUploadDisplayState
import ai.dokus.app.cashflow.model.DocumentUploadTask
import ai.dokus.app.cashflow.model.UploadStatus
import ai.dokus.app.resources.generated.Res
import tech.dokus.domain.model.DocumentDto
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
import kotlin.time.Duration.Companion.milliseconds
import org.jetbrains.compose.resources.stringResource

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
    private var fallbackErrorMessage: String = ""

    /**
     * Updates the state with new data from the manager.
     */
    fun update(
        task: DocumentUploadTask?,
        document: DocumentDto?,
        deletionHandle: DocumentDeletionHandle?,
        fallbackErrorMessage: String
    ) {
        currentTask = task
        currentDocument = document
        this.fallbackErrorMessage = fallbackErrorMessage

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
                error = task.error ?: fallbackErrorMessage
            )

            UploadStatus.COMPLETED -> {
                if (document != null) {
                    DocumentUploadDisplayState.Uploaded(
                        id = task.id,
                        fileName = task.fileName,
                        fileSize = task.fileSize,
                        document = document
                    )
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
        stringResource(Res.string.upload_failed_message)
    )

    return state
}
