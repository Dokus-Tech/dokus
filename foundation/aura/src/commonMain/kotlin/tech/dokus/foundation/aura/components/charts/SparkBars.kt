package tech.dokus.foundation.aura.components.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import tech.dokus.foundation.aura.style.positionNegative

private val BarWidth = 5.dp
private val BarGap = 3.dp
private val BarMinHeight = 2.dp
private const val BarStaggerDelay = 40
private const val BarAnimDuration = 300
private const val OpacityMin = 0.12f
private const val OpacityMax = 0.42f
private val BarTopRadius = 1.5.dp

/**
 * Mini bar chart for dashboard summary.
 *
 * Each bar height is proportional to the max absolute value.
 * Opacity ramps from 0.12 (first) to 0.42 (last).
 * Entrance animation: staggered scaleY from bottom.
 *
 * @param data Values to chart (absolute values used for height)
 * @param height Container height
 * @param color Bar fill color
 */
@Composable
fun SparkBars(
    data: List<Double>,
    modifier: Modifier = Modifier,
    height: Dp = 40.dp,
    color: Color = MaterialTheme.colorScheme.positionNegative,
) {
    if (data.isEmpty()) return

    val maxAbs = data.maxOf { abs(it) }.coerceAtLeast(0.01)

    Row(
        modifier = modifier.height(height),
        horizontalArrangement = Arrangement.spacedBy(BarGap),
        verticalAlignment = Alignment.Bottom,
    ) {
        data.forEachIndexed { index, value ->
            key(index) {
                val fraction = (abs(value) / maxAbs).toFloat().coerceIn(0f, 1f)
                val barHeight = (height - BarMinHeight) * fraction + BarMinHeight
                val opacity = OpacityMin + (OpacityMax - OpacityMin) * (index.toFloat() / (data.size - 1).coerceAtLeast(1))

                val animatable = remember { Animatable(0f) }
                LaunchedEffect(Unit) {
                    animatable.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = BarAnimDuration,
                            delayMillis = index * BarStaggerDelay,
                        ),
                    )
                }

                Box(
                    modifier = Modifier
                        .width(BarWidth)
                        .height(barHeight)
                        .graphicsLayer {
                            scaleY = animatable.value
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                        }
                        .clip(RoundedCornerShape(topStart = BarTopRadius, topEnd = BarTopRadius))
                        .background(color.copy(alpha = opacity)),
                )
            }
        }
    }
}
