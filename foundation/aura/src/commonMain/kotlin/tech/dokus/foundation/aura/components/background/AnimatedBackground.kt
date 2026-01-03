@file:Suppress("TopLevelPropertyNaming") // Using PascalCase for animation/color constants (Kotlin convention)

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

// Spotlight animation constants
private const val SpotlightPulseDurationMs = 2000
private const val SpotlightDriftDurationMs = 4000
private const val SpotlightRadiusDurationMs = 2500
private const val SpotlightPulseMin = 0.2f
private const val SpotlightPulseMax = 0.6f
private const val SpotlightDriftMin = -20f
private const val SpotlightDriftMax = 20f
private const val SpotlightRadiusMin = 0.5f
private const val SpotlightRadiusMax = 0.7f
private const val SpotlightPositionY = 0.25f
private const val SpotlightCoreRadiusScale = 0.3f
private const val SpotlightOuterRadiusScale = 0.9f

// Particle system constants
private const val StardustParticleCount = 60
private const val CrystalParticleCount = 25
private const val FireflyParticleCount = 20
private const val WarpStarCount = 200

// Animation timing constants
private const val GlobalTimeDurationMs = 60000
private const val BreathingPulseDurationMs = 4000
private const val MagneticFieldDurationMs = 10000
private const val WarpDurationMs = 2500
private const val StarRotationDurationMs = 20000
private const val TunnelPulseDurationMs = 1000

// Animation value constants
private const val BreathingPulseMin = 0.8f
private const val BreathingPulseMax = 1.2f
private const val TwoPI = 6.28f
private const val DegreesToRadians = 0.017453f
private const val ConstellationMaxDistance = 150f

// Warp effect thresholds
private const val WarpPhase1End = 0.3f
private const val WarpPhase2Start = 0.2f
private const val WarpPhase3Start = 0.5f
private const val WarpFadeStart = 0.7f
private const val WarpCompletionThreshold = 0.95f

// Color palette - Neutral metallic tones (hex codes are standard color notation)
@Suppress("MagicNumber")
private val ColorGold = Color(0xFFD4AF37)

@Suppress("MagicNumber")
private val ColorSilver = Color(0xFFC0C0C0)

@Suppress("MagicNumber")
private val ColorLightSilver = Color(0xFFE8E8E8)

@Suppress("MagicNumber")
private val ColorGainsboro = Color(0xFFDCDCDC)

@Suppress("MagicNumber")
private val ColorLightGray = Color(0xFFD3D3D3)

@Suppress("MagicNumber")
private val ColorMediumLightGray = Color(0xFFC8C8C8)

@Suppress("MagicNumber")
private val ColorGray = Color(0xFFBDBDBD)

@Suppress("MagicNumber")
private val ColorMediumSilver = Color(0xFFB8B8B8)

@Suppress("MagicNumber")
private val ColorDarkGray = Color(0xFFA9A9A9)

@Suppress("MagicNumber")
private val ColorNeutralGray = Color(0xFFCCCCCC)

@Suppress("MagicNumber")
private val ColorPaleGray = Color(0xFFE0E0E0)

@Suppress("MagicNumber")
private val ColorSilverGray = Color(0xFFD4D4D4)

// Animation alphas and multipliers
private const val SpotlightMainAlpha = 0.7f
private const val SpotlightMidAlpha = 0.4f
private const val SpotlightOuterAlpha = 0.15f
private const val SpotlightCoreHighAlpha = 0.8f
private const val SpotlightCoreLowAlpha = 0.3f
private const val SpotlightAmbientHighAlpha = 0.25f
private const val SpotlightAmbientLowAlpha = 0.1f

// Particle generation constants
private const val BackgroundZMax = 0.3f
private const val StardustSizeMax = 1.5f
private const val StardustSizeMin = 0.3f
private const val StardustSpeedMax = 0.15f
private const val StardustSpeedMin = 0.05f
private const val CrystalZMin = 0.3f
private const val CrystalZRange = 0.4f
private const val CrystalSizeMax = 4f
private const val CrystalSizeMin = 2f
private const val CrystalSpeedMax = 0.25f
private const val CrystalSpeedMin = 0.1f
private const val CrystalOrbitMax = 30f
private const val CrystalOrbitMin = 10f
private const val FireflyZMin = 0.7f
private const val FireflyZRange = 0.3f
private const val FireflySizeMax = 2f
private const val FireflySizeMin = 1f
private const val FireflySpeedMax = 0.3f
private const val FireflySpeedMin = 0.15f
private const val FireflyOrbitMax = 50f
private const val FireflyOrbitMin = 20f

