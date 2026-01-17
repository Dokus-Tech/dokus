package tech.dokus.features.cashflow.presentation.ledger.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_action_mark_paid
import tech.dokus.aura.resources.cashflow_action_record_payment
import tech.dokus.aura.resources.cashflow_action_view_document
import tech.dokus.aura.resources.cashflow_ledger_amount
import tech.dokus.aura.resources.cashflow_ledger_contact
import tech.dokus.aura.resources.cashflow_ledger_description
import tech.dokus.aura.resources.cashflow_ledger_due_date
import tech.dokus.aura.resources.cashflow_ledger_status
import tech.dokus.domain.enums.CashflowDirection
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.model.CashflowEntry
import tech.dokus.features.cashflow.presentation.common.utils.formatShortDate
import tech.dokus.foundation.aura.components.layout.DokusTableCell
import tech.dokus.foundation.aura.components.layout.DokusTableColumnSpec
import tech.dokus.foundation.aura.components.layout.DokusTableRow
import tech.dokus.foundation.aura.constrains.Constrains
import tech.dokus.foundation.aura.style.surfaceHover

private val TableRowHeight = 56.dp
private val DirectionIconSize = 16.dp
private val StatusIconSize = 20.dp

@Immutable
private object CashflowTableColumns {
    val DueDate = DokusTableColumnSpec(weight = 0.8f)
    val Counterparty = DokusTableColumnSpec(weight = 1.5f)
    val Description = DokusTableColumnSpec(weight = 1.5f)
    val Status = DokusTableColumnSpec(width = 48.dp, horizontalAlignment = Alignment.CenterHorizontally)
    val Amount = DokusTableColumnSpec(weight = 1f, horizontalAlignment = Alignment.End)
    val Actions = DokusTableColumnSpec(width = 48.dp, horizontalAlignment = Alignment.CenterHorizontally)
}

private val HeaderRowHeight = 40.dp

@Composable
internal fun CashflowLedgerHeaderRow(
    modifier: Modifier = Modifier
) {
    DokusTableRow(
        modifier = modifier,
        minHeight = HeaderRowHeight,
        contentPadding = PaddingValues(horizontal = Constrains.Spacing.large)
    ) {
        DokusTableCell(CashflowTableColumns.DueDate) {
            SubtleHeaderLabel(text = stringResource(Res.string.cashflow_ledger_due_date))
        }
        DokusTableCell(CashflowTableColumns.Counterparty) {
            SubtleHeaderLabel(text = stringResource(Res.string.cashflow_ledger_contact))
        }
        DokusTableCell(CashflowTableColumns.Description) {
            SubtleHeaderLabel(text = stringResource(Res.string.cashflow_ledger_description))
        }
        DokusTableCell(CashflowTableColumns.Status) {
            SubtleHeaderLabel(
                text = stringResource(Res.string.cashflow_ledger_status),
                textAlign = TextAlign.Center
            )
        }
        DokusTableCell(CashflowTableColumns.Amount) {
            SubtleHeaderLabel(
                text = stringResource(Res.string.cashflow_ledger_amount),
                textAlign = TextAlign.End
            )
        }
        // Empty cell for actions column in header
        DokusTableCell(CashflowTableColumns.Actions) {
            Spacer(modifier = Modifier.width(1.dp))
        }
    }
}

