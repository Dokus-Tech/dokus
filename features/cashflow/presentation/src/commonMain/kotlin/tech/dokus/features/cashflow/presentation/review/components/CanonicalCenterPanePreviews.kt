package tech.dokus.features.cashflow.presentation.review.components

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.features.cashflow.presentation.review.DocumentPreviewState
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

@Preview(name = "Center Pane - Document", widthDp = 1080, heightDp = 760)
@Composable
private fun CanonicalCenterPaneDocumentPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        CanonicalCenterPane(
            state = previewReviewContentState(entryStatus = CashflowEntryStatus.Open),
            onIntent = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview(name = "Center Pane - Source", widthDp = 1080, heightDp = 760)
@Composable
private fun CanonicalCenterPaneSourcePreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        val baseState = previewReviewContentState(entryStatus = CashflowEntryStatus.Open)
        val sources = baseState.documentRecord?.sources.orEmpty()
        val peppolSourceId = sources
            .firstOrNull { it.sourceChannel == DocumentSource.Peppol }
            ?.id
            ?: sources.first().id
        val sourceViewerState = previewSourceEvidenceViewerState(
            sourceType = DocumentSource.Peppol,
            previewState = DocumentPreviewState.NotPdf,
            isTechnicalDetailsExpanded = true,
        ).copy(sourceId = peppolSourceId)

        CanonicalCenterPane(
            state = baseState.copy(sourceViewerState = sourceViewerState),
            onIntent = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}
