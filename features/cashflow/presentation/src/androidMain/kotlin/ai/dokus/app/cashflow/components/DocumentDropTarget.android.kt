package ai.dokus.app.cashflow.components

import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope

actual fun Modifier.documentDropTarget(
    scope: CoroutineScope,
    onFilesDropped: (List<DroppedFile>) -> Unit
): Modifier = this
