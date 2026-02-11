package tech.dokus.app.share

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Global ingress for files shared from platform share sheets.
 *
 * - platform code pushes files via [onNewSharedFile]
 * - app navigation listens on [events]
 * - import flow consumes payload via [consumePendingFile]
 */
object ExternalShareImportHandler {
    private val eventFlow = MutableSharedFlow<SharedImportFile>(extraBufferCapacity = 1)
    private val pendingFlow = MutableStateFlow<SharedImportFile?>(null)

    val events = eventFlow.asSharedFlow()
    val pendingState = pendingFlow.asStateFlow()

    fun onNewSharedFile(file: SharedImportFile) {
        pendingFlow.value = file
        eventFlow.tryEmit(file)
    }

    fun consumePendingFile(): SharedImportFile? {
        val file = pendingFlow.value
        pendingFlow.value = null
        return file
    }
}
