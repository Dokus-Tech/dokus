package tech.dokus.features.cashflow.presentation.cashflow.components

import androidx.compose.foundation.background
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
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.pending_documents_empty
import tech.dokus.aura.resources.pending_documents_title
import tech.dokus.domain.model.BillDraftData
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.ReceiptDraftData
import tech.dokus.features.cashflow.presentation.model.toUiStatus
import tech.dokus.foundation.aura.components.DocumentStatusBadge
import tech.dokus.domain.model.common.PaginationState
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.common.DokusErrorContent
import tech.dokus.foundation.aura.components.common.ShimmerBox
import tech.dokus.foundation.aura.components.common.ShimmerLine
import tech.dokus.foundation.aura.extensions.localizedUppercase

// UI dimensions
private val CardPadding = 16.dp
private val DividerSpacing = 12.dp
private val DividerHeight = 1.dp
private val ItemVerticalPadding = 8.dp
private val ItemCornerRadius = 8.dp
private val ItemSpacing = 16.dp
private val ShimmerNameWidth = 180.dp
private val ShimmerNameHeight = 16.dp
private val ShimmerBadgeWidth = 100.dp
private val ShimmerBadgeHeight = 22.dp
private val BadgeCornerRadius = 16.dp
private val LoadingIndicatorSize = 24.dp
private val LoadingIndicatorStrokeWidth = 2.dp

// Pagination constants
private const val SkeletonRowCount = 4
private const val LastDividerIndex = 3
private const val LoadMoreThreshold = 2
private const val DocumentIdPreviewLength = 8

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
    DokusCardSurface(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(CardPadding)
        ) {
            // Title
            Text(
                text = stringResource(Res.string.pending_documents_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(CardPadding))

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
        // Show skeleton rows
        repeat(SkeletonRowCount) { index ->
            PendingDocumentItemSkeleton()
            if (index < LastDividerIndex) {
                Spacer(modifier = Modifier.height(DividerSpacing))
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(DividerHeight)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
                Spacer(modifier = Modifier.height(DividerSpacing))
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
            .padding(vertical = ItemVerticalPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Document name skeleton
        ShimmerLine(
            modifier = Modifier.width(ShimmerNameWidth),
            height = ShimmerNameHeight
        )

        Spacer(Modifier.width(ItemSpacing))

        // Badge skeleton
        ShimmerBox(
            modifier = Modifier
                .width(ShimmerBadgeWidth)
                .height(ShimmerBadgeHeight),
            shape = RoundedCornerShape(BadgeCornerRadius)
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
                // Load more when within threshold items of the end
                lastVisible >= total - LoadMoreThreshold && hasMorePages && !isLoadingMore
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
                Spacer(modifier = Modifier.height(DividerSpacing))
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(DividerHeight)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
                Spacer(modifier = Modifier.height(DividerSpacing))
            }
        }

        // Loading indicator at the bottom
        if (isLoadingMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = ItemVerticalPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(LoadingIndicatorSize),
                        strokeWidth = LoadingIndicatorStrokeWidth
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
            .clip(RoundedCornerShape(ItemCornerRadius))
            .clickable(onClick = onClick)
            .padding(vertical = ItemVerticalPadding),
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

        Spacer(Modifier.width(ItemSpacing))

        // Status badge
        DocumentStatusBadge(status = processing.toUiStatus())
    }
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
    val documentNumber = when (extractedData) {
        is InvoiceDraftData -> extractedData.invoiceNumber
        is BillDraftData -> extractedData.invoiceNumber
        is CreditNoteDraftData -> extractedData.creditNoteNumber
        is ReceiptDraftData -> extractedData.receiptNumber
        else -> null
    }

    // Get document type prefix (localizedUppercase is @Composable, call outside remembering)
    val typePrefix = record.draft?.documentType?.localizedUppercase.orEmpty()

    return remember(record.document.id, typePrefix, documentNumber, filename) {
        when {
            !documentNumber.isNullOrBlank() -> {
                "$typePrefix $documentNumber"
            }

            filename.isNotBlank() -> {
                val nameWithoutExtension = filename.substringBeforeLast(".").uppercase()
                "$typePrefix $nameWithoutExtension"
            }

            else -> {
                // Fallback to document ID if no filename
                "$typePrefix ${record.document.id.toString().take(DocumentIdPreviewLength).uppercase()}"
            }
        }
    }
}
