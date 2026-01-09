package tech.dokus.features.cashflow.presentation.ledger.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_ledger_amount
import tech.dokus.aura.resources.cashflow_ledger_counterparty
import tech.dokus.aura.resources.cashflow_ledger_direction
import tech.dokus.aura.resources.cashflow_ledger_due_date
import tech.dokus.aura.resources.cashflow_ledger_status
import tech.dokus.aura.resources.cashflow_ledger_view_details
import tech.dokus.aura.resources.cashflow_ledger_direction_in
import tech.dokus.aura.resources.cashflow_ledger_direction_out
import tech.dokus.aura.resources.cashflow_ledger_status_cancelled
import tech.dokus.aura.resources.cashflow_ledger_status_open
import tech.dokus.aura.resources.cashflow_ledger_status_overdue
import tech.dokus.aura.resources.cashflow_ledger_status_paid
import tech.dokus.domain.enums.CashflowDirection
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.model.CashflowEntry
import tech.dokus.features.cashflow.presentation.common.components.chips.DokusStatusChip
import tech.dokus.features.cashflow.presentation.common.components.table.DokusTableChevronIcon
import tech.dokus.features.cashflow.presentation.common.components.table.DokusTableHeaderLabel
import tech.dokus.features.cashflow.presentation.common.utils.formatShortDate
import tech.dokus.foundation.aura.components.layout.DokusTableCell
import tech.dokus.foundation.aura.components.layout.DokusTableColumnSpec
import tech.dokus.foundation.aura.components.layout.DokusTableRow
import tech.dokus.foundation.aura.constrains.Constrains

private val TableRowHeight = 56.dp
private val ActionIconSize = 18.dp

@Immutable
private object CashflowTableColumns {
    val DueDate = DokusTableColumnSpec(weight = 0.9f)
    val Counterparty = DokusTableColumnSpec(weight = 2.2f)
    val Direction = DokusTableColumnSpec(width = 88.dp, horizontalAlignment = Alignment.CenterHorizontally)
    val Amount = DokusTableColumnSpec(weight = 0.9f, horizontalAlignment = Alignment.End)
    val Status = DokusTableColumnSpec(width = 120.dp, horizontalAlignment = Alignment.CenterHorizontally)
    val Action = DokusTableColumnSpec(width = 36.dp, horizontalAlignment = Alignment.CenterHorizontally)
}

@Composable
internal fun CashflowLedgerHeaderRow(
    modifier: Modifier = Modifier
) {
    DokusTableRow(
        modifier = modifier,
        minHeight = TableRowHeight,
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        contentPadding = PaddingValues(horizontal = Constrains.Spacing.large)
    ) {
        DokusTableCell(CashflowTableColumns.DueDate) {
            DokusTableHeaderLabel(text = stringResource(Res.string.cashflow_ledger_due_date))
        }
        DokusTableCell(CashflowTableColumns.Counterparty) {
            DokusTableHeaderLabel(text = stringResource(Res.string.cashflow_ledger_counterparty))
        }
        DokusTableCell(CashflowTableColumns.Direction) {
            DokusTableHeaderLabel(
                text = stringResource(Res.string.cashflow_ledger_direction),
                textAlign = TextAlign.Center
            )
        }
        DokusTableCell(CashflowTableColumns.Amount) {
            DokusTableHeaderLabel(
                text = stringResource(Res.string.cashflow_ledger_amount),
                textAlign = TextAlign.End
            )
        }
        DokusTableCell(CashflowTableColumns.Status) {
            DokusTableHeaderLabel(
                text = stringResource(Res.string.cashflow_ledger_status),
                textAlign = TextAlign.Center
            )
        }
        DokusTableCell(CashflowTableColumns.Action) {
            Spacer(modifier = Modifier.width(1.dp))
        }
    }
}

@Composable
internal fun CashflowLedgerTableRow(
    entry: CashflowEntry,
    isHighlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val highlightColor by animateColorAsState(
        targetValue = if (isHighlighted) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "cashflow-ledger-highlight"
    )

    DokusTableRow(
        modifier = modifier,
        minHeight = TableRowHeight,
        backgroundColor = highlightColor,
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = Constrains.Spacing.large)
    ) {
        DokusTableCell(CashflowTableColumns.DueDate) {
            Text(
                text = formatShortDate(entry.eventDate),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        DokusTableCell(CashflowTableColumns.Counterparty) {
            Text(
                text = formatSourceLabel(entry),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        DokusTableCell(CashflowTableColumns.Direction) {
            CashflowDirectionChip(direction = entry.direction)
        }
        DokusTableCell(CashflowTableColumns.Amount) {
            Text(
                text = entry.amountGross.toDisplayString(),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = amountColor(entry.direction),
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        DokusTableCell(CashflowTableColumns.Status) {
            CashflowStatusChip(status = entry.status)
        }
        DokusTableCell(CashflowTableColumns.Action) {
            DokusTableChevronIcon(
                contentDescription = stringResource(Res.string.cashflow_ledger_view_details),
                modifier = Modifier.size(ActionIconSize)
            )
        }
    }
}

@Composable
internal fun CashflowLedgerMobileRow(
    entry: CashflowEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = Constrains.Spacing.large, vertical = Constrains.Spacing.medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = formatSourceLabel(entry),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = formatShortDate(entry.eventDate),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = entry.amountGross.toDisplayString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = amountColor(entry.direction),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CashflowStatusChip(status = entry.status)
            DokusTableChevronIcon(
                contentDescription = null,
                modifier = Modifier.size(ActionIconSize)
            )
        }
    }
}

@Composable
private fun CashflowDirectionChip(
    direction: CashflowDirection,
    modifier: Modifier = Modifier
) {
    val (label, color) = when (direction) {
        CashflowDirection.In -> stringResource(Res.string.cashflow_ledger_direction_in) to
            MaterialTheme.colorScheme.tertiary
        CashflowDirection.Out -> stringResource(Res.string.cashflow_ledger_direction_out) to
            MaterialTheme.colorScheme.error
    }

    DokusStatusChip(
        label = label,
        color = color,
        modifier = modifier
    )
}

@Composable
internal fun CashflowStatusChip(
    status: CashflowEntryStatus,
    modifier: Modifier = Modifier
) {
    val (label, color) = when (status) {
        CashflowEntryStatus.Open -> stringResource(Res.string.cashflow_ledger_status_open) to
            MaterialTheme.colorScheme.primary
        CashflowEntryStatus.Paid -> stringResource(Res.string.cashflow_ledger_status_paid) to
            MaterialTheme.colorScheme.tertiary
        CashflowEntryStatus.Overdue -> stringResource(Res.string.cashflow_ledger_status_overdue) to
            MaterialTheme.colorScheme.error
        CashflowEntryStatus.Cancelled -> stringResource(Res.string.cashflow_ledger_status_cancelled) to
            MaterialTheme.colorScheme.outline
    }

    DokusStatusChip(
        label = label,
        color = color,
        modifier = modifier
    )
}

private fun formatSourceLabel(entry: CashflowEntry): String {
    return entry.sourceType.name.lowercase().replaceFirstChar { it.uppercase() }
}

@Composable
private fun amountColor(direction: CashflowDirection) = when (direction) {
    CashflowDirection.In -> MaterialTheme.colorScheme.tertiary
    CashflowDirection.Out -> MaterialTheme.colorScheme.error
}
