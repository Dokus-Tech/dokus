package ai.dokus.app.cashflow.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.FilenameFilter
import java.nio.file.Files
import javax.swing.SwingUtilities

/**
 * Desktop implementation of DocumentFilePickerLauncher using AWT FileDialog.
 * This avoids the JNA issues that Calf has on macOS.
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
    return remember(onFilesSelected) {
        DocumentFilePickerLauncher {
            SwingUtilities.invokeLater {
                val frame = Frame()
                val dialog = FileDialog(frame, "Select Documents", FileDialog.LOAD).apply {
                    isMultipleMode = true
                    filenameFilter = createDocumentFilenameFilter()
                }

                dialog.isVisible = true

                val selectedFiles = dialog.files?.toList() ?: emptyList()

                frame.dispose()

                if (selectedFiles.isNotEmpty()) {
                    val droppedFiles = selectedFiles.mapNotNull { file ->
                        try {
                            DroppedFile(
                                name = file.name,
                                bytes = file.readBytes(),
                                mimeType = Files.probeContentType(file.toPath())
                            )
                        } catch (e: Exception) {
                            println("[FilePicker] Error reading file: ${file.name} - ${e.message}")
                            null
                        }
                    }
                    if (droppedFiles.isNotEmpty()) {
                        onFilesSelected(droppedFiles)
                    }
                }
            }
        }
    }
}

/**
 * Creates a filename filter for common document types.
 */
private fun createDocumentFilenameFilter(): FilenameFilter {
    val extensions = setOf(
        "pdf", "png", "jpg", "jpeg", "gif", "webp", "bmp",
        "doc", "docx", "xls", "xlsx", "csv", "txt"
    )

    return FilenameFilter { _, name ->
        val lowerName = name.lowercase()
        extensions.any { ext -> lowerName.endsWith(".$ext") }
    }
}