@Composable
private fun SubtleHeaderLabel(
    text: String,
    textAlign: TextAlign = TextAlign.Start
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        textAlign = textAlign,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
internal fun CashflowLedgerTableRow(
    entry: CashflowEntry,
    isHighlighted: Boolean,
    showActionsMenu: Boolean,
    onClick: () -> Unit,
    onShowActions: () -> Unit,
    onHideActions: () -> Unit,
    onRecordPayment: () -> Unit,
    onMarkAsPaid: () -> Unit,
    onViewDocument: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Hover state tracking
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    // Determine background color based on highlight and hover state
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isHighlighted -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            isHovered -> MaterialTheme.colorScheme.surfaceHover
            else -> MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(durationMillis = 100),
        label = "cashflow-ledger-row-bg"
    )

    DokusTableRow(
        modifier = modifier
            .hoverable(interactionSource = interactionSource)
            .pointerHoverIcon(PointerIcon.Hand),
        minHeight = TableRowHeight,
        backgroundColor = backgroundColor,
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
                text = entry.contactName ?: "—",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        DokusTableCell(CashflowTableColumns.Description) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (entry.direction == CashflowDirection.In) {
                        Icons.AutoMirrored.Filled.TrendingUp
                    } else {
                        Icons.AutoMirrored.Filled.TrendingDown
                    },
                    contentDescription = null,
                    tint = directionColor(entry.direction),
                    modifier = Modifier.size(DirectionIconSize)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = entry.description ?: "—",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        DokusTableCell(CashflowTableColumns.Status) {
            CashflowStatusIcon(status = entry.status)
        }
        DokusTableCell(CashflowTableColumns.Amount) {
            Text(
                text = entry.amountGross.toDisplayString(),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        // Actions column - visible on hover only
        DokusTableCell(CashflowTableColumns.Actions) {
            // Animate alpha for smooth fade in/out
            val actionsAlpha by animateFloatAsState(
                targetValue = if (isHovered || showActionsMenu) 1f else 0f,
                animationSpec = tween(durationMillis = 100),
                label = "actions-alpha"
            )

            Box {
                if (actionsAlpha > 0f) {
                    IconButton(
                        onClick = { onShowActions() },
                        modifier = Modifier
                            .size(32.dp)
                            .alpha(actionsAlpha)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Actions",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                // Dropdown menu
                DropdownMenu(
                    expanded = showActionsMenu,
                    onDismissRequest = onHideActions
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.cashflow_action_record_payment)) },
                        onClick = onRecordPayment
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.cashflow_action_mark_paid)) },
                        onClick = onMarkAsPaid
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.cashflow_action_view_document)) },
                        onClick = onViewDocument
                    )
                }
            }
        }
    }
}

@Composable
internal fun CashflowLedgerMobileRow(
    entry: CashflowEntry,
    onClick: () -> Unit,
    onShowActions: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surface)
            .padding(start = Constrains.Spacing.large, top = Constrains.Spacing.medium, bottom = Constrains.Spacing.medium, end = Constrains.Spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (entry.direction == CashflowDirection.In) {
                        Icons.AutoMirrored.Filled.TrendingUp
                    } else {
                        Icons.AutoMirrored.Filled.TrendingDown
                    },
                    contentDescription = null,
                    tint = directionColor(entry.direction),
                    modifier = Modifier.size(DirectionIconSize)
                )
                Text(
                    text = entry.contactName ?: "—",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        CashflowStatusIcon(status = entry.status)

        // Always visible action button for mobile
        IconButton(
            onClick = onShowActions,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Actions",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun CashflowStatusIcon(
    status: CashflowEntryStatus,
    modifier: Modifier = Modifier
) {
    val (icon, color) = when (status) {
        CashflowEntryStatus.Open -> Icons.Default.RadioButtonUnchecked to
            MaterialTheme.colorScheme.onSurfaceVariant
        CashflowEntryStatus.Paid -> Icons.Default.CheckCircle to
            MaterialTheme.colorScheme.tertiary
        CashflowEntryStatus.Overdue -> Icons.Default.Warning to
            MaterialTheme.colorScheme.error
        CashflowEntryStatus.Cancelled -> Icons.Default.Cancel to
            MaterialTheme.colorScheme.outline
    }

    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = color,
        modifier = modifier.size(StatusIconSize)
    )
}

@Composable
private fun directionColor(direction: CashflowDirection) = when (direction) {
    CashflowDirection.In -> MaterialTheme.colorScheme.tertiary
    CashflowDirection.Out -> MaterialTheme.colorScheme.error
    CashflowDirection.Neutral -> MaterialTheme.colorScheme.outline
}
