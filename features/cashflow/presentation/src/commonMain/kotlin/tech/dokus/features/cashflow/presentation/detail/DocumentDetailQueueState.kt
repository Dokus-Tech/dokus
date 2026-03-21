package tech.dokus.features.cashflow.presentation.detail

import androidx.compose.runtime.Immutable
import tech.dokus.foundation.app.shell.DocQueueItem
import tech.dokus.navigation.destinations.CashFlowDestination

@Immutable
data class DocumentDetailQueueState(
    val context: CashFlowDestination.DocumentDetailQueueContext,
    val items: List<DocQueueItem> = emptyList(),
    val currentPage: Int = -1,
    val hasMore: Boolean = true,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
)
