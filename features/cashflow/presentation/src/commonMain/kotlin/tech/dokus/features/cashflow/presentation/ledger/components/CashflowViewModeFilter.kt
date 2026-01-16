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
import tech.dokus.aura.resources.cashflow_direction_out
import tech.dokus.aura.resources.cashflow_view_history
import tech.dokus.aura.resources.cashflow_view_upcoming
import tech.dokus.features.cashflow.presentation.ledger.mvi.CashflowViewMode
import tech.dokus.features.cashflow.presentation.ledger.mvi.DirectionFilter

/**
 * View mode and direction filter for cashflow ledger.
 *
 * Two rows:
 * 1. Primary: Upcoming | History (view mode - mutually exclusive)
 * 2. Secondary: All | In | Out (direction filter)
 */
@Composable
internal fun CashflowViewModeFilter(
    viewMode: CashflowViewMode,
    direction: DirectionFilter,
    onViewModeChange: (CashflowViewMode) -> Unit,
    onDirectionChange: (DirectionFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Primary: View mode (mutually exclusive)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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

        // Secondary: Direction filter
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = direction == DirectionFilter.All,
                onClick = { onDirectionChange(DirectionFilter.All) },
                label = { Text(stringResource(Res.string.cashflow_direction_all)) }
            )
            FilterChip(
                selected = direction == DirectionFilter.In,
                onClick = { onDirectionChange(DirectionFilter.In) },
                label = { Text(stringResource(Res.string.cashflow_direction_in)) }
            )
            FilterChip(
                selected = direction == DirectionFilter.Out,
                onClick = { onDirectionChange(DirectionFilter.Out) },
                label = { Text(stringResource(Res.string.cashflow_direction_out)) }
            )
        }
    }
}
