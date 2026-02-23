package tech.dokus.features.cashflow.presentation.cashflow.components

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_close
import tech.dokus.aura.resources.cashflow_add_document
import tech.dokus.aura.resources.upload_instructions
import tech.dokus.aura.resources.upload_no_app_hint
import tech.dokus.aura.resources.upload_no_documents
import tech.dokus.aura.resources.upload_uploads_title
import tech.dokus.domain.model.DocumentDto
import tech.dokus.features.cashflow.presentation.cashflow.model.DocumentDeletionHandle
import tech.dokus.features.cashflow.presentation.cashflow.model.DocumentUploadTask
import tech.dokus.features.cashflow.presentation.cashflow.model.UploadStatus
import tech.dokus.features.cashflow.presentation.cashflow.model.manager.DocumentUploadManager
import tech.dokus.foundation.aura.components.DokusCardSurface

// Animation durations
private const val FadeDurationMs = 200
private const val SlideDurationMs = 300

// Sidebar dimensions
private val SidebarMinWidth = 320.dp
private val SidebarMaxWidth = 360.dp

// Spacing
private val ContentPadding = 16.dp
private val HeaderSpacing = 24.dp
private val UploadZoneSpacing = 12.dp
private val InstructionsSpacing = 8.dp

// Scrim opacity
private const val ScrimAlpha = 0.32f

// Sidebar width ratio
private const val SidebarWidthDivider = 3

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
    val filePickerLauncher = rememberDocumentFilePicker { files ->
        if (files.isNotEmpty()) {
            uploadManager.enqueueFiles(files)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Backdrop
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(FadeDurationMs)),
            exit = fadeOut(tween(FadeDurationMs))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = ScrimAlpha))
            )
        }

        // Sidebar
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(SlideDurationMs)
            ) + fadeIn(tween(SlideDurationMs)),
            exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(SlideDurationMs)
            ) + fadeOut(tween(SlideDurationMs)),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            BoxWithConstraints {
                val sidebarWidth = (maxWidth / SidebarWidthDivider).coerceIn(SidebarMinWidth, SidebarMaxWidth)

                DokusCardSurface(
                    modifier = Modifier
                        .width(sidebarWidth)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { /* Consume click to prevent backdrop dismissal */ }
                        ),
                    shape = MaterialTheme.shapes.medium.copy(
                        topEnd = MaterialTheme.shapes.extraSmall.topEnd,
                        bottomEnd = MaterialTheme.shapes.extraSmall.bottomEnd
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(ContentPadding)
                    ) {
                        // Header
                        SidebarHeader(onClose = onDismiss)

                        Spacer(modifier = Modifier.height(HeaderSpacing))

                        // Upload zone with drag & drop
                        var isDragging by remember { mutableStateOf(false) }

                        DocumentUploadZone(
                            isDragging = isDragging,
                            onClick = { filePickerLauncher.launch() },
                            onDragStateChange = { isDragging = it },
                            onFilesDropped = { files ->
                                uploadManager.enqueueFiles(files)
                            },
                            isUploading = tasks.any { it.status == UploadStatus.UPLOADING },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(UploadZoneSpacing))

                        // Supporting text
                        Text(
                            text = stringResource(Res.string.upload_instructions),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(InstructionsSpacing))

                        // "Don't have the application?" link
                        TextButton(
                            onClick = onShowQrCode,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text(
                                text = stringResource(Res.string.upload_no_app_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(ContentPadding))

                        val hasUploads = tasks.isNotEmpty()

                        // Upload list section header
                        if (hasUploads) {
                            Text(
                                text = stringResource(Res.string.upload_uploads_title),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(InstructionsSpacing))
                        }

                        if (hasUploads) {
                            // Scrollable upload list
                            DocumentUploadList(
                                tasks = tasks,
                                documents = documents,
                                deletionHandles = deletionHandles,
                                uploadManager = uploadManager,
                                scrollable = true,
                                showEmptyState = false,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = stringResource(Res.string.upload_no_documents),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
            text = stringResource(Res.string.cashflow_add_document),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(Res.string.action_close),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
