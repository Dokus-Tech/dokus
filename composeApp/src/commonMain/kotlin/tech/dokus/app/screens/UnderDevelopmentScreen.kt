package tech.dokus.app.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.under_development_subtitle
import tech.dokus.aura.resources.under_development_title
import tech.dokus.foundation.aura.constrains.withContentPadding
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Animation durations in milliseconds
private const val ANIM_DURATION_PRIMARY = 3000
private const val ANIM_DURATION_SECONDARY = 2500
private const val ANIM_DURATION_TERTIARY = 2800
private const val ANIM_DURATION_PARTICLE = 4000
private const val ANIM_DURATION_FADE = 1000
private const val ANIM_DELAY_SHORT = 300
private const val ANIM_DELAY_MEDIUM = 600

// Canvas drawing constants
private const val GRID_SPACING = 30f
private const val GRID_ALPHA = 0.2f
private const val PARTICLE_COUNT = 12
private const val PARTICLE_RADIUS = 3f
private const val PARTICLE_ORBIT_RADIUS = 100f
private const val PARTICLE_ALPHA_BASE = 0.4f
private const val PARTICLE_ALPHA_RANGE = 0.4f
private const val HEX_RADIUS = 80f
private const val HEX_SIDES = 6
private const val STROKE_WIDTH_THICK = 3f
private const val STROKE_WIDTH_MEDIUM = 2.5f
private const val STROKE_WIDTH_THIN = 2f
private const val STROKE_WIDTH_LINE = 1f
private const val TRIANGLE_RADIUS = 50f
private const val TRIANGLE_SIDES = 3
private const val TRIANGLE_ALPHA = 0.6f
private const val SQUARE_SIZE = 140f
private const val SQUARE_ALPHA = 0.3f
private const val CENTER_DOT_RADIUS = 8f
private const val LINE_LENGTH = 60f
private const val LINE_GRADIENT_ALPHA = 0.8f
private const val FULL_ROTATION_DEGREES = 360f
private const val TWO_PI_MULTIPLIER = 2
private const val CANVAS_SIZE_DP = 280
private const val SPACER_LARGE_DP = 48
private const val SPACER_MEDIUM_DP = 16

