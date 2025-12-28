package ai.dokus.app.cashflow.components

import tech.dokus.foundation.app.state.DokusState
import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.pending_documents_empty
import ai.dokus.app.resources.generated.pending_documents_need_confirmation
import ai.dokus.app.resources.generated.pending_documents_title
import ai.dokus.foundation.design.components.common.DokusErrorContent
import ai.dokus.foundation.design.components.common.ShimmerBox
import ai.dokus.foundation.design.components.common.ShimmerLine
import ai.dokus.foundation.design.extensions.localizedUppercase
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.model.common.PaginationState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import org.jetbrains.compose.resources.stringResource

/**
 * A card component displaying pending documents that need confirmation.
 *
 * Features lazy loading with infinite scroll - automatically loads more
 * items when scrolling near the bottom.
 *
 * @param state Full DokusState containing pagination data, loading, or error
 * @param onDocumentClick Callback when a document row is clicked
 * @param onLoadMore Callback to load more items when reaching the end
 * @param modifier Optional modifier for the card
 */
@Composable
fun PendingDocumentsCard(
    state: DokusState<PaginationState<DocumentRecordDto>>,
    onDocumentClick: (DocumentRecordDto) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(24.dp)
        ) {
            // Title
            Text(
                text = stringResource(Res.string.pending_documents_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            when (state) {
                is DokusState.Loading, is DokusState.Idle -> {
                    PendingDocumentsLoadingContent(
                        modifier = Modifier.weight(1f)
                    )
                }

                is DokusState.Error -> {
                    PendingDocumentsErrorContent(
                        state = state,
                        modifier = Modifier.weight(1f)
                    )
                }

                is DokusState.Success -> {
                    val paginationState = state.data
                    if (paginationState.data.isEmpty()) {
                        PendingDocumentsEmptyContent(
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        PendingDocumentsLazyList(
                            documents = paginationState.data,
                            hasMorePages = paginationState.hasMorePages,
                            isLoadingMore = paginationState.isLoadingMore,
                            onDocumentClick = onDocumentClick,
                            onLoadMore = onLoadMore,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Loading state content with shimmer skeleton rows.
 */
@Composable
private fun PendingDocumentsLoadingContent(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Show 4 skeleton rows
        repeat(4) { index ->
            PendingDocumentItemSkeleton()
            if (index < 3) {
                Spacer(modifier = Modifier.height(12.dp))
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

/**
 * Skeleton for a single pending document item.
 */
@Composable
private fun PendingDocumentItemSkeleton(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Document name skeleton
        ShimmerLine(
            modifier = Modifier.width(180.dp),
            height = 16.dp
        )

        Spacer(Modifier.width(16.dp))

        // Badge skeleton
        ShimmerBox(
            modifier = Modifier
                .width(100.dp)
                .height(22.dp),
            shape = RoundedCornerShape(16.dp)
        )
    }
}

/**
 * Empty state content with a message.
 */
@Composable
private fun PendingDocumentsEmptyContent(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(Res.string.pending_documents_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Error state content with error message and retry button.
 */
@Composable
private fun PendingDocumentsErrorContent(
    state: DokusState.Error<*>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        DokusErrorContent(
            exception = state.exception,
            retryHandler = state.retryHandler
        )
    }
}

/**
 * Lazy list of pending documents with infinite scroll.
 */
@Composable
private fun PendingDocumentsLazyList(
    documents: List<DocumentRecordDto>,
    hasMorePages: Boolean,
    isLoadingMore: Boolean,
    onDocumentClick: (DocumentRecordDto) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Infinite scroll trigger - load more when near the end
    LaunchedEffect(listState, hasMorePages, isLoadingMore) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = layoutInfo.totalItemsCount
            lastVisibleIndex to totalItems
        }
            .distinctUntilChanged()
            .filter { (lastVisible, total) ->
                // Load more when within 2 items of the end
                lastVisible >= total - 2 && hasMorePages && !isLoadingMore
            }
            .collect { onLoadMore() }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        itemsIndexed(
            items = documents,
            key = { _, doc -> doc.document.id.toString() }
        ) { index, processing ->
            PendingDocumentItem(
                processing = processing,
                onClick = { onDocumentClick(processing) }
            )

            // Add divider between items (not after the last item unless loading more)
            if (index < documents.size - 1 || isLoadingMore) {
                Spacer(modifier = Modifier.height(12.dp))
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // Loading indicator at the bottom
        if (isLoadingMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}

/**
 * A single pending document item row displaying the document name and "Need confirmation" badge.
 */
@Composable
private fun PendingDocumentItem(
    processing: DocumentRecordDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val documentName = getDocumentDisplayName(processing)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Document name
        Text(
            text = documentName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Spacer(Modifier.width(16.dp))

        // "Need confirmation" badge
        NeedConfirmationBadge()
    }
}

/**
 * Badge showing "Need confirmation" status.
 */
@Composable
private fun NeedConfirmationBadge(
    modifier: Modifier = Modifier
) {
    Text(
        text = stringResource(Res.string.pending_documents_need_confirmation),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.error,
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 12.dp, vertical = 4.dp)
    )
}

/**
 * Get a display name for a pending document.
 * Uses extracted invoice/bill number if available, otherwise falls back to filename.
 */
@Composable
private fun getDocumentDisplayName(record: DocumentRecordDto): String {
    val filename = record.document.filename
    val extractedData = record.draft?.extractedData

    // Try to get invoice/bill number from extracted data
    val documentNumber = extractedData?.invoice?.invoiceNumber
        ?: extractedData?.bill?.invoiceNumber

    // Get document type prefix (localizedUppercase is @Composable, call outside remembering)
    val typePrefix = record.draft?.documentType?.localizedUppercase.orEmpty()

    return remember(record.document.id, typePrefix, documentNumber, filename) {
        when {
            !documentNumber.isNullOrBlank() -> {
                "$typePrefix $documentNumber"
            }

            !filename.isNullOrBlank() -> {
                val nameWithoutExtension = filename.substringBeforeLast(".").uppercase()
                "$typePrefix $nameWithoutExtension"
            }

            else -> {
                // Fallback to document ID if no filename
                "$typePrefix ${record.document.id.toString().take(8).uppercase()}"
            }
        }
    }
}
