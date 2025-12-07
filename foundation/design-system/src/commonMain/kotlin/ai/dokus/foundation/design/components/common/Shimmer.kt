package ai.dokus.foundation.design.components.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Creates an animated shimmer brush effect.
 *
 * @param baseColor The base color of the shimmer (typically a surface variant)
 * @param highlightColor The highlight color that moves across (typically lighter)
 * @return An animated linear gradient brush
 */
@Composable
fun shimmerBrush(
    baseColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    highlightColor: Color = MaterialTheme.colorScheme.surface
): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnimation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    return Brush.linearGradient(
        colors = listOf(
            baseColor,
            highlightColor,
            baseColor
        ),
        start = Offset(translateAnimation - 200f, translateAnimation - 200f),
        end = Offset(translateAnimation, translateAnimation)
    )
}

/**
 * A rectangular shimmer placeholder box.
 *
 * @param modifier Modifier for the box
 * @param shape Shape of the shimmer box
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(4.dp)
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(shimmerBrush())
    )
}

/**
 * A circular shimmer placeholder.
 *
 * @param size Size of the circle
 * @param modifier Additional modifier
 */
@Composable
fun ShimmerCircle(
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(shimmerBrush())
    )
}

/**
 * A text-line shimmer placeholder.
 *
 * @param width Width of the line (use fillMaxWidth() for full width)
 * @param height Height of the line (defaults to typical text line height)
 * @param modifier Additional modifier
 */
@Composable
fun ShimmerLine(
    modifier: Modifier = Modifier,
    height: Dp = 14.dp
) {
    ShimmerBox(
        modifier = modifier.height(height),
        shape = RoundedCornerShape(2.dp)
    )
}

/**
 * A spacer that can be used between shimmer elements.
 */
@Composable
fun ShimmerSpacer(height: Dp = 8.dp) {
    Spacer(modifier = Modifier.height(height))
}