@Composable
internal fun UnderDevelopmentScreen() {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    val progress1 = remember { Animatable(0f) }
    val progress2 = remember { Animatable(0f) }
    val progress3 = remember { Animatable(0f) }
    val particleProgress = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Staggered shape animations
        progress1.animateTo(
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(ANIM_DURATION_PRIMARY, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    LaunchedEffect(Unit) {
        progress2.animateTo(
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    ANIM_DURATION_SECONDARY,
                    delayMillis = ANIM_DELAY_SHORT,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    LaunchedEffect(Unit) {
        progress3.animateTo(
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    ANIM_DURATION_TERTIARY,
                    delayMillis = ANIM_DELAY_MEDIUM,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    LaunchedEffect(Unit) {
        particleProgress.animateTo(
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(ANIM_DURATION_PARTICLE, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        )
    }

    LaunchedEffect(Unit) {
        titleAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(ANIM_DURATION_FADE, delayMillis = ANIM_DELAY_SHORT, easing = FastOutSlowInEasing)
        )
    }

    LaunchedEffect(Unit) {
        subtitleAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(ANIM_DURATION_FADE, delayMillis = ANIM_DELAY_MEDIUM, easing = FastOutSlowInEasing)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .withContentPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            // Custom Canvas Animation
            Canvas(
                modifier = Modifier.size(CANVAS_SIZE_DP.dp)
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val centerX = canvasWidth / 2
                val centerY = canvasHeight / 2

                // Background grid pattern (subtle)
                for (i in 0..canvasWidth.toInt() step GRID_SPACING.toInt()) {
                    drawLine(
                        color = surfaceVariant.copy(alpha = GRID_ALPHA),
                        start = Offset(i.toFloat(), 0f),
                        end = Offset(i.toFloat(), canvasHeight),
                        strokeWidth = STROKE_WIDTH_LINE
                    )
                }
                for (i in 0..canvasHeight.toInt() step GRID_SPACING.toInt()) {
                    drawLine(
                        color = surfaceVariant.copy(alpha = GRID_ALPHA),
                        start = Offset(0f, i.toFloat()),
                        end = Offset(canvasWidth, i.toFloat()),
                        strokeWidth = STROKE_WIDTH_LINE
                    )
                }

                // Animated particles orbiting
                for (i in 0 until PARTICLE_COUNT) {
                    val baseAngle = TWO_PI_MULTIPLIER * PI * i / PARTICLE_COUNT
                    val animOffset = particleProgress.value * TWO_PI_MULTIPLIER * PI
                    val angle = baseAngle + animOffset
                    val x = centerX + (PARTICLE_ORBIT_RADIUS * cos(angle)).toFloat()
                    val y = centerY + (PARTICLE_ORBIT_RADIUS * sin(angle)).toFloat()
                    val particleAlpha = PARTICLE_ALPHA_BASE +
                        (sin(angle + animOffset) * PARTICLE_ALPHA_RANGE).toFloat()

                    drawCircle(
                        color = primaryColor.copy(alpha = particleAlpha),
                        radius = PARTICLE_RADIUS,
                        center = Offset(x, y)
                    )
                }

                // Central hexagon outline (animated)
                val hexPath = Path().apply {
                    for (i in 0..HEX_SIDES) {
                        val angle = (PI / TRIANGLE_SIDES * i) - PI / 2
                        val x = centerX + (HEX_RADIUS * cos(angle) * progress1.value).toFloat()
                        val y = centerY + (HEX_RADIUS * sin(angle) * progress1.value).toFloat()
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                }
                drawPath(
                    path = hexPath,
                    color = primaryColor,
                    style = Stroke(width = STROKE_WIDTH_THICK, cap = StrokeCap.Round)
                )

                // Inner triangle (rotating)
                rotate(degrees = particleProgress.value * FULL_ROTATION_DEGREES, pivot = Offset(centerX, centerY)) {
                    val triangleRadius = TRIANGLE_RADIUS * progress2.value
                    val trianglePath = Path().apply {
                        for (i in 0..TRIANGLE_SIDES) {
                            val angle = (TWO_PI_MULTIPLIER * PI / TRIANGLE_SIDES * i) - PI / 2
                            val x = centerX + (triangleRadius * cos(angle)).toFloat()
                            val y = centerY + (triangleRadius * sin(angle)).toFloat()
                            if (i == 0) moveTo(x, y) else lineTo(x, y)
                        }
                    }
                    drawPath(
                        path = trianglePath,
                        color = primaryColor.copy(alpha = TRIANGLE_ALPHA),
                        style = Stroke(width = STROKE_WIDTH_MEDIUM, cap = StrokeCap.Round)
                    )
                }

                // Outer square (pulsing)
                val squareSize = SQUARE_SIZE * progress3.value
                drawRect(
                    color = primaryColor.copy(alpha = SQUARE_ALPHA),
                    topLeft = Offset(centerX - squareSize / 2, centerY - squareSize / 2),
                    size = Size(squareSize, squareSize),
                    style = Stroke(width = STROKE_WIDTH_THIN, cap = StrokeCap.Round)
                )

                // Central dot
                drawCircle(
                    color = primaryColor,
                    radius = CENTER_DOT_RADIUS,
                    center = Offset(centerX, centerY)
                )

                // Connecting lines (animated)
                val lineProgress = progress1.value
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0f),
                            primaryColor.copy(alpha = LINE_GRADIENT_ALPHA)
                        ),
                        start = Offset(centerX, centerY),
                        end = Offset(centerX + LINE_LENGTH * lineProgress, centerY)
                    ),
                    start = Offset(centerX, centerY),
                    end = Offset(centerX + LINE_LENGTH * lineProgress, centerY),
                    strokeWidth = STROKE_WIDTH_THIN,
                    cap = StrokeCap.Round
                )

                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0f),
                            primaryColor.copy(alpha = LINE_GRADIENT_ALPHA)
                        ),
                        start = Offset(centerX, centerY),
                        end = Offset(centerX - LINE_LENGTH * lineProgress, centerY)
                    ),
                    start = Offset(centerX, centerY),
                    end = Offset(centerX - LINE_LENGTH * lineProgress, centerY),
                    strokeWidth = STROKE_WIDTH_THIN,
                    cap = StrokeCap.Round
                )
            }

            Spacer(modifier = Modifier.height(SPACER_LARGE_DP.dp))

            // Title
            Text(
                text = stringResource(Res.string.under_development_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(titleAlpha.value)
            )

            Spacer(modifier = Modifier.height(SPACER_MEDIUM_DP.dp))

            // Subtitle
            Text(
                text = stringResource(Res.string.under_development_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(subtitleAlpha.value)
            )
        }
    }
}
