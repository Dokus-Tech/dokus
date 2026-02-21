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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
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
import tech.dokus.domain.model.CashflowEntry
import tech.dokus.features.cashflow.presentation.ledger.mvi.CashflowViewMode
import tech.dokus.features.cashflow.presentation.common.utils.formatShortDate
import tech.dokus.foundation.aura.components.layout.DokusTableCell
import tech.dokus.foundation.aura.components.layout.DokusTableColumnSpec
import tech.dokus.foundation.aura.components.layout.DokusTableRow
import tech.dokus.foundation.aura.components.text.Amt
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.amberSoft
import tech.dokus.foundation.aura.style.redSoft
import tech.dokus.foundation.aura.style.surfaceHover
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.foundation.aura.style.textMuted
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

private val TableRowHeight = Constraints.Height.input

/**
 * v2 column specs: DueDate(80dp) | Contact(flex) | Description(flex 0.8) | Status(70dp) | Amount(90dp) | Actions
 */
@Immutable
private object CashflowTableColumns {
    val DueDate = DokusTableColumnSpec(width = 80.dp)
    val Contact = DokusTableColumnSpec(weight = 1f)
    val Description = DokusTableColumnSpec(weight = 0.8f)
    val Status = DokusTableColumnSpec(width = 70.dp)
    val Amount = DokusTableColumnSpec(width = 90.dp, horizontalAlignment = Alignment.End)
    val Actions = DokusTableColumnSpec(
        width = Constraints.IconSize.xxLarge,
        horizontalAlignment = Alignment.CenterHorizontally,
    )
}

@Composable
internal fun CashflowLedgerHeaderRow(
    modifier: Modifier = Modifier
) {
    DokusTableRow(
        modifier = modifier,
        minHeight = Constraints.CropGuide.cornerLength,
        contentPadding = PaddingValues(horizontal = Constraints.Spacing.large)
    ) {
        DokusTableCell(CashflowTableColumns.DueDate) {
            HeaderLabel(text = stringResource(Res.string.cashflow_ledger_due_date))
        }
        DokusTableCell(CashflowTableColumns.Contact) {
            HeaderLabel(text = stringResource(Res.string.cashflow_ledger_contact))
        }
        DokusTableCell(CashflowTableColumns.Description) {
            HeaderLabel(text = stringResource(Res.string.cashflow_ledger_description))
        }
        DokusTableCell(CashflowTableColumns.Status) {
            HeaderLabel(text = stringResource(Res.string.cashflow_ledger_status))
        }
        DokusTableCell(CashflowTableColumns.Amount) {
            HeaderLabel(
                text = stringResource(Res.string.cashflow_ledger_amount),
                textAlign = TextAlign.End,
            )
        }
        DokusTableCell(CashflowTableColumns.Actions) {
            Spacer(modifier = Modifier.width(Constraints.Stroke.thin))
        }
    }
}

