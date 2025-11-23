package ai.dokus.foundation.design.components.background

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Spotlight effect that creates a pulsing golden light from the top of the screen.
 * Features breathing animation, horizontal drift, and radius scaling.
 */
@Composable
fun SpotlightEffect() {
    val infiniteTransition = rememberInfiniteTransition(label = "spotlight")

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "spotlightPulse"
    )

    val drift by infiniteTransition.animateFloat(
        initialValue = -20f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "spotlightDrift"
    )

    val radiusScale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "radiusBreathing"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2f + drift
        val topY = size.height * 0.25f

        // Main spotlight cone from top
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFD4AF37).copy(alpha = pulseAlpha * 0.7f),
                    Color(0xFFFFE4A0).copy(alpha = pulseAlpha * 0.4f),
                    Color(0xFFFFE4A0).copy(alpha = pulseAlpha * 0.15f),
                    Color.Transparent
                ),
                center = Offset(centerX, topY),
                radius = size.maxDimension * radiusScale
            )
        )

        // Secondary bright inner core
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFE4A0).copy(alpha = pulseAlpha * 0.8f),
                    Color(0xFFD4AF37).copy(alpha = pulseAlpha * 0.3f),
                    Color.Transparent
                ),
                center = Offset(centerX, topY),
                radius = size.maxDimension * 0.3f * radiusScale
            )
        )

        // Ambient outer glow
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = pulseAlpha * 0.25f),
                    Color(0xFFD4AF37).copy(alpha = pulseAlpha * 0.1f),
                    Color.Transparent
                ),
                center = Offset(centerX, topY),
                radius = size.maxDimension * 0.9f * radiusScale
            )
        )
    }
}

/**
 * Spotlight that follows the user's pointer/touch.
 * Uses the brand gold color and a soft breathing animation for brightness.
 */
@Composable
fun SpotlightFollowEffect(
    color: Color = Color(0xFFD4AF37),
) {
    val infiniteTransition = rememberInfiniteTransition(label = "spotlightFollow")

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "spotlightFollowPulse"
    )

    // Track pointer position; default to null until first move/touch
    var pointer by remember { mutableStateOf<Offset?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull()
                        if (change != null) {
                            val pos = change.position
                            pointer = pos
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = pointer ?: Offset(size.width / 2f, size.height / 2f)
            val radius = size.maxDimension * 0.45f

            // Bright core
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        color.copy(alpha = pulseAlpha * 0.85f),
                        Color.White.copy(alpha = pulseAlpha * 0.25f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 0.5f
                )
            )

            // Soft outer glow
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        color.copy(alpha = pulseAlpha * 0.35f),
                        color.copy(alpha = pulseAlpha * 0.12f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius
                )
            )
        }
    }
}

/**
 * Enhanced floating bubbles effect with 100 particles featuring complex animations:
 * - Upward floating motion
 * - Horizontal wobble
 * - Circular drift
 * - Multi-layer glow effects
 * - 3D highlights and shadows
 */
@Composable
fun EnhancedFloatingBubbles() {
    val bubbleCount = 100
    val bubbles = remember {
        List(bubbleCount) {
            Bubble(
                x = Random.nextFloat(),
                startY = Random.nextFloat(),
                size = Random.nextFloat() * 3f + 0.5f,
                speed = Random.nextFloat() * 0.5f + 0.2f,
                wobbleAmplitude = Random.nextFloat() * 25f + 10f,
                wobbleFrequency = Random.nextFloat() * 4f + 1f,
                delay = Random.nextFloat() * 20f,
                opacity = Random.nextFloat() * 0.3f + 0.15f,
                angle = Random.nextFloat() * 360f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "bubbles")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 50000, easing = LinearEasing)
        ),
        label = "bubbleTime"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        bubbles.forEach { bubble ->
            val progress = ((time + bubble.delay) * bubble.speed) % 100f / 100f

            val baseY = (bubble.startY + progress) % 1.2f
            val y = canvasHeight * baseY

            val wobblePhase = (time + bubble.delay) * bubble.wobbleFrequency * 0.05f
            val wobble = sin(wobblePhase) * bubble.wobbleAmplitude

            val circularPhase = (time + bubble.delay + bubble.angle) * 0.02f
            val circularDrift = cos(circularPhase) * 15f

            val x = (bubble.x * canvasWidth) + wobble + circularDrift

            val distanceFromCenter = kotlin.math.abs(x - canvasWidth / 2f) / (canvasWidth / 2f)
            val centerFade = 1f - (distanceFromCenter * 0.3f)

            val verticalFade = when {
                baseY < 0.15f -> baseY / 0.15f
                baseY > 1.0f -> kotlin.math.max(0f, (1.2f - baseY) / 0.2f)
                else -> 1f
            }

            val alpha = centerFade * verticalFade * bubble.opacity

            val sizeSpeedFactor = 1f + (1f - bubble.size / 3.5f) * 0.3f
            val adjustedY = y - (progress * 10f * sizeSpeedFactor)

            // Outer glow
            drawCircle(
                color = Color.White.copy(alpha = alpha * 0.06f),
                radius = bubble.size * 5f,
                center = Offset(x, adjustedY)
            )

            // Middle glow
            drawCircle(
                color = Color.White.copy(alpha = alpha * 0.12f),
                radius = bubble.size * 2.5f,
                center = Offset(x, adjustedY)
            )

            // Core glow with gold tint
            drawCircle(
                color = Color(0xFFD4AF37).copy(alpha = alpha * 0.08f),
                radius = bubble.size * 1.3f,
                center = Offset(x, adjustedY)
            )

            // Main bubble
            drawCircle(
                color = Color.White.copy(alpha = alpha * 0.4f),
                radius = bubble.size,
                center = Offset(x, adjustedY)
            )

            // Highlight for 3D effect
            drawCircle(
                color = Color.White.copy(alpha = alpha * 0.7f),
                radius = bubble.size * 0.35f,
                center = Offset(
                    x - bubble.size * 0.3f,
                    adjustedY - bubble.size * 0.3f
                )
            )

            // Subtle shadow/depth
            drawCircle(
                color = Color(0xFF000000).copy(alpha = alpha * 0.08f),
                radius = bubble.size * 0.25f,
                center = Offset(
                    x + bubble.size * 0.35f,
                    adjustedY + bubble.size * 0.35f
                )
            )
        }
    }
}

/**
 * Data class representing a single floating bubble particle.
 */
data class Bubble(
    val x: Float,
    val startY: Float,
    val size: Float,
    val speed: Float,
    val wobbleAmplitude: Float,
    val wobbleFrequency: Float,
    val delay: Float,
    val opacity: Float,
    val angle: Float
)
