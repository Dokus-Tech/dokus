package tech.dokus.features.cashflow.presentation.review.components.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_back
import tech.dokus.aura.resources.document_source_original_document
import tech.dokus.aura.resources.document_source_technical_details
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.features.cashflow.presentation.review.DocumentPreviewState
import tech.dokus.features.cashflow.presentation.review.DocumentReviewState
import tech.dokus.features.cashflow.presentation.review.SourceEvidenceViewerState
import tech.dokus.features.cashflow.presentation.review.components.SourceEvidenceBody
import tech.dokus.features.cashflow.presentation.review.components.previewReviewContentState
import tech.dokus.features.cashflow.presentation.review.components.previewSourceEvidenceViewerState
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.PIcon
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.extensions.colorized
import tech.dokus.foundation.aura.extensions.localizedUppercase
import tech.dokus.foundation.aura.extensions.sourceViewerSubtitleLocalized
import tech.dokus.foundation.aura.style.textMuted
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

@Composable
internal fun MobileSourceViewerScreen(
    contentState: DocumentReviewState.Content,
    viewerState: SourceEvidenceViewerState,
    onBack: () -> Unit,
    onToggleTechnicalDetails: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        MobileSourceViewerHeader(
            viewerState = viewerState,
            onBack = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = Constraints.Spacing.medium,
                    vertical = Constraints.Spacing.small,
                ),
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        DokusCardSurface(
            modifier = Modifier
                .fillMaxSize()
                .padding(Constraints.Spacing.medium),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Constraints.Spacing.medium),
                verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
            ) {
                SourceEvidenceBody(
                    contentState = contentState,
                    viewerState = viewerState,
                    onRetry = onRetry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )

                if (viewerState.sourceType == DocumentSource.Peppol) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onToggleTechnicalDetails)
                            .padding(vertical = Constraints.Spacing.xSmall),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
                    ) {
                        Text(
                            text = if (viewerState.isTechnicalDetailsExpanded) "\u2304" else "\u203A",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.textMuted,
                        )
                        Text(
                            text = stringResource(Res.string.document_source_technical_details),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.textMuted,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MobileSourceViewerHeader(
    viewerState: SourceEvidenceViewerState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = onBack),
            ) {
                PIcon(
                    icon = FeatherIcons.ArrowLeft,
                    description = stringResource(Res.string.action_back),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(Res.string.action_back),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = Constraints.Spacing.xSmall),
                )
            }

            Surface(
                color = viewerState.sourceType.colorized.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    text = viewerState.sourceType.localizedUppercase,
                    style = MaterialTheme.typography.labelMedium,
                    color = viewerState.sourceType.colorized,
                    modifier = Modifier.padding(
                        horizontal = Constraints.Spacing.small,
                        vertical = Constraints.Spacing.xSmall,
                    ),
                )
            }
        }

        Text(
            text = stringResource(Res.string.document_source_original_document),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = viewerState.sourceType.sourceViewerSubtitleLocalized,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.textMuted,
        )
    }
}

@Preview
@Composable
private fun MobileSourceViewerPdfPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        MobileSourceViewerScreen(
            contentState = previewReviewContentState(),
            viewerState = previewSourceEvidenceViewerState(
                sourceType = DocumentSource.Upload,
                previewState = DocumentPreviewState.Ready(
                    pages = emptyList(),
                    totalPages = 1,
                    renderedPages = 0,
                    dpi = 180,
                    hasMore = false,
                ),
            ),
            onBack = {},
            onToggleTechnicalDetails = {},
            onRetry = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview
@Composable
private fun MobileSourceViewerPeppolCollapsedPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        MobileSourceViewerScreen(
            contentState = previewReviewContentState(),
            viewerState = previewSourceEvidenceViewerState(
                sourceType = DocumentSource.Peppol,
                previewState = DocumentPreviewState.NotPdf,
                isTechnicalDetailsExpanded = false,
            ),
            onBack = {},
            onToggleTechnicalDetails = {},
            onRetry = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview
@Composable
private fun MobileSourceViewerPeppolTechnicalPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        MobileSourceViewerScreen(
            contentState = previewReviewContentState(),
            viewerState = previewSourceEvidenceViewerState(
                sourceType = DocumentSource.Peppol,
                previewState = DocumentPreviewState.NotPdf,
                isTechnicalDetailsExpanded = true,
                rawContent = "<Invoice>\\n  <ID>INV-8847291</ID>\\n</Invoice>",
            ),
            onBack = {},
            onToggleTechnicalDetails = {},
            onRetry = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}
