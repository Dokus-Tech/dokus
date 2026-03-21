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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.pending_documents_empty
import tech.dokus.aura.resources.pending_documents_title
import tech.dokus.domain.model.DocumentListItemDto
import tech.dokus.domain.model.common.PaginationState
import tech.dokus.features.cashflow.presentation.model.toUiStatus
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.app.state.isError
import tech.dokus.foundation.app.state.isSuccess
import tech.dokus.foundation.aura.components.DocumentStatusBadge
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.common.ErrorOverlay
import tech.dokus.foundation.aura.components.common.DokusLoader
import tech.dokus.foundation.aura.components.common.DokusLoaderSize
import tech.dokus.foundation.aura.components.common.ShimmerBox
import tech.dokus.foundation.aura.components.common.ShimmerLine
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.extensions.localizedUppercase

// UI dimensions
private val CardPadding = Constraints.Spacing.large
private val DividerSpacing = Constraints.Spacing.medium
private val DividerHeight = Constraints.Stroke.thin
private val ItemVerticalPadding = Constraints.Spacing.small
private val ItemCornerRadius = Constraints.Spacing.small
private val ItemSpacing = Constraints.Spacing.large
private val ShimmerNameWidth =
    Constraints.AvatarSize.large +
        Constraints.AvatarSize.small +
        Constraints.Spacing.large +
        Constraints.Spacing.xSmall
private val ShimmerNameHeight = Constraints.IconSize.xSmall
private val ShimmerBadgeWidth =
    Constraints.IconSize.xxLarge + Constraints.IconSize.medium + Constraints.Spacing.medium
private val ShimmerBadgeHeight = Constraints.IconSize.smallMedium + Constraints.Spacing.xxSmall
private val BadgeCornerRadius = Constraints.CornerRadius.window

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
    state: DokusState<PaginationState<DocumentListItemDto>>,
    onDocumentClick: (DocumentListItemDto) -> Unit,
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

            ErrorOverlay(
                exception = if (state is DokusState.Error) state.exception else null,
                retryHandler = if (state is DokusState.Error) state.retryHandler else null,
                modifier = Modifier.weight(1f),
            ) {
                when (state) {
                    is DokusState.Loading, is DokusState.Idle -> {
                        PendingDocumentsLoadingContent()
                    }
                    is DokusState.Success -> {
                        if (state.data.data.isEmpty()) {
                            PendingDocumentsEmptyContent()
                        } else {
                            PendingDocumentsLazyList(
                                documents = state.data.data,
                                hasMorePages = state.data.hasMorePages,
                                isLoadingMore = false,
                                onDocumentClick = onDocumentClick,
                                onLoadMore = onLoadMore,
                            )
                        }
                    }
                    is DokusState.Error -> PendingDocumentsEmptyContent()
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
        verticalArrangement = Arrangement.spacedBy(Constraints.Elevation.none)
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
 * Lazy list of pending documents with infinite scroll.
 */
@Composable
private fun PendingDocumentsLazyList(
    documents: List<DocumentListItemDto>,
    hasMorePages: Boolean,
    isLoadingMore: Boolean,
    onDocumentClick: (DocumentListItemDto) -> Unit,
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
        verticalArrangement = Arrangement.spacedBy(Constraints.Elevation.none)
    ) {
        itemsIndexed(
            items = documents,
            key = { _, doc -> doc.documentId.toString() }
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
                    DokusLoader(size = DokusLoaderSize.Small)
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
    processing: DocumentListItemDto,
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
            style = MaterialTheme.typography.bodyLarge,
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
 * Uses purposeRendered or counterparty if available, otherwise falls back to filename.
 */
@Composable
private fun getDocumentDisplayName(record: DocumentListItemDto): String {
    val filename = record.filename

    // Get document type prefix (localizedUppercase is @Composable, call outside remembering)
    val typePrefix = record.documentType?.localizedUppercase.orEmpty()

    // Use purposeRendered as the best available reference
    val displayRef = record.purposeRendered?.takeIf { it.isNotBlank() }

    return remember(record.documentId, typePrefix, displayRef, filename) {
        when {
            !displayRef.isNullOrBlank() -> {
                "$typePrefix $displayRef"
            }

            filename.isNotBlank() -> {
                val nameWithoutExtension = filename.substringBeforeLast(".").uppercase()
                "$typePrefix $nameWithoutExtension"
            }

            else -> {
                // Fallback to document ID if no filename
                "$typePrefix ${record.documentId.toString().take(DocumentIdPreviewLength).uppercase()}"
            }
        }
    }
}

// Previews are in PendingDocumentsCardPreview.kt
