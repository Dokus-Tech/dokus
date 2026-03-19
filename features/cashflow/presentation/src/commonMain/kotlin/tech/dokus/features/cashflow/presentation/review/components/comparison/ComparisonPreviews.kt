package tech.dokus.features.cashflow.presentation.review.components.comparison

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import tech.dokus.domain.enums.ReviewReason
import tech.dokus.features.cashflow.presentation.review.DocumentPreviewState
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

private val PreviewSize = Modifier.width(1200.dp).height(800.dp)

// ═══════════════════════════════════════════════════════════════════
// FULL COMPARISON PANE
// ═══════════════════════════════════════════════════════════════════

@Preview
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
            modifier = PreviewSize,
        )
    }
}

@Preview
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
            modifier = PreviewSize,
        )
    }
}

@Preview
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
            modifier = PreviewSize,
        )
    }
}

@Preview
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
            modifier = PreviewSize,
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
