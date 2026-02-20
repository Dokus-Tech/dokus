package tech.dokus.foundation.aura.components.status

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.statusConfirmed
import tech.dokus.foundation.aura.style.statusError
import tech.dokus.foundation.aura.style.statusWarning
import tech.dokus.foundation.aura.style.textMuted

private const val PulseMaxScale = 1.8f
private const val PulseMinAlpha = 0f
private const val PulseMaxAlpha = 0.6f

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
 * @param type The status type determining color and fill
 * @param size Dot size (default 5dp)
 * @param pulse When true, renders an animated expanding ring behind the dot
 * @param modifier Optional modifier
 */
@Composable
fun StatusDot(
    type: StatusDotType,
    modifier: Modifier = Modifier,
    size: Dp = Constraints.StatusDot.size,
    pulse: Boolean = false,
) {
    val color = type.toColor()

    if (pulse && type != StatusDotType.Empty) {
        val transition = rememberInfiniteTransition()
        val scale by transition.animateFloat(
            initialValue = 1f,
            targetValue = PulseMaxScale,
            animationSpec = infiniteRepeatable(
                animation = tween(Constraints.StatusDot.pulseDuration),
                repeatMode = RepeatMode.Restart,
            ),
        )
        val alpha by transition.animateFloat(
            initialValue = PulseMaxAlpha,
            targetValue = PulseMinAlpha,
            animationSpec = infiniteRepeatable(
                animation = tween(Constraints.StatusDot.pulseDuration),
                repeatMode = RepeatMode.Restart,
            ),
        )

        Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
            // Pulse ring
            Box(
                modifier = Modifier
                    .size(size)
                    .scale(scale)
                    .background(color.copy(alpha = alpha), CircleShape),
            )
            // Solid dot
            Box(
                modifier = Modifier
                    .size(size)
                    .background(color, CircleShape),
            )
        }
    } else {
        Box(
            modifier = modifier
                .size(size)
                .then(
                    if (type == StatusDotType.Empty) {
                        Modifier.border(1.dp, MaterialTheme.colorScheme.textMuted, CircleShape)
                    } else {
                        Modifier.background(color, CircleShape)
                    },
                ),
        )
    }
}

/**
 * Gets the appropriate color for a StatusDotType.
 */
@Composable
fun StatusDotType.toColor(): Color = when (this) {
    StatusDotType.Confirmed -> MaterialTheme.colorScheme.statusConfirmed
    StatusDotType.Warning -> MaterialTheme.colorScheme.statusWarning
    StatusDotType.Error -> MaterialTheme.colorScheme.statusError
    StatusDotType.Neutral -> MaterialTheme.colorScheme.textMuted
    StatusDotType.Empty -> MaterialTheme.colorScheme.textMuted
}
