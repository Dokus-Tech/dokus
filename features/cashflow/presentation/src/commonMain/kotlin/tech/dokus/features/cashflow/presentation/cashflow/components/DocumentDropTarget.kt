package tech.dokus.features.cashflow.presentation.cashflow.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Represents a file dropped onto the upload zone.
 */
data class DroppedFile(
    val name: String,
    val bytes: ByteArray,
    val mimeType: String? = null
)

/**
 * Whether the current platform supports drag and drop from external sources.
 */
expect val isDragDropSupported: Boolean

/**
 * Modifier that enables drag and drop file receiving from external sources.
 *
 * Uses native platform APIs for reliable drag-and-drop functionality:
 * - JVM/Desktop: Compose DragAndDropTarget with DragData.FilesList
 * - Android: View.OnDragListener
 * - iOS: Not supported (use file picker instead)
 * - Web: Not supported (use file picker instead)
 *
 * @param onDragStateChange Callback when drag state changes (enter/exit)
 * @param onFilesDropped Callback when files are dropped
 * @return Modified Modifier with drag and drop support
 */
@Composable
expect fun Modifier.fileDropTarget(
    onDragStateChange: (isDragging: Boolean) -> Unit,
    onFilesDropped: (List<DroppedFile>) -> Unit
): Modifier