// Particle rendering constants
private const val GlobalTimeMax = 360f
private const val TimeOffsetMultiplier = 0.01f
private const val ParallaxFactor = 0.5f
private const val BaseYOffsetMax = 1.3f
private const val BaseYOffsetMin = 0.15f
private const val MagneticInfluenceScale = 20f
private const val WobbleMultiplier = 0.02f
private const val DepthAlphaBase = 0.3f
private const val DepthAlphaRange = 0.7f
private const val BlurRadiusMax = 3f
private const val LowDepthThreshold = 0.2f

// Crystal rendering constants
private const val CrystalRotationMultiplier = 2f
private const val CrystalAngleToDegrees = 57.3f
private const val CrystalPrismSpacing = 72f
private const val CrystalOffsetScale = 0.3f
private const val CrystalPrismAlpha = 0.15f
private const val CrystalCoreScale = 1.2f
private const val CrystalCoreHighAlpha = 0.8f
private const val CrystalCoreLowAlpha = 0.4f
private const val CrystalFacetCount = 3
private const val CrystalFacetSpacing = 120f
private const val CrystalFacetOffsetScale = 0.5f
private const val CrystalFacetAlpha = 0.9f
private const val CrystalFacetRadiusScale = 0.15f

// Firefly rendering constants
private const val FireflyPulseMultiplier = 0.05f
private const val FireflyPulseOffset = 0.5f
private const val FireflyOuterFieldAlpha = 0.1f
private const val FireflyOuterFieldScale = 8f
private const val FireflyMiddleGlowAlpha = 0.3f
private const val FireflyMiddleGlowScale = 3f
private const val FireflyCoreHighAlpha = 0.95f
private const val FireflyCoreMidAlpha = 0.7f
private const val FireflyCoreLowAlpha = 0.3f
private const val FireflySparkCount = 4
private const val FireflySparkMultiplier = 3f
private const val FireflySparkSpacing = 90f
private const val FireflySparkDistanceScale = 2f
private const val FireflySparkAlpha = 0.6f
private const val FireflySparkRadius = 0.5f

// Stardust rendering constants
private const val StardustTwinkleMultiplier = 0.1f
private const val StardustBokehAlpha = 0.1f
private const val StardustCoreAlpha = 0.6f
private const val StardustRayScale = 4f
private const val StardustRayAlpha = 0.3f
private const val StardustRayStrokeWidth = 0.5f

// Star ray angles
private const val RayAngle0 = 0f
private const val RayAngle90 = 90f
private const val RayAngle180 = 180f
private const val RayAngle270 = 270f

// Constellation constants
private const val ConstellationBaseAlpha = 0.15f
private const val ConstellationFadeAlpha = 0.3f
private const val ConstellationStrokeBase = 0.5f

// Warp star generation constants
private const val WarpStarAngleMax = 360f
private const val WarpStarSizeMax = 2f
private const val WarpStarSizeMin = 0.5f
private const val WarpStarSpeedRange = 0.5f
private const val WarpStarSpeedMin = 0.5f

// Warp effect rendering constants
private const val WarpMinProgress = 0.01f
private const val WarpBurstScale = 0.8f
private const val WarpBurstHighAlpha = 0.9f
private const val WarpBurstMidAlpha = 0.5f
private const val WarpBurstLowAlpha = 0.3f
private const val WarpStreakLengthScale = 2f
private const val WarpStreakDistanceScale = 0.5f
private const val WarpStreakHighAlpha = 0.8f
private const val WarpStreakLowAlpha = 0.3f
private const val WarpTunnelRingCount = 8
private const val WarpTunnelRingStep = 0.1f
private const val WarpTunnelRingAlphaFade = 0.1f
private const val WarpTunnelRingAlphaMax = 0.5f
private const val WarpTunnelStrokeBase = 2f
private const val WarpVortexHighAlpha = 0.9f
private const val WarpVortexMidHighAlpha = 0.5f
private const val WarpVortexMidAlpha = 0.3f
private const val WarpVortexLowAlpha = 0.1f
private const val WarpVortexRadiusScale = 0.3f
private const val WarpFlashThreshold = 0.8f
private const val WarpFlashRange = 0.2f
private const val WarpFlashAlpha = 0.9f
private const val WarpFadeAlpha = 0.8f

