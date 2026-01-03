package tech.dokus.features.cashflow.presentation.cashflow.components

import androidx.compose.runtime.Composable
import tech.dokus.foundation.app.picker.FilePickerLauncher
import tech.dokus.foundation.app.picker.FilePickerType
import tech.dokus.foundation.app.picker.PickedFile
import tech.dokus.foundation.app.picker.rememberFilePicker

/**
 * Platform-agnostic file picker launcher for documents.
 * This is a compatibility wrapper for the new unified FilePicker in app-common.
 */
typealias DocumentFilePickerLauncher = FilePickerLauncher

/**
 * Remember a file picker launcher for selecting documents.
 *
 * @param onFilesSelected Callback with list of selected files as [DroppedFile]
 * @return A launcher that can be used to open the file picker
 */
@Composable
fun rememberDocumentFilePicker(
    onFilesSelected: (List<DroppedFile>) -> Unit
): DocumentFilePickerLauncher = rememberFilePicker(
    type = FilePickerType.Document,
    allowMultiple = true,
    onFilesSelected = { pickedFiles ->
        onFilesSelected(pickedFiles.map { it.toDroppedFile() })
    }
)

/**
 * Convert PickedFile to DroppedFile for backwards compatibility.
 */
private fun PickedFile.toDroppedFile(): DroppedFile = DroppedFile(
    name = name,
    bytes = bytes,
    mimeType = mimeType
)
