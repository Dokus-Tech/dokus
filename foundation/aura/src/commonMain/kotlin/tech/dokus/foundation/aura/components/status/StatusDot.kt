package tech.dokus.foundation.aura.components.status

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import tech.dokus.foundation.aura.style.statusConfirmed
import tech.dokus.foundation.aura.style.statusError
import tech.dokus.foundation.aura.style.statusWarning
import tech.dokus.foundation.aura.style.textMuted

/**
 * Status indicator type for visual status representation.
 */
enum class StatusDotType {
    /** Green - Success/Active/Verified */
    Confirmed,
    /** Amber - Warning/Attention needed */
    Warning,
    /** Red - Error/Failed */
    Error,
    /** Gray solid - Neutral/Processing/Pending */
    Neutral,
    /** Gray hollow - Empty/Not configured */
    Empty
}

/**
 * A small circular status indicator dot.
 *
 * Design System v1: Uses colored dots for status indication.
 * - Confirmed: Green (#16A34A)
 * - Warning: Amber (#D97706)
 * - Error: Red (#B91C1C)
 * - Neutral: Gray filled
 * - Empty: Gray hollow (border only)
 *
 * @param type The status type determining color and fill
 * @param size Dot size (default 6.dp)
 * @param modifier Optional modifier
 */
@Composable
fun StatusDot(
    type: StatusDotType,
    modifier: Modifier = Modifier,
    size: Dp = 6.dp,
) {
    val color = when (type) {
        StatusDotType.Confirmed -> MaterialTheme.colorScheme.statusConfirmed
        StatusDotType.Warning -> MaterialTheme.colorScheme.statusWarning
        StatusDotType.Error -> MaterialTheme.colorScheme.statusError
        StatusDotType.Neutral -> MaterialTheme.colorScheme.textMuted
        StatusDotType.Empty -> Color.Transparent
    }

    val borderColor = if (type == StatusDotType.Empty) {
        MaterialTheme.colorScheme.textMuted
    } else {
        Color.Transparent
    }

    Box(
        modifier = modifier
            .size(size)
            .then(
                if (type == StatusDotType.Empty) {
                    Modifier.border(1.dp, borderColor, CircleShape)
                } else {
                    Modifier.background(color, CircleShape)
                }
            )
    )
}

/**
 * Gets the appropriate StatusDotType color for use in text or other elements.
 */
@Composable
fun StatusDotType.toColor(): Color = when (this) {
    StatusDotType.Confirmed -> MaterialTheme.colorScheme.statusConfirmed
    StatusDotType.Warning -> MaterialTheme.colorScheme.statusWarning
    StatusDotType.Error -> MaterialTheme.colorScheme.statusError
    StatusDotType.Neutral -> MaterialTheme.colorScheme.textMuted
    StatusDotType.Empty -> MaterialTheme.colorScheme.textMuted
}
