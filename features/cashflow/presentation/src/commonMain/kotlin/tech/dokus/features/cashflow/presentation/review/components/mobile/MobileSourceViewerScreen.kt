package tech.dokus.features.cashflow.presentation.review.components.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
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
import tech.dokus.foundation.aura.constrains.Constraints
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
        MobileSourceViewerTopBar(
            viewerState = viewerState,
            onBackClick = onBack,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Constraints.Spacing.medium)
                .padding(top = Constraints.Spacing.small, bottom = Constraints.Spacing.small),
            verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
        ) {
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

            DokusCardSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
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
