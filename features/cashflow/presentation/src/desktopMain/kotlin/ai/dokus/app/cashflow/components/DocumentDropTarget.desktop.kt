package ai.dokus.app.cashflow.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.ExperimentalComposeUiApi
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTargetDropEvent
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
actual fun Modifier.documentDropTarget(
    scope: CoroutineScope,
    onFilesDropped: (List<DroppedFile>) -> Unit
): Modifier = this.dragAndDropTarget(
    shouldStartDragAndDrop = { true },
    target = object : DragAndDropTarget {
        override fun onDrop(event: DragAndDropEvent): Boolean {
            val native = event.nativeEvent as? DropTargetDropEvent ?: return false
            native.acceptDrop(DnDConstants.ACTION_COPY)

            val transferable = native.transferable ?: return false
            val files = runCatching {
                @Suppress("UNCHECKED_CAST")
                transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<*>
            }.getOrNull()?.mapNotNull { it as? File }.orEmpty()

            if (files.isEmpty()) {
                native.dropComplete(false)
                return false
            }

            scope.launch(Dispatchers.IO) {
                val dropped = files.mapNotNull { path ->
                    runCatching {
                        DroppedFile(
                            name = path.name,
                            bytes = path.readBytes(),
                            mimeType = Files.probeContentType(path.toPath())
                        )
                    }.getOrNull()
                }
                if (dropped.isNotEmpty()) {
                    onFilesDropped(dropped)
                }
            }

            native.dropComplete(true)
            return true
        }
    }
)
