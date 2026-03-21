package tech.dokus.features.cashflow.presentation.detail.components.comparison

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
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.comparison_existing_label
import tech.dokus.aura.resources.comparison_incoming_label
import tech.dokus.aura.resources.comparison_title
import tech.dokus.domain.enums.ReviewReason
import tech.dokus.features.cashflow.presentation.detail.DocumentPreviewState
import tech.dokus.features.cashflow.presentation.detail.PdfPreviewPane
import tech.dokus.foundation.aura.constrains.Constraints

/**
 * Side-by-side PDF comparison for document match review.
 * Shows two PDF previews with EXISTING/INCOMING labels and action buttons.
 */
@Composable
internal fun DocumentComparisonPane(
    existingPreviewState: DocumentPreviewState,
    incomingPreviewState: DocumentPreviewState?,
    reasonType: ReviewReason,
    onSameDocument: () -> Unit,
    onDifferentDocument: () -> Unit,
    isResolving: Boolean,
    onLoadMore: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = Constraints.Spacing.large,
                    vertical = Constraints.Spacing.medium,
                ),
            verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
        ) {
            Text(
                text = stringResource(Res.string.comparison_title),
                style = MaterialTheme.typography.titleMedium,
            )
            ComparisonReasonBanner(reasonType = reasonType)
        }

        HorizontalDivider()

        // Side-by-side PDFs
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = Constraints.Spacing.small),
            horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
        ) {
            // Existing document PDF
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xxSmall),
            ) {
                SectionLabel(
                    text = stringResource(Res.string.comparison_existing_label),
                    modifier = Modifier.padding(
                        horizontal = Constraints.Spacing.small,
                        vertical = Constraints.Spacing.xxSmall,
                    ),
                )
                PdfPreviewPane(
                    state = existingPreviewState,
                    selectedFieldPath = null,
                    onLoadMore = onLoadMore,
                    modifier = Modifier.weight(1f),
                )
            }

            // Incoming document PDF
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xxSmall),
            ) {
                SectionLabel(
                    text = stringResource(Res.string.comparison_incoming_label),
                    modifier = Modifier.padding(
                        horizontal = Constraints.Spacing.small,
                        vertical = Constraints.Spacing.xxSmall,
                    ),
                )
                PdfPreviewPane(
                    state = incomingPreviewState ?: DocumentPreviewState.NoPreview,
                    selectedFieldPath = null,
                    onLoadMore = {},
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Action bar
        HorizontalDivider()
        ComparisonActionBar(
            onSameDocument = onSameDocument,
            onDifferentDocument = onDifferentDocument,
            isResolving = isResolving,
        )
    }
}

@Composable
private fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier,
    )
}
