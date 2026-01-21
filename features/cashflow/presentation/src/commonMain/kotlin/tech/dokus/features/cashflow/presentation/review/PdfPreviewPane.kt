@file:Suppress("UnusedParameter") // reserved params

package tech.dokus.features.cashflow.presentation.review

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.SubcomposeAsyncImage
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_document_preview_title
import tech.dokus.aura.resources.cashflow_preview_highlight_hint
import tech.dokus.aura.resources.cashflow_preview_load_more
import tech.dokus.aura.resources.cashflow_preview_page_failed
import tech.dokus.aura.resources.cashflow_preview_page_label
import tech.dokus.aura.resources.state_retry
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.model.DocumentPagePreviewDto
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.extensions.localized

// Layout dimensions
private val ErrorPadding = 32.dp
private val RetryButtonTopPadding = 16.dp
private val RetryIconEndPadding = 8.dp
private val PreviewMaxWidth = 920.dp
private val PreviewContentPadding = 12.dp
private val PageSpacing = 12.dp
private val LoadMoreButtonVerticalPadding = 8.dp
private val PlaceholderPadding = 32.dp
private val PlaceholderIconBottomPadding = 16.dp
private val HintTopPadding = 8.dp

// Page constants
private const val A4AspectRatio = 0.707f
private const val HintTextAlpha = 0.7f
private const val PageBatchSize = 10

/**
 * PDF preview pane that displays rendered page images.
 *
 * Features:
 * - Lazy loading of pages using LazyColumn
 * - Authenticated image loading via Coil
 * - Zoom controls (in/out/fit)
 * - Loading/error/empty states
 * - "Load more" for paginated loading
 *
 * @param state The preview state (loading, ready, error, etc.)
 * @param selectedFieldPath Optional field path for future highlight overlay
 * @param onLoadMore Callback when user requests more pages
 * @param modifier Modifier for the container
 */
@Composable
fun PdfPreviewPane(
    state: DocumentPreviewState,
    selectedFieldPath: String?,
    onLoadMore: (Int) -> Unit,
    modifier: Modifier = Modifier,
    showScanAnimation: Boolean = false
) {
    val imageLoader = rememberAuthenticatedImageLoader()

    when (state) {
        is DocumentPreviewState.Loading -> {
            LoadingPreview(modifier)
        }
        is DocumentPreviewState.Error -> {
            ErrorPreview(
                exception = state.exception,
                onRetry = state.retry,
                modifier = modifier
            )
        }
        is DocumentPreviewState.Ready -> {
            ReadyPreview(
                pages = state.pages,
                hasMore = state.hasMore,
                totalPages = state.totalPages,
                renderedPages = state.renderedPages,
                onLoadMore = onLoadMore,
                imageLoader = imageLoader,
                selectedFieldPath = selectedFieldPath,
                modifier = modifier,
                showScanAnimation = showScanAnimation
            )
        }
        DocumentPreviewState.NotPdf,
        DocumentPreviewState.NoPreview -> {
            NoPreviewPlaceholder(modifier)
        }
    }
}

@Composable
private fun LoadingPreview(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorPreview(
    exception: DokusException,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(ErrorPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = exception.localized,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
        Button(
            onClick = onRetry,
            modifier = Modifier.padding(top = RetryButtonTopPadding)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.padding(end = RetryIconEndPadding)
            )
            Text(stringResource(Res.string.state_retry))
        }
    }
}

/**
 * Desktop-optimized preview with fit-width pages and max 920dp constraint.
 * Pages render as paper cards with shadow for a document-like appearance.
 */
@Composable
private fun ReadyPreview(
    pages: List<DocumentPagePreviewDto>,
    hasMore: Boolean,
    totalPages: Int,
    renderedPages: Int,
    onLoadMore: (Int) -> Unit,
    imageLoader: ImageLoader,
    selectedFieldPath: String?,
    modifier: Modifier = Modifier,
    showScanAnimation: Boolean = false
) {
    val scrollState = rememberLazyListState()

    // Container with subtle background for paper contrast
    DokusCardSurface(
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                state = scrollState,
                modifier = Modifier
                    .widthIn(max = PreviewMaxWidth)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(PreviewContentPadding),
                verticalArrangement = Arrangement.spacedBy(PageSpacing),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                itemsIndexed(pages) { index, page ->
                    PdfPageImage(
                        page = page,
                        imageLoader = imageLoader,
                        modifier = Modifier.fillMaxWidth(),
                        showScanAnimation = showScanAnimation
                    )
                }

                if (hasMore) {
                    item {
                        LoadMoreButton(
                            currentCount = renderedPages,
                            totalCount = totalPages,
                            onClick = { onLoadMore(renderedPages + PageBatchSize) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual PDF page rendered as a paper card with shadow.
 * Uses fit-width scaling to maximize readability.
 */
@Composable
private fun PdfPageImage(
    page: DocumentPagePreviewDto,
    imageLoader: ImageLoader,
    modifier: Modifier = Modifier,
    showScanAnimation: Boolean = false
) {
    DokusCardSurface(
        modifier = modifier,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            SubcomposeAsyncImage(
                model = page.imageUrl,
                contentDescription = stringResource(Res.string.cashflow_preview_page_label, page.page),
                imageLoader = imageLoader,
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(A4AspectRatio)
                            .background(Color.White),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                },
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(A4AspectRatio)
                            .background(MaterialTheme.colorScheme.errorContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Text(
                                text = stringResource(Res.string.cashflow_preview_page_failed, page.page),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                },
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth(),
            )

            if (showScanAnimation) {
                ScanningLineOverlay(modifier = Modifier.matchParentSize())
            }
        }
    }
}

@Composable
private fun LoadMoreButton(
    currentCount: Int,
    totalCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = LoadMoreButtonVerticalPadding)
    ) {
        Text(stringResource(Res.string.cashflow_preview_load_more, currentCount, totalCount))
    }
}

@Composable
private fun NoPreviewPlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(PlaceholderPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Description,
            contentDescription = null,
            modifier = Modifier.padding(bottom = PlaceholderIconBottomPadding),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(Res.string.cashflow_document_preview_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(Res.string.cashflow_preview_highlight_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = HintTextAlpha),
            modifier = Modifier.padding(top = HintTopPadding)
        )
    }
}
