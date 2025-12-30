package ai.dokus.app.cashflow.components

import ai.dokus.app.cashflow.components.upload.CancelUploadAction
import ai.dokus.app.cashflow.components.upload.DeleteDocumentAction
import ai.dokus.app.cashflow.components.upload.DeletingFileIcon
import ai.dokus.app.cashflow.components.upload.DeletingFileInfo
import ai.dokus.app.cashflow.components.upload.DeletionProgressIndicator
import ai.dokus.app.cashflow.components.upload.FailedOverlay
import ai.dokus.app.cashflow.components.upload.FailedUploadActions
import ai.dokus.app.cashflow.components.upload.FileIconWithOverlay
import ai.dokus.app.cashflow.components.upload.PendingOverlay
import ai.dokus.app.cashflow.components.upload.UndoDeleteAction
import ai.dokus.app.cashflow.components.upload.UploadItemRow
import ai.dokus.app.cashflow.components.upload.UploadProgressIndicator
import ai.dokus.app.cashflow.components.upload.UploadedOverlay
import ai.dokus.app.cashflow.components.upload.UploadingOverlay
import ai.dokus.app.cashflow.manager.DocumentUploadManager
import ai.dokus.app.cashflow.model.DocumentDeletionHandle
import ai.dokus.app.cashflow.model.DocumentUploadDisplayState
import ai.dokus.app.cashflow.model.DocumentUploadTask
import ai.dokus.app.cashflow.state.DocumentUploadItemState
import ai.dokus.app.cashflow.state.rememberDocumentUploadItemState
import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.common_file_size_bytes
import ai.dokus.app.resources.generated.common_file_size_kb
import ai.dokus.app.resources.generated.common_file_size_mb
import ai.dokus.app.resources.generated.upload_status_waiting
import ai.dokus.foundation.design.extensions.localized
import tech.dokus.domain.model.DocumentDto
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource

/**
 * Unified document upload item component that renders all possible states:
 * - Pending (waiting in upload queue)
 * - Uploading (in progress with progress bar)
 * - Failed (with retry/cancel actions)
 * - Uploaded (completed, can be deleted)
 * - Deleting (with undo countdown)
 *
 * Each item manages its own state via [DocumentUploadItemState], making it independent
 * of the parent ViewModel for actions like retry, cancel, and delete.
 *
 * Uses extracted components from [ai.dokus.app.cashflow.components.upload] for consistent
 * styling and reduced code duplication.
 */
@Composable
fun DocumentUploadItem(
    taskId: String,
    task: DocumentUploadTask?,
    document: DocumentDto?,
    deletionHandle: DocumentDeletionHandle?,
    uploadManager: DocumentUploadManager,
    modifier: Modifier = Modifier
) {
    val state = rememberDocumentUploadItemState(
        taskId = taskId,
        task = task,
        document = document,
        deletionHandle = deletionHandle,
        uploadManager = uploadManager
    )

    DocumentUploadItemContent(
        state = state,
        modifier = modifier
    )
}

/**
 * Content composable that renders based on [DocumentUploadItemState].
 */
