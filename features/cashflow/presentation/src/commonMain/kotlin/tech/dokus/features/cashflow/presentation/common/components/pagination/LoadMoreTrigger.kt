package tech.dokus.features.cashflow.presentation.common.components.pagination

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

@Composable
internal fun rememberLoadMoreTrigger(
    listState: LazyListState,
    hasMore: Boolean,
    isLoading: Boolean,
    buffer: Int = 3
): Boolean {
    val shouldLoadMore by remember(listState, hasMore, isLoading, buffer) {
        derivedStateOf {
            if (!hasMore || isLoading) return@derivedStateOf false
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            lastVisibleItem >= totalItems - buffer
        }
    }
    return shouldLoadMore
}
