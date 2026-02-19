package tech.dokus.foundation.app.shell

import androidx.compose.runtime.staticCompositionLocalOf
import tech.dokus.domain.ids.DocumentId

/**
 * Queue item for the document detail mode sidebar.
 */
data class DocQueueItem(
    val id: DocumentId,
    val vendorName: String,
    val date: String,
    val amount: String,
    val isConfirmed: Boolean,
)

/**
 * Host interface for entering/exiting document detail mode.
 * Provided by HomeScreen, consumed by DocumentsRoute.
 */
interface DocDetailModeHost {
    fun enter(documentId: DocumentId, documents: List<DocQueueItem>)
    fun select(documentId: DocumentId)
    fun exit()
}

val LocalDocDetailModeHost = staticCompositionLocalOf<DocDetailModeHost?> { null }
