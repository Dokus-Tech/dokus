package tech.dokus.foundation.aura.components.filter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import tech.dokus.foundation.aura.style.statusWarning

/**
 * A subtle filter toggle button with minimal styling.
 *
 * Uses a TextButton with:
 * - Active: light outline background, onSurface text
 * - Inactive: transparent background, onSurfaceVariant text
 *
 * @param selected Whether the toggle is currently selected
 * @param onClick Callback when the toggle is clicked
 * @param label The text label to display
 * @param modifier Optional modifier
 * @param badge Optional count badge (only displayed when > 0, shown in statusWarning color)
 */
@Composable
fun DokusFilterToggle(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    badge: Int? = null
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.textButtonColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0f)
            },
            contentColor = if (selected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        ),
        shape = RoundedCornerShape(6.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium
        )
        if (badge != null && badge > 0) {
            Spacer(Modifier.width(6.dp))
            Text(
                text = badge.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.statusWarning
            )
        }
    }
}

/**
 * A row container for filter toggles with consistent spacing.
 */
@Composable
fun DokusFilterToggleRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}
