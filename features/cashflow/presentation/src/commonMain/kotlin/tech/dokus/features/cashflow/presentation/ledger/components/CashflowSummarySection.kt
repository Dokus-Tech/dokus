package tech.dokus.features.cashflow.presentation.ledger.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_balance_label
import tech.dokus.aura.resources.cashflow_balance_unavailable
import tech.dokus.aura.resources.cashflow_summary_in
import tech.dokus.aura.resources.cashflow_summary_last_30_days
import tech.dokus.aura.resources.cashflow_summary_last_30d
import tech.dokus.aura.resources.cashflow_summary_next_30_days
import tech.dokus.aura.resources.cashflow_summary_next_30d
import tech.dokus.aura.resources.cashflow_summary_overdue
import tech.dokus.aura.resources.cashflow_summary_overdue_short
import tech.dokus.aura.resources.cashflow_summary_out
import tech.dokus.aura.resources.cashflow_summary_paid
import tech.dokus.aura.resources.cashflow_summary_received
import tech.dokus.aura.resources.currency_symbol_eur
import tech.dokus.domain.Money
import tech.dokus.features.cashflow.presentation.ledger.mvi.BalanceState
import tech.dokus.features.cashflow.presentation.ledger.mvi.CashflowSummary
import tech.dokus.features.cashflow.presentation.ledger.mvi.CashflowViewMode
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.aura.style.positionNegative
import tech.dokus.foundation.aura.style.positionPositive
import tech.dokus.foundation.aura.style.textFaint
import tech.dokus.foundation.aura.style.textMuted

/**
 * Summary section showing balance and net cashflow position.
 *
 * Desktop: Side-by-side layout (Balance left, Movement right)
 * Mobile: Movement primary (36px), Balance secondary with separator
 *         Compresses to single line when scrolling
 */
@Composable
internal fun CashflowSummarySection(
    summary: CashflowSummary,
    balance: BalanceState?,
    viewMode: CashflowViewMode,
    modifier: Modifier = Modifier,
    isCompressed: Boolean = false
) {
    val isDesktop = LocalScreenSize.current.isLarge

    if (isDesktop) {
        DesktopSummarySection(
            summary = summary,
            balance = balance,
            viewMode = viewMode,
            modifier = modifier
        )
    } else {
        if (isCompressed) {
            CompressedMobileSummarySection(
                summary = summary,
                balance = balance,
                viewMode = viewMode,
                modifier = modifier
            )
        } else {
            ExpandedMobileSummarySection(
                summary = summary,
                balance = balance,
                viewMode = viewMode,
                modifier = modifier
            )
        }
    }
}

/**
 * Desktop: Side-by-side layout
 * Balance (context, 28px) | Movement (hero, 32px)
 */
@Composable
private fun DesktopSummarySection(
    summary: CashflowSummary,
    balance: BalanceState?,
    viewMode: CashflowViewMode,
    modifier: Modifier = Modifier
) {
    val currencySymbol = stringResource(Res.string.currency_symbol_eur)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        // Left: Balance (context)
        Column {
            if (balance != null) {
                Text(
                    text = "$currencySymbol${balance.amount.toDisplayString()}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Medium,
                    fontSize = 28.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${stringResource(Res.string.cashflow_balance_label)} · ${formatShortDate(balance.asOf)} · ${balance.accountName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textMuted
                )
            } else {
                // Balance unavailable
                Text(
                    text = "—",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Medium,
                    fontSize = 28.sp,
                    color = MaterialTheme.colorScheme.textFaint,
                )
                Text(
                    text = stringResource(Res.string.cashflow_balance_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textMuted
                )
            }
        }

        // Right: Movement (hero)
        Column(horizontalAlignment = Alignment.End) {
            val netAmountText = formatNetAmount(summary.netAmount, currencySymbol)
            val netColor = if (summary.netAmount >= Money.ZERO) {
                MaterialTheme.colorScheme.positionPositive
            } else {
                MaterialTheme.colorScheme.positionNegative
            }

            Text(
                text = netAmountText,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold,
                fontSize = 32.sp,
                color = netColor,
                textAlign = TextAlign.End
            )

            // Time label + breakdown
            val timeLabel = when (viewMode) {
                CashflowViewMode.Upcoming -> stringResource(Res.string.cashflow_summary_next_30_days)
                CashflowViewMode.Overdue -> stringResource(Res.string.cashflow_summary_overdue)
                CashflowViewMode.History -> stringResource(Res.string.cashflow_summary_last_30_days)
            }
            val breakdownText = formatBreakdown(summary, viewMode, currencySymbol)

            Text(
                text = "$timeLabel · $breakdownText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textMuted,
                textAlign = TextAlign.End
            )
        }
    }
}

/**
 * Mobile Expanded: Movement primary, Balance secondary with separator
 */
@Composable
private fun ExpandedMobileSummarySection(
    summary: CashflowSummary,
    balance: BalanceState?,
    viewMode: CashflowViewMode,
    modifier: Modifier = Modifier
) {
    val currencySymbol = stringResource(Res.string.currency_symbol_eur)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        // Movement (primary)
        val netAmountText = formatNetAmount(summary.netAmount, currencySymbol)
        val netColor = if (summary.netAmount >= Money.ZERO) {
            MaterialTheme.colorScheme.positionPositive
        } else {
            MaterialTheme.colorScheme.positionNegative
        }

        Text(
            text = netAmountText,
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.SemiBold,
            fontSize = 36.sp,
            color = netColor,
        )

        Spacer(Modifier.height(2.dp))

        // Time label
        val timeLabel = when (viewMode) {
            CashflowViewMode.Upcoming -> stringResource(Res.string.cashflow_summary_next_30_days)
            CashflowViewMode.Overdue -> stringResource(Res.string.cashflow_summary_overdue)
            CashflowViewMode.History -> stringResource(Res.string.cashflow_summary_last_30_days)
        }
        Text(
            text = timeLabel,
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.textMuted,
        )

        Spacer(Modifier.height(2.dp))

        // Breakdown
        val breakdownText = formatBreakdown(summary, viewMode, currencySymbol)
        Text(
            text = breakdownText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.textFaint
        )

        // Separator
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(12.dp))

        // Balance (secondary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(Res.string.cashflow_balance_label),
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.textMuted,
            )
            Spacer(Modifier.width(6.dp))
            if (balance != null) {
                Text(
                    text = "$currencySymbol${balance.amount.toDisplayString()}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = "—",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.textFaint,
                )
            }
        }

        Spacer(Modifier.height(2.dp))

        // Balance date + account
        if (balance != null) {
            Text(
                text = "${formatShortDate(balance.asOf)} · ${balance.accountName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textFaint
            )
        } else {
            Text(
                text = stringResource(Res.string.cashflow_balance_unavailable),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textFaint
            )
        }
    }
}

