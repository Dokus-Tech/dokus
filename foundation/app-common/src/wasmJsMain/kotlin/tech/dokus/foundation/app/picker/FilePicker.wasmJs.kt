package tech.dokus.foundation.app.picker

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
 * WASM/Web implementation of FilePickerLauncher using Calf.
 */
actual class FilePickerLauncher(
    private val launchAction: () -> Unit
) {
    actual fun launch() {
        launchAction()
    }
}

@Composable
actual fun rememberFilePicker(
    type: FilePickerType,
    allowMultiple: Boolean,
    onFilesSelected: (List<PickedFile>) -> Unit
): FilePickerLauncher {
    val scope = rememberCoroutineScope()
    val platformContext = LocalPlatformContext.current

    val calfType = when (type) {
        FilePickerType.Image -> FilePickerFileType.Image
        FilePickerType.Document -> FilePickerFileType.Document
    }

    val selectionMode = if (allowMultiple) {
        FilePickerSelectionMode.Multiple
    } else {
        FilePickerSelectionMode.Single
    }

    val filePickerLauncher = rememberFilePickerLauncher(
        type = calfType,
        selectionMode = selectionMode
    ) { files ->
        scope.launch {
            val picked = files.mapNotNull { file ->
                val bytes = runCatching { file.readByteArray(platformContext) }.getOrNull()
                val name = file.getName(platformContext) ?: "file"
                bytes?.let {
                    when (type) {
                        FilePickerType.Image -> PickedFile.Image(name = name, bytes = it, mimeType = null)
                        FilePickerType.Document -> PickedFile.Document(name = name, bytes = it, mimeType = null)
                    }
                }
            }
            if (picked.isNotEmpty()) {
                onFilesSelected(picked)
            }
        }
    }

    return FilePickerLauncher {
        filePickerLauncher.launch()
    }
}
