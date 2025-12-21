package ai.dokus.app.auth.components

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
 * iOS implementation of AvatarPickerLauncher using Calf.
 */
actual class AvatarPickerLauncher(
    private val launchAction: () -> Unit
) {
    actual fun launch() {
        launchAction()
    }
}

@Composable
actual fun rememberAvatarPicker(
    onImageSelected: (PickedImage) -> Unit
): AvatarPickerLauncher {
    val scope = rememberCoroutineScope()
    val platformContext = LocalPlatformContext.current

    val filePickerLauncher = rememberFilePickerLauncher(
        type = FilePickerFileType.Image,
        selectionMode = FilePickerSelectionMode.Single
    ) { files ->
        scope.launch {
            files.firstOrNull()?.let { file ->
                val bytes = runCatching { file.readByteArray(platformContext) }.getOrNull()
                val name = file.getName(platformContext) ?: "avatar"
                bytes?.let {
                    onImageSelected(PickedImage(name = name, bytes = it, mimeType = null))
                }
            }
        }
    }

    return AvatarPickerLauncher {
        filePickerLauncher.launch()
    }
}
