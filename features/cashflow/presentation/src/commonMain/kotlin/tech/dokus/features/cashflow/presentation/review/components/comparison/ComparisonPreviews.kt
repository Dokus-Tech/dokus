package tech.dokus.features.cashflow.presentation.review.components.comparison

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.domain.enums.ReviewReason
import tech.dokus.features.cashflow.presentation.review.DocumentPreviewState
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

// ═══════════════════════════════════════════════════════════════════
// FULL COMPARISON PANE
// ═══════════════════════════════════════════════════════════════════

@Preview(name = "Comparison - MaterialConflict", widthDp = 1440, heightDp = 900)
@Composable
private fun ComparisonMaterialConflictPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        DocumentComparisonPane(
            existingPreviewState = DocumentPreviewState.NoPreview,
            incomingPreviewState = DocumentPreviewState.NoPreview,
            reasonType = ReviewReason.MaterialConflict,
            onSameDocument = {},
            onDifferentDocument = {},
            isResolving = false,
            onLoadMore = {},
        )
    }
}

@Preview(name = "Comparison - FuzzyMatch", widthDp = 1440, heightDp = 900)
@Composable
private fun ComparisonFuzzyMatchPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        DocumentComparisonPane(
            existingPreviewState = DocumentPreviewState.Loading,
            incomingPreviewState = DocumentPreviewState.Loading,
            reasonType = ReviewReason.FuzzyCandidate,
            onSameDocument = {},
            onDifferentDocument = {},
            isResolving = false,
            onLoadMore = {},
        )
    }
}

@Preview(name = "Comparison - Resolving", widthDp = 1440, heightDp = 900)
@Composable
private fun ComparisonResolvingPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        DocumentComparisonPane(
            existingPreviewState = DocumentPreviewState.NoPreview,
            incomingPreviewState = DocumentPreviewState.NoPreview,
            reasonType = ReviewReason.MaterialConflict,
            onSameDocument = {},
            onDifferentDocument = {},
            isResolving = true,
            onLoadMore = {},
        )
    }
}

@Preview(name = "Comparison - NoIncoming", widthDp = 1440, heightDp = 900)
@Composable
private fun ComparisonNoIncomingPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        DocumentComparisonPane(
            existingPreviewState = DocumentPreviewState.NoPreview,
            incomingPreviewState = null,
            reasonType = ReviewReason.FuzzyCandidate,
            onSameDocument = {},
            onDifferentDocument = {},
            isResolving = false,
            onLoadMore = {},
        )
    }
}

// ═══════════════════════════════════════════════════════════════════
// COMPONENT PREVIEWS
// ═══════════════════════════════════════════════════════════════════

@Preview
@Composable
private fun ReasonBannerMaterialConflictPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        ComparisonReasonBanner(reasonType = ReviewReason.MaterialConflict)
    }
}

@Preview
@Composable
private fun ReasonBannerFuzzyPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        ComparisonReasonBanner(reasonType = ReviewReason.FuzzyCandidate)
    }
}

@Preview
@Composable
private fun ComparisonActionBarPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        ComparisonActionBar(
            onSameDocument = {},
            onDifferentDocument = {},
            isResolving = false,
        )
    }
}

@Preview
@Composable
private fun ComparisonActionBarResolvingPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        ComparisonActionBar(
            onSameDocument = {},
            onDifferentDocument = {},
            isResolving = true,
        )
    }
}
