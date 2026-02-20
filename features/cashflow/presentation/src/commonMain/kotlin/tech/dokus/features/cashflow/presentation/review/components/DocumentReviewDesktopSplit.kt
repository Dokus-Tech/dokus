package tech.dokus.features.cashflow.presentation.review.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import tech.dokus.domain.ids.DocumentId
import tech.dokus.foundation.app.shell.DocQueueItem
import tech.dokus.foundation.aura.components.status.StatusDot
import tech.dokus.foundation.aura.components.status.StatusDotType
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.surfaceHover
import tech.dokus.foundation.aura.style.textFaint
import tech.dokus.foundation.aura.style.textMuted

private val QueuePaneWidth = 248.dp

@Composable
internal fun DocumentReviewDesktopSplit(
    documents: List<DocQueueItem>,
    selectedDocumentId: DocumentId,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    onSelectDocument: (DocumentId) -> Unit,
    onLoadMore: () -> Unit,
    onExit: () -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(Constraints.Shell.gap),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .width(QueuePaneWidth),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 1.dp,
        ) {
            DocumentReviewQueuePane(
                documents = documents,
                selectedDocumentId = selectedDocumentId,
                hasMore = hasMore,
                isLoadingMore = isLoadingMore,
                onSelectDocument = onSelectDocument,
                onLoadMore = onLoadMore,
                onExit = onExit,
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            content()
        }
    }
}

@Composable
private fun DocumentReviewQueuePane(
    documents: List<DocQueueItem>,
    selectedDocumentId: DocumentId,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    onSelectDocument: (DocumentId) -> Unit,
    onLoadMore: () -> Unit,
    onExit: () -> Unit,
) {
    val listState = rememberLazyListState()
    val selectedIndex = documents.indexOfFirst { it.id == selectedDocumentId }
    val positionText = if (selectedIndex >= 0) "${selectedIndex + 1}/${documents.size}" else ""

    LaunchedEffect(documents.size, hasMore, isLoadingMore) {
        if (documents.isEmpty()) return@LaunchedEffect
        snapshotFlow {
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            hasMore && !isLoadingMore && lastVisibleIndex >= (documents.lastIndex - 2)
        }
            .distinctUntilChanged()
            .filter { it }
            .collect { onLoadMore() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "< All docs",
                modifier = Modifier.clickable(onClick = onExit),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = positionText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.textFaint,
            )
        }
        HorizontalDivider()

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            items(items = documents, key = { it.id.toString() }) { item ->
                val isSelected = item.id == selectedDocumentId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.surfaceHover
                            else MaterialTheme.colorScheme.surface
                        )
                        .clickable { onSelectDocument(item.id) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatusDot(
                        type = if (item.isConfirmed) StatusDotType.Confirmed else StatusDotType.Warning,
                        size = 5.dp,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.vendorName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = item.date,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.textMuted,
                            fontSize = 10.sp,
                        )
                    }
                    Text(
                        text = item.amount,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                HorizontalDivider()
            }
        }
    }
}
