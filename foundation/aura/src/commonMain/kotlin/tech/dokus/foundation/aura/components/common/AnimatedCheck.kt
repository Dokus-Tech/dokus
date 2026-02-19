package tech.dokus.foundation.aura.components.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tech.dokus.foundation.aura.style.statusConfirmed

/**
 * Animated success indicator.
 *
 * Draws a circle stroke, then a checkmark. Intended to be generic (not Peppol-specific).
 */
@Composable
fun AnimatedCheck(
    play: Boolean,
    modifier: Modifier = Modifier,
    diameter: Dp = 72.dp,
    color: Color = MaterialTheme.colorScheme.statusConfirmed,
) {
    val easing = remember { CubicBezierEasing(0.65f, 0f, 0.35f, 1f) }
    val circleProgress = remember { Animatable(0f) }
    val checkProgress = remember { Animatable(0f) }
    val glowAlpha = remember { Animatable(0f) }
    val transparentColor = color.copy(alpha = 0f)

    LaunchedEffect(play) {
        circleProgress.snapTo(0f)
        checkProgress.snapTo(0f)
        glowAlpha.snapTo(0f)

        if (!play) return@LaunchedEffect

        delay(200)

        circleProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 500, easing = easing)
        )

        coroutineScope {
            launch {
                glowAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 800)
                )
            }
            launch {
                delay(100)
                checkProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 300, easing = easing)
                )
            }
        }
    }

    Canvas(
        modifier = modifier.size(diameter)
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val minDim = size.minDimension

        val circleStroke = 1.5.dp.toPx()
        val checkStroke = 2.dp.toPx()

        val glowRadius = (minDim / 2f) + 20.dp.toPx()
        val glow = Brush.radialGradient(
            colors = listOf(color.copy(alpha = 0.2f * glowAlpha.value), transparentColor),
            center = center,
            radius = glowRadius,
        )
        drawCircle(
            brush = glow,
            radius = glowRadius,
            center = center,
        )

        val circleRadius = (minDim / 2f) - (circleStroke / 2f)
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360f * circleProgress.value,
            useCenter = false,
            topLeft = Offset(center.x - circleRadius, center.y - circleRadius),
            size = Size(circleRadius * 2f, circleRadius * 2f),
            style = Stroke(width = circleStroke, cap = StrokeCap.Round),
        )

        val checkPath = Path().apply {
            moveTo(size.width * 0.333f, size.height * 0.5f)
            lineTo(size.width * 0.444f, size.height * 0.611f)
            lineTo(size.width * 0.667f, size.height * 0.389f)
        }

        val measure = PathMeasure().apply { setPath(checkPath, false) }
        val outPath = Path()
        measure.getSegment(
            startDistance = 0f,
            stopDistance = measure.length * checkProgress.value,
            destination = outPath,
            startWithMoveTo = true
        )

        drawPath(
            path = outPath,
            color = color,
            style = Stroke(
                width = checkStroke,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}
