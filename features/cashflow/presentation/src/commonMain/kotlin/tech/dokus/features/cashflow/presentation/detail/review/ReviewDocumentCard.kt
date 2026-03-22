package tech.dokus.features.cashflow.presentation.detail.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.review_surface_click_to_zoom
import tech.dokus.features.cashflow.presentation.detail.DocumentPreviewState
import tech.dokus.features.cashflow.presentation.detail.PdfThumbnail
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.textMuted

private val CardWidth = 180.dp
private val ThumbnailShape = RoundedCornerShape(6.dp)

/**
 * Document thumbnail in the review surface.
 *
 * Shows the real PDF first page in a shadowed container.
 * No border, no text overlay — vendor/amount info is on the right side.
 * Clicking opens PDF zoom.
 */
@Composable
internal fun ReviewDocumentCard(
    previewState: DocumentPreviewState,
    onZoomClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.width(CardWidth),
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

        Surface(
            shape = ThumbnailShape,
            shadowElevation = 4.dp,
            tonalElevation = 0.dp,
        ) {
            PdfThumbnail(
                firstPageUrl = firstPageUrl,
                totalPages = totalPages,
                isLoading = isLoading,
                onClick = onZoomClick,
            )
        }

        // "Click or Z to zoom" hint
        Text(
            text = stringResource(Res.string.review_surface_click_to_zoom),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.textMuted,
        )
    }
}
