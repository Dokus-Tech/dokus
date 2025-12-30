package tech.dokus.foundation.aura.components

import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.cashflow_cash_in
import ai.dokus.app.resources.generated.cashflow_cash_out
import tech.dokus.foundation.aura.constrains.Constrains
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource

/**
 * Type of cashflow transaction badge
 */
enum class CashflowType {
    CashIn,
    CashOut
}

/**
 * Badge component for cashflow transaction types (CASH-IN/CASH-OUT).
 * Uses Material Theme colors for consistent theming.
 *
 * @param type The cashflow transaction type
 * @param modifier Optional modifier for the badge
 */
@Composable
fun CashflowTypeBadge(
    type: CashflowType,
    modifier: Modifier = Modifier
) {
    // Use Material Theme colors based on type
    val (backgroundColor, textColor, text) = when (type) {
        CashflowType.CashIn -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            stringResource(Res.string.cashflow_cash_in).uppercase()
        )

        CashflowType.CashOut -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            stringResource(Res.string.cashflow_cash_out).uppercase()
        )
    }

    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = textColor,
        modifier = modifier
            .background(
                color = backgroundColor,
                shape = MaterialTheme.shapes.medium
            )
            .padding(horizontal = Constrains.Spacing.medium, vertical = Constrains.Spacing.xxSmall)
    )
}
