package tech.dokus.features.cashflow.presentation.cashflow.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// iOS drag and drop would require UIDropInteraction integration
// which is not yet stable in Compose Multiplatform. Use file picker instead.
actual val isDragDropSupported: Boolean = false

@Composable
actual fun Modifier.fileDropTarget(
    onDragStateChange: (isDragging: Boolean) -> Unit,
    onFilesDropped: (List<DroppedFile>) -> Unit
): Modifier = this // No-op on iOS - use file picker
