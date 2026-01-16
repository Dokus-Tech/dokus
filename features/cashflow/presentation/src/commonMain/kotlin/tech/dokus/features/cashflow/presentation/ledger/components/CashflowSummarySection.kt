package tech.dokus.features.cashflow.presentation.ledger.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_summary_expected_in
import tech.dokus.aura.resources.cashflow_summary_expected_out
import tech.dokus.aura.resources.cashflow_summary_last_30_days
import tech.dokus.aura.resources.cashflow_summary_next_30_days
import tech.dokus.aura.resources.cashflow_summary_paid_out
import tech.dokus.aura.resources.cashflow_summary_received
import tech.dokus.aura.resources.currency_symbol_eur
import tech.dokus.domain.Money
import tech.dokus.features.cashflow.presentation.ledger.mvi.CashflowSummary
import tech.dokus.features.cashflow.presentation.ledger.mvi.CashflowViewMode
import tech.dokus.foundation.aura.style.positionNegative
import tech.dokus.foundation.aura.style.positionPositive
import tech.dokus.foundation.aura.style.textMuted

/**
 * Summary section showing the net cashflow position.
 *
 * Design:
 * - Period label: "NEXT 30 DAYS" or "LAST 30 DAYS" (uppercase, muted)
 * - Net amount: Large number with proper minus sign (−), colored by position
 * - Breakdown: "Expected in €X · Expected out €Y" or "Received €X · Paid out €Y"
 */
@Composable
internal fun CashflowSummarySection(
    summary: CashflowSummary,
    viewMode: CashflowViewMode,
    modifier: Modifier = Modifier
) {
    val currencySymbol = stringResource(Res.string.currency_symbol_eur)

    // Determine period label based on view mode
    val periodLabel = when (viewMode) {
        CashflowViewMode.Upcoming -> stringResource(Res.string.cashflow_summary_next_30_days)
        CashflowViewMode.History -> stringResource(Res.string.cashflow_summary_last_30_days)
    }

    // Format net amount with proper minus sign (U+2212, not hyphen)
    val isNegative = summary.netAmount < Money.ZERO
    val absAmount = if (isNegative) -summary.netAmount else summary.netAmount
    val netAmountText = buildString {
        if (isNegative) {
            append("\u2212") // Proper minus sign
        } else if (summary.netAmount > Money.ZERO) {
            append("+")
        }
        append(currencySymbol)
        append(absAmount.toDisplayString())
    }

    // Determine color based on position
    val netColor = if (summary.netAmount >= Money.ZERO) {
        MaterialTheme.colorScheme.positionPositive
    } else {
        MaterialTheme.colorScheme.positionNegative
    }

    // Build breakdown text based on view mode
    val inLabel = when (viewMode) {
        CashflowViewMode.Upcoming -> stringResource(Res.string.cashflow_summary_expected_in)
        CashflowViewMode.History -> stringResource(Res.string.cashflow_summary_received)
    }
    val outLabel = when (viewMode) {
        CashflowViewMode.Upcoming -> stringResource(Res.string.cashflow_summary_expected_out)
        CashflowViewMode.History -> stringResource(Res.string.cashflow_summary_paid_out)
    }
    val breakdownText = "$inLabel $currencySymbol${summary.totalIn.toDisplayString()} · $outLabel $currencySymbol${summary.totalOut.toDisplayString()}"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Period label (uppercase, muted)
        Text(
            text = periodLabel.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.textMuted,
            letterSpacing = 0.5.sp
        )

        Spacer(Modifier.height(4.dp))

        // Net amount (large, colored)
        Text(
            text = netAmountText,
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = netColor
        )

        Spacer(Modifier.height(4.dp))

        // Breakdown line (muted)
        Text(
            text = breakdownText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.textMuted
        )
    }
}
