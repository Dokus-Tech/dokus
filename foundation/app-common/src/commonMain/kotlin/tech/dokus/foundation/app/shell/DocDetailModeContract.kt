package tech.dokus.foundation.app.shell

import androidx.compose.runtime.staticCompositionLocalOf
import tech.dokus.domain.ids.DocumentId

/**
 * Queue item for the document detail mode sidebar.
 */
enum class DocQueueStatus {
    Paid,
    Unpaid,
    Overdue,
    Review,
    Processing,
}

data class DocQueueItem(
    val id: DocumentId,
    val vendorName: String,
    val date: String,
    val amount: String,
    val status: DocQueueStatus,
    val statusDetail: String? = null,
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

/**
 * True when the current composition is rendered inside the document detail mode content pane.
 * Used by DocumentReviewScreen to suppress its own top bar and use transparent background.
 */
val LocalIsInDocDetailMode = staticCompositionLocalOf { false }
