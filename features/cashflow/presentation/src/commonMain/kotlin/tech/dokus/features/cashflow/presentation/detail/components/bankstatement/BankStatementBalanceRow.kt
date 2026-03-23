package tech.dokus.features.cashflow.presentation.detail.components.bankstatement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import tech.dokus.foundation.aura.style.positionNegative
import tech.dokus.foundation.aura.style.positionPositive
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.bank_statement_closing_balance
import tech.dokus.aura.resources.bank_statement_movement
import tech.dokus.aura.resources.bank_statement_opening_balance
import org.jetbrains.compose.resources.stringResource
import tech.dokus.foundation.aura.constrains.Constraints

@Composable
internal fun BankStatementBalanceRow(
    openingBalance: String?,
    closingBalance: String?,
    movement: String?,
    currencyPrefix: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.xxLarge),
    ) {
        openingBalance?.let {
            BalanceCell(
                label = stringResource(Res.string.bank_statement_opening_balance),
                value = "$currencyPrefix$it",
                modifier = Modifier.weight(1f),
            )
        }
        closingBalance?.let {
            BalanceCell(
                label = stringResource(Res.string.bank_statement_closing_balance),
                value = "$currencyPrefix$it",
                modifier = Modifier.weight(1f),
            )
        }
        movement?.let {
            val movementColor = if (it.startsWith("-")) {
                MaterialTheme.colorScheme.positionNegative
            } else {
                MaterialTheme.colorScheme.positionPositive
            }
            BalanceCell(
                label = stringResource(Res.string.bank_statement_movement),
                value = "$currencyPrefix$it",
                valueColor = movementColor,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun BalanceCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xxSmall),
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = valueColor,
        )
    }
}
