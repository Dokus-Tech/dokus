package tech.dokus.features.banking.presentation.payments.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.banking_filter_all
import tech.dokus.aura.resources.banking_filter_ignored
import tech.dokus.aura.resources.banking_filter_matched
import tech.dokus.aura.resources.banking_filter_needs_review
import tech.dokus.aura.resources.banking_filter_unmatched
import tech.dokus.domain.model.BankTransactionSummary
import tech.dokus.features.banking.presentation.payments.mvi.PaymentFilterTab

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun PaymentFilterTabs(
    selectedTab: PaymentFilterTab,
    summary: BankTransactionSummary?,
    onTabSelected: (PaymentFilterTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = PaymentFilterTab.entries
    val selectedIndex = tabs.indexOf(selectedTab)

    PrimaryScrollableTabRow(
        selectedTabIndex = selectedIndex,
        edgePadding = 0.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        tabs.forEach { tab ->
            val count = summary?.countFor(tab)
            val label = tab.label()
            val text = if (count != null) "$label ($count)" else label

            Tab(
                selected = tab == selectedTab,
                onClick = { onTabSelected(tab) },
                text = { Text(text) },
            )
        }
    }
}

@Composable
private fun PaymentFilterTab.label(): String = when (this) {
    PaymentFilterTab.All -> stringResource(Res.string.banking_filter_all)
    PaymentFilterTab.NeedsReview -> stringResource(Res.string.banking_filter_needs_review)
    PaymentFilterTab.Unmatched -> stringResource(Res.string.banking_filter_unmatched)
    PaymentFilterTab.Matched -> stringResource(Res.string.banking_filter_matched)
    PaymentFilterTab.Ignored -> stringResource(Res.string.banking_filter_ignored)
}

private fun BankTransactionSummary.countFor(tab: PaymentFilterTab): Int = when (tab) {
    PaymentFilterTab.All -> totalCount
    PaymentFilterTab.NeedsReview -> needsReviewCount
    PaymentFilterTab.Unmatched -> unmatchedCount
    PaymentFilterTab.Matched -> matchedCount
    PaymentFilterTab.Ignored -> ignoredCount
}
