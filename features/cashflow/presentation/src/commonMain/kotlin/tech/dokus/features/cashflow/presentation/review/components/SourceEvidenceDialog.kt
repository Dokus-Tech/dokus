package tech.dokus.features.cashflow.presentation.review.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.window.Dialog
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_close
import tech.dokus.aura.resources.document_source_original_document
import tech.dokus.aura.resources.document_source_received_on
import tech.dokus.aura.resources.document_source_technical_details
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.features.cashflow.presentation.common.utils.formatShortDate
import tech.dokus.features.cashflow.presentation.review.DocumentPreviewState
import tech.dokus.features.cashflow.presentation.review.DocumentReviewState
import tech.dokus.features.cashflow.presentation.review.SourceEvidenceViewerState
import tech.dokus.foundation.aura.components.PIcon
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.extensions.colorized
import tech.dokus.foundation.aura.extensions.localizedUppercase
import tech.dokus.foundation.aura.extensions.viewerHeaderBackgroundColorized
import tech.dokus.foundation.aura.style.textMuted
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

private const val SourceDialogMaxHeightScale = 1.45f

@Composable
internal fun SourceEvidenceDialog(
    contentState: DocumentReviewState.Content,
    viewerState: SourceEvidenceViewerState,
    onClose: () -> Unit,
    onToggleTechnicalDetails: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(onDismissRequest = onClose) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(
                    min = Constraints.DialogSize.maxWidth,
                    max = Constraints.DialogSize.maxWidth * SourceDialogMaxHeightScale,
                ),
            shape = MaterialTheme.shapes.medium,
            color = AlertDialogDefaults.containerColor,
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
            ) {
                SourceDialogHeader(
                    viewerState = viewerState,
                    onClose = onClose,
                    modifier = Modifier.fillMaxWidth(),
                )

                SourceEvidenceBody(
                    contentState = contentState,
                    viewerState = viewerState,
                    onRetry = onRetry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = Constraints.Spacing.medium),
                )

                if (viewerState.sourceType == DocumentSource.Peppol) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onToggleTechnicalDetails)
                            .padding(
                                horizontal = Constraints.Spacing.medium,
                                vertical = Constraints.Spacing.small,
                            ),
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
private fun SourceDialogHeader(
    viewerState: SourceEvidenceViewerState,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(viewerState.sourceType.viewerHeaderBackgroundColorized)
            .padding(
                horizontal = Constraints.Spacing.medium,
                vertical = Constraints.Spacing.small,
            ),
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
                modifier = Modifier.weight(1f),
            ) {
                SourceTypeChip(type = viewerState.sourceType)
                Text(
                    text = stringResource(Res.string.document_source_original_document),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            IconButton(onClick = onClose) {
                PIcon(
                    icon = Icons.Outlined.Close,
                    description = stringResource(Res.string.action_close),
                    tint = MaterialTheme.colorScheme.textMuted,
                )
            }
        }

        val receivedLabel = viewerState.sourceReceivedAt?.let { receivedAt ->
            stringResource(
                Res.string.document_source_received_on,
                formatShortDate(receivedAt.date),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = viewerState.sourceName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textMuted,
            )
            receivedLabel?.let { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textMuted,
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun SourceTypeChip(type: DocumentSource) {
    Surface(
        color = type.viewerHeaderBackgroundColorized,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = type.localizedUppercase,
            modifier = Modifier.padding(
                horizontal = Constraints.Spacing.small,
                vertical = Constraints.Spacing.xxSmall,
            ),
            style = MaterialTheme.typography.labelSmall,
            color = type.colorized,
        )
    }
}

@Preview
@Composable
private fun SourceEvidenceDialogPdfPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        SourceEvidenceDialog(
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
            onClose = {},
            onToggleTechnicalDetails = {},
            onRetry = {},
        )
    }
}

@Preview
@Composable
private fun SourceEvidenceDialogPeppolCollapsedPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        SourceEvidenceDialog(
            contentState = previewReviewContentState(),
            viewerState = previewSourceEvidenceViewerState(
                sourceType = DocumentSource.Peppol,
                previewState = DocumentPreviewState.NotPdf,
                isTechnicalDetailsExpanded = false,
            ),
            onClose = {},
            onToggleTechnicalDetails = {},
            onRetry = {},
        )
    }
}

@Preview
@Composable
private fun SourceEvidenceDialogPeppolTechnicalPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        SourceEvidenceDialog(
            contentState = previewReviewContentState(),
            viewerState = previewSourceEvidenceViewerState(
                sourceType = DocumentSource.Peppol,
                previewState = DocumentPreviewState.NotPdf,
                isTechnicalDetailsExpanded = true,
                rawContent = "<Invoice>\\n  <ID>INV-8847291</ID>\\n</Invoice>",
            ),
            onClose = {},
            onToggleTechnicalDetails = {},
            onRetry = {},
        )
    }
}
