package tech.dokus.foundation.aura.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_cash_in
import tech.dokus.aura.resources.cashflow_cash_out
import tech.dokus.foundation.aura.style.positionNegative
import tech.dokus.foundation.aura.style.positionPositive

/**
 * Type of cashflow transaction
 */
enum class CashflowType {
    CashIn,
    CashOut
}

/**
 * Status indicator for cashflow transaction types (CASH-IN/CASH-OUT).
 * Uses dot + text pattern.
 *
 * @param type The cashflow transaction type
 * @param modifier Optional modifier for the indicator
 */
@Composable
fun CashflowTypeBadge(
    type: CashflowType,
    modifier: Modifier = Modifier
) {
    val (color, text) = when (type) {
        CashflowType.CashIn -> Pair(
            MaterialTheme.colorScheme.positionPositive,
            stringResource(Res.string.cashflow_cash_in).uppercase()
        )
        CashflowType.CashOut -> Pair(
            MaterialTheme.colorScheme.positionNegative,
            stringResource(Res.string.cashflow_cash_out).uppercase()
        )
    }

    // Dot + text pattern
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color
        )
    }
}
