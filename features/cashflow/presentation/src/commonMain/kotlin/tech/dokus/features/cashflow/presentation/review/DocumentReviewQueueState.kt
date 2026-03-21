package tech.dokus.features.cashflow.presentation.review

import androidx.compose.runtime.Immutable
import tech.dokus.foundation.app.shell.DocQueueItem
import tech.dokus.navigation.destinations.CashFlowDestination

@Immutable
data class DocumentReviewQueueState(
    val context: CashFlowDestination.DocumentReviewQueueContext,
    val items: List<DocQueueItem> = emptyList(),
    val currentPage: Int = -1,
    val hasMore: Boolean = true,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
)
