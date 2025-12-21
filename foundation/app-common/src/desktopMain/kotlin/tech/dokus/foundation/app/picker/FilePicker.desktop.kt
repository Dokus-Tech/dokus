package tech.dokus.foundation.app.picker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.FilenameFilter
import java.nio.file.Files
import javax.swing.SwingUtilities

/**
 * Desktop implementation of FilePickerLauncher using AWT FileDialog.
 * This avoids the JNA issues that Calf has on macOS.
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
    return remember(type, allowMultiple, onFilesSelected) {
        FilePickerLauncher {
            SwingUtilities.invokeLater {
                val frame = Frame()
                val dialog = FileDialog(frame, selectDialogTitle(type), FileDialog.LOAD).apply {
                    isMultipleMode = allowMultiple
                    filenameFilter = createFilenameFilter(type)
                }

                dialog.isVisible = true

                val selectedFiles = dialog.files?.toList() ?: emptyList()

                frame.dispose()

                if (selectedFiles.isNotEmpty()) {
                    val pickedFiles = selectedFiles.mapNotNull { file ->
                        try {
                            val mimeType = Files.probeContentType(file.toPath())
                                ?: detectMimeTypeFromExtension(file.name)
                            when (type) {
                                FilePickerType.Image -> PickedFile.Image(
                                    name = file.name,
                                    bytes = file.readBytes(),
                                    mimeType = mimeType
                                )
                                FilePickerType.Document -> PickedFile.Document(
                                    name = file.name,
                                    bytes = file.readBytes(),
                                    mimeType = mimeType
                                )
                            }
                        } catch (e: Exception) {
                            println("[FilePicker] Error reading file: ${file.name} - ${e.message}")
                            null
                        }
                    }
                    if (pickedFiles.isNotEmpty()) {
                        onFilesSelected(pickedFiles)
                    }
                }
            }
        }
    }
}

private fun selectDialogTitle(type: FilePickerType): String = when (type) {
    FilePickerType.Image -> "Select Image"
    FilePickerType.Document -> "Select Documents"
}

private fun createFilenameFilter(type: FilePickerType): FilenameFilter {
    val extensions = when (type) {
        FilePickerType.Image -> setOf("png", "jpg", "jpeg", "gif", "webp", "bmp")
        FilePickerType.Document -> setOf(
            "pdf", "png", "jpg", "jpeg", "gif", "webp", "bmp",
            "doc", "docx", "xls", "xlsx", "csv", "txt"
        )
    }

    return FilenameFilter { _, name ->
        val lowerName = name.lowercase()
        extensions.any { ext -> lowerName.endsWith(".$ext") }
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
        "pdf" -> "application/pdf"
        "doc" -> "application/msword"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "xls" -> "application/vnd.ms-excel"
        "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        "csv" -> "text/csv"
        "txt" -> "text/plain"
        else -> null
    }
}
