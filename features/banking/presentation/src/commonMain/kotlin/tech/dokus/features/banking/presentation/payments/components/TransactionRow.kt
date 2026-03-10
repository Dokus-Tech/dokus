package tech.dokus.features.banking.presentation.payments.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.banking_action_link
import tech.dokus.aura.resources.banking_col_account
import tech.dokus.aura.resources.banking_col_amount
import tech.dokus.aura.resources.banking_col_counterparty
import tech.dokus.aura.resources.banking_col_date
import tech.dokus.aura.resources.banking_col_description
import tech.dokus.aura.resources.banking_col_status
import tech.dokus.domain.enums.BankTransactionStatus
import tech.dokus.domain.model.BankTransactionDto
import tech.dokus.foundation.aura.components.layout.DokusTableCell
import tech.dokus.foundation.aura.components.layout.DokusTableColumnSpec
import tech.dokus.foundation.aura.components.layout.DokusTableRow
import tech.dokus.foundation.aura.components.text.Amt
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.foundation.aura.extensions.statusColor
import tech.dokus.foundation.aura.style.amberSoft
import tech.dokus.foundation.aura.style.redSoft
import tech.dokus.foundation.aura.style.surfaceHover
import tech.dokus.foundation.aura.style.textMuted

/**
 * Column layout: Date(60dp) | Description(flex) | Counterparty(flex 0.7) | Acct(50dp) | Status(90dp) | Document(70dp) | Amount(100dp)
 */
@Immutable
internal object PaymentsTableColumns {
    val Date = DokusTableColumnSpec(width = 60.dp)
    val Description = DokusTableColumnSpec(weight = 1f)
    val Counterparty = DokusTableColumnSpec(weight = 0.7f)
    val Account = DokusTableColumnSpec(width = 50.dp)
    val Status = DokusTableColumnSpec(width = 90.dp)
    val Document = DokusTableColumnSpec(width = 70.dp)
    val Amount = DokusTableColumnSpec(width = 100.dp, horizontalAlignment = Alignment.End)
}

private val TableRowHeight = Constraints.Height.input

@Composable
internal fun TransactionHeaderRow(
    modifier: Modifier = Modifier,
) {
    DokusTableRow(
        modifier = modifier,
        minHeight = Constraints.CropGuide.cornerLength,
        contentPadding = PaddingValues(horizontal = Constraints.Spacing.large),
    ) {
        DokusTableCell(PaymentsTableColumns.Date) {
            HeaderLabel(text = stringResource(Res.string.banking_col_date))
        }
        DokusTableCell(PaymentsTableColumns.Description) {
            HeaderLabel(text = stringResource(Res.string.banking_col_description))
        }
        DokusTableCell(PaymentsTableColumns.Counterparty) {
            HeaderLabel(text = stringResource(Res.string.banking_col_counterparty))
        }
        DokusTableCell(PaymentsTableColumns.Account) {
            HeaderLabel(text = stringResource(Res.string.banking_col_account))
        }
        DokusTableCell(PaymentsTableColumns.Status) {
            HeaderLabel(text = stringResource(Res.string.banking_col_status))
        }
        DokusTableCell(PaymentsTableColumns.Document) {
            Spacer(modifier = Modifier.width(Constraints.Stroke.thin))
        }
        DokusTableCell(PaymentsTableColumns.Amount) {
            HeaderLabel(
                text = stringResource(Res.string.banking_col_amount),
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
internal fun TransactionRow(
    transaction: BankTransactionDto,
    isSelected: Boolean,
    onClick: () -> Unit,
    accountName: String? = null,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            isHovered -> MaterialTheme.colorScheme.surfaceHover
            else -> MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(durationMillis = 100),
        label = "tx-row-bg",
    )

    DokusTableRow(
        modifier = modifier
            .hoverable(interactionSource = interactionSource)
            .pointerHoverIcon(PointerIcon.Hand),
        minHeight = TableRowHeight,
        backgroundColor = backgroundColor,
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = Constraints.Spacing.large),
    ) {
        // Date
        DokusTableCell(PaymentsTableColumns.Date) {
            Text(
                text = formatShortDate(transaction.transactionDate),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    fontFamily = MaterialTheme.typography.labelLarge.fontFamily,
                ),
                color = MaterialTheme.colorScheme.textMuted,
                maxLines = 1,
            )
        }

        // Description
        DokusTableCell(PaymentsTableColumns.Description) {
            Text(
                text = transaction.descriptionRaw ?: transaction.counterpartyName ?: "\u2014",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.5.sp,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Counterparty
        DokusTableCell(PaymentsTableColumns.Counterparty) {
            Text(
                text = transaction.counterpartyName ?: "\u2014",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Account (short label)
        DokusTableCell(PaymentsTableColumns.Account) {
            Text(
                text = accountName ?: "\u2014",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Status badge
        DokusTableCell(PaymentsTableColumns.Status) {
            TransactionStatusBadge(status = transaction.status)
        }

        // Document action
        DokusTableCell(PaymentsTableColumns.Document) {
            if (transaction.status == BankTransactionStatus.Unmatched ||
                transaction.status == BankTransactionStatus.NeedsReview
            ) {
                Text(
                    text = "+ ${stringResource(Res.string.banking_action_link)}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                )
            }
        }

        // Amount
        DokusTableCell(PaymentsTableColumns.Amount) {
            Amt(
                minorUnits = transaction.signedAmount.minor,
                size = 12.sp,
            )
        }
    }
}

/**
 * Mobile-friendly card layout: two lines (counterparty + date/status) with amount on the right.
 */
@Composable
internal fun TransactionCard(
    transaction: BankTransactionDto,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedBg = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0f)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(selectedBg)
            .clickable(onClick = onClick)
            .padding(
                horizontal = Constraints.Spacing.large,
                vertical = Constraints.Spacing.medium,
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xxSmall),
        ) {
            Text(
                text = transaction.counterpartyName ?: transaction.descriptionRaw ?: "\u2014",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatShortDate(transaction.transactionDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TransactionStatusBadge(status = transaction.status)
            }
        }

        Amt(minorUnits = transaction.signedAmount.minor)
    }
}

@Composable
private fun TransactionStatusBadge(
    status: BankTransactionStatus,
    modifier: Modifier = Modifier,
) {
    val bgColor = when (status) {
        BankTransactionStatus.Unmatched -> MaterialTheme.colorScheme.amberSoft
        BankTransactionStatus.NeedsReview -> MaterialTheme.colorScheme.redSoft
        BankTransactionStatus.Matched -> MaterialTheme.colorScheme.surface
        BankTransactionStatus.Ignored -> MaterialTheme.colorScheme.surface
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(bgColor)
            .padding(horizontal = 7.dp, vertical = 2.dp),
    ) {
        Text(
            text = status.localized,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = MaterialTheme.typography.labelLarge.fontFamily,
            color = status.statusColor,
            maxLines = 1,
        )
    }
}

@Composable
private fun HeaderLabel(
    text: String,
    textAlign: TextAlign = TextAlign.Start,
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

