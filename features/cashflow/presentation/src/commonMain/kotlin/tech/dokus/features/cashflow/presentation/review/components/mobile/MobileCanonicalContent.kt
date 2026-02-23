package tech.dokus.features.cashflow.presentation.review.components.mobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.ids.DocumentSourceId
import tech.dokus.features.cashflow.presentation.review.DocumentReviewIntent
import tech.dokus.features.cashflow.presentation.review.DocumentReviewState
import tech.dokus.features.cashflow.presentation.review.components.previewReviewContentState
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

@Composable
internal fun MobileCanonicalContent(
    state: DocumentReviewState.Content,
    onIntent: (DocumentReviewIntent) -> Unit,
    onBackClick: () -> Unit,
    onOpenSource: (DocumentSourceId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accordionState = remember {
        mutableStateMapOf(
            "items" to true,
            "sources" to false,
            "bank" to false,
            "notes" to false,
        )
    }

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        MobileDocumentDetailTopBar(
            state = state,
            onBackClick = onBackClick,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Constraints.Spacing.medium)
                .padding(top = Constraints.Spacing.small),
            verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
        ) {
            MobileCanonicalHeader(state = state)

            MobileAmountHeroCard(state = state)
            MobilePaymentStateCard(
                state = state,
                onIntent = onIntent,
            )

            MobileItemsAccordion(
                state = state,
                expanded = accordionState["items"] == true,
                onToggle = { accordionState["items"] = accordionState["items"] != true },
            )

            MobileSourcesAccordion(
                state = state,
                expanded = accordionState["sources"] == true,
                onToggle = { accordionState["sources"] = accordionState["sources"] != true },
                onIntent = onIntent,
                onOpenSource = onOpenSource,
            )

            MobileBankDetailsAccordion(
                state = state,
                expanded = accordionState["bank"] == true,
                onToggle = { accordionState["bank"] = accordionState["bank"] != true },
            )

            MobileNotesAccordion(
                state = state,
                expanded = accordionState["notes"] == true,
                onToggle = { accordionState["notes"] = accordionState["notes"] != true },
            )

            Spacer(modifier = Modifier.height(Constraints.Spacing.medium))
        }
    }
}

@Preview
@Composable
private fun MobileCanonicalContentPaidPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        MobileCanonicalContent(
            state = previewReviewContentState(entryStatus = CashflowEntryStatus.Paid),
            onIntent = {},
            onBackClick = {},
            onOpenSource = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview
@Composable
private fun MobileCanonicalContentUnpaidPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        MobileCanonicalContent(
            state = previewReviewContentState(entryStatus = CashflowEntryStatus.Open),
            onIntent = {},
            onBackClick = {},
            onOpenSource = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview
@Composable
private fun MobileCanonicalContentOverduePreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        MobileCanonicalContent(
            state = previewReviewContentState(entryStatus = CashflowEntryStatus.Overdue),
            onIntent = {},
            onBackClick = {},
            onOpenSource = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview
@Composable
private fun MobileCanonicalContentReviewPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        MobileCanonicalContent(
            state = previewReviewContentState(entryStatus = null, isDocumentConfirmed = false),
            onIntent = {},
            onBackClick = {},
            onOpenSource = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}
