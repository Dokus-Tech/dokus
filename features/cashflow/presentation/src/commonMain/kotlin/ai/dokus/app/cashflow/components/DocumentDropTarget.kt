package ai.dokus.app.cashflow.components

import kotlinx.coroutines.CoroutineScope
import androidx.compose.ui.Modifier

data class DroppedFile(
    val name: String,
    val bytes: ByteArray,
    val mimeType: String? = null
)

expect fun Modifier.documentDropTarget(
    scope: CoroutineScope,
    onFilesDropped: (List<DroppedFile>) -> Unit
): Modifier
