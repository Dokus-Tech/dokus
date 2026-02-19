package tech.dokus.foundation.aura.components.text

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToLong
import tech.dokus.foundation.aura.style.positionNegative
import tech.dokus.foundation.aura.style.positionPositive
import tech.dokus.foundation.aura.style.textFaint

private const val TypographicMinus = '\u2212' // −

/**
 * Formatted currency amount with sign-based coloring.
 *
 * Format: German locale — `€1.306,12` (dot thousands, comma decimals).
 * Negative amounts use typographic minus `−` (U+2212).
 *
 * @param value Amount (negative = expense)
 * @param size Font size (default 13sp)
 * @param weight Font weight (default SemiBold/600)
 */
@Composable
fun Amt(
    value: Double,
    modifier: Modifier = Modifier,
    size: TextUnit = 13.sp,
    weight: FontWeight = FontWeight.SemiBold,
) {
    val color = when {
        value < 0 -> MaterialTheme.colorScheme.positionNegative
        value > 0 -> MaterialTheme.colorScheme.positionPositive
        else -> MaterialTheme.colorScheme.textFaint
    }

    Text(
        text = formatEuroCurrency(value),
        modifier = modifier,
        color = color,
        fontSize = size,
        fontWeight = weight,
        fontFamily = MaterialTheme.typography.labelLarge.fontFamily,
        letterSpacing = (-0.02).em,
        maxLines = 1,
    )
}

/**
 * Formats a Double as Euro currency in German locale: `€1.306,12` or `−€1.306,12`.
 */
internal fun formatEuroCurrency(value: Double): String {
    val absValue = abs(value)
    val cents = (absValue * 100).roundToLong()
    val intPart = cents / 100
    val decPart = (cents % 100).toInt()

    val intStr = intPart.toString()
    val withThousands = buildString {
        var count = 0
        for (i in intStr.lastIndex downTo 0) {
            if (count > 0 && count % 3 == 0) append('.')
            append(intStr[i])
            count++
        }
    }.reversed()

    val decStr = decPart.toString().padStart(2, '0')

    return buildString {
        if (value < 0) append(TypographicMinus)
        append('€')
        append(withThousands)
        append(',')
        append(decStr)
    }
}
