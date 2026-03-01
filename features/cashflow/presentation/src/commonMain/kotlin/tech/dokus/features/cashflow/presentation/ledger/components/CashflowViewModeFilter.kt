package tech.dokus.features.cashflow.presentation.ledger.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_create_invoice
import tech.dokus.aura.resources.cashflow_direction_all
import tech.dokus.aura.resources.cashflow_direction_in
import tech.dokus.aura.resources.cashflow_direction_out
import tech.dokus.aura.resources.cashflow_view_history
import tech.dokus.aura.resources.cashflow_view_overdue
import tech.dokus.aura.resources.cashflow_view_upcoming
import tech.dokus.features.cashflow.presentation.ledger.mvi.CashflowViewMode
import tech.dokus.features.cashflow.presentation.ledger.mvi.DirectionFilter
import tech.dokus.foundation.aura.components.PPrimaryButton
import tech.dokus.foundation.aura.components.tabs.DokusTab
import tech.dokus.foundation.aura.components.tabs.DokusTabs
import tech.dokus.foundation.aura.style.redSoft
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

private val FilterRowVerticalPadding = 8.dp
private val FilterGroupSpacing = 14.dp
private val RightClusterSpacing = 14.dp

/**
 * View mode and direction filter for cashflow ledger.
 * Uses DokusTabs pill-group switcher.
 *
 * Period tabs: Upcoming / Overdue (red count) / History
 * Direction tabs: All / In / Out
 */
@Composable
internal fun CashflowViewModeFilter(
    viewMode: CashflowViewMode,
    direction: DirectionFilter,
    upcomingCount: Int = 0,
    overdueCount: Int = 0,
    onViewModeChange: (CashflowViewMode) -> Unit,
    onDirectionChange: (DirectionFilter) -> Unit,
    onCreateInvoiceClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val periodTabs: @Composable () -> Unit = {
        DokusTabs(
            tabs = listOf(
                DokusTab(
                    id = CashflowViewMode.Upcoming.name,
                    label = stringResource(Res.string.cashflow_view_upcoming),
                    count = upcomingCount.takeIf { it > 0 },
                ),
                DokusTab(
                    id = CashflowViewMode.Overdue.name,
                    label = stringResource(Res.string.cashflow_view_overdue),
                    count = overdueCount.takeIf { it > 0 },
                    countColor = MaterialTheme.colorScheme.error,
                    countBackground = MaterialTheme.colorScheme.redSoft,
                ),
                DokusTab(
                    id = CashflowViewMode.History.name,
                    label = stringResource(Res.string.cashflow_view_history),
                ),
            ),
            activeId = viewMode.name,
            onTabSelected = { id ->
                val mode = CashflowViewMode.entries.first { it.name == id }
                onViewModeChange(mode)
            },
        )
    }

    val directionTabs: @Composable () -> Unit = {
        DokusTabs(
            tabs = listOf(
                DokusTab(
                    id = DirectionFilter.All.name,
                    label = stringResource(Res.string.cashflow_direction_all),
                ),
                DokusTab(
                    id = DirectionFilter.In.name,
                    label = stringResource(Res.string.cashflow_direction_in),
                ),
                DokusTab(
                    id = DirectionFilter.Out.name,
                    label = stringResource(Res.string.cashflow_direction_out),
                ),
            ),
            activeId = direction.name,
            onTabSelected = { id ->
                val dir = DirectionFilter.entries.first { it.name == id }
                onDirectionChange(dir)
            },
        )
    }

    if (onCreateInvoiceClick != null) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = FilterRowVerticalPadding),
            horizontalArrangement = Arrangement.spacedBy(FilterGroupSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            periodTabs()

            Spacer(modifier = Modifier.weight(1f))

            Row(
                horizontalArrangement = Arrangement.spacedBy(RightClusterSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                directionTabs()
                PPrimaryButton(
                    text = "+ ${stringResource(Res.string.cashflow_create_invoice)}",
                    onClick = onCreateInvoiceClick
                )
            }
        }
    } else {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = FilterRowVerticalPadding),
            horizontalArrangement = Arrangement.spacedBy(FilterGroupSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            periodTabs()
            directionTabs()
        }
    }
}

// =============================================================================
// Previews
// =============================================================================

@Preview
@Composable
private fun CashflowViewModeFilterPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        CashflowViewModeFilter(
            viewMode = CashflowViewMode.Upcoming,
            direction = DirectionFilter.All,
            onViewModeChange = {},
            onDirectionChange = {}
        )
    }
}

@Preview(widthDp = 1600)
@Composable
private fun CashflowViewModeFilterWithCreateButtonPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        CashflowViewModeFilter(
            viewMode = CashflowViewMode.Overdue,
            direction = DirectionFilter.All,
            upcomingCount = 1,
            overdueCount = 8,
            onViewModeChange = {},
            onDirectionChange = {},
            onCreateInvoiceClick = {}
        )
    }
}
