package ai.dokus.app.cashflow.components

import ai.dokus.app.cashflow.manager.DocumentUploadManager
import ai.dokus.app.cashflow.model.DocumentDeletionHandle
import ai.dokus.app.cashflow.model.DocumentUploadTask
import ai.dokus.foundation.domain.model.DocumentDto
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.calf.core.LocalPlatformContext
import com.mohamedrejeb.calf.io.getName
import com.mohamedrejeb.calf.io.readByteArray
import com.mohamedrejeb.calf.picker.FilePickerFileType
import com.mohamedrejeb.calf.picker.FilePickerSelectionMode
import com.mohamedrejeb.calf.picker.rememberFilePickerLauncher
import kotlinx.coroutines.launch

/**
 * Document upload sidebar for desktop.
 *
 * Displays on the right side of the screen with:
 * - Header with close button
 * - Document upload zone (drag & drop)
 * - "Don't have the application?" link
 * - Scrollable list of uploading documents
 *
 * @param isVisible Whether the sidebar is visible
 * @param onDismiss Called when the sidebar should be closed
 * @param tasks List of upload tasks to display
 * @param documents Map of task ID to uploaded document
 * @param deletionHandles Map of task ID to deletion handle
 * @param uploadManager The upload manager for handling uploads
 * @param onShowQrCode Called when user clicks "Don't have the application?"
 */
@Composable
fun DocumentUploadSidebar(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    tasks: List<DocumentUploadTask>,
    documents: Map<String, DocumentDto>,
    deletionHandles: Map<String, DocumentDeletionHandle>,
    uploadManager: DocumentUploadManager,
    onShowQrCode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val platformContext = LocalPlatformContext.current

    val filePickerLauncher = rememberFilePickerLauncher(
        type = FilePickerFileType.Document,
        selectionMode = FilePickerSelectionMode.Multiple
    ) { files ->
        scope.launch {
            val dropped = files.mapNotNull { file ->
                val bytes = runCatching { file.readByteArray(platformContext) }.getOrNull()
                val name = file.getName(platformContext) ?: return@mapNotNull null
                bytes?.let { DroppedFile(name = name, bytes = it, mimeType = null) }
            }
            if (dropped.isNotEmpty()) {
                uploadManager.enqueueFiles(dropped)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Backdrop
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )
        }

        // Sidebar
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(300)
            ) + fadeIn(tween(300)),
            exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(300)
            ) + fadeOut(tween(300)),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            BoxWithConstraints {
                val sidebarWidth = (maxWidth / 3).coerceIn(320.dp, 400.dp)

                Card(
                    modifier = Modifier
                        .width(sidebarWidth)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { /* Consume click to prevent backdrop dismissal */ }
                        ),
                    shape = MaterialTheme.shapes.large.copy(
                        topEnd = MaterialTheme.shapes.extraSmall.topEnd,
                        bottomEnd = MaterialTheme.shapes.extraSmall.bottomEnd
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Header
                        SidebarHeader(onClose = onDismiss)

                        Spacer(modifier = Modifier.height(24.dp))

                        // Upload zone with drag & drop
                        var isDragging by remember { mutableStateOf(false) }

                        DocumentUploadZone(
                            isDragging = isDragging,
                            onClick = { filePickerLauncher.launch() },
                            onDragStateChange = { isDragging = it },
                            onFilesDropped = { files ->
                                uploadManager.enqueueFiles(files)
                            },
                            isUploading = tasks.any {
                                it.status == ai.dokus.app.cashflow.model.UploadStatus.UPLOADING
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // "Don't have the application?" link
                        TextButton(
                            onClick = onShowQrCode,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text(
                                text = "Don't have the application? Click here",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Upload list section header
                        if (tasks.isNotEmpty()) {
                            Text(
                                text = "Uploads",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Scrollable upload list
                        DocumentUploadList(
                            tasks = tasks,
                            documents = documents,
                            deletionHandles = deletionHandles,
                            uploadManager = uploadManager,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SidebarHeader(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Add Document",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
