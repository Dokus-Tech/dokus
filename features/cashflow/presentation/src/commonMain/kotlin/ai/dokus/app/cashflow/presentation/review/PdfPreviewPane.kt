package ai.dokus.app.cashflow.presentation.review

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.SubcomposeAsyncImage
import tech.dokus.domain.model.DocumentPagePreviewDto

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
    modifier: Modifier = Modifier
) {
    val imageLoader = rememberAuthenticatedImageLoader()

    when (state) {
        is DocumentPreviewState.Loading -> {
            LoadingPreview(modifier)
        }
        is DocumentPreviewState.Error -> {
            ErrorPreview(
                message = state.message,
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
                modifier = modifier
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
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
        Button(
            onClick = onRetry,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text("Retry")
        }
    }
}

@Composable
private fun ReadyPreview(
    pages: List<DocumentPagePreviewDto>,
    hasMore: Boolean,
    totalPages: Int,
    renderedPages: Int,
    onLoadMore: (Int) -> Unit,
    imageLoader: ImageLoader,
    selectedFieldPath: String?,
    modifier: Modifier = Modifier
) {
    var zoomLevel by remember { mutableFloatStateOf(1f) }
    val scrollState = rememberLazyListState()

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(pages) { index, page ->
                PdfPageImage(
                    page = page,
                    imageLoader = imageLoader,
                    zoomLevel = zoomLevel,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (hasMore) {
                item {
                    LoadMoreButton(
                        currentCount = renderedPages,
                        totalCount = totalPages,
                        onClick = { onLoadMore(renderedPages + 10) }
                    )
                }
            }
        }

        // Zoom controls overlay
        ZoomControls(
            zoomLevel = zoomLevel,
            onZoomChange = { zoomLevel = it },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }
}

@Composable
private fun PdfPageImage(
    page: DocumentPagePreviewDto,
    imageLoader: ImageLoader,
    zoomLevel: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .graphicsLayer(
                scaleX = zoomLevel,
                scaleY = zoomLevel
            )
            .clip(RoundedCornerShape(8.dp))
    ) {
        SubcomposeAsyncImage(
            model = page.imageUrl,
            contentDescription = "Page ${page.page}",
            imageLoader = imageLoader,
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.707f) // A4 aspect ratio
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            },
            error = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.707f)
                        .background(MaterialTheme.colorScheme.errorContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "Page ${page.page} failed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            },
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth()
        )
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
            .padding(vertical = 8.dp)
    ) {
        Text("Load more (showing $currentCount of $totalCount)")
    }
}

@Composable
private fun NoPreviewPlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Description,
            contentDescription = null,
            modifier = Modifier.padding(bottom = 16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Document Preview",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Preview will highlight relevant sections when you select a field",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
