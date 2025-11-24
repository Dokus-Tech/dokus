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
 * Enum representing different types of mystical particles
 */
enum class ParticleType {
    CRYSTAL,  // Prismatic crystal fragments
    FIREFLY,  // Glowing energy orbs
    STARDUST  // Twinkling star particles
}

/**
 * Data class representing a mystical particle with advanced properties
 */
data class MysticalParticle(
    val type: ParticleType,
    val x: Float,              // Horizontal position (0-1)
    val y: Float,              // Vertical position (0-1)
    val z: Float,              // Depth layer (0=back, 1=front)
    val size: Float,           // Base particle size
    val speed: Float,          // Movement speed multiplier
    val color: Color,          // Base color
    val pulsePhase: Float,     // Individual animation phase offset
    val orbitRadius: Float     // Orbital movement radius
)

/**
 * Advanced mystical particle system featuring multiple particle types:
 * - Crystal fragments with prismatic refraction
 * - Firefly-like energy orbs with pulsing light
 * - Stardust trails with constellation connections
 * - Magnetic field interactions between particles
 * - Depth layers with bokeh and parallax effects
 */
@Composable
fun EnhancedFloatingBubbles() {
    // Create diverse particle types for rich visual effect
    val particles = remember {
        val particleList = mutableListOf<MysticalParticle>()

        // Layer 1: Background stardust (60 particles)
        repeat(60) {
            particleList.add(MysticalParticle(
                type = ParticleType.STARDUST,
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                z = Random.nextFloat() * 0.3f, // Far background
                size = Random.nextFloat() * 1.5f + 0.3f,
                speed = Random.nextFloat() * 0.15f + 0.05f,
                color = listOf(
                    Color(0xFFB8D4E3), // Pale blue
                    Color(0xFFE3D4FF), // Pale purple
                    Color(0xFFFFE4E1)  // Pale pink
                ).random(),
                pulsePhase = Random.nextFloat() * 6.28f,
                orbitRadius = 0f
            ))
        }

        // Layer 2: Crystal fragments (25 particles)
        repeat(25) {
            particleList.add(MysticalParticle(
                type = ParticleType.CRYSTAL,
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                z = Random.nextFloat() * 0.4f + 0.3f, // Mid-ground
                size = Random.nextFloat() * 4f + 2f,
                speed = Random.nextFloat() * 0.25f + 0.1f,
                color = listOf(
                    Color(0xFF7B68EE), // Medium slate blue
                    Color(0xFF9370DB), // Medium purple
                    Color(0xFF40E0D0), // Turquoise
                    Color(0xFFDDA0DD), // Plum
                ).random(),
                pulsePhase = Random.nextFloat() * 6.28f,
                orbitRadius = Random.nextFloat() * 30f + 10f
            ))
        }

        // Layer 3: Firefly energy orbs (20 particles)
        repeat(20) {
            particleList.add(MysticalParticle(
                type = ParticleType.FIREFLY,
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                z = Random.nextFloat() * 0.3f + 0.7f, // Foreground
                size = Random.nextFloat() * 2f + 1f,
                speed = Random.nextFloat() * 0.3f + 0.15f,
                color = listOf(
                    Color(0xFFFFD700), // Gold
                    Color(0xFFFFA500), // Orange
                    Color(0xFF00CED1), // Dark turquoise
                ).random(),
                pulsePhase = Random.nextFloat() * 6.28f,
                orbitRadius = Random.nextFloat() * 50f + 20f
            ))
        }

        particleList
    }

    val infiniteTransition = rememberInfiniteTransition(label = "mysticalParticles")

    val globalTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 60000, easing = LinearEasing)
        ),
        label = "globalTime"
    )

    val breathingPulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingPulse"
    )

    val magneticField by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = LinearEasing)
        ),
        label = "magneticField"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Sort particles by Z-depth for proper layering
        val sortedParticles = particles.sortedBy { it.z }

        // Track particle positions for constellation connections
        val crystalPositions = mutableListOf<Offset>()

        sortedParticles.forEach { particle ->
            val timeOffset = globalTime * 0.01f * particle.speed

            // Calculate position with depth-based parallax
            val parallaxFactor = 1f - particle.z * 0.5f
            val baseX = particle.x * canvasWidth
            val baseY = ((particle.y + timeOffset) % 1.3f - 0.15f) * canvasHeight

            // Add magnetic field influence
            val magneticInfluence = sin(magneticField + particle.pulsePhase) * 20f * particle.z
            val wobble = cos(globalTime * 0.02f + particle.pulsePhase) * particle.orbitRadius * parallaxFactor

            val x = baseX + wobble + magneticInfluence
            val y = baseY

            // Depth-based opacity and blur effect
            val depthAlpha = 0.3f + particle.z * 0.7f
            val blurRadius = (1f - particle.z) * 3f

            when (particle.type) {
                ParticleType.CRYSTAL -> {
                    val position = Offset(x, y)
                    crystalPositions.add(position)

                    // Crystal refraction effect - multiple color layers
                    val rotation = globalTime * 2f + particle.pulsePhase * 57.3f

                    // Prismatic light splitting
                    val prismColors = listOf(
                        Color(0xFFFF6B6B), // Red
                        Color(0xFF4ECDC4), // Cyan
                        Color(0xFF95E77E), // Green
                        Color(0xFFF7DC6F), // Yellow
                        Color(0xFFBB8FCE), // Purple
                    )

                    prismColors.forEachIndexed { index, prismColor ->
                        val angle = (rotation + index * 72f) * 0.017453f
                        val offset = Offset(
                            x + cos(angle) * particle.size * 0.3f,
                            y + sin(angle) * particle.size * 0.3f
                        )

                        drawCircle(
                            color = prismColor.copy(alpha = depthAlpha * 0.15f * breathingPulse),
                            radius = particle.size * 1.2f,
                            center = offset
                        )
                    }

                    // Crystal core with gradient
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                particle.color.copy(alpha = depthAlpha * 0.8f),
                                particle.color.copy(alpha = depthAlpha * 0.4f),
                                Color.Transparent
                            ),
                            center = position,
                            radius = particle.size
                        ),
                        center = position,
                        radius = particle.size
                    )

                    // Crystal facet highlights
                    repeat(3) { facet ->
                        val facetAngle = (rotation + facet * 120f) * 0.017453f
                        val facetOffset = Offset(
                            x + cos(facetAngle) * particle.size * 0.5f,
                            y + sin(facetAngle) * particle.size * 0.5f
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = depthAlpha * 0.9f * breathingPulse),
                            radius = particle.size * 0.15f,
                            center = facetOffset
                        )
                    }
                }

                ParticleType.FIREFLY -> {
                    val pulseFactor = sin(globalTime * 0.05f + particle.pulsePhase) * 0.5f + 0.5f
                    val glowIntensity = pulseFactor * breathingPulse

                    // Outer energy field
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                particle.color.copy(alpha = depthAlpha * 0.1f * glowIntensity),
                                Color.Transparent
                            ),
                            center = Offset(x, y),
                            radius = particle.size * 8f
                        ),
                        center = Offset(x, y),
                        radius = particle.size * 8f
                    )

                    // Middle glow ring
                    drawCircle(
                        color = particle.color.copy(alpha = depthAlpha * 0.3f * glowIntensity),
                        radius = particle.size * 3f,
                        center = Offset(x, y)
                    )

                    // Bright core
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = depthAlpha * 0.95f),
                                particle.color.copy(alpha = depthAlpha * 0.7f),
                                particle.color.copy(alpha = depthAlpha * 0.3f)
                            ),
                            center = Offset(x, y),
                            radius = particle.size * glowIntensity
                        ),
                        center = Offset(x, y),
                        radius = particle.size * glowIntensity
                    )

                    // Energy sparks
                    repeat(4) { spark ->
                        val sparkAngle = (globalTime * 3f + spark * 90f) * 0.017453f
                        val sparkDistance = particle.size * 2f * glowIntensity
                        drawCircle(
                            color = Color.White.copy(alpha = depthAlpha * 0.6f * pulseFactor),
                            radius = 0.5f,
                            center = Offset(
                                x + cos(sparkAngle) * sparkDistance,
                                y + sin(sparkAngle) * sparkDistance
                            )
                        )
                    }
                }

                ParticleType.STARDUST -> {
                    val twinkle = kotlin.math.abs(sin(globalTime * 0.1f + particle.pulsePhase))

                    // Bokeh blur for depth
                    if (particle.z < 0.2f) {
                        drawCircle(
                            color = particle.color.copy(alpha = depthAlpha * 0.1f),
                            radius = particle.size * (3f + blurRadius),
                            center = Offset(x, y)
                        )
                    }

                    // Star core
                    drawCircle(
                        color = particle.color.copy(alpha = depthAlpha * 0.6f * twinkle),
                        radius = particle.size,
                        center = Offset(x, y)
                    )

                    // Star rays
                    val rayLength = particle.size * 4f * twinkle
                    listOf(0f, 90f, 180f, 270f).forEach { angle ->
                        val rad = angle * 0.017453f
                        drawLine(
                            color = particle.color.copy(alpha = depthAlpha * 0.3f * twinkle),
                            start = Offset(x, y),
                            end = Offset(
                                x + cos(rad) * rayLength,
                                y + sin(rad) * rayLength
                            ),
                            strokeWidth = 0.5f
                        )
                    }
                }
            }
        }

        // Draw constellation connections between nearby crystals
        crystalPositions.forEachIndexed { i, pos1 ->
            crystalPositions.drop(i + 1).forEach { pos2 ->
                val distance = kotlin.math.sqrt(
                    (pos2.x - pos1.x) * (pos2.x - pos1.x) +
                    (pos2.y - pos1.y) * (pos2.y - pos1.y)
                )

                if (distance < 150f) {
                    val connectionAlpha = (1f - distance / 150f) * 0.15f
                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF9370DB).copy(alpha = connectionAlpha),
                                Color(0xFF9370DB).copy(alpha = connectionAlpha * 0.3f),
                                Color(0xFF9370DB).copy(alpha = connectionAlpha)
                            ),
                            start = pos1,
                            end = pos2
                        ),
                        start = pos1,
                        end = pos2,
                        strokeWidth = 0.5f * breathingPulse
                    )
                }
            }
        }
    }
}

