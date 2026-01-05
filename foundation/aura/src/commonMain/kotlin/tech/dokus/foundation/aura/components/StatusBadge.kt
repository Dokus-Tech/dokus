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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Generic status indicator using dot + text pattern (Design System v1).
 * No filled backgrounds, no pills - just a colored dot and text.
 *
 * @param text The status text to display
 * @param color Status color for both dot and text
 * @param modifier Optional modifier for the indicator
 */
@Composable
fun StatusBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
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

/**
 * @deprecated Use StatusBadge(text, color) instead. Background colors are no longer used.
 */
@Deprecated(
    message = "Design System v1: Use dot + text pattern instead of filled backgrounds",
    replaceWith = ReplaceWith("StatusBadge(text = text, color = textColor)")
)
@Composable
fun StatusBadge(
    text: String,
    @Suppress("UNUSED_PARAMETER") backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    StatusBadge(text = text, color = textColor, modifier = modifier)
}
