@file:Suppress("LongMethod")

package tech.dokus.features.cashflow.presentation.detail.review

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.features.cashflow.presentation.detail.components.previewReviewContentState
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

@Preview(name = "Duplicate Review Surface", widthDp = 1080, heightDp = 760)
@Composable
private fun DuplicateReviewSurfacePreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        DesktopDuplicateReviewSurface(
            state = previewReviewContentState(
                documentStatus = DocumentStatus.Confirmed,
                showPendingMatchReview = true,
            ),
            contentPadding = PaddingValues(0.dp),
            onIntent = {},
            onSwitchToDetail = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}
