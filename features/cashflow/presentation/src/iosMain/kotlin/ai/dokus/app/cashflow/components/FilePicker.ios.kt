package ai.dokus.app.cashflow.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.mohamedrejeb.calf.core.LocalPlatformContext
import com.mohamedrejeb.calf.io.getName
import com.mohamedrejeb.calf.io.readByteArray
import com.mohamedrejeb.calf.picker.FilePickerFileType
import com.mohamedrejeb.calf.picker.FilePickerSelectionMode
import com.mohamedrejeb.calf.picker.rememberFilePickerLauncher
import kotlinx.coroutines.launch

/**
 * iOS implementation of DocumentFilePickerLauncher using Calf.
 */
actual class DocumentFilePickerLauncher(
    private val launchAction: () -> Unit
) {
    actual fun launch() {
        launchAction()
    }
}

@Composable
actual fun rememberDocumentFilePicker(
    onFilesSelected: (List<DroppedFile>) -> Unit
): DocumentFilePickerLauncher {
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
                onFilesSelected(dropped)
            }
        }
    }

    return DocumentFilePickerLauncher {
        filePickerLauncher.launch()
    }
}
