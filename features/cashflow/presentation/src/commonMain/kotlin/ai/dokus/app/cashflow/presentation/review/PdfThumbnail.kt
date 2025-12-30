package ai.dokus.app.cashflow.presentation.review

import ai.dokus.app.resources.generated.Res
import ai.dokus.foundation.design.constrains.Constrains
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import org.jetbrains.compose.resources.stringResource

/**
 * Small clickable thumbnail preview for mobile Document Review.
 * Shows first page of PDF with page count badge.
 *
 * Dimensions: 80dp x 113dp (A4 aspect ratio)
 *
 * @param firstPageUrl URL of first page preview image
 * @param totalPages Total number of pages in document
 * @param isLoading Whether preview is still loading
 * @param onClick Callback when thumbnail is tapped (opens full preview)
 */
@Composable
fun PdfThumbnail(
    firstPageUrl: String?,
    totalPages: Int,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val imageLoader = rememberAuthenticatedImageLoader()

    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .width(80.dp)
            .height(113.dp),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
        ),
    ) {
        Box(
            modifier = Modifier.size(80.dp, 113.dp),
            contentAlignment = Alignment.Center,
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                    )
                }
                firstPageUrl != null -> {
                    SubcomposeAsyncImage(
                        model = firstPageUrl,
                        contentDescription = stringResource(Res.string.cashflow_document_preview_title),
                        imageLoader = imageLoader,
                        loading = {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                            )
                        },
                        error = {
                            PlaceholderIcon()
                        },
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(80.dp, 113.dp)
                            .clip(RoundedCornerShape(4.dp)),
                    )
                }
                else -> {
                    PlaceholderIcon()
                }
            }

            // Page count badge
            if (totalPages > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(4.dp),
                        )
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = totalPages.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaceholderIcon() {
    Icon(
        imageVector = Icons.Default.Description,
        contentDescription = null,
        modifier = Modifier.size(32.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
    )
}

/**
 * Row component that displays PDF thumbnail with label and page count.
 * Used in mobile layout to replace collapsible preview section.
 */
@Composable
fun PdfPreviewRow(
    previewState: DocumentPreviewState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (firstPageUrl, totalPages, isLoading) = when (previewState) {
        is DocumentPreviewState.Loading -> Triple(null, 0, true)
        is DocumentPreviewState.Ready -> {
            Triple(
                previewState.pages.firstOrNull()?.imageUrl,
                previewState.totalPages,
                false,
            )
        }
        else -> Triple(null, 0, false)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(Constrains.Spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Constrains.Spacing.medium),
    ) {
        PdfThumbnail(
            firstPageUrl = firstPageUrl,
            totalPages = totalPages,
            isLoading = isLoading,
            onClick = onClick,
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.cashflow_document_preview_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = when {
                    isLoading -> stringResource(Res.string.state_loading)
                    totalPages == 1 -> stringResource(Res.string.cashflow_page_single, totalPages)
                    totalPages > 1 -> stringResource(Res.string.cashflow_page_plural, totalPages)
                    else -> stringResource(Res.string.cashflow_no_preview)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Chevron indicator
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(Constrains.IconSize.medium),
        )
    }
}
