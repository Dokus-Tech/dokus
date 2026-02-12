package tech.dokus.app.share

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Global ingress for files shared from platform share sheets.
 *
 * - platform code pushes files via [onNewSharedFiles]
 * - app navigation listens on [events]
 * - import flow consumes payload via [consumePendingFiles]
 */
object ExternalShareImportHandler {
    private val eventFlow = MutableSharedFlow<List<SharedImportFile>>(extraBufferCapacity = 1)
    private val pendingFlow = MutableStateFlow<List<SharedImportFile>?>(null)

    val events = eventFlow.asSharedFlow()
    val pendingState = pendingFlow.asStateFlow()

    fun onNewSharedFiles(files: List<SharedImportFile>) {
        if (files.isEmpty()) return
        pendingFlow.value = files
        eventFlow.tryEmit(files)
    }

    fun onNewSharedFile(file: SharedImportFile) {
        onNewSharedFiles(listOf(file))
    }

    fun consumePendingFiles(): List<SharedImportFile>? {
        val file = pendingFlow.value
        pendingFlow.value = null
        return file
    }

    fun consumePendingFile(): SharedImportFile? = consumePendingFiles()?.firstOrNull()
}