/**
 * Mobile Compressed: Single self-explanatory line
 * "Balance €12,482 · Next 30d −€896"
 */
@Composable
private fun CompressedMobileSummarySection(
    summary: CashflowSummary,
    balance: BalanceState?,
    viewMode: CashflowViewMode,
    modifier: Modifier = Modifier
) {
    val currencySymbol = stringResource(Res.string.currency_symbol_eur)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Balance part
        val balanceText = if (balance != null) {
            "${stringResource(Res.string.cashflow_balance_label)} $currencySymbol${balance.amount.toCompactString()}"
        } else {
            "${stringResource(Res.string.cashflow_balance_label)} —"
        }

        Text(
            text = balanceText,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = " · ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.textMuted
        )

        // Movement part
        val timeLabel = when (viewMode) {
            CashflowViewMode.Upcoming -> stringResource(Res.string.cashflow_summary_next_30d)
            CashflowViewMode.Overdue -> stringResource(Res.string.cashflow_summary_overdue_short)
            CashflowViewMode.History -> stringResource(Res.string.cashflow_summary_last_30d)
        }
        val netAmountCompact = formatNetAmountCompact(summary.netAmount, currencySymbol)
        val netColor = if (summary.netAmount >= Money.ZERO) {
            MaterialTheme.colorScheme.positionPositive
        } else {
            MaterialTheme.colorScheme.positionNegative
        }

        Text(
            text = "$timeLabel ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.textMuted
        )
        Text(
            text = netAmountCompact,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = netColor
        )
    }
}

// Helper functions

private fun formatNetAmount(amount: Money, currencySymbol: String): String {
    val isNegative = amount < Money.ZERO
    val absAmount = if (isNegative) -amount else amount
    return buildString {
        if (isNegative) {
            append("\u2212") // Proper minus sign
        } else if (amount > Money.ZERO) {
            append("+")
        }
        append(currencySymbol)
        append(absAmount.toDisplayString())
    }
}

private fun formatNetAmountCompact(amount: Money, currencySymbol: String): String {
    val isNegative = amount < Money.ZERO
    val absAmount = if (isNegative) -amount else amount
    return buildString {
        if (isNegative) {
            append("\u2212")
        } else if (amount > Money.ZERO) {
            append("+")
        }
        append(currencySymbol)
        append(absAmount.toCompactString())
    }
}

@Composable
private fun formatBreakdown(
    summary: CashflowSummary,
    viewMode: CashflowViewMode,
    currencySymbol: String
): String {
    val inLabel = when (viewMode) {
        CashflowViewMode.Upcoming, CashflowViewMode.Overdue -> stringResource(Res.string.cashflow_summary_in)
        CashflowViewMode.History -> stringResource(Res.string.cashflow_summary_received)
    }
    val outLabel = when (viewMode) {
        CashflowViewMode.Upcoming, CashflowViewMode.Overdue -> stringResource(Res.string.cashflow_summary_out)
        CashflowViewMode.History -> stringResource(Res.string.cashflow_summary_paid)
    }
    return "$inLabel $currencySymbol${summary.totalIn.toDisplayString()} · $outLabel $currencySymbol${summary.totalOut.toDisplayString()}"
}

private fun formatShortDate(date: kotlinx.datetime.LocalDate): String {
    val day = date.dayOfMonth
    val month = date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    return "$day $month"
}

// Extension for compact display (e.g., "12,482" instead of "12,482.34")
private fun Money.toCompactString(): String {
    // Show whole euros only for compact display
    val euros = this.minor / 100
    return euros.toString().reversed().chunked(3).joinToString(",").reversed()
}
