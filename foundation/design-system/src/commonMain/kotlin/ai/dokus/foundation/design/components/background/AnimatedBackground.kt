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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
 * Mystical aurora effect that follows the user's pointer/touch.
 * Features multiple layers of magical elements including particle trails,
 * energy ribbons, sacred geometry, and ethereal glows with color shifting.
 */
@Composable
fun SpotlightFollowEffect() {
    val infiniteTransition = rememberInfiniteTransition(label = "mysticalAura")

    // Multiple animation phases for rich effect
    val cosmicPulse by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cosmicPulse"
    )

    val energyFlow by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing)
        ),
        label = "energyFlow"
    )

    val colorShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "colorShift"
    )

    val ribbonPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,  // 2π
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing)
        ),
        label = "ribbonPhase"
    )

    // Track pointer position and maintain trail history
    var pointer by remember { mutableStateOf<Offset?>(null) }
    var trailPositions by remember { mutableStateOf(listOf<Pair<Offset, Float>>()) }

    // Aurora colors that shift over time
    val auroraColors = remember(colorShift) {
        listOf(
            Color(0xFF9B59B6).copy(alpha = 0.6f), // Purple
            Color(0xFF3498DB).copy(alpha = 0.5f), // Blue
            Color(0xFF1ABC9C).copy(alpha = 0.4f), // Turquoise
            Color(0xFFE74C3C).copy(alpha = 0.3f), // Red
            Color(0xFFF39C12).copy(alpha = 0.4f), // Orange
            Color(0xFF9B59B6).copy(alpha = 0.5f)  // Back to purple
        )
    }

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

                            // Update trail with fade effect
                            trailPositions = (listOf(pos to 1f) + trailPositions
                                .map { it.copy(second = it.second * 0.92f) })
                                .take(20)
                                .filter { it.second > 0.05f }
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = pointer ?: Offset(size.width / 2f, size.height / 2f)

            // Layer 1: Deep cosmic background aura
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1A0033).copy(alpha = cosmicPulse * 0.15f),
                        Color(0xFF2E0854).copy(alpha = cosmicPulse * 0.08f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.maxDimension * 0.8f
                )
            )

            // Layer 2: Particle trail with sparkles
            trailPositions.forEachIndexed { index, (pos, alpha) ->
                val sparkleSize = (20f - index) * cosmicPulse

                // Glowing trail particles
                drawCircle(
                    color = Color(0xFFE8D4FF).copy(alpha = alpha * 0.5f),
                    radius = sparkleSize * 0.8f,
                    center = pos
                )

                // Star sparkles
                for (i in 0..3) {
                    val angle = (energyFlow + i * 90f) * 0.017453f // Convert to radians
                    val sparkleOffset = Offset(
                        pos.x + cos(angle) * sparkleSize * 1.5f,
                        pos.y + sin(angle) * sparkleSize * 1.5f
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = alpha * cosmicPulse * 0.8f),
                        radius = 1.5f,
                        center = sparkleOffset
                    )
                }
            }

            // Layer 3: Energy ribbons flowing around cursor
            for (ribbon in 0..2) {
                val ribbonOffset = ribbon * 120f
                val ribbonAngle = (energyFlow + ribbonOffset) * 0.017453f
                val ribbonRadius = 60f + sin(ribbonPhase + ribbon) * 20f

                val ribbonStart = Offset(
                    center.x + cos(ribbonAngle) * ribbonRadius,
                    center.y + sin(ribbonAngle) * ribbonRadius
                )

                val ribbonEnd = Offset(
                    center.x + cos(ribbonAngle + 1.57f) * ribbonRadius * 0.7f,
                    center.y + sin(ribbonAngle + 1.57f) * ribbonRadius * 0.7f
                )

                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            auroraColors[ribbon % auroraColors.size],
                            Color.Transparent
                        ),
                        start = ribbonStart,
                        end = ribbonEnd
                    ),
                    start = ribbonStart,
                    end = ribbonEnd,
                    strokeWidth = 3f * cosmicPulse
                )
            }

            // Layer 4: Sacred geometry - rotating triangular pattern
            val geometryRadius = 100f * cosmicPulse
            for (i in 0..5) {
                val angle1 = (energyFlow * 2 + i * 60f) * 0.017453f
                val angle2 = angle1 + 2.0944f // 120 degrees
                val angle3 = angle2 + 2.0944f

                val p1 = Offset(
                    center.x + cos(angle1) * geometryRadius,
                    center.y + sin(angle1) * geometryRadius
                )
                val p2 = Offset(
                    center.x + cos(angle2) * geometryRadius * 0.8f,
                    center.y + sin(angle2) * geometryRadius * 0.8f
                )
                val p3 = Offset(
                    center.x + cos(angle3) * geometryRadius * 0.6f,
                    center.y + sin(angle3) * geometryRadius * 0.6f
                )

                // Draw connecting lines with gradient
                listOf(p1 to p2, p2 to p3, p3 to p1).forEach { (start, end) ->
                    drawLine(
                        color = Color(0xFF9B59B6).copy(alpha = cosmicPulse * 0.15f),
                        start = start,
                        end = end,
                        strokeWidth = 1f
                    )
                }
            }

            // Layer 5: Aurora borealis effect with color waves
            val waveCount = 5
            for (wave in 0 until waveCount) {
                val waveOffset = wave * 40f
                val waveRadius = 150f + waveOffset + sin(ribbonPhase + wave) * 30f

                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            auroraColors[(wave + colorShift.toInt()) % auroraColors.size]
                                .copy(alpha = cosmicPulse * 0.2f * (1f - wave * 0.15f)),
                            auroraColors[(wave + 1 + colorShift.toInt()) % auroraColors.size]
                                .copy(alpha = cosmicPulse * 0.1f * (1f - wave * 0.15f)),
                            Color.Transparent
                        ),
                        center = center,
                        radius = waveRadius
                    )
                )
            }

            // Layer 6: Central mystical core with prismatic effect
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = cosmicPulse * 0.9f),
                        Color(0xFFE8D4FF).copy(alpha = cosmicPulse * 0.6f),
                        Color(0xFF9B59B6).copy(alpha = cosmicPulse * 0.3f),
                        Color(0xFF3498DB).copy(alpha = cosmicPulse * 0.15f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = 40f * cosmicPulse
                ),
                center = center,
                radius = 40f * cosmicPulse
            )

            // Layer 7: Ethereal glow particles orbiting
            val particleCount = 8
            for (p in 0 until particleCount) {
                val particleAngle = (energyFlow * 3 + p * (360f / particleCount)) * 0.017453f
                val orbitRadius = 80f + sin(ribbonPhase * 2 + p) * 20f

                val particlePos = Offset(
                    center.x + cos(particleAngle) * orbitRadius,
                    center.y + sin(particleAngle) * orbitRadius
                )

                // Particle glow
                drawCircle(
                    color = Color(0xFFFFD700).copy(alpha = cosmicPulse * 0.6f),
                    radius = 6f,
                    center = particlePos
                )

                // Particle core
                drawCircle(
                    color = Color.White.copy(alpha = cosmicPulse * 0.9f),
                    radius = 2f,
                    center = particlePos
                )
            }

            // Layer 8: Final mystic highlight burst
            val burstAlpha = if (cosmicPulse > 0.7f) (cosmicPulse - 0.7f) * 3.33f else 0f
            if (burstAlpha > 0f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = burstAlpha * 0.4f),
                            Color(0xFFE8D4FF).copy(alpha = burstAlpha * 0.2f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = 200f * burstAlpha
                    ),
                    center = center,
                    radius = 200f * burstAlpha
                )
            }
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
