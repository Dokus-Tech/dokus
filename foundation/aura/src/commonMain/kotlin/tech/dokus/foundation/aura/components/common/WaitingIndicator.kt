package tech.dokus.foundation.aura.components.common

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import tech.dokus.foundation.aura.style.textMuted

/**
 * Subtle waiting indicator (Design System v1).
 *
 * A thin outlined circle with a pulsing ring and a small center dot.
 * Intended to be generic (not Peppol-specific).
 */
@Composable
fun WaitingIndicator(
    modifier: Modifier = Modifier,
    diameter: Dp = 72.dp,
) {
    val colors = MaterialTheme.colorScheme
    val outline = colors.outlineVariant
    val dot = colors.textMuted

    val infinite = rememberInfiniteTransition(label = "WaitingIndicator")
    val scale by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val alpha by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Canvas(modifier = modifier.size(diameter)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val stroke = 1.5.dp.toPx()

        val baseRadius = (size.minDimension / 2f) - (stroke * 2f)
        val pulseRadius = baseRadius * scale

        drawCircle(
            color = outline.copy(alpha = alpha),
            radius = pulseRadius,
            center = center,
            style = Stroke(width = stroke),
        )

        drawCircle(
            color = outline,
            radius = baseRadius,
            center = center,
            style = Stroke(width = stroke),
        )

        drawCircle(
            color = dot,
            radius = 4.dp.toPx(),
            center = center,
        )
    }
}
