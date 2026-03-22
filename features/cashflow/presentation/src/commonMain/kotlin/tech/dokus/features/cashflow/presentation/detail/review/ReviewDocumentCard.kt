package tech.dokus.features.cashflow.presentation.detail.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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

/**
 * Small document thumbnail card shown in the review surface.
 *
 * Displays the PDF first page (via PdfThumbnail) with vendor name,
 * source type, and amount below. Clicking opens PDF zoom.
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
        // PDF thumbnail
        DokusCardSurface(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Constraints.Spacing.medium),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
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

                Text(
                    text = sourceLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.textMuted,
                    textAlign = TextAlign.Center,
                )

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
