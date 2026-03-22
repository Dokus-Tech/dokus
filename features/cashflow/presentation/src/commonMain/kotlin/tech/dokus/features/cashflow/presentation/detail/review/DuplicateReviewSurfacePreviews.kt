@file:Suppress("LongMethod")

package tech.dokus.features.cashflow.presentation.detail.review

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.domain.Money
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.ReviewReason
import tech.dokus.domain.model.DocDto
import tech.dokus.domain.model.PartyDraftDto
import tech.dokus.features.cashflow.presentation.detail.DocumentPreviewState
import tech.dokus.features.cashflow.presentation.detail.DuplicateDiff
import tech.dokus.features.cashflow.presentation.detail.DuplicateReviewState
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

// === Preview Data ===

private val previewExistingDraft: DocDto = DocDto.Invoice.Draft(
    direction = DocumentDirection.Inbound,
    invoiceNumber = "384421507",
    issueDate = kotlinx.datetime.LocalDate(2025, 1, 9),
    dueDate = kotlinx.datetime.LocalDate(2025, 1, 8),
    currency = Currency.Eur,
    subtotalAmount = Money.from("57.01"),
    vatAmount = Money.from("11.97"),
    totalAmount = Money.from("68.98"),
    lineItems = emptyList(),
    iban = null,
    notes = null,
    counterparty = PartyDraftDto(name = "Coolblue België NV"),
)

private val previewIncomingDraft: DocDto = DocDto.Invoice.Draft(
    direction = DocumentDirection.Inbound,
    invoiceNumber = "INV-2026-0005",
    issueDate = kotlinx.datetime.LocalDate(2026, 2, 14),
    dueDate = kotlinx.datetime.LocalDate(2026, 2, 14),
    currency = Currency.Eur,
    subtotalAmount = Money.from("238.84"),
    vatAmount = Money.from("50.16"),
    totalAmount = Money.from("289.00"),
    lineItems = emptyList(),
    iban = null,
    notes = null,
    counterparty = PartyDraftDto(name = "COOLBLUE BELGI\u00CB"),
)

private val previewDiffs = listOf(
    DuplicateDiff("total", "\u20AC68.98", "\u20AC289.00"),
    DuplicateDiff("invoiceNo", "384421507", "INV-2026-0005"),
    DuplicateDiff("issueDate", "2025-01-09", "2026-02-14"),
)

private fun previewDuplicateStateWithDiffs() = DuplicateReviewState(
    existingDraft = previewExistingDraft,
    incomingDraft = previewIncomingDraft,
    reasonType = ReviewReason.MaterialConflict,
    diffs = previewDiffs,
    existingPreview = DocumentPreviewState.NoPreview,
    incomingPreview = DocumentPreviewState.NoPreview,
)

private fun previewDuplicateStateNoDiffs() = DuplicateReviewState(
    existingDraft = previewExistingDraft,
    incomingDraft = previewExistingDraft,
    reasonType = ReviewReason.FuzzyCandidate,
    diffs = emptyList(),
    existingPreview = DocumentPreviewState.NoPreview,
    incomingPreview = DocumentPreviewState.NoPreview,
)

// === Previews ===

@Preview(name = "Duplicate Review - With Diffs", widthDp = 1080, heightDp = 760)
@Composable
private fun DuplicateReviewWithDiffsPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        DesktopDuplicateReviewSurface(
            state = previewDuplicateStateWithDiffs(),
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
            state = previewDuplicateStateNoDiffs(),
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
            onResolveSame = {},
            onResolveDifferent = {},
            onSwitchToDetail = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}
