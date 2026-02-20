package tech.dokus.features.cashflow.presentation.ledger.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_summary_in
import tech.dokus.aura.resources.cashflow_summary_next_30_days
import tech.dokus.aura.resources.cashflow_summary_last_30_days
import tech.dokus.aura.resources.cashflow_summary_out
import tech.dokus.aura.resources.cashflow_summary_overdue
import tech.dokus.aura.resources.cashflow_summary_paid
import tech.dokus.aura.resources.cashflow_summary_received
import tech.dokus.aura.resources.currency_symbol_eur
import tech.dokus.domain.Money
import tech.dokus.features.cashflow.presentation.ledger.mvi.CashflowSummary
import tech.dokus.features.cashflow.presentation.ledger.mvi.CashflowViewMode
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.charts.SparkBars
import tech.dokus.foundation.aura.components.text.DokusLabel
import tech.dokus.foundation.aura.style.positionNegative
import tech.dokus.foundation.aura.style.positionPositive
import tech.dokus.foundation.aura.style.textMuted

/**
 * Summary hero card showing cashflow position with SparkBars.
 *
 * v2 layout: accent card with large amount + subtitle on left, label + SparkBars on right.
 */
@Composable
internal fun CashflowSummarySection(
    summary: CashflowSummary,
    viewMode: CashflowViewMode,
    modifier: Modifier = Modifier,
    sparkData: List<Double> = emptyList(),
) {
    val currencySymbol = stringResource(Res.string.currency_symbol_eur)
    val netAmountText = formatNetAmount(summary.netAmount, currencySymbol)
    val netColor = if (summary.netAmount >= Money.ZERO) {
        MaterialTheme.colorScheme.positionPositive
    } else {
        MaterialTheme.colorScheme.positionNegative
    }

    val timeLabel = when (viewMode) {
        CashflowViewMode.Upcoming -> stringResource(Res.string.cashflow_summary_next_30_days)
        CashflowViewMode.Overdue -> stringResource(Res.string.cashflow_summary_overdue)
        CashflowViewMode.History -> stringResource(Res.string.cashflow_summary_last_30_days)
    }
    val breakdownText = formatBreakdown(summary, viewMode, currencySymbol)

    DokusCardSurface(
        modifier = modifier.fillMaxWidth(),
        accent = true,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            // Left: Cash position
            Column(modifier = Modifier.weight(1f)) {
                DokusLabel(
                    text = "Cash position",
                    color = MaterialTheme.colorScheme.textMuted,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = netAmountText,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = MaterialTheme.typography.labelLarge.fontFamily,
                        letterSpacing = (-0.04).em,
                        lineHeight = 32.sp,
                    ),
                    color = netColor,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "$timeLabel \u00b7 $breakdownText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textMuted,
                )
            }

            // Right: SparkBars (when data available)
            if (sparkData.isNotEmpty()) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    DokusLabel(text = "8 weeks")
                    SparkBars(
                        data = sparkData,
                        height = 44.dp,
                    )
                }
            }
        }
    }
}

// Helper functions

private fun formatNetAmount(amount: Money, currencySymbol: String): String {
    val isNegative = amount < Money.ZERO
    val absAmount = if (isNegative) -amount else amount
    return buildString {
        if (isNegative) {
            append("\u2212") // Typographic minus
        } else if (amount > Money.ZERO) {
            append("+")
        }
        append(currencySymbol)
        append(absAmount.toDisplayString())
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
    return "$inLabel $currencySymbol${summary.totalIn.toDisplayString()} \u00b7 $outLabel $currencySymbol${summary.totalOut.toDisplayString()}"
}