@Composable
private fun HeaderLabel(
    text: String,
    textAlign: TextAlign = TextAlign.Start
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.SemiBold,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        textAlign = textAlign,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/**
 * Desktop table row for cashflow entries.
 * v2: due date (mono) | contact (500) | description (muted) | status badge | Amt | actions
 */
@Composable
internal fun CashflowLedgerTableRow(
    entry: CashflowEntry,
    viewMode: CashflowViewMode,
    isHighlighted: Boolean,
    showActionsMenu: Boolean,
    onClick: () -> Unit,
    onShowActions: () -> Unit,
    onHideActions: () -> Unit,
    onRecordPayment: () -> Unit,
    onMarkAsPaid: () -> Unit,
    onViewDocument: () -> Unit,
    modifier: Modifier = Modifier,
    today: LocalDate = rememberToday(),
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isHighlighted -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            isHovered -> MaterialTheme.colorScheme.surfaceHover
            else -> MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(durationMillis = 100),
        label = "cashflow-ledger-row-bg"
    )

    val daysLate = (today.toEpochDays() - entry.eventDate.toEpochDays()).toInt()

    DokusTableRow(
        modifier = modifier
            .hoverable(interactionSource = interactionSource)
            .pointerHoverIcon(PointerIcon.Hand),
        minHeight = TableRowHeight,
        backgroundColor = backgroundColor,
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = Constraints.Spacing.large)
    ) {
        // Due date (mono)
        DokusTableCell(CashflowTableColumns.DueDate) {
            Text(
                text = formatShortDate(entry.eventDate),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    fontFamily = MaterialTheme.typography.labelLarge.fontFamily,
                ),
                color = MaterialTheme.colorScheme.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Contact (500 weight)
        DokusTableCell(CashflowTableColumns.Contact) {
            Text(
                text = entry.contactName ?: "\u2014",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.5.sp,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Description (muted)
        DokusTableCell(CashflowTableColumns.Description) {
            Text(
                text = entry.description ?: "\u2014",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Status badge
        DokusTableCell(CashflowTableColumns.Status) {
            DaysLateBadge(daysLate = daysLate)
        }

        // Amount (negative = expense)
        DokusTableCell(CashflowTableColumns.Amount) {
            Amt(
                value = -entry.amountGross.toDouble(),
                size = 12.sp,
            )
        }

        // Actions (hover)
        DokusTableCell(CashflowTableColumns.Actions) {
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
                            .size(Constraints.AvatarSize.small)
                            .alpha(actionsAlpha)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Actions",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(Constraints.IconSize.smallMedium)
                        )
                    }
                }
                DropdownMenu(
                    expanded = showActionsMenu,
                    onDismissRequest = onHideActions
                ) {
                    if (viewMode != CashflowViewMode.History) {
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.cashflow_action_record_payment)) },
                            onClick = onRecordPayment
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.cashflow_action_mark_paid)) },
                            onClick = onMarkAsPaid
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.cashflow_action_view_document)) },
                        onClick = onViewDocument
                    )
                }
            }
        }
    }
}

/**
 * Mobile row for cashflow entries.
 */
@Composable
internal fun CashflowLedgerMobileRow(
    entry: CashflowEntry,
    viewMode: CashflowViewMode,
    onClick: () -> Unit,
    onShowActions: () -> Unit,
    modifier: Modifier = Modifier,
    today: LocalDate = rememberToday(),
) {
    val daysLate = (today.toEpochDays() - entry.eventDate.toEpochDays()).toInt()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surface)
            .padding(
                start = Constraints.Spacing.large,
                top = Constraints.Spacing.medium,
                bottom = Constraints.Spacing.medium,
                end = Constraints.Spacing.small
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
        ) {
            Text(
                text = entry.contactName ?: "\u2014",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
            ) {
                Text(
                    text = formatShortDate(entry.eventDate),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = MaterialTheme.typography.labelLarge.fontFamily,
                    ),
                    color = MaterialTheme.colorScheme.textMuted,
                    maxLines = 1,
                )
                DaysLateBadge(daysLate = daysLate)
            }
        }

        Amt(
            value = -entry.amountGross.toDouble(),
            size = 12.sp,
        )

        IconButton(
            onClick = onShowActions,
            modifier = Modifier.size(Constraints.CropGuide.cornerLength)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Actions",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Constraints.IconSize.smallMedium)
            )
        }
    }
}

// =============================================================================
// Status Badge
// =============================================================================

/**
 * Badge showing days late/until due.
 * - >30d late: red text, redSoft bg
 * - >14d late: amber text, amberSoft bg
 * - <=14d late or upcoming: muted text, surfaceVariant bg
 */
@Composable
private fun DaysLateBadge(
    daysLate: Int,
    modifier: Modifier = Modifier,
) {
    val text = if (daysLate > 0) "${daysLate}d late" else "in ${-daysLate}d"

    val (textColor, bgColor) = when {
        daysLate > 30 -> MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.redSoft
        daysLate > 14 -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.amberSoft
        else -> MaterialTheme.colorScheme.textMuted to MaterialTheme.colorScheme.surfaceVariant
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(bgColor)
            .padding(horizontal = 7.dp, vertical = 2.dp),
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = MaterialTheme.typography.labelLarge.fontFamily,
            color = textColor,
            maxLines = 1,
        )
    }
}

// =============================================================================
// Helpers
// =============================================================================

@Composable
private fun rememberToday(): LocalDate {
    return remember {
        kotlinx.datetime.Clock.System.todayIn(TimeZone.currentSystemDefault())
    }
}

@Preview
@Composable
private fun CashflowLedgerHeaderRowPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        CashflowLedgerHeaderRow()
    }
}
