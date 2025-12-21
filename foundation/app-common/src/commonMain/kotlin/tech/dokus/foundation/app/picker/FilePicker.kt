package tech.dokus.foundation.app.picker

import androidx.compose.runtime.Composable

/**
 * Type of files to pick.
 */
enum class FilePickerType {
    /** Image files only (png, jpg, jpeg, gif, webp, bmp) */
    Image,
    /** Document files (pdf, doc, docx, xls, xlsx, csv, txt, images) */
    Document
}

/**
 * Result of picking a file, indicating the type of file picked.
 */
sealed class PickedFile {
    abstract val name: String
    abstract val bytes: ByteArray
    abstract val mimeType: String?

    /**
     * A picked image file.
     */
    data class Image(
        override val name: String,
        override val bytes: ByteArray,
        override val mimeType: String?
    ) : PickedFile()

    /**
     * A picked document file.
     */
    data class Document(
        override val name: String,
        override val bytes: ByteArray,
        override val mimeType: String?
    ) : PickedFile()
}

/**
 * Platform-agnostic file picker launcher.
 *
 * Each platform provides its own implementation:
 * - Desktop: AWT FileDialog with appropriate filters
 * - Android/iOS/Web: Uses Calf file picker with appropriate type
 */
expect class FilePickerLauncher {
    fun launch()
}

/**
 * Remember a file picker launcher for selecting files.
 *
 * @param type The type of files to pick (Image or Document)
 * @param allowMultiple Whether to allow selecting multiple files
 * @param onFilesSelected Callback with the selected files as [PickedFile]
 * @return A launcher that can be used to open the file picker
 */
@Composable
expect fun rememberFilePicker(
    type: FilePickerType,
    allowMultiple: Boolean = false,
    onFilesSelected: (List<PickedFile>) -> Unit
): FilePickerLauncher

/**
 * Convenience function for picking a single image.
 */
@Composable
fun rememberImagePicker(
    onImageSelected: (PickedFile.Image) -> Unit
): FilePickerLauncher = rememberFilePicker(
    type = FilePickerType.Image,
    allowMultiple = false,
    onFilesSelected = { files ->
        files.firstOrNull()?.let { file ->
            if (file is PickedFile.Image) {
                onImageSelected(file)
            }
        }
    }
)

/**
 * Convenience function for picking documents.
 */
@Composable
fun rememberDocumentPicker(
    allowMultiple: Boolean = true,
    onFilesSelected: (List<PickedFile.Document>) -> Unit
): FilePickerLauncher = rememberFilePicker(
    type = FilePickerType.Document,
    allowMultiple = allowMultiple,
    onFilesSelected = { files ->
        onFilesSelected(files.filterIsInstance<PickedFile.Document>())
    }
)
