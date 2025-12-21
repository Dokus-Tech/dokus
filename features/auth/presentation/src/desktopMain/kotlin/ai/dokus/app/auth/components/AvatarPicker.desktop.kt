package ai.dokus.app.auth.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.nio.file.Files

/**
 * Desktop implementation of AvatarPickerLauncher using AWT FileDialog.
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

    val launcher = remember(onImageSelected) {
        AvatarPickerLauncher {
            scope.launch {
                val file = withContext(Dispatchers.IO) {
                    pickImageFile()
                }
                if (file != null) {
                    val bytes = withContext(Dispatchers.IO) {
                        file.readBytes()
                    }
                    val mimeType = withContext(Dispatchers.IO) {
                        Files.probeContentType(file.toPath()) ?: detectMimeTypeFromExtension(file.name)
                    }
                    onImageSelected(PickedImage(
                        name = file.name,
                        bytes = bytes,
                        mimeType = mimeType
                    ))
                }
            }
        }
    }

    return launcher
}

private fun pickImageFile(): File? {
    val fileDialog = FileDialog(null as Frame?, "Select Image", FileDialog.LOAD)
    fileDialog.isMultipleMode = false

    // Filter for image files
    fileDialog.setFilenameFilter { _, name ->
        val lowerName = name.lowercase()
        lowerName.endsWith(".png") ||
            lowerName.endsWith(".jpg") ||
            lowerName.endsWith(".jpeg") ||
            lowerName.endsWith(".gif") ||
            lowerName.endsWith(".webp") ||
            lowerName.endsWith(".bmp")
    }

    fileDialog.isVisible = true

    val directory = fileDialog.directory
    val filename = fileDialog.file

    return if (directory != null && filename != null) {
        File(directory, filename)
    } else {
        null
    }
}

private fun detectMimeTypeFromExtension(filename: String): String? {
    val extension = filename.substringAfterLast('.', "").lowercase()
    return when (extension) {
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        "bmp" -> "image/bmp"
        else -> null
    }
}
