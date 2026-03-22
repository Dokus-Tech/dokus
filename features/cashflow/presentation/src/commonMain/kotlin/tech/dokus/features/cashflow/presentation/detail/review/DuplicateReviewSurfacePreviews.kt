@file:Suppress("LongMethod")

package tech.dokus.features.cashflow.presentation.detail.review

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import tech.dokus.domain.Money
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.ReviewReason
import tech.dokus.features.cashflow.presentation.detail.DocumentPreviewState
import tech.dokus.features.cashflow.presentation.detail.DuplicateDiff
import tech.dokus.features.cashflow.presentation.detail.DuplicateReviewState
import tech.dokus.features.cashflow.presentation.detail.models.DocumentUiData
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

private val previewExistingUiData = DocumentUiData.Invoice(
    direction = DocumentDirection.Inbound,
    invoiceNumber = "384421507",
    issueDate = "2025-01-09",
    dueDate = "2025-01-08",
    subtotalAmount = Money.from("57.01"),
    vatAmount = Money.from("11.97"),
    totalAmount = Money.from("68.98"),
    currencySign = "\u20AC",
    lineItems = emptyList(),
    notes = null,
    iban = null,
    primaryDescription = "Coolblue Belgi\u00EB NV",
)

private val previewIncomingUiData = DocumentUiData.Invoice(
    direction = DocumentDirection.Inbound,
    invoiceNumber = "INV-2026-0005",
    issueDate = "2026-02-14",
    dueDate = "2026-02-14",
    subtotalAmount = Money.from("238.84"),
    vatAmount = Money.from("50.16"),
    totalAmount = Money.from("289.00"),
    currencySign = "\u20AC",
    lineItems = emptyList(),
    notes = null,
    iban = null,
    primaryDescription = "COOLBLUE BELGI\u00CB",
)

private val previewDiffs = listOf(
    DuplicateDiff("total", "\u20AC68.98", "\u20AC289.00"),
    DuplicateDiff("invoiceNo", "384421507", "INV-2026-0005"),
    DuplicateDiff("issueDate", "2025-01-09", "2026-02-14"),
)

@Preview(name = "Duplicate Review - With Diffs", widthDp = 1080, heightDp = 760)
@Composable
private fun DuplicateReviewWithDiffsPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        DesktopDuplicateReviewSurface(
            state = DuplicateReviewState(
                existingUiData = previewExistingUiData,
                incomingUiData = previewIncomingUiData,
                reasonType = ReviewReason.MaterialConflict,
                diffs = previewDiffs,
                existingPreview = DocumentPreviewState.NoPreview,
                incomingPreview = DocumentPreviewState.NoPreview,
            ),
            contentPadding = PaddingValues(0.dp),
            onResolveSame = {},
            onResolveDifferent = {},
            onSwitchToDetail = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview(name = "Duplicate Review - No Diffs", widthDp = 1080, heightDp = 760)
@Composable
private fun DuplicateReviewNoDiffsPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        DesktopDuplicateReviewSurface(
            state = DuplicateReviewState(
                existingUiData = previewExistingUiData,
                incomingUiData = previewExistingUiData,
                reasonType = ReviewReason.FuzzyCandidate,
                diffs = emptyList(),
                existingPreview = DocumentPreviewState.NoPreview,
                incomingPreview = DocumentPreviewState.NoPreview,
            ),
            contentPadding = PaddingValues(0.dp),
            onResolveSame = {},
            onResolveDifferent = {},
            onSwitchToDetail = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview(name = "Duplicate Review - Loading")
@Composable
private fun DuplicateReviewLoadingPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        DesktopDuplicateReviewSurface(
            state = DuplicateReviewState(),
            contentPadding = PaddingValues(0.dp),
            onResolveSame = {},
            onResolveDifferent = {},
            onSwitchToDetail = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}
