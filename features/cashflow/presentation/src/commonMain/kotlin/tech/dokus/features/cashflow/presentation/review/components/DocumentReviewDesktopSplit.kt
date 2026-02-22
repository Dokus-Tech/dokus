package tech.dokus.features.cashflow.presentation.review.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.document_detail_confirmed
import tech.dokus.aura.resources.document_detail_needs_review
import tech.dokus.aura.resources.document_detail_vendor_fallback
import tech.dokus.domain.ids.DocumentId
import tech.dokus.foundation.app.shell.DocQueueItem
import tech.dokus.foundation.app.shell.LocalIsInDocDetailMode
import tech.dokus.foundation.aura.components.background.AmbientBackground
import tech.dokus.foundation.aura.components.common.KeyboardNavigationHint
import tech.dokus.foundation.aura.components.queue.DocQueueHeader
import tech.dokus.foundation.aura.components.queue.DocQueueItemRow
import tech.dokus.foundation.aura.components.status.StatusDot
import tech.dokus.foundation.aura.components.status.StatusDotType
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.dokusEffects
import tech.dokus.foundation.aura.style.dokusSpacing
import tech.dokus.foundation.aura.style.glass
import tech.dokus.foundation.aura.style.glassBorder
import tech.dokus.foundation.aura.style.glassContent
import tech.dokus.foundation.aura.style.glassHeader
import tech.dokus.foundation.aura.style.textMuted
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

// Same height as DesktopShellTopBarFrame in HomeShellChrome.kt.
private val TitleBarHeight = Constraints.Height.button + Constraints.Spacing.medium

@Composable
internal fun DocumentReviewDesktopSplit(
    documents: List<DocQueueItem>,
    selectedDocumentId: DocumentId,
    selectedDoc: DocQueueItem?,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    onSelectDocument: (DocumentId) -> Unit,
    onLoadMore: () -> Unit,
    onExit: () -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        AmbientBackground()

        Row(
            Modifier
                .fillMaxSize()
                .padding(Constraints.Shell.padding)
        ) {
            // Queue panel — matches home sidebar styling
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(Constraints.DocumentDetail.queueWidth),
                shape = MaterialTheme.shapes.large,
                color = colorScheme.glass,
                border = BorderStroke(1.dp, colorScheme.glassBorder),
                tonalElevation = 0.dp,
                shadowElevation = 8.dp,
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

            // Content panel — matches home content area styling
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = Constraints.Shell.gap),
                color = colorScheme.glassContent,
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, colorScheme.glassBorder),
                tonalElevation = 0.dp,
                shadowElevation = 8.dp,
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    DetailTitleBar(
                        vendorName = selectedDoc?.vendorName
                            ?: stringResource(Res.string.document_detail_vendor_fallback),
                        amount = selectedDoc?.amount ?: "",
                        isConfirmed = selectedDoc?.isConfirmed ?: false,
                    )

                    CompositionLocalProvider(LocalIsInDocDetailMode provides true) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            content()
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Title bar
// ---------------------------------------------------------------------------

@Composable
private fun DetailTitleBar(
    vendorName: String,
    amount: String,
    isConfirmed: Boolean,
) {
    val colorScheme = MaterialTheme.colorScheme
    val spacing = MaterialTheme.dokusSpacing
    val effects = MaterialTheme.dokusEffects
    val statusColor = if (isConfirmed) colorScheme.tertiary else colorScheme.primary

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(TitleBarHeight)
                .background(colorScheme.glassHeader)
                .padding(horizontal = spacing.xLarge),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.medium),
            ) {
                Text(
                    text = vendorName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = amount,
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = MaterialTheme.typography.labelLarge.fontFamily,
                    color = colorScheme.textMuted,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                StatusDot(
                    type = if (isConfirmed) StatusDotType.Confirmed else StatusDotType.Warning,
                    size = 5.dp,
                )
                Text(
                    text = stringResource(
                        if (isConfirmed) Res.string.document_detail_confirmed
                        else Res.string.document_detail_needs_review
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = statusColor,
                )
            }
        }

        HorizontalDivider(color = effects.railTrackLine)
    }
}

// ---------------------------------------------------------------------------
// Queue pane
// ---------------------------------------------------------------------------

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
    val effects = MaterialTheme.dokusEffects

    // Pagination: load more when scrolled near the end
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

    Column(modifier = Modifier.fillMaxHeight()) {
        DocQueueHeader(
            positionText = positionText,
            onExit = onExit,
        )

        HorizontalDivider(color = effects.railTrackLine)

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            items(items = documents, key = { it.id.toString() }) { item ->
                DocQueueItemRow(
                    vendorName = item.vendorName,
                    date = item.date,
                    amount = item.amount,
                    isConfirmed = item.isConfirmed,
                    isSelected = item.id == selectedDocumentId,
                    onClick = { onSelectDocument(item.id) },
                )
            }
        }

        HorizontalDivider(color = effects.railTrackLine)
        KeyboardNavigationHint()
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview
@Composable
private fun DocumentReviewDesktopSplitPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    val mockId = DocumentId.generate()
    val mockDoc = DocQueueItem(
        id = mockId,
        vendorName = "Acme Corp",
        date = "Feb 15",
        amount = "1,234.56",
        isConfirmed = false,
    )
    val mockDocuments = listOf(
        mockDoc,
        DocQueueItem(
            id = DocumentId.generate(),
            vendorName = "Tech Solutions",
            date = "Feb 14",
            amount = "890.50",
            isConfirmed = true,
        ),
    )
    TestWrapper(parameters) {
        DocumentReviewDesktopSplit(
            documents = mockDocuments,
            selectedDocumentId = mockId,
            selectedDoc = mockDoc,
            hasMore = false,
            isLoadingMore = false,
            onSelectDocument = {},
            onLoadMore = {},
            onExit = {},
            content = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Document content area")
                }
            },
        )
    }
}