/**
 * Spotlight effect that creates a pulsing golden light from the top of the screen.
 * Features breathing animation, horizontal drift, and radius scaling.
 */
@Composable
fun SpotlightEffect() {
    val infiniteTransition = rememberInfiniteTransition(label = "spotlight")

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = SpotlightPulseMin,
        targetValue = SpotlightPulseMax,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SpotlightPulseDurationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "spotlightPulse"
    )

    val drift by infiniteTransition.animateFloat(
        initialValue = SpotlightDriftMin,
        targetValue = SpotlightDriftMax,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SpotlightDriftDurationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "spotlightDrift"
    )

    val radiusScale by infiniteTransition.animateFloat(
        initialValue = SpotlightRadiusMin,
        targetValue = SpotlightRadiusMax,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SpotlightRadiusDurationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "radiusBreathing"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2f + drift
        val topY = size.height * SpotlightPositionY

        // Main spotlight cone from top
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    ColorGold.copy(alpha = pulseAlpha * SpotlightMainAlpha),
                    ColorSilver.copy(alpha = pulseAlpha * SpotlightMidAlpha),
                    ColorSilver.copy(alpha = pulseAlpha * SpotlightOuterAlpha),
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
                    ColorPaleGray.copy(alpha = pulseAlpha * SpotlightCoreHighAlpha),
                    ColorGold.copy(alpha = pulseAlpha * SpotlightCoreLowAlpha),
                    Color.Transparent
                ),
                center = Offset(centerX, topY),
                radius = size.maxDimension * SpotlightCoreRadiusScale * radiusScale
            )
        )

        // Ambient outer glow
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = pulseAlpha * SpotlightAmbientHighAlpha),
                    ColorGold.copy(alpha = pulseAlpha * SpotlightAmbientLowAlpha),
                    Color.Transparent
                ),
                center = Offset(centerX, topY),
                radius = size.maxDimension * SpotlightOuterRadiusScale * radiusScale
            )
        )
    }
}

/**
 * Enum representing different types of mystical particles
 */
enum class ParticleType {
    CRYSTAL, // Prismatic crystal fragments
    FIREFLY, // Glowing energy orbs
    STARDUST // Twinkling star particles
}

/**
 * Data class representing a mystical particle with advanced properties
 */
