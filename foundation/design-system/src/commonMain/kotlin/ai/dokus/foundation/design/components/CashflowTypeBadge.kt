package ai.dokus.foundation.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
            "CASH-IN"
        )

        CashflowType.CashOut -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            "CASH-OUT"
        )
    }

    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = textColor,
        modifier = modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 2.dp)
    )
}
