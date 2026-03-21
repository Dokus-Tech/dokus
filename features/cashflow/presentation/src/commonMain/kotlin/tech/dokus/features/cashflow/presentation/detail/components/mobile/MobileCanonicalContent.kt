package tech.dokus.features.cashflow.presentation.detail.components.mobile

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
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.ids.DocumentSourceId
import tech.dokus.features.cashflow.presentation.detail.models.DocumentUiData
import tech.dokus.features.cashflow.presentation.detail.DocumentDetailIntent
import tech.dokus.features.cashflow.presentation.detail.DocumentDetailState
import tech.dokus.features.cashflow.presentation.detail.components.previewAutoPaymentStatus
import tech.dokus.features.cashflow.presentation.detail.components.previewReviewContentState
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

@Composable
internal fun MobileCanonicalContent(
    state: DocumentDetailState,
    isAccountantReadOnly: Boolean,
    onIntent: (DocumentDetailIntent) -> Unit,
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

            val uiData = state.uiData
            if (uiData != null && uiData !is DocumentUiData.BankStatement) {
                MobileAmountHeroCard(state = state, uiData = uiData)
                MobilePaymentStateCard(
                    state = state,
                    isAccountantReadOnly = isAccountantReadOnly,
                    onIntent = onIntent,
                )
            }

            // Items accordion — Invoice and CreditNote only
            val lineItems = when (uiData) {
                is DocumentUiData.Invoice -> uiData.lineItems
                is DocumentUiData.CreditNote -> uiData.lineItems
                else -> emptyList()
            }
            if (lineItems.isNotEmpty()) {
                MobileItemsAccordion(
                    lineItems = lineItems,
                    subtotalAmount = when (uiData) {
                        is DocumentUiData.Invoice -> uiData.subtotalAmount
                        is DocumentUiData.CreditNote -> uiData.subtotalAmount
                        else -> null
                    },
                    vatAmount = when (uiData) {
                        is DocumentUiData.Invoice -> uiData.vatAmount
                        is DocumentUiData.CreditNote -> uiData.vatAmount
                        else -> null
                    },
                    totalAmount = state.totalAmount,
                    currencySign = when (uiData) {
                        is DocumentUiData.Invoice -> uiData.currencySign
                        is DocumentUiData.CreditNote -> uiData.currencySign
                        else -> "\u20AC"
                    },
                    expanded = accordionState["items"] == true,
                    onToggle = { accordionState["items"] = accordionState["items"] != true },
                )
            }

            MobileSourcesAccordion(
                state = state,
                isAccountantReadOnly = isAccountantReadOnly,
                expanded = accordionState["sources"] == true,
                onToggle = { accordionState["sources"] = accordionState["sources"] != true },
                onIntent = onIntent,
                onOpenSource = onOpenSource,
            )

            MobileBankDetailsAccordion(
                bankDetails = (uiData as? DocumentUiData.Invoice)?.iban,
                expanded = accordionState["bank"] == true,
                onToggle = { accordionState["bank"] = accordionState["bank"] != true },
            )

            val notes = when (uiData) {
                is DocumentUiData.Invoice -> uiData.notes
                is DocumentUiData.CreditNote -> uiData.notes
                is DocumentUiData.Receipt -> uiData.notes
                else -> null
            }
            MobileNotesAccordion(
                notes = notes,
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
            isAccountantReadOnly = false,
            onIntent = {},
            onBackClick = {},
            onOpenSource = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview
@Composable
private fun MobileCanonicalContentAutoPaidPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        MobileCanonicalContent(
            state = previewReviewContentState(
                entryStatus = CashflowEntryStatus.Paid,
                autoPaymentStatus = previewAutoPaymentStatus(canUndo = true),
            ),
            isAccountantReadOnly = false,
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
            isAccountantReadOnly = false,
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
            isAccountantReadOnly = false,
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
            state = previewReviewContentState(entryStatus = null, documentStatus = DocumentStatus.NeedsReview),
            isAccountantReadOnly = false,
            onIntent = {},
            onBackClick = {},
            onOpenSource = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}
