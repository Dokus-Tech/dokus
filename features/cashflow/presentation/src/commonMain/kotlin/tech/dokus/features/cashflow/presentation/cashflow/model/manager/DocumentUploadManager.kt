package tech.dokus.features.cashflow.presentation.cashflow.model.manager

import tech.dokus.features.cashflow.presentation.cashflow.components.DroppedFile
import tech.dokus.features.cashflow.datasource.CashflowRemoteDataSource
import tech.dokus.features.cashflow.presentation.cashflow.model.DocumentDeletionHandle
import tech.dokus.features.cashflow.presentation.cashflow.model.DocumentUploadTask
import tech.dokus.features.cashflow.presentation.cashflow.model.UploadStatus
import tech.dokus.domain.model.DocumentDto
import tech.dokus.foundation.platform.Logger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.utils.currentTimeMillis
import kotlin.time.Duration.Companion.seconds

/**
 * Manages document uploads with queue management, progress tracking, and deletion with undo.
 *
 * Features:
 * - Parallel uploads with configurable concurrency limit (default 3)
 * - Real-time progress tracking via callbacks
 * - Retry support for failed uploads
 * - Soft delete with 5-second undo window
 * - Persists state across UI lifecycle
 *
 * This is a singleton scoped at the application level to maintain state
 * when navigating between screens or closing/opening the sidebar.
 */
