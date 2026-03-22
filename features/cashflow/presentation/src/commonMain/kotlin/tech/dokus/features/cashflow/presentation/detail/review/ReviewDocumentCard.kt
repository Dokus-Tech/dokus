package tech.dokus.features.cashflow.presentation.detail.review

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.review_surface_click_to_zoom
import tech.dokus.features.cashflow.presentation.detail.DocumentPreviewState
import tech.dokus.features.cashflow.presentation.detail.PdfThumbnail
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.textMuted

private val CardWidth = 180.dp
private val CardMinHeight = 240.dp

/**
 * Document thumbnail card shown in the review surface.
 *
 * Sized to match the content area height. Displays a PDF first page
 * thumbnail with vendor name, source type, and amount inside a card.
 * Clicking opens PDF zoom.
 */
@Composable
internal fun ReviewDocumentCard(
    vendorName: String,
    sourceLabel: String,
    totalAmount: String,
    previewState: DocumentPreviewState,
    onZoomClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.width(CardWidth),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
    ) {
        DokusCardSurface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = CardMinHeight)
                .clickable(onClick = onZoomClick),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Constraints.Spacing.large),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                val (firstPageUrl, totalPages, isLoading) = when (previewState) {
                    is DocumentPreviewState.Ready -> Triple(
                        previewState.pages.firstOrNull()?.imageUrl,
                        previewState.totalPages,
                        false,
                    )
                    is DocumentPreviewState.Loading -> Triple(null, 0, true)
                    is DocumentPreviewState.Error,
                    is DocumentPreviewState.NotPdf,
                    is DocumentPreviewState.NoPreview -> Triple(null, 0, false)
                }

                PdfThumbnail(
                    firstPageUrl = firstPageUrl,
                    totalPages = totalPages,
                    isLoading = isLoading,
                    onClick = onZoomClick,
                )

                Spacer(modifier = Modifier.height(Constraints.Spacing.large))

                Text(
                    text = vendorName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(Constraints.Spacing.xxSmall))

                Text(
                    text = sourceLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.textMuted,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(Constraints.Spacing.small))

                Text(
                    text = totalAmount,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
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
