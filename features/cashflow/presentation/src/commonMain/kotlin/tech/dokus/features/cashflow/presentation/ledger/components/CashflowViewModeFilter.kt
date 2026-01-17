package tech.dokus.features.cashflow.presentation.ledger.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_direction_all
import tech.dokus.aura.resources.cashflow_direction_in
import tech.dokus.aura.resources.cashflow_direction_money_in
import tech.dokus.aura.resources.cashflow_direction_money_out
import tech.dokus.aura.resources.cashflow_direction_out
import tech.dokus.aura.resources.cashflow_view_history
import tech.dokus.aura.resources.cashflow_view_upcoming
import tech.dokus.features.cashflow.presentation.ledger.mvi.CashflowViewMode
import tech.dokus.features.cashflow.presentation.ledger.mvi.DirectionFilter
import tech.dokus.foundation.aura.local.LocalScreenSize

/**
 * View mode and direction filter for cashflow ledger.
 *
 * Desktop: Single row with view mode left, direction right.
 * Mobile: Two rows - view mode on top, direction below with longer labels.
 */
@Composable
internal fun CashflowViewModeFilter(
    viewMode: CashflowViewMode,
    direction: DirectionFilter,
    onViewModeChange: (CashflowViewMode) -> Unit,
    onDirectionChange: (DirectionFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDesktop = LocalScreenSize.current.isLarge

    // Direction labels: short on desktop, descriptive on mobile
    val inLabel = if (isDesktop) {
        stringResource(Res.string.cashflow_direction_in)
    } else {
        stringResource(Res.string.cashflow_direction_money_in)
    }
    val outLabel = if (isDesktop) {
        stringResource(Res.string.cashflow_direction_out)
    } else {
        stringResource(Res.string.cashflow_direction_money_out)
    }

    if (isDesktop) {
        // Desktop: Single row with space between
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: View mode chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = viewMode == CashflowViewMode.Upcoming,
                    onClick = { onViewModeChange(CashflowViewMode.Upcoming) },
                    label = { Text(stringResource(Res.string.cashflow_view_upcoming)) }
                )
                FilterChip(
                    selected = viewMode == CashflowViewMode.History,
                    onClick = { onViewModeChange(CashflowViewMode.History) },
                    label = { Text(stringResource(Res.string.cashflow_view_history)) }
                )
            }

            // Right: Direction chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = direction == DirectionFilter.All,
                    onClick = { onDirectionChange(DirectionFilter.All) },
                    label = { Text(stringResource(Res.string.cashflow_direction_all)) }
                )
                FilterChip(
                    selected = direction == DirectionFilter.In,
                    onClick = { onDirectionChange(DirectionFilter.In) },
                    label = { Text(inLabel) }
                )
                FilterChip(
                    selected = direction == DirectionFilter.Out,
                    onClick = { onDirectionChange(DirectionFilter.Out) },
                    label = { Text(outLabel) }
                )
            }
        }
    } else {
        // Mobile: Two rows stacked
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: View mode
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = viewMode == CashflowViewMode.Upcoming,
                    onClick = { onViewModeChange(CashflowViewMode.Upcoming) },
                    label = { Text(stringResource(Res.string.cashflow_view_upcoming)) }
                )
                FilterChip(
                    selected = viewMode == CashflowViewMode.History,
                    onClick = { onViewModeChange(CashflowViewMode.History) },
                    label = { Text(stringResource(Res.string.cashflow_view_history)) }
                )
            }

            // Row 2: Direction filter with descriptive labels
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = direction == DirectionFilter.All,
                    onClick = { onDirectionChange(DirectionFilter.All) },
                    label = { Text(stringResource(Res.string.cashflow_direction_all)) }
                )
                FilterChip(
                    selected = direction == DirectionFilter.In,
                    onClick = { onDirectionChange(DirectionFilter.In) },
                    label = { Text(inLabel) }
                )
                FilterChip(
                    selected = direction == DirectionFilter.Out,
                    onClick = { onDirectionChange(DirectionFilter.Out) },
                    label = { Text(outLabel) }
                )
            }
        }
    }
}
