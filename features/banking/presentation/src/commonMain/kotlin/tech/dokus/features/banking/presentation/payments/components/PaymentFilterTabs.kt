package tech.dokus.features.banking.presentation.payments.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.banking_filter_all
import tech.dokus.aura.resources.banking_filter_ignored
import tech.dokus.aura.resources.banking_filter_matched
import tech.dokus.aura.resources.banking_filter_needs_review
import tech.dokus.aura.resources.banking_filter_unmatched
import tech.dokus.domain.Money
import tech.dokus.domain.model.BankTransactionSummary
import tech.dokus.features.banking.presentation.payments.mvi.PaymentFilterTab
import tech.dokus.foundation.aura.components.tabs.DokusTab
import tech.dokus.foundation.aura.components.tabs.DokusTabs
import tech.dokus.foundation.aura.style.amberSoft
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

@Composable
internal fun PaymentFilterTabs(
    selectedTab: PaymentFilterTab,
    summary: BankTransactionSummary?,
    onTabSelected: (PaymentFilterTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = listOf(
        DokusTab(
            id = PaymentFilterTab.All.name,
            label = stringResource(Res.string.banking_filter_all),
            count = summary?.totalCount?.takeIf { it > 0 },
        ),
        DokusTab(
            id = PaymentFilterTab.NeedsReview.name,
            label = stringResource(Res.string.banking_filter_needs_review),
            count = summary?.needsReviewCount?.takeIf { it > 0 },
            countColor = MaterialTheme.colorScheme.primary,
            countBackground = MaterialTheme.colorScheme.amberSoft,
        ),
        DokusTab(
            id = PaymentFilterTab.Unmatched.name,
            label = stringResource(Res.string.banking_filter_unmatched),
            count = summary?.unmatchedCount?.takeIf { it > 0 },
            countColor = MaterialTheme.colorScheme.primary,
            countBackground = MaterialTheme.colorScheme.amberSoft,
        ),
        DokusTab(
            id = PaymentFilterTab.Matched.name,
            label = stringResource(Res.string.banking_filter_matched),
            count = summary?.matchedCount?.takeIf { it > 0 },
        ),
        DokusTab(
            id = PaymentFilterTab.Ignored.name,
            label = stringResource(Res.string.banking_filter_ignored),
            count = summary?.ignoredCount?.takeIf { it > 0 },
        ),
    )

    DokusTabs(
        tabs = tabs,
        activeId = selectedTab.name,
        onTabSelected = { id ->
            val tab = PaymentFilterTab.entries.first { it.name == id }
            onTabSelected(tab)
        },
        modifier = modifier,
    )
}

@Preview(name = "Payment Filter Tabs")
@Composable
private fun PaymentFilterTabsPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        PaymentFilterTabs(
            selectedTab = PaymentFilterTab.All,
            summary = BankTransactionSummary(
                unmatchedCount = 12,
                needsReviewCount = 3,
                matchedCount = 45,
                ignoredCount = 5,
                totalCount = 65,
                totalUnresolvedAmount = Money.parseOrThrow("8420.50"),
            ),
            onTabSelected = {},
        )
    }
}
