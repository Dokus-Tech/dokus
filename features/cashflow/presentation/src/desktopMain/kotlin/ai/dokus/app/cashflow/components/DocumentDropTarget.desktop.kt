package ai.dokus.app.cashflow.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
import java.io.File
import java.net.URI
import java.nio.file.Files

actual val isDragDropSupported: Boolean = true

/**
 * JVM/Desktop implementation using Compose Multiplatform drag and drop API.
 *
 * Uses DragData.FilesList to receive files dropped from external sources.
 * Tracks drag state for visual feedback.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
actual fun Modifier.fileDropTarget(
    onDragStateChange: (isDragging: Boolean) -> Unit,
    onFilesDropped: (List<DroppedFile>) -> Unit
): Modifier {
    val currentOnDragStateChange by rememberUpdatedState(onDragStateChange)
    val currentOnFilesDropped by rememberUpdatedState(onFilesDropped)

    val dragAndDropTarget = remember {
        object : DragAndDropTarget {
            override fun onStarted(event: DragAndDropEvent) {
                currentOnDragStateChange(true)
            }

            override fun onEntered(event: DragAndDropEvent) {
                currentOnDragStateChange(true)
            }

            override fun onExited(event: DragAndDropEvent) {
                currentOnDragStateChange(false)
            }

            override fun onEnded(event: DragAndDropEvent) {
                currentOnDragStateChange(false)
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                currentOnDragStateChange(false)

                val filesList = event.dragData() as? DragData.FilesList
                if (filesList != null) {
                    val uris = filesList.readFiles()
                    val droppedFiles = uris.mapNotNull { uriString ->
                        try {
                            val file = File(URI(uriString))
                            if (file.exists()) {
                                DroppedFile(
                                    name = file.name,
                                    bytes = file.readBytes(),
                                    mimeType = Files.probeContentType(file.toPath())
                                )
                            } else {
                                null
                            }
                        } catch (e: Exception) {
                            println("[DnD] Error processing file: $uriString - ${e.message}")
                            null
                        }
                    }

                    if (droppedFiles.isNotEmpty()) {
                        currentOnFilesDropped(droppedFiles)
                        return true
                    }
                }

                return false
            }
        }
    }

    return this.dragAndDropTarget(
        shouldStartDragAndDrop = { event ->
            event.dragData() is DragData.FilesList
        },
        target = dragAndDropTarget
    )
}
