package ai.dokus.app.cashflow.viewmodel

import ai.dokus.app.cashflow.components.DroppedFile
import ai.dokus.app.cashflow.manager.DocumentUploadManager
import ai.dokus.app.cashflow.model.DocumentDeletionHandle
import ai.dokus.app.cashflow.model.DocumentUploadTask
import ai.dokus.app.cashflow.model.UploadStatus
import ai.dokus.app.core.viewmodel.BaseViewModel
import ai.dokus.foundation.domain.model.DocumentDto
import ai.dokus.foundation.platform.Logger
import kotlinx.coroutines.flow.StateFlow
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
internal class AddDocumentViewModel : BaseViewModel<AddDocumentViewModel.State>(State()), KoinComponent {

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

    init {
        // Observe upload tasks to update the simplified state
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
     * Returns the upload manager for components that need direct access.
     */
    fun provideUploadManager(): DocumentUploadManager = uploadManager
}