class DocumentUploadManager(
    private val dataSource: CashflowRemoteDataSource,
    private val maxConcurrentUploads: Int = 3
) {
    private val logger = Logger.forClass<DocumentUploadManager>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val uploadSemaphore = Semaphore(maxConcurrentUploads)

    private val _uploadTasks = MutableStateFlow<List<DocumentUploadTask>>(emptyList())
    val uploadTasks: StateFlow<List<DocumentUploadTask>> = _uploadTasks.asStateFlow()

    private val _deletionHandles = MutableStateFlow<Map<String, DocumentDeletionHandle>>(emptyMap())
    val deletionHandles: StateFlow<Map<String, DocumentDeletionHandle>> = _deletionHandles.asStateFlow()

    private val _uploadedDocuments = MutableStateFlow<Map<String, DocumentDto>>(emptyMap())
    val uploadedDocuments: StateFlow<Map<String, DocumentDto>> = _uploadedDocuments.asStateFlow()

    // Callback for when uploads complete (for refreshing CashflowScreen)
    private var onUploadCompleteCallback: (() -> Unit)? = null

    private val uploadJobs = mutableMapOf<String, Job>()

    /**
     * Sets the callback to be invoked when any upload completes successfully.
     */
    fun setOnUploadCompleteCallback(callback: () -> Unit) {
        onUploadCompleteCallback = callback
    }

    /**
     * Clears the upload complete callback.
     */
    fun clearOnUploadCompleteCallback() {
        onUploadCompleteCallback = null
    }

    /**
     * Enqueues files for upload.
     *
     * @param files List of files to upload
     * @return List of created task IDs
     */
    fun enqueueFiles(files: List<DroppedFile>): List<String> {
        val tasks = files.map { file ->
            DocumentUploadTask(
                fileName = file.name,
                fileSize = file.bytes.size.toLong(),
                mimeType = file.mimeType ?: mimeTypeFromName(file.name),
                bytes = file.bytes,
                status = UploadStatus.PENDING
            )
        }

        _uploadTasks.update { current -> current + tasks }

        // Start uploading each task
        tasks.forEach { task ->
            startUpload(task)
        }

        return tasks.map { it.id }
    }

    /**
     * Cancels an upload task.
     *
     * @param taskId The task ID to cancel
     */
    fun cancelUpload(taskId: String) {
        uploadJobs[taskId]?.cancel()
        uploadJobs.remove(taskId)

        _uploadTasks.update { current ->
            current.filter { it.id != taskId }
        }
    }

    /**
     * Retries a failed upload.
     *
     * @param taskId The task ID to retry
     */
    fun retryUpload(taskId: String) {
        val task = _uploadTasks.value.find { it.id == taskId } ?: return

        if (!task.canManualRetry) {
            logger.w { "Task $taskId cannot be retried (max retries reached)" }
            return
        }

        val updatedTask = task.copy(
            status = UploadStatus.PENDING,
            progress = 0f,
            error = null,
            retryCount = task.retryCount + 1
        )

        _uploadTasks.update { current ->
            current.map { if (it.id == taskId) updatedTask else it }
        }

        startUpload(updatedTask)
    }

    /**
     * Initiates deletion of an uploaded document with 5-second undo window.
     *
     * @param taskId The task ID of the uploaded document
     * @return The deletion handle, or null if the task wasn't found or wasn't completed
     */
    fun deleteDocument(taskId: String): DocumentDeletionHandle? {
        val task = _uploadTasks.value.find { it.id == taskId } ?: return null
        val document = _uploadedDocuments.value[taskId] ?: return null

        if (task.status != UploadStatus.COMPLETED || task.documentId == null) {
            logger.w { "Task $taskId is not in COMPLETED state, cannot delete" }
            return null
        }

        val resultDeferred = CompletableDeferred<Result<Unit>>()

        val job = scope.launch {
            delay(5.seconds)

            // If we get here, the deletion wasn't cancelled
            _deletionHandles.update { it - taskId }

            val result = dataSource.deleteDocument(task.documentId)
            resultDeferred.complete(result)

            if (result.isSuccess) {
                _uploadTasks.update { current -> current.filter { it.id != taskId } }
                _uploadedDocuments.update { it - taskId }
                logger.i { "Document ${task.documentId} deleted successfully" }
            } else {
                logger.e(result.exceptionOrNull()) { "Failed to delete document ${task.documentId}" }
            }
        }

        val handle = DocumentDeletionHandle(
            documentId = task.documentId,
            fileName = task.fileName,
            fileSize = task.fileSize,
            job = job,
            startedAt = currentTimeMillis(),
            resultDeferred = resultDeferred
        )

        _deletionHandles.update { it + (taskId to handle) }

        return handle
    }

    /**
     * Cancels a pending deletion (undo).
     *
     * @param taskId The task ID of the document being deleted
     * @return true if the deletion was cancelled, false if it wasn't found or already completed
     */
    fun cancelDeletion(taskId: String): Boolean {
        val handle = _deletionHandles.value[taskId] ?: return false

        handle.cancel()
        _deletionHandles.update { it - taskId }

        logger.i { "Deletion cancelled for document ${handle.documentId}" }
        return true
    }

    /**
     * Removes a completed upload from the list without deleting the document.
     *
     * @param taskId The task ID to remove
     */
    fun removeCompletedTask(taskId: String) {
        val task = _uploadTasks.value.find { it.id == taskId } ?: return

        if (task.status == UploadStatus.COMPLETED) {
            _uploadTasks.update { current -> current.filter { it.id != taskId } }
            _uploadedDocuments.update { it - taskId }
        }
    }

    /**
     * Clears all completed uploads from the list.
     */
    fun clearCompletedUploads() {
        val completedIds = _uploadTasks.value
            .filter { it.status == UploadStatus.COMPLETED }
            .map { it.id }

        _uploadTasks.update { current ->
            current.filter { it.status != UploadStatus.COMPLETED }
        }

        completedIds.forEach { id ->
            _uploadedDocuments.update { it - id }
        }
    }

    /**
     * Checks if there are any active uploads in progress.
     */
    fun hasActiveUploads(): Boolean {
        return _uploadTasks.value.any {
            it.status == UploadStatus.PENDING || it.status == UploadStatus.UPLOADING
        }
    }

    private fun startUpload(task: DocumentUploadTask) {
        val job = scope.launch {
            uploadSemaphore.withPermit {
                performUpload(task)
            }
        }
        uploadJobs[task.id] = job
    }

    private suspend fun performUpload(task: DocumentUploadTask) {
        // Update to uploading state
        updateTask(task.id) {
            it.copy(status = UploadStatus.UPLOADING, progress = 0f)
        }

        val result = dataSource.uploadDocumentWithProgress(
            fileContent = task.bytes,
            filename = task.fileName,
            contentType = task.mimeType,
            prefix = "documents",
            onProgress = { progress ->
                updateTask(task.id) {
                    it.copy(progress = progress)
                }
            }
        )

        result.fold(
            onSuccess = { document ->
                updateTask(task.id) {
                    it.copy(
                        status = UploadStatus.COMPLETED,
                        progress = 1f,
                        documentId = document.id
                    )
                }
                _uploadedDocuments.update { it + (task.id to document) }
                uploadJobs.remove(task.id)
                logger.i { "Upload completed: ${task.fileName} -> ${document.id}" }

                // Notify that upload completed
                onUploadCompleteCallback?.invoke()
            },
            onFailure = { error ->
                val exception = error.asDokusException
                val displayException = if (exception is DokusException.Unknown) {
                    DokusException.DocumentUploadFailed
                } else {
                    exception
                }
                updateTask(task.id) {
                    it.copy(
                        status = UploadStatus.FAILED,
                        error = displayException
                    )
                }
                uploadJobs.remove(task.id)
                logger.e(error) { "Upload failed: ${task.fileName}" }
            }
        )
    }

    private fun updateTask(taskId: String, update: (DocumentUploadTask) -> DocumentUploadTask) {
        _uploadTasks.update { current ->
            current.map { if (it.id == taskId) update(it) else it }
        }
    }

    private fun mimeTypeFromName(filename: String): String {
        val lower = filename.lowercase()
        return when {
            lower.endsWith(".pdf") -> "application/pdf"
            lower.endsWith(".png") -> "image/png"
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> "image/jpeg"
            lower.endsWith(".webp") -> "image/webp"
            lower.endsWith(".gif") -> "image/gif"
            lower.endsWith(".csv") -> "text/csv"
            lower.endsWith(".txt") -> "text/plain"
            lower.endsWith(".doc") || lower.endsWith(".docx") ->
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            lower.endsWith(".xls") || lower.endsWith(".xlsx") ->
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            else -> "application/octet-stream"
        }
    }
}