@Composable
private fun DocumentUploadItemContent(
    state: DocumentUploadItemState,
    modifier: Modifier = Modifier
) {
    val displayState by state.displayState.collectAsState()

    val currentState = displayState ?: return

    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small
    ) {
        // Use contentKey to only animate on STATE TYPE changes, not progress updates
        AnimatedContent(
            targetState = currentState,
            contentKey = { it::class },
            transitionSpec = {
                when {
                    // Uploading → Uploaded: smooth fade
                    initialState is DocumentUploadDisplayState.Uploading &&
                            targetState is DocumentUploadDisplayState.Uploaded ->
                        fadeIn(tween(300)) togetherWith fadeOut(tween(150))

                    // Uploaded → Deleting: slide right
                    initialState is DocumentUploadDisplayState.Uploaded &&
                            targetState is DocumentUploadDisplayState.Deleting ->
                        (slideInHorizontally { it } + fadeIn()) togetherWith
                                (slideOutHorizontally { -it } + fadeOut())

                    // Deleting → Uploaded: slide back (undo)
                    initialState is DocumentUploadDisplayState.Deleting &&
                            targetState is DocumentUploadDisplayState.Uploaded ->
                        (slideInHorizontally { -it } + fadeIn()) togetherWith
                                (slideOutHorizontally { it } + fadeOut())

                    // Default: simple fade
                    else -> fadeIn() togetherWith fadeOut()
                }
            },
            label = "document-upload-item-state-transition"
        ) { current ->
            when (current) {
                is DocumentUploadDisplayState.Pending -> PendingContent(
                    state = current,
                    onCancel = { state.cancelUpload() }
                )

                is DocumentUploadDisplayState.Uploading -> UploadingContent(
                    state = current,
                    onCancel = { state.cancelUpload() }
                )

                is DocumentUploadDisplayState.Failed -> FailedContent(
                    state = current,
                    onRetry = { state.retry() },
                    onCancel = { state.cancelUpload() }
                )

                is DocumentUploadDisplayState.Uploaded -> UploadedContent(
                    state = current,
                    onDelete = { state.initiateDelete() }
                )

                is DocumentUploadDisplayState.Deleting -> DeletingContent(
                    state = current,
                    onUndo = { state.cancelDelete() }
                )
            }
        }
    }
}

// --- State-specific content composables ---

@Composable
private fun PendingContent(
    state: DocumentUploadDisplayState.Pending,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    UploadItemRow(
        fileName = state.fileName,
        subtitle = stringResource(Res.string.upload_status_waiting),
        subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant,
        icon = {
            FileIconWithOverlay { PendingOverlay() }
        },
        actions = {
            CancelUploadAction(onClick = onCancel)
        },
        modifier = modifier
    )
}

@Composable
private fun UploadingContent(
    state: DocumentUploadDisplayState.Uploading,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    UploadItemRow(
        fileName = state.fileName,
        subtitle = null,
        icon = {
            FileIconWithOverlay { UploadingOverlay() }
        },
        actions = {
            CancelUploadAction(onClick = onCancel)
        },
        subtitleContent = {
            UploadProgressIndicator(
                progress = state.progress,
                progressPercent = state.progressPercent
            )
        },
        modifier = modifier
    )
}

@Composable
private fun FailedContent(
    state: DocumentUploadDisplayState.Failed,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    UploadItemRow(
        fileName = state.fileName,
        subtitle = state.error.localized,
        subtitleColor = MaterialTheme.colorScheme.error,
        icon = {
            FileIconWithOverlay { FailedOverlay() }
        },
        actions = {
            FailedUploadActions(
                canRetry = state.canRetry,
                onRetry = onRetry,
                onCancel = onCancel
            )
        },
        modifier = modifier
    )
}

@Composable
private fun UploadedContent(
    state: DocumentUploadDisplayState.Uploaded,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    UploadItemRow(
        fileName = state.fileName,
        subtitle = formatFileSize(state.fileSize),
        subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant,
        icon = {
            FileIconWithOverlay { UploadedOverlay() }
        },
        actions = {
            DeleteDocumentAction(onClick = onDelete)
        },
        modifier = modifier
    )
}

@Composable
private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> stringResource(Res.string.common_file_size_bytes, bytes)
        bytes < 1024 * 1024 -> {
            val kb = bytes / 1024.0
            val displayKb = (kb * 10).toInt() / 10.0
            stringResource(Res.string.common_file_size_kb, displayKb)
        }
        else -> {
            val mb = bytes / (1024.0 * 1024.0)
            val displayMb = (mb * 10).toInt() / 10.0
            stringResource(Res.string.common_file_size_mb, displayMb)
        }
    }
}

@Composable
private fun DeletingContent(
    state: DocumentUploadDisplayState.Deleting,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DeletingFileIcon()
            DeletingFileInfo(
                fileName = state.fileName,
                modifier = Modifier.weight(1f)
            )
            UndoDeleteAction(onClick = onUndo)
        }

        DeletionProgressIndicator(progress = state.progress)
    }
}
