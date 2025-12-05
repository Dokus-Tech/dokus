package ai.dokus.app.cashflow.viewmodel

import ai.dokus.app.cashflow.components.DroppedFile
import ai.dokus.app.cashflow.manager.DocumentUploadManager
import ai.dokus.app.cashflow.model.DocumentDeletionHandle
import ai.dokus.app.cashflow.model.DocumentUploadDisplayState
import ai.dokus.app.cashflow.model.DocumentUploadTask
import ai.dokus.app.cashflow.model.UploadStatus
import ai.dokus.app.core.viewmodel.BaseViewModel
import ai.dokus.foundation.domain.model.DocumentDto
import ai.dokus.foundation.platform.Logger
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * ViewModel for document upload functionality.
 *
 * This ViewModel delegates to [DocumentUploadManager] for actual upload operations
 * and exposes the upload state for UI consumption.
 *
 * The upload manager is shared across the app, so uploads persist when navigating
 * between screens or closing/reopening the upload sidebar.
 */
class AddDocumentViewModel : BaseViewModel<AddDocumentViewModel.State>(State()), KoinComponent {

    private val uploadManager: DocumentUploadManager by inject()
    private val logger = Logger.forClass<AddDocumentViewModel>()

    /**
     * UI state for the add document screen.
     */
    data class State(
        val isUploading: Boolean = false,
        val hasCompletedUploads: Boolean = false,
        val hasFailedUploads: Boolean = false
    )

    /**
     * List of all upload tasks.
     */
    val uploadTasks: StateFlow<List<DocumentUploadTask>> = uploadManager.uploadTasks

    /**
     * Map of uploaded documents by task ID.
     */
    val uploadedDocuments: StateFlow<Map<String, DocumentDto>> = uploadManager.uploadedDocuments

    /**
     * Map of deletion handles by task ID.
     */
    val deletionHandles: StateFlow<Map<String, DocumentDeletionHandle>> = uploadManager.deletionHandles

    /**
     * Combined display states for all upload items.
     */
    val displayStates: StateFlow<List<DocumentUploadDisplayState>> = combine(
        uploadTasks,
        uploadedDocuments,
        deletionHandles
    ) { tasks, documents, deletions ->
        tasks.mapNotNull { task ->
            computeDisplayState(task, documents[task.id], deletions[task.id])
        }
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Observe upload tasks to update simplified state
        scope.launch {
            uploadTasks.collect { tasks ->
                mutableState.value = State(
                    isUploading = tasks.any { it.status == UploadStatus.UPLOADING || it.status == UploadStatus.PENDING },
                    hasCompletedUploads = tasks.any { it.status == UploadStatus.COMPLETED },
                    hasFailedUploads = tasks.any { it.status == UploadStatus.FAILED }
                )
            }
        }
    }

    /**
     * Uploads files using the shared upload manager.
     *
     * @param files List of files to upload
     * @return List of created task IDs
     */
    fun uploadFiles(files: List<DroppedFile>): List<String> {
        if (files.isEmpty()) return emptyList()

        logger.d { "Enqueueing ${files.size} files for upload" }
        return uploadManager.enqueueFiles(files)
    }

    /**
     * Cancels an upload task.
     *
     * @param taskId The task ID to cancel
     */
    fun cancelUpload(taskId: String) {
        uploadManager.cancelUpload(taskId)
    }

    /**
     * Retries a failed upload.
     *
     * @param taskId The task ID to retry
     */
    fun retryUpload(taskId: String) {
        uploadManager.retryUpload(taskId)
    }

    /**
     * Initiates deletion of an uploaded document.
     *
     * @param taskId The task ID of the uploaded document
     */
    fun deleteDocument(taskId: String) {
        uploadManager.deleteDocument(taskId)
    }

    /**
     * Cancels a pending deletion (undo).
     *
     * @param taskId The task ID of the document being deleted
     */
    fun cancelDeletion(taskId: String) {
        uploadManager.cancelDeletion(taskId)
    }

    /**
     * Returns the upload manager for components that need direct access.
     */
    fun provideUploadManager(): DocumentUploadManager = uploadManager

    /**
     * Checks if there are any active uploads.
     */
    fun hasActiveUploads(): Boolean = uploadManager.hasActiveUploads()

    private fun computeDisplayState(
        task: DocumentUploadTask,
        document: DocumentDto?,
        deletionHandle: DocumentDeletionHandle?
    ): DocumentUploadDisplayState? {
        // If we have a deletion handle, show deleting state
        if (deletionHandle != null && document != null) {
            return DocumentUploadDisplayState.Deleting(
                id = task.id,
                fileName = task.fileName,
                fileSize = task.fileSize,
                document = document,
                progress = deletionHandle.progressFraction()
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
                error = task.error ?: "Upload failed"
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
                    // Document not yet available
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
