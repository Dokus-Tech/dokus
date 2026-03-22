package tech.dokus.features.cashflow.presentation.detail.review

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.composables.icons.lucide.FileText
import com.composables.icons.lucide.Lucide
import androidx.compose.material3.Icon
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_document_preview_title
import tech.dokus.aura.resources.review_surface_click_to_zoom
import tech.dokus.features.cashflow.presentation.detail.DocumentPreviewState
import tech.dokus.foundation.app.network.rememberAuthenticatedImageLoader
import tech.dokus.foundation.aura.components.common.DokusLoader
import tech.dokus.foundation.aura.components.common.DokusLoaderSize
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.textMuted

private val CardWidth = 180.dp
private val ThumbnailShape = RoundedCornerShape(6.dp)
private const val A4_ASPECT_RATIO = 0.707f

/**
 * Document thumbnail in the review surface.
 *
 * Shows the real PDF first page at full card size (180dp wide, A4 aspect ratio).
 * No border, no text overlay — vendor/amount info is on the right side.
 * Clicking opens PDF zoom.
 */
@Composable
internal fun ReviewDocumentCard(
    previewState: DocumentPreviewState,
    onZoomClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val imageLoader = rememberAuthenticatedImageLoader()

    Column(
        modifier = modifier.width(CardWidth),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(A4_ASPECT_RATIO)
                .clickable(onClick = onZoomClick),
            shape = ThumbnailShape,
            shadowElevation = 4.dp,
            tonalElevation = 0.dp,
        ) {
            when (previewState) {
                is DocumentPreviewState.Ready -> {
                    val firstPageUrl = previewState.pages.firstOrNull()?.imageUrl
                    if (firstPageUrl != null) {
                        SubcomposeAsyncImage(
                            model = firstPageUrl,
                            contentDescription = stringResource(Res.string.cashflow_document_preview_title),
                            imageLoader = imageLoader,
                            loading = {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    DokusLoader(size = DokusLoaderSize.Small)
                                }
                            },
                            error = { PlaceholderContent() },
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(ThumbnailShape),
                        )
                    } else {
                        PlaceholderContent()
                    }

                    // Page count badge
                    if (previewState.totalPages > 0) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(Constraints.Spacing.small)
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
                                        shape = MaterialTheme.shapes.extraSmall,
                                    )
                                    .padding(
                                        horizontal = Constraints.Spacing.xSmall,
                                        vertical = Constraints.Spacing.xxSmall,
                                    ),
                            ) {
                                Text(
                                    text = previewState.totalPages.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
                is DocumentPreviewState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        DokusLoader(size = DokusLoaderSize.Small)
                    }
                }
                is DocumentPreviewState.Error,
                is DocumentPreviewState.NotPdf,
                is DocumentPreviewState.NoPreview -> {
                    PlaceholderContent()
                }
            }
        }

        // "Click or Z to zoom" hint
        Text(
            text = stringResource(Res.string.review_surface_click_to_zoom),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.textMuted,
        )
    }
}

@Composable
private fun PlaceholderContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Lucide.FileText,
            contentDescription = null,
            modifier = Modifier.size(Constraints.AvatarSize.medium),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
    }
}
