package ai.dokus.app.cashflow.components

import ai.dokus.app.cashflow.manager.DocumentUploadManager
import ai.dokus.app.cashflow.model.DocumentDeletionHandle
import ai.dokus.app.cashflow.model.DocumentUploadDisplayState
import ai.dokus.app.cashflow.model.DocumentUploadTask
import ai.dokus.app.cashflow.state.DocumentUploadItemState
import ai.dokus.app.cashflow.state.rememberDocumentUploadItemState
import ai.dokus.foundation.domain.model.DocumentDto
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

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
        subtitle = "Waiting...",
        subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant,
        icon = {
            FileIconWithOverlay(
                overlay = {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                }
            )
        },
        actions = {
            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
            FileIconWithOverlay(
                overlay = {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            )
        },
        actions = {
            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        subtitleContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LinearProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp),
                    strokeCap = StrokeCap.Round
                )
                Text(
                    text = "${state.progressPercent}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
        subtitle = state.error,
        subtitleColor = MaterialTheme.colorScheme.error,
        icon = {
            FileIconWithOverlay(
                overlay = {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(16.dp)
                            .background(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.shapes.extraSmall
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Failed",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        },
        actions = {
            Row {
                if (state.canRetry) {
                    IconButton(onClick = onRetry) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                IconButton(onClick = onCancel) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
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
        subtitle = state.formattedSize,
        subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant,
        icon = {
            FileIconWithOverlay(
                overlay = {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(16.dp)
                            .background(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.shapes.extraSmall
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Uploaded",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        actions = {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        },
        modifier = modifier
    )
}

@Composable
private fun DeletingContent(
    state: DocumentUploadDisplayState.Deleting,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = state.progress,
        animationSpec = tween(durationMillis = 100),
        label = "deletion-progress"
    )

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
            // File icon (dimmed)
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.InsertDriveFile,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                )
            }

            // File info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Deleting...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Undo button
            IconButton(onClick = onUndo) {
                Icon(
                    imageVector = Icons.Default.Undo,
                    contentDescription = "Undo delete",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Countdown progress bar
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = MaterialTheme.colorScheme.error,
            trackColor = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
            strokeCap = StrokeCap.Round
        )
    }
}

// --- Shared components ---

@Composable
private fun UploadItemRow(
    fileName: String,
    subtitle: String?,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    icon: @Composable () -> Unit,
    actions: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    subtitleContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        icon()

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = fileName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (subtitleContent != null) {
                subtitleContent()
            } else if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = subtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        actions()
    }
}

@Composable
private fun FileIconWithOverlay(
    overlay: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier.size(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.InsertDriveFile,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        overlay()
    }
}
