package ai.dokus.app.cashflow.components

import androidx.compose.runtime.Composable

/**
 * Platform-agnostic file picker launcher.
 *
 * Each platform provides its own implementation:
 * - Desktop: AWT FileDialog (avoids Calf JNA issues on macOS)
 * - Android/iOS/Web: Uses Calf file picker
 */
expect class DocumentFilePickerLauncher {
    fun launch()
}

/**
 * Remember a file picker launcher for selecting documents.
 *
 * @param onFilesSelected Callback with list of selected files as [DroppedFile]
 * @return A launcher that can be used to open the file picker
 */
@Composable
expect fun rememberDocumentFilePicker(
    onFilesSelected: (List<DroppedFile>) -> Unit
): DocumentFilePickerLauncher
