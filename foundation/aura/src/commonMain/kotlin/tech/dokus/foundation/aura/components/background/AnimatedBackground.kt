package tech.dokus.foundation.aura.components.background

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
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
                    Color(0xFFC0C0C0).copy(alpha = pulseAlpha * 0.4f),
                    Color(0xFFC0C0C0).copy(alpha = pulseAlpha * 0.15f),
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
                    Color(0xFFE0E0E0).copy(alpha = pulseAlpha * 0.8f),
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
            particleList.add(
                MysticalParticle(
                    type = ParticleType.STARDUST,
                    x = Random.nextFloat(),
                    y = Random.nextFloat(),
                    z = Random.nextFloat() * 0.3f, // Far background
                    size = Random.nextFloat() * 1.5f + 0.3f,
                    speed = Random.nextFloat() * 0.15f + 0.05f,
                    color = listOf(
                        Color(0xFFE8E8E8), // Light silver
                        Color(0xFFDCDCDC), // Gainsboro
                        Color(0xFFD3D3D3)  // Light gray
                    ).random(),
                    pulsePhase = Random.nextFloat() * 6.28f,
                    orbitRadius = 0f
                )
            )
        }

        // Layer 2: Crystal fragments (25 particles)
        repeat(25) {
            particleList.add(
                MysticalParticle(
                    type = ParticleType.CRYSTAL,
                    x = Random.nextFloat(),
                    y = Random.nextFloat(),
                    z = Random.nextFloat() * 0.4f + 0.3f, // Mid-ground
                    size = Random.nextFloat() * 4f + 2f,
                    speed = Random.nextFloat() * 0.25f + 0.1f,
                    color = listOf(
                        Color(0xFFC0C0C0), // Silver
                        Color(0xFFB8B8B8), // Medium silver
                        Color(0xFFA9A9A9), // Dark gray
                        Color(0xFFBDBDBD), // Gray
                    ).random(),
                    pulsePhase = Random.nextFloat() * 6.28f,
                    orbitRadius = Random.nextFloat() * 30f + 10f
                )
            )
        }

        // Layer 3: Firefly energy orbs (20 particles)
        repeat(20) {
            particleList.add(
                MysticalParticle(
                    type = ParticleType.FIREFLY,
                    x = Random.nextFloat(),
                    y = Random.nextFloat(),
                    z = Random.nextFloat() * 0.3f + 0.7f, // Foreground
                    size = Random.nextFloat() * 2f + 1f,
                    speed = Random.nextFloat() * 0.3f + 0.15f,
                    color = listOf(
                        Color(0xFFE0E0E0), // Light gray
                        Color(0xFFCCCCCC), // Silver gray
                        Color(0xFFD4D4D4), // Neutral silver
                    ).random(),
                    pulsePhase = Random.nextFloat() * 6.28f,
                    orbitRadius = Random.nextFloat() * 50f + 20f
                )
            )
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

                    // Neutral shimmer effect (replacing prismatic colors)
                    val prismColors = listOf(
                        Color(0xFFE8E8E8), // Light silver
                        Color(0xFFDCDCDC), // Gainsboro
                        Color(0xFFD3D3D3), // Light gray
                        Color(0xFFC8C8C8), // Medium light gray
                        Color(0xFFBDBDBD), // Gray
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
                    val twinkle = abs(sin(globalTime * 0.1f + particle.pulsePhase))

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
                val distance = sqrt(
                    (pos2.x - pos1.x) * (pos2.x - pos1.x) +
                            (pos2.y - pos1.y) * (pos2.y - pos1.y)
                )

                if (distance < 150f) {
                    val connectionAlpha = (1f - distance / 150f) * 0.15f
                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFC0C0C0).copy(alpha = connectionAlpha), // Neutral silver
                                Color(0xFFC0C0C0).copy(alpha = connectionAlpha * 0.3f),
                                Color(0xFFC0C0C0).copy(alpha = connectionAlpha)
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

/**
 * Space warp jump animation effect for transitioning between screens.
 * Creates a hyperspace-like effect with star streaks, warp tunnel, and energy burst.
 *
 * @param isActive Whether the warp animation is currently active
 * @param selectedItemPosition The position of the selected item to warp from
 * @param onAnimationComplete Callback when the animation completes
 */
@Composable
fun WarpJumpEffect(
    isActive: Boolean,
    selectedItemPosition: Offset? = null,
    onAnimationComplete: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "warpStars")

    // Animation values for the warp effect
    val warpProgress by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(
            durationMillis = 2500,
            easing = FastOutSlowInEasing
        ),
        label = "warpProgress"
    )

    // Star field rotation for depth
    val starFieldRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing)
        ),
        label = "starRotation"
    )

    // Tunnel pulsation
    val tunnelPulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tunnelPulse"
    )

    // Trigger completion callback
    LaunchedEffect(warpProgress, isActive) {
        if (isActive && warpProgress >= 0.95f) {
            onAnimationComplete()
        }
    }

    // Generate star positions once
    val stars = remember {
        List(200) {
            WarpStar(
                angle = Random.nextFloat() * 360f,
                distance = Random.nextFloat(),
                size = Random.nextFloat() * 2f + 0.5f,
                speed = Random.nextFloat() * 0.5f + 0.5f,
                color = listOf(
                    Color.White,
                    Color(0xFFE8E8E8), // Light silver
                    Color(0xFFDCDCDC), // Gainsboro
                    Color(0xFFD3D3D3)  // Light gray
                ).random()
            )
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        if (!isActive && warpProgress <= 0.01f) return@Canvas

        val centerPoint = selectedItemPosition ?: Offset(size.width / 2f, size.height / 2f)

        // Phase 1: Initial burst and card scaling (0-0.2)
        if (warpProgress < 0.3f) {
            val burstPhase = (warpProgress / 0.3f)
            val burstRadius = size.minDimension * burstPhase * 0.8f

            // Energy burst from selected item
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = (1f - burstPhase) * 0.9f),
                        Color(0xFFE0E0E0).copy(alpha = (1f - burstPhase) * 0.5f),
                        Color(0xFFC0C0C0).copy(alpha = (1f - burstPhase) * 0.3f),
                        Color.Transparent
                    ),
                    center = centerPoint,
                    radius = burstRadius
                ),
                center = centerPoint,
                radius = burstRadius
            )
        }

        // Phase 2: Star streaks forming (0.2-0.7)
        if (warpProgress > 0.2f) {
            val streakPhase = ((warpProgress - 0.2f) / 0.5f).coerceIn(0f, 1f)
            val warpCenter = Offset(
                centerPoint.x + (size.width / 2f - centerPoint.x) * streakPhase,
                centerPoint.y + (size.height / 2f - centerPoint.y) * streakPhase
            )

            // Draw star streaks
            stars.forEach { star ->
                val angle = (star.angle + starFieldRotation * star.speed) * 0.017453f
                val baseDistance = star.distance * size.maxDimension

                // Calculate streak length based on progress
                val streakLength = baseDistance * streakPhase * 2f * star.speed
                val startDistance = baseDistance * (1f - streakPhase * 0.5f)

                val startPoint = Offset(
                    warpCenter.x + cos(angle) * startDistance,
                    warpCenter.y + sin(angle) * startDistance
                )

                val endPoint = Offset(
                    warpCenter.x + cos(angle) * (startDistance + streakLength),
                    warpCenter.y + sin(angle) * (startDistance + streakLength)
                )

                // Draw streak with gradient
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            star.color.copy(alpha = 0f),
                            star.color.copy(alpha = streakPhase * 0.8f),
                            star.color.copy(alpha = streakPhase * 0.3f)
                        ),
                        start = startPoint,
                        end = endPoint
                    ),
                    start = startPoint,
                    end = endPoint,
                    strokeWidth = star.size * (1f + streakPhase)
                )
            }
        }

        // Phase 3: Warp tunnel (0.5-1.0)
        if (warpProgress > 0.5f) {
            val tunnelPhase = ((warpProgress - 0.5f) / 0.5f).coerceIn(0f, 1f)
            val warpCenter = Offset(size.width / 2f, size.height / 2f)

            // Draw concentric warp rings
            for (ring in 0..8) {
                val ringProgress = (tunnelPhase - ring * 0.1f).coerceIn(0f, 1f)
                if (ringProgress > 0f) {
                    val ringRadius = size.minDimension * 0.1f * ring * tunnelPulse
                    val ringAlpha = ringProgress * (1f - ring * 0.1f) * 0.5f

                    drawCircle(
                        color = Color(0xFFBDBDBD).copy(alpha = ringAlpha),
                        radius = ringRadius,
                        center = warpCenter,
                        style = Stroke(
                            width = 2f * (1f + tunnelPhase)
                        )
                    )
                }
            }

            // Central warp vortex
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = tunnelPhase * 0.9f),
                        Color(0xFFE0E0E0).copy(alpha = tunnelPhase * 0.5f),
                        Color(0xFFCCCCCC).copy(alpha = tunnelPhase * 0.3f),
                        Color(0xFFB8B8B8).copy(alpha = tunnelPhase * 0.1f),
                        Color.Transparent
                    ),
                    center = warpCenter,
                    radius = size.minDimension * 0.3f * tunnelPhase
                ),
                center = warpCenter,
                radius = size.minDimension * 0.3f * tunnelPhase
            )

            // Hyperspace flash at the end
            if (tunnelPhase > 0.8f) {
                val flashAlpha = ((tunnelPhase - 0.8f) / 0.2f) * 0.9f
                drawRect(
                    color = Color.White.copy(alpha = flashAlpha),
                    size = size
                )
            }
        }

        // Overall fade effect
        if (warpProgress > 0.7f) {
            val fadePhase = ((warpProgress - 0.7f) / 0.3f).coerceIn(0f, 1f)
            drawRect(
                color = Color.Black.copy(alpha = fadePhase * 0.8f),
                size = size
            )
        }
    }
}

/**
 * Data class representing a star in the warp field
 */
private data class WarpStar(
    val angle: Float,
    val distance: Float,
    val size: Float,
    val speed: Float,
    val color: Color
)

