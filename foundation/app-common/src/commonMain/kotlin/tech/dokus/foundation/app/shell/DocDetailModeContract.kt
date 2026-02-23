package tech.dokus.foundation.app.shell

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.datetime.LocalDate
import tech.dokus.domain.DisplayName
import tech.dokus.domain.Money
import tech.dokus.domain.enums.Currency
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

sealed interface DocQueueStatusDetail {
    data object Processing : DocQueueStatusDetail
    data class OverdueDays(val days: Int) : DocQueueStatusDetail
}

data class DocQueueItem(
    val id: DocumentId,
    val vendorName: DisplayName,
    val date: LocalDate,
    val amount: Money?,
    val currency: Currency = Currency.default,
    val status: DocQueueStatus,
    val statusDetail: DocQueueStatusDetail? = null,
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