data class MysticalParticle(
    val type: ParticleType,
    val x: Float, // Horizontal position (0-1)
    val y: Float, // Vertical position (0-1)
    val z: Float, // Depth layer (0=back, 1=front)
    val size: Float, // Base particle size
    val speed: Float, // Movement speed multiplier
    val color: Color, // Base color
    val pulsePhase: Float, // Individual animation phase offset
    val orbitRadius: Float // Orbital movement radius
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

        // Layer 1: Background stardust
        repeat(StardustParticleCount) {
            particleList.add(
                MysticalParticle(
                    type = ParticleType.STARDUST,
                    x = Random.nextFloat(),
                    y = Random.nextFloat(),
                    z = Random.nextFloat() * BackgroundZMax,
                    size = Random.nextFloat() * StardustSizeMax + StardustSizeMin,
                    speed = Random.nextFloat() * StardustSpeedMax + StardustSpeedMin,
                    color = listOf(ColorLightSilver, ColorGainsboro, ColorLightGray).random(),
                    pulsePhase = Random.nextFloat() * TwoPI,
                    orbitRadius = 0f
                )
            )
        }

        // Layer 2: Crystal fragments
        repeat(CrystalParticleCount) {
            particleList.add(
                MysticalParticle(
                    type = ParticleType.CRYSTAL,
                    x = Random.nextFloat(),
                    y = Random.nextFloat(),
                    z = Random.nextFloat() * CrystalZRange + CrystalZMin,
                    size = Random.nextFloat() * CrystalSizeMax + CrystalSizeMin,
                    speed = Random.nextFloat() * CrystalSpeedMax + CrystalSpeedMin,
                    color = listOf(ColorSilver, ColorMediumSilver, ColorDarkGray, ColorGray).random(),
                    pulsePhase = Random.nextFloat() * TwoPI,
                    orbitRadius = Random.nextFloat() * CrystalOrbitMax + CrystalOrbitMin
                )
            )
        }

        // Layer 3: Firefly energy orbs
        repeat(FireflyParticleCount) {
            particleList.add(
                MysticalParticle(
                    type = ParticleType.FIREFLY,
                    x = Random.nextFloat(),
                    y = Random.nextFloat(),
                    z = Random.nextFloat() * FireflyZRange + FireflyZMin,
                    size = Random.nextFloat() * FireflySizeMax + FireflySizeMin,
                    speed = Random.nextFloat() * FireflySpeedMax + FireflySpeedMin,
                    color = listOf(ColorPaleGray, ColorNeutralGray, ColorSilverGray).random(),
                    pulsePhase = Random.nextFloat() * TwoPI,
                    orbitRadius = Random.nextFloat() * FireflyOrbitMax + FireflyOrbitMin
                )
            )
        }

        particleList
    }

    val infiniteTransition = rememberInfiniteTransition(label = "mysticalParticles")

    val globalTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = GlobalTimeMax,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = GlobalTimeDurationMs, easing = LinearEasing)
        ),
        label = "globalTime"
    )

    val breathingPulse by infiniteTransition.animateFloat(
        initialValue = BreathingPulseMin,
        targetValue = BreathingPulseMax,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = BreathingPulseDurationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingPulse"
    )

    val magneticField by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = TwoPI,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = MagneticFieldDurationMs, easing = LinearEasing)
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
            val timeOffset = globalTime * TimeOffsetMultiplier * particle.speed

            // Calculate position with depth-based parallax
            val parallaxFactorCalc = 1f - particle.z * ParallaxFactor
            val baseX = particle.x * canvasWidth
            val baseY = ((particle.y + timeOffset) % BaseYOffsetMax - BaseYOffsetMin) * canvasHeight

            // Add magnetic field influence
            val magneticInfluence = sin(magneticField + particle.pulsePhase) * MagneticInfluenceScale * particle.z
            val wobble = cos(globalTime * WobbleMultiplier + particle.pulsePhase) *
                particle.orbitRadius * parallaxFactorCalc

            val x = baseX + wobble + magneticInfluence
            val y = baseY

            // Depth-based opacity and blur effect
            val depthAlpha = DepthAlphaBase + particle.z * DepthAlphaRange
            val blurRadius = (1f - particle.z) * BlurRadiusMax

            when (particle.type) {
                ParticleType.CRYSTAL -> {
                    val position = Offset(x, y)
                    crystalPositions.add(position)

                    // Crystal refraction effect - multiple color layers
                    val rotation = globalTime * CrystalRotationMultiplier +
                        particle.pulsePhase * CrystalAngleToDegrees

                    // Neutral shimmer effect (replacing prismatic colors)
                    val prismColors = listOf(
                        ColorLightSilver,
                        ColorGainsboro,
                        ColorLightGray,
                        ColorMediumLightGray,
                        ColorGray,
                    )

                    prismColors.forEachIndexed { index, prismColor ->
                        val angle = (rotation + index * CrystalPrismSpacing) * DegreesToRadians
                        val offset = Offset(
                            x + cos(angle) * particle.size * CrystalOffsetScale,
                            y + sin(angle) * particle.size * CrystalOffsetScale
                        )

                        drawCircle(
                            color = prismColor.copy(alpha = depthAlpha * CrystalPrismAlpha * breathingPulse),
                            radius = particle.size * CrystalCoreScale,
                            center = offset
                        )
                    }

                    // Crystal core with gradient
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                particle.color.copy(alpha = depthAlpha * CrystalCoreHighAlpha),
                                particle.color.copy(alpha = depthAlpha * CrystalCoreLowAlpha),
                                Color.Transparent
                            ),
                            center = position,
                            radius = particle.size
                        ),
                        center = position,
                        radius = particle.size
                    )

                    // Crystal facet highlights
                    repeat(CrystalFacetCount) { facet ->
                        val facetAngle = (rotation + facet * CrystalFacetSpacing) * DegreesToRadians
                        val facetOffset = Offset(
                            x + cos(facetAngle) * particle.size * CrystalFacetOffsetScale,
                            y + sin(facetAngle) * particle.size * CrystalFacetOffsetScale
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = depthAlpha * CrystalFacetAlpha * breathingPulse),
                            radius = particle.size * CrystalFacetRadiusScale,
                            center = facetOffset
                        )
                    }
                }

                ParticleType.FIREFLY -> {
                    val pulseFactor = sin(globalTime * FireflyPulseMultiplier + particle.pulsePhase) *
                        FireflyPulseOffset + FireflyPulseOffset
                    val glowIntensity = pulseFactor * breathingPulse

                    // Outer energy field
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                particle.color.copy(alpha = depthAlpha * FireflyOuterFieldAlpha * glowIntensity),
                                Color.Transparent
                            ),
                            center = Offset(x, y),
                            radius = particle.size * FireflyOuterFieldScale
                        ),
                        center = Offset(x, y),
                        radius = particle.size * FireflyOuterFieldScale
                    )

                    // Middle glow ring
                    drawCircle(
                        color = particle.color.copy(alpha = depthAlpha * FireflyMiddleGlowAlpha * glowIntensity),
                        radius = particle.size * FireflyMiddleGlowScale,
                        center = Offset(x, y)
                    )

                    // Bright core
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = depthAlpha * FireflyCoreHighAlpha),
                                particle.color.copy(alpha = depthAlpha * FireflyCoreMidAlpha),
                                particle.color.copy(alpha = depthAlpha * FireflyCoreLowAlpha)
                            ),
                            center = Offset(x, y),
                            radius = particle.size * glowIntensity
                        ),
                        center = Offset(x, y),
                        radius = particle.size * glowIntensity
                    )

                    // Energy sparks
                    repeat(FireflySparkCount) { spark ->
                        val sparkAngle = (
                            globalTime * FireflySparkMultiplier +
                                spark * FireflySparkSpacing
                            ) * DegreesToRadians
                        val sparkDistance = particle.size * FireflySparkDistanceScale * glowIntensity
                        drawCircle(
                            color = Color.White.copy(alpha = depthAlpha * FireflySparkAlpha * pulseFactor),
                            radius = FireflySparkRadius,
                            center = Offset(
                                x + cos(sparkAngle) * sparkDistance,
                                y + sin(sparkAngle) * sparkDistance
                            )
                        )
                    }
                }

                ParticleType.STARDUST -> {
                    val twinkle = abs(sin(globalTime * StardustTwinkleMultiplier + particle.pulsePhase))

                    // Bokeh blur for depth
                    if (particle.z < LowDepthThreshold) {
                        drawCircle(
                            color = particle.color.copy(alpha = depthAlpha * StardustBokehAlpha),
                            radius = particle.size * (BlurRadiusMax + blurRadius),
                            center = Offset(x, y)
                        )
                    }

                    // Star core
                    drawCircle(
                        color = particle.color.copy(alpha = depthAlpha * StardustCoreAlpha * twinkle),
                        radius = particle.size,
                        center = Offset(x, y)
                    )

                    // Star rays
                    val rayLength = particle.size * StardustRayScale * twinkle
                    listOf(RayAngle0, RayAngle90, RayAngle180, RayAngle270).forEach { angle ->
                        val rad = angle * DegreesToRadians
                        drawLine(
                            color = particle.color.copy(alpha = depthAlpha * StardustRayAlpha * twinkle),
                            start = Offset(x, y),
                            end = Offset(
                                x + cos(rad) * rayLength,
                                y + sin(rad) * rayLength
                            ),
                            strokeWidth = StardustRayStrokeWidth
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

                if (distance < ConstellationMaxDistance) {
                    val connectionAlpha = (1f - distance / ConstellationMaxDistance) * ConstellationBaseAlpha
                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                ColorSilver.copy(alpha = connectionAlpha),
                                ColorSilver.copy(alpha = connectionAlpha * ConstellationFadeAlpha),
                                ColorSilver.copy(alpha = connectionAlpha)
                            ),
                            start = pos1,
                            end = pos2
                        ),
                        start = pos1,
                        end = pos2,
                        strokeWidth = ConstellationStrokeBase * breathingPulse
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
            durationMillis = WarpDurationMs,
            easing = FastOutSlowInEasing
        ),
        label = "warpProgress"
    )

    // Star field rotation for depth
    val starFieldRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = WarpStarAngleMax,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = StarRotationDurationMs, easing = LinearEasing)
        ),
        label = "starRotation"
    )

    // Tunnel pulsation
    val tunnelPulse by infiniteTransition.animateFloat(
        initialValue = BreathingPulseMin,
        targetValue = BreathingPulseMax,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = TunnelPulseDurationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tunnelPulse"
    )

    // Trigger completion callback
    LaunchedEffect(warpProgress, isActive) {
        if (isActive && warpProgress >= WarpCompletionThreshold) {
            onAnimationComplete()
        }
    }

    // Generate star positions once
    val stars = remember {
        List(WarpStarCount) {
            WarpStar(
                angle = Random.nextFloat() * WarpStarAngleMax,
                distance = Random.nextFloat(),
                size = Random.nextFloat() * WarpStarSizeMax + WarpStarSizeMin,
                speed = Random.nextFloat() * WarpStarSpeedRange + WarpStarSpeedMin,
                color = listOf(Color.White, ColorLightSilver, ColorGainsboro, ColorLightGray).random()
            )
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        if (!isActive && warpProgress <= WarpMinProgress) return@Canvas

        val centerPoint = selectedItemPosition ?: Offset(size.width / 2f, size.height / 2f)

        // Phase 1: Initial burst and card scaling
        if (warpProgress < WarpPhase1End) {
            val burstPhase = (warpProgress / WarpPhase1End)
            val burstRadius = size.minDimension * burstPhase * WarpBurstScale

            // Energy burst from selected item
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = (1f - burstPhase) * WarpBurstHighAlpha),
                        ColorPaleGray.copy(alpha = (1f - burstPhase) * WarpBurstMidAlpha),
                        ColorSilver.copy(alpha = (1f - burstPhase) * WarpBurstLowAlpha),
                        Color.Transparent
                    ),
                    center = centerPoint,
                    radius = burstRadius
                ),
                center = centerPoint,
                radius = burstRadius
            )
        }

        // Phase 2: Star streaks forming
        if (warpProgress > WarpPhase2Start) {
            val streakPhase = ((warpProgress - WarpPhase2Start) / WarpPhase3Start).coerceIn(0f, 1f)
            val warpCenter = Offset(
                centerPoint.x + (size.width / 2f - centerPoint.x) * streakPhase,
                centerPoint.y + (size.height / 2f - centerPoint.y) * streakPhase
            )

            // Draw star streaks
            stars.forEach { star ->
                val angle = (star.angle + starFieldRotation * star.speed) * DegreesToRadians
                val baseDistance = star.distance * size.maxDimension

                // Calculate streak length based on progress
                val streakLength = baseDistance * streakPhase * WarpStreakLengthScale * star.speed
                val startDistance = baseDistance * (1f - streakPhase * WarpStreakDistanceScale)

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
                            star.color.copy(alpha = streakPhase * WarpStreakHighAlpha),
                            star.color.copy(alpha = streakPhase * WarpStreakLowAlpha)
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

        // Phase 3: Warp tunnel
        if (warpProgress > WarpPhase3Start) {
            val tunnelPhase = ((warpProgress - WarpPhase3Start) / WarpPhase3Start).coerceIn(0f, 1f)
            val warpCenter = Offset(size.width / 2f, size.height / 2f)

            // Draw concentric warp rings
            for (ring in 0..WarpTunnelRingCount) {
                val ringProgress = (tunnelPhase - ring * WarpTunnelRingStep).coerceIn(0f, 1f)
                if (ringProgress > 0f) {
                    val ringRadius = size.minDimension * WarpTunnelRingStep * ring * tunnelPulse
                    val ringAlpha = ringProgress * (1f - ring * WarpTunnelRingAlphaFade) * WarpTunnelRingAlphaMax

                    drawCircle(
                        color = ColorGray.copy(alpha = ringAlpha),
                        radius = ringRadius,
                        center = warpCenter,
                        style = Stroke(
                            width = WarpTunnelStrokeBase * (1f + tunnelPhase)
                        )
                    )
                }
            }

            // Central warp vortex
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = tunnelPhase * WarpVortexHighAlpha),
                        ColorPaleGray.copy(alpha = tunnelPhase * WarpVortexMidHighAlpha),
                        ColorNeutralGray.copy(alpha = tunnelPhase * WarpVortexMidAlpha),
                        ColorMediumSilver.copy(alpha = tunnelPhase * WarpVortexLowAlpha),
                        Color.Transparent
                    ),
                    center = warpCenter,
                    radius = size.minDimension * WarpVortexRadiusScale * tunnelPhase
                ),
                center = warpCenter,
                radius = size.minDimension * WarpVortexRadiusScale * tunnelPhase
            )

            // Hyperspace flash at the end
            if (tunnelPhase > WarpFlashThreshold) {
                val flashAlpha = ((tunnelPhase - WarpFlashThreshold) / WarpFlashRange) * WarpFlashAlpha
                drawRect(
                    color = Color.White.copy(alpha = flashAlpha),
                    size = size
                )
            }
        }

        // Overall fade effect
        if (warpProgress > WarpFadeStart) {
            val fadePhase = ((warpProgress - WarpFadeStart) / WarpPhase1End).coerceIn(0f, 1f)
            drawRect(
                color = Color.Black.copy(alpha = fadePhase * WarpFadeAlpha),
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
