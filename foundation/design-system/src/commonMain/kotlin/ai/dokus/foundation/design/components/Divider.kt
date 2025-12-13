package ai.dokus.foundation.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A dashed horizontal divider line.
 *
 * @param modifier Modifier for the divider
 * @param color Color of the dashes (default: MaterialTheme.colorScheme.outlineVariant)
 * @param dashWidth Width of each dash (default: 6.dp)
 * @param dashHeight Height/thickness of the dashes (default: 1.dp)
 * @param dashCount Number of dashes to display (default: 30)
 */
@Composable
fun PDashedDivider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.outlineVariant,
    dashWidth: Dp = 6.dp,
    dashHeight: Dp = 1.dp,
    dashCount: Int = 30
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        repeat(dashCount) {
            Box(
                modifier = Modifier
                    .width(dashWidth)
                    .height(dashHeight)
                    .background(color)
            )
        }
    }
}
