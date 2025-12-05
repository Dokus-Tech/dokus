package ai.dokus.app.cashflow.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// Web/WASM drag and drop is not yet supported in Compose Multiplatform.
// Use file picker instead.
actual val isDragDropSupported: Boolean = false

@Composable
actual fun Modifier.fileDropTarget(
    onDragStateChange: (isDragging: Boolean) -> Unit,
    onFilesDropped: (List<DroppedFile>) -> Unit
): Modifier = this // No-op on Web - use file picker
