package ai.dokus.app.cashflow.viewmodel

import ai.dokus.app.cashflow.components.DroppedFile
import ai.dokus.app.cashflow.manager.DocumentUploadManager
import ai.dokus.app.cashflow.model.DocumentDeletionHandle
import ai.dokus.app.cashflow.model.DocumentUploadTask
import ai.dokus.app.cashflow.model.UploadStatus
import tech.dokus.domain.model.DocumentDto
import ai.dokus.foundation.platform.Logger
import kotlinx.coroutines.flow.StateFlow
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.reduce

internal typealias AddDocumentCtx = PipelineContext<AddDocumentState, AddDocumentIntent, AddDocumentAction>

/**
 * Container for document upload functionality using FlowMVI.
 *
 * This Container delegates to [DocumentUploadManager] for actual upload operations
 * and exposes upload task flows directly for UI consumption.
 *
 * The upload manager is shared across the app, so uploads persist when navigating
 * between screens or closing/reopening the upload sidebar.
 *
 * Note: The state managed by this Container is minimal (Idle/Uploading/Error) since
 * the detailed upload task state is exposed via [uploadTasks] StateFlow for direct
 * UI observation. This avoids duplicating state that the upload manager already manages.
 *
 * Use with Koin's `container<>` DSL for automatic ViewModel wrapping and lifecycle management.
 */
internal class AddDocumentContainer(
    private val uploadManager: DocumentUploadManager,
) : Container<AddDocumentState, AddDocumentIntent, AddDocumentAction> {

    private val logger = Logger.forClass<AddDocumentContainer>()

    /**
     * List of all upload tasks.
     * Exposed directly from the upload manager for UI consumption.
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

    override val store: Store<AddDocumentState, AddDocumentIntent, AddDocumentAction> =
        store(AddDocumentState.Idle()) {
            reduce { intent ->
                when (intent) {
                    is AddDocumentIntent.SelectFile -> handleSelectFile()
                    is AddDocumentIntent.Upload -> handleUpload(intent.files)
                    is AddDocumentIntent.Cancel -> handleCancel()
                }
            }
        }

    /**
     * Computes the current screen state from upload tasks.
     * Can be used by the UI to derive state from uploadTasks flow.
     */
    fun computeStateFromTasks(tasks: List<DocumentUploadTask>): AddDocumentState {
        val hasActiveUploads = tasks.any {
            it.status == UploadStatus.UPLOADING || it.status == UploadStatus.PENDING
        }
        val hasCompletedUploads = tasks.any { it.status == UploadStatus.COMPLETED }
        val hasFailedUploads = tasks.any { it.status == UploadStatus.FAILED }

        return if (hasActiveUploads) {
            AddDocumentState.Uploading(
                hasCompletedUploads = hasCompletedUploads,
                hasFailedUploads = hasFailedUploads
            )
        } else {
            AddDocumentState.Idle(
                hasCompletedUploads = hasCompletedUploads,
                hasFailedUploads = hasFailedUploads
            )
        }
    }

    private suspend fun AddDocumentCtx.handleSelectFile() {
        action(AddDocumentAction.LaunchFilePicker)
    }

    private suspend fun AddDocumentCtx.handleUpload(files: List<DroppedFile>) {
        if (files.isEmpty()) return

        logger.d { "Enqueueing ${files.size} files for upload" }
        uploadManager.enqueueFiles(files)

        // Update state to reflect uploading
        updateState {
            AddDocumentState.Uploading(
                hasCompletedUploads = uploadManager.uploadTasks.value.any { it.status == UploadStatus.COMPLETED },
                hasFailedUploads = uploadManager.uploadTasks.value.any { it.status == UploadStatus.FAILED }
            )
        }
    }

    private suspend fun AddDocumentCtx.handleCancel() {
        action(AddDocumentAction.NavigateBack)
    }

    /**
     * Returns the upload manager for components that need direct access.
     * Used for individual task operations like cancel, retry, delete.
     */
    fun provideUploadManager(): DocumentUploadManager = uploadManager
}
