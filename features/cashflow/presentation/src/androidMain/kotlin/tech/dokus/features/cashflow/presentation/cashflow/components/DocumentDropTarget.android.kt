package tech.dokus.features.cashflow.presentation.cashflow.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// Android drag and drop from external sources requires custom View integration
// which is complex in Compose. For now, use file picker instead.
actual val isDragDropSupported: Boolean = false

@Composable
actual fun Modifier.fileDropTarget(
    onDragStateChange: (isDragging: Boolean) -> Unit,
    onFilesDropped: (List<DroppedFile>) -> Unit
): Modifier = this // No-op on Android - use file picker
