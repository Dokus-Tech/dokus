package ai.dokus.app.cashflow.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URI
import java.nio.file.Files

actual val isDragDropSupported: Boolean = true

/**
 * Set of allowed file extensions for document uploads.
 * Supports PDF, images, and Office documents.
 */
private val allowedExtensions = setOf(
    "pdf",
    "png", "jpg", "jpeg", "webp", "gif",
    "doc", "docx", "xls", "xlsx", "ppt", "pptx"
)

/**
 * Checks if a file has an allowed extension for upload.
 *
 * @param file The file to check
 * @return true if the file extension is in the allowed list, false otherwise
 */
private fun isAllowedFileType(file: File): Boolean {
    val extension = file.extension.lowercase()
    return extension in allowedExtensions
}

/**
 * Recursively collects all allowed files from a directory and its subdirectories.
 *
 * This function traverses the directory tree, filtering out:
 * - Hidden files and folders (names starting with '.')
 * - Files with unsupported extensions
 *
 * The result is a flat list of files regardless of the original directory structure.
 *
 * @param directory The directory to traverse
 * @return List of allowed files found in the directory tree
 */
private fun collectFilesFromDirectory(directory: File): List<File> {
    val files = mutableListOf<File>()

    // listFiles() returns null if the path is not a directory or if an I/O error occurs
    directory.listFiles()?.forEach { file ->
        // Skip hidden files and folders (e.g., .DS_Store, .gitignore)
        if (file.name.startsWith(".")) return@forEach

        if (file.isDirectory) {
            // Recursively collect files from subdirectories
            files.addAll(collectFilesFromDirectory(file))
        } else if (isAllowedFileType(file)) {
            // Only include files with allowed extensions
            files.add(file)
        }
    }

    return files
}

/**
 * JVM/Desktop implementation using Compose Multiplatform drag and drop API.
 *
 * Uses DragData.FilesList to receive files dropped from external sources.
 * Tracks drag state for visual feedback.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
actual fun Modifier.fileDropTarget(
    onDragStateChange: (isDragging: Boolean) -> Unit,
    onFilesDropped: (List<DroppedFile>) -> Unit
): Modifier {
    val currentOnDragStateChange by rememberUpdatedState(onDragStateChange)
    val currentOnFilesDropped by rememberUpdatedState(onFilesDropped)
    val scope = rememberCoroutineScope()

    val dragAndDropTarget = remember {
        object : DragAndDropTarget {
            override fun onStarted(event: DragAndDropEvent) {
                currentOnDragStateChange(true)
            }

            override fun onEntered(event: DragAndDropEvent) {
                currentOnDragStateChange(true)
            }

            override fun onExited(event: DragAndDropEvent) {
                currentOnDragStateChange(false)
            }

            override fun onEnded(event: DragAndDropEvent) {
                currentOnDragStateChange(false)
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                currentOnDragStateChange(false)

                val filesList = event.dragData() as? DragData.FilesList
                if (filesList != null) {
                    // Collect URIs quickly on main thread (fast operation)
                    val uris = filesList.readFiles()
                    if (uris.isEmpty()) return false

                    // Move all file I/O to background thread to avoid blocking UI
                    scope.launch {
                        val droppedFiles = withContext(Dispatchers.IO) {
                            // Collect all files from dropped items (both files and directories)
                            val allFiles = mutableListOf<File>()
                            uris.forEach { uriString ->
                                try {
                                    val file = File(URI(uriString))
                                    if (file.exists()) {
                                        if (file.isDirectory) {
                                            // Recursively collect all allowed files from directory
                                            allFiles.addAll(collectFilesFromDirectory(file))
                                        } else if (isAllowedFileType(file)) {
                                            // Single file with allowed type
                                            allFiles.add(file)
                                        }
                                    }
                                } catch (_: Exception) {
                                    // Skip items that can't be processed
                                }
                            }

                            // Convert collected files to DroppedFile objects
                            allFiles.mapNotNull { file ->
                                try {
                                    DroppedFile(
                                        name = file.name,
                                        bytes = file.readBytes(),
                                        mimeType = Files.probeContentType(file.toPath())
                                    )
                                } catch (_: Exception) {
                                    // Skip files that can't be read (permissions, etc.)
                                    null
                                }
                            }
                        }

                        if (droppedFiles.isNotEmpty()) {
                            currentOnFilesDropped(droppedFiles)
                        }
                    }

                    // Accept the drop immediately, file reading happens async
                    return true
                }

                return false
            }
        }
    }

    return this.dragAndDropTarget(
        shouldStartDragAndDrop = { event ->
            event.dragData() is DragData.FilesList
        },
        target = dragAndDropTarget
    )
}
