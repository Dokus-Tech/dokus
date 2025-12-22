package ai.dokus.foundation.design.components

import ai.dokus.foundation.design.constrains.Constrains
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Generic status badge component that displays status text with colored background.
 * Uses Material Theme colors for consistent theming.
 *
 * @param text The status text to display
 * @param backgroundColor Background color for the badge
 * @param textColor Text color for the badge
 * @param modifier Optional modifier for the badge
 */
@Composable
fun StatusBadge(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = textColor,
        modifier = modifier
            .background(
                color = backgroundColor,
                shape = MaterialTheme.shapes.medium
            )
            .padding(horizontal = Constrains.Spacing.medium, vertical = Constrains.Spacing.xSmall)
    )
}
