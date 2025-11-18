package ai.dokus.foundation.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
 * Uses Material Theme colors with custom styling for green (cash-in) and orange (cash-out).
 *
 * @param type The cashflow transaction type
 * @param modifier Optional modifier for the badge
 */
@Composable
fun CashflowTypeBadge(
    type: CashflowType,
    modifier: Modifier = Modifier
) {
    // Define colors based on type
    val (backgroundColor, textColor, text) = when (type) {
        CashflowType.CashIn -> Triple(
            Color(0xFFDBF1DB), // Light green background
            Color(0xFF4CAF50), // Green text
            "CASH-IN"
        )

        CashflowType.CashOut -> Triple(
            Color(0xFFFFF4E5), // Light orange background
            Color(0xFFFF8431), // Orange text
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
