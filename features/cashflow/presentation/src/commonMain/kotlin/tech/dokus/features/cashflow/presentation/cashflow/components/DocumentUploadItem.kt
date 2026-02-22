package tech.dokus.features.cashflow.presentation.cashflow.components

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.common_file_size_bytes
import tech.dokus.aura.resources.common_file_size_kb
import tech.dokus.aura.resources.common_file_size_mb
import tech.dokus.aura.resources.upload_status_linked_to
import tech.dokus.aura.resources.upload_status_linked_to_with_sources
import tech.dokus.aura.resources.upload_status_needs_review
import tech.dokus.aura.resources.upload_status_waiting
import tech.dokus.domain.model.DocumentDto
import tech.dokus.features.cashflow.presentation.cashflow.components.upload.CancelUploadAction
import tech.dokus.features.cashflow.presentation.cashflow.components.upload.DeleteDocumentAction
import tech.dokus.features.cashflow.presentation.cashflow.components.upload.DeletingFileIcon
import tech.dokus.features.cashflow.presentation.cashflow.components.upload.DeletingFileInfo
import tech.dokus.features.cashflow.presentation.cashflow.components.upload.DeletionProgressIndicator
import tech.dokus.features.cashflow.presentation.cashflow.components.upload.FailedOverlay
import tech.dokus.features.cashflow.presentation.cashflow.components.upload.FailedUploadActions
import tech.dokus.features.cashflow.presentation.cashflow.components.upload.FileIconWithOverlay
import tech.dokus.features.cashflow.presentation.cashflow.components.upload.PendingOverlay
import tech.dokus.features.cashflow.presentation.cashflow.components.upload.UndoDeleteAction
import tech.dokus.features.cashflow.presentation.cashflow.components.upload.UploadItemRow
import tech.dokus.features.cashflow.presentation.cashflow.components.upload.UploadProgressIndicator
import tech.dokus.features.cashflow.presentation.cashflow.components.upload.UploadedOverlay
import tech.dokus.features.cashflow.presentation.cashflow.components.upload.UploadingOverlay
import tech.dokus.features.cashflow.presentation.cashflow.model.DocumentDeletionHandle
import tech.dokus.features.cashflow.presentation.cashflow.model.DocumentUploadDisplayState
import tech.dokus.features.cashflow.presentation.cashflow.model.DocumentUploadTask
import tech.dokus.features.cashflow.presentation.cashflow.model.manager.DocumentUploadManager
import tech.dokus.features.cashflow.presentation.cashflow.model.state.DocumentUploadItemState
import tech.dokus.features.cashflow.presentation.cashflow.model.state.rememberDocumentUploadItemState
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.DokusCardVariant
import tech.dokus.foundation.aura.extensions.localized

// Animation constants
private const val FadeInDurationMs = 300
private const val FadeOutDurationMs = 150

// File size constants
private const val BytesPerKb = 1024
private const val BytesPerMb = 1024 * 1024
private const val FileSizeDecimalMultiplier = 10

// UI dimensions
private val DeletingContentPadding = 12.dp
private val DeletingContentSpacing = 8.dp

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
 * Uses extracted components from [tech.dokus.features.cashflow.presentation.cashflow.components.upload] for consistent
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

    DokusCardSurface(
        modifier = modifier.fillMaxWidth(),
        variant = DokusCardVariant.Soft,
    ) {
        // Use contentKey to only animate on STATE TYPE changes, not progress updates
        AnimatedContent(
            targetState = currentState,
            contentKey = { it::class },
            transitionSpec = {
                when {
                    // Uploading → Uploaded: smooth fade
                    initialState is DocumentUploadDisplayState.Uploading &&
                        (
                            targetState is DocumentUploadDisplayState.Uploaded ||
                                targetState is DocumentUploadDisplayState.Linked ||
                                targetState is DocumentUploadDisplayState.NeedsReview
                            ) ->
                        fadeIn(tween(FadeInDurationMs)) togetherWith fadeOut(tween(FadeOutDurationMs))

                    // Uploaded → Deleting: slide right
                    (
                        initialState is DocumentUploadDisplayState.Uploaded ||
                            initialState is DocumentUploadDisplayState.Linked ||
                            initialState is DocumentUploadDisplayState.NeedsReview
                        ) &&
                        targetState is DocumentUploadDisplayState.Deleting ->
                        (slideInHorizontally { it } + fadeIn()) togetherWith
                            (slideOutHorizontally { -it } + fadeOut())

                    // Deleting → Uploaded: slide back (undo)
                    initialState is DocumentUploadDisplayState.Deleting &&
                        (
                            targetState is DocumentUploadDisplayState.Uploaded ||
                                targetState is DocumentUploadDisplayState.Linked ||
                                targetState is DocumentUploadDisplayState.NeedsReview
                            ) ->
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

                is DocumentUploadDisplayState.Linked -> LinkedContent(
                    state = current,
                    onDelete = { state.initiateDelete() }
                )

                is DocumentUploadDisplayState.NeedsReview -> NeedsReviewContent(
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
private fun LinkedContent(
    state: DocumentUploadDisplayState.Linked,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val subtitle = if (state.otherSources > 0) {
        stringResource(
            Res.string.upload_status_linked_to_with_sources,
            state.document.id.toString(),
            state.otherSources
        )
    } else {
        stringResource(
            Res.string.upload_status_linked_to,
            state.document.id.toString()
        )
    }
    UploadItemRow(
        fileName = state.fileName,
        subtitle = subtitle,
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
private fun NeedsReviewContent(
    state: DocumentUploadDisplayState.NeedsReview,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    UploadItemRow(
        fileName = state.fileName,
        subtitle = stringResource(Res.string.upload_status_needs_review),
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
        bytes < BytesPerKb -> stringResource(Res.string.common_file_size_bytes, bytes)
        bytes < BytesPerMb -> {
            val kb = bytes / BytesPerKb.toDouble()
            val displayKb = (kb * FileSizeDecimalMultiplier).toInt() / FileSizeDecimalMultiplier.toDouble()
            stringResource(Res.string.common_file_size_kb, displayKb)
        }
        else -> {
            val mb = bytes / BytesPerMb.toDouble()
            val displayMb = (mb * FileSizeDecimalMultiplier).toInt() / FileSizeDecimalMultiplier.toDouble()
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
            .padding(DeletingContentPadding),
        verticalArrangement = Arrangement.spacedBy(DeletingContentSpacing)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DeletingContentSpacing)
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
