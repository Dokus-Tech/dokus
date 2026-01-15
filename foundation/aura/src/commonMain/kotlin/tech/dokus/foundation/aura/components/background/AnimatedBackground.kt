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
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ============================================================================
// CALM PARTICLE FIELD - Subtle ambient background animation
// ============================================================================

// Particle counts per layer
private const val CalmParticleTotal = 130
private const val CalmBackgroundPercent = 0.40f
private const val CalmMiddlePercent = 0.35f

// Drift timing (particles take 30-60 seconds to cross screen)
private const val CalmDriftDurationMs = 60000

// Layer speed multipliers for parallax
private const val CalmBackgroundSpeed = 0.3f
private const val CalmMiddleSpeed = 0.6f
private const val CalmForegroundSpeed = 1.0f

// Breathing animation
private const val CalmBreathingAmplitudeDp = 1f
private const val CalmBreathingPeriodMin = 4f
private const val CalmBreathingPeriodMax = 6f

// Size ranges per layer (dp)
private const val CalmBackgroundSizeMin = 1f
private const val CalmBackgroundSizeMax = 2f
private const val CalmMiddleSizeMin = 2f
private const val CalmMiddleSizeMax = 3f
private const val CalmForegroundSizeMin = 3f
private const val CalmForegroundSizeMax = 4f

// Opacity ranges per layer
private const val CalmBackgroundAlphaMin = 0.15f
private const val CalmBackgroundAlphaMax = 0.25f
private const val CalmMiddleAlphaMin = 0.25f
private const val CalmMiddleAlphaMax = 0.40f
private const val CalmForegroundAlphaMin = 0.40f
private const val CalmForegroundAlphaMax = 0.60f

// Amber particles get a brightness boost
private const val CalmAmberAlphaBoost = 1.3f

// Color palette - Zinc tones with amber accents
private val CalmZinc700 = Color(0xFF3f3f46)
private val CalmZinc600 = Color(0xFF52525b)
private val CalmZinc500 = Color(0xFF71717a)
private val CalmZinc400 = Color(0xFFa1a1aa)
private val CalmAmber500 = Color(0xFFf59e0b)

// Weighted color distribution (30% zinc-700, 30% zinc-600, 25% zinc-500, 5% zinc-400, 10% amber)
private val CalmColorWeights = listOf(
    CalmZinc700 to 30,
    CalmZinc600 to 30,
    CalmZinc500 to 25,
    CalmZinc400 to 5,
    CalmAmber500 to 10
)

// Grid distribution for organic-but-structured feel
private const val CalmGridCols = 12
private const val CalmGridRows = 10
private const val CalmGridJitter = 0.4f // How much particles can deviate from grid

/**
 * A calm particle representing a single floating dot.
 */
private data class CalmParticle(
    val x: Float,           // 0-1 normalized position
    val y: Float,           // 0-1 normalized position
    val layer: Int,         // 0=background, 1=middle, 2=foreground
    val size: Float,        // radius in dp
    val color: Color,
    val isAmber: Boolean,   // amber particles get brightness boost
    val alpha: Float,       // base alpha for this particle
    val driftAngle: Float,  // direction of drift (radians)
    val driftSpeed: Float,  // speed multiplier based on layer
    val breathPhase: Float, // 0-2π phase offset for breathing
    val breathPeriod: Float // 4-6 seconds period
)

/**
 * Selects a color based on weighted distribution.
 */
private fun selectWeightedColor(random: Random): Pair<Color, Boolean> {
    val totalWeight = CalmColorWeights.sumOf { it.second }
    var roll = random.nextInt(totalWeight)
    for ((color, weight) in CalmColorWeights) {
        roll -= weight
        if (roll < 0) {
            return color to (color == CalmAmber500)
        }
    }
    return CalmZinc600 to false
}

/**
 * Generates particles with loosely grid-like distribution.
 */
private fun generateCalmParticles(random: Random): List<CalmParticle> {
    val particles = mutableListOf<CalmParticle>()
    val backgroundCount = (CalmParticleTotal * CalmBackgroundPercent).toInt()
    val middleCount = (CalmParticleTotal * CalmMiddlePercent).toInt()
    val foregroundCount = CalmParticleTotal - backgroundCount - middleCount

    // Distribute particles across a loose grid
    val cellWidth = 1f / CalmGridCols
    val cellHeight = 1f / CalmGridRows
    var particleIndex = 0

    fun addParticle(layer: Int, sizeMin: Float, sizeMax: Float, alphaMin: Float, alphaMax: Float, speed: Float) {
        // Calculate grid position with jitter
        val gridX = particleIndex % CalmGridCols
        val gridY = (particleIndex / CalmGridCols) % CalmGridRows
        val baseX = (gridX + 0.5f) * cellWidth
        val baseY = (gridY + 0.5f) * cellHeight
        val jitterX = (random.nextFloat() - 0.5f) * cellWidth * CalmGridJitter * 2
        val jitterY = (random.nextFloat() - 0.5f) * cellHeight * CalmGridJitter * 2

        val (color, isAmber) = selectWeightedColor(random)
        val baseAlpha = random.nextFloat() * (alphaMax - alphaMin) + alphaMin
        val alpha = if (isAmber) (baseAlpha * CalmAmberAlphaBoost).coerceAtMost(0.8f) else baseAlpha

        particles.add(
            CalmParticle(
                x = (baseX + jitterX).coerceIn(0f, 1f),
                y = (baseY + jitterY).coerceIn(0f, 1f),
                layer = layer,
                size = random.nextFloat() * (sizeMax - sizeMin) + sizeMin,
                color = color,
                isAmber = isAmber,
                alpha = alpha,
                driftAngle = random.nextFloat() * TwoPI,
                driftSpeed = speed,
                breathPhase = random.nextFloat() * TwoPI,
                breathPeriod = random.nextFloat() * (CalmBreathingPeriodMax - CalmBreathingPeriodMin) + CalmBreathingPeriodMin
            )
        )
        particleIndex++
    }

    // Generate particles for each layer
    repeat(backgroundCount) {
        addParticle(0, CalmBackgroundSizeMin, CalmBackgroundSizeMax, CalmBackgroundAlphaMin, CalmBackgroundAlphaMax, CalmBackgroundSpeed)
    }
    repeat(middleCount) {
        addParticle(1, CalmMiddleSizeMin, CalmMiddleSizeMax, CalmMiddleAlphaMin, CalmMiddleAlphaMax, CalmMiddleSpeed)
    }
    repeat(foregroundCount) {
        addParticle(2, CalmForegroundSizeMin, CalmForegroundSizeMax, CalmForegroundAlphaMin, CalmForegroundAlphaMax, CalmForegroundSpeed)
    }

    return particles.sortedBy { it.layer } // Draw background first
}

/**
 * A subtle, floating particle field that feels calm and professional.
 *
 * Design intent: The user is entering an already-ordered space — no story, no drama,
 * just quiet presence. Like dust particles in sunlight — present but not demanding attention.
 *
 * Features:
 * - 130 particles across 3 depth layers with parallax
 * - Loosely grid-like distribution with organic variation
 * - Very slow drift (30-60 seconds to cross screen)
 * - Subtle breathing oscillation per particle
 * - 90% neutral zinc tones, 10% amber accents
 */
@Composable
fun CalmParticleField() {
    val random = remember { Random(System.currentTimeMillis()) }
    val particles = remember { generateCalmParticles(random) }

    val infiniteTransition = rememberInfiniteTransition(label = "calmParticles")

    // Global time for drift animation (0 to 1 over 60 seconds)
    val globalTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = CalmDriftDurationMs, easing = LinearEasing)
        ),
        label = "calmDrift"
    )

    // Breathing time (cycles faster for smooth oscillation)
    val breathTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = TwoPI,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing)
        ),
        label = "calmBreath"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val density = 1f // Assume 1dp = 1px for simplicity; scales naturally

        particles.forEach { particle ->
            // Calculate drift offset (wraps around screen)
            val driftDistance = globalTime * particle.driftSpeed
            val driftX = particle.x + cos(particle.driftAngle) * driftDistance
            val driftY = particle.y + sin(particle.driftAngle) * driftDistance

            // Wrap around edges seamlessly
            val wrappedX = ((driftX % 1f) + 1f) % 1f
            val wrappedY = ((driftY % 1f) + 1f) % 1f

            // Calculate breathing offset (vertical sine wave)
            val breathCycle = breathTime * (1f / particle.breathPeriod) + particle.breathPhase
            val breathOffset = sin(breathCycle) * CalmBreathingAmplitudeDp * density

            // Final position
            val finalX = wrappedX * canvasWidth
            val finalY = wrappedY * canvasHeight + breathOffset

            // Draw simple circle
            drawCircle(
                color = particle.color.copy(alpha = particle.alpha),
                radius = particle.size * density,
                center = Offset(finalX, finalY)
            )
        }
    }
}

// ============================================================================
// WARP JUMP EFFECT - Space warp transition animation
// ============================================================================

// Warp particle count
private const val WarpStarCount = 200

// Animation timing constants
private const val WarpDurationMs = 2500
private const val StarRotationDurationMs = 20000
private const val TunnelPulseDurationMs = 1000

// Animation value constants
private const val BreathingPulseMin = 0.8f
private const val BreathingPulseMax = 1.2f
private const val TwoPI = 6.28f
private const val DegreesToRadians = 0.017453f

// Warp effect thresholds
private const val WarpPhase1End = 0.3f
private const val WarpPhase2Start = 0.2f
private const val WarpPhase3Start = 0.5f
private const val WarpFadeStart = 0.7f
private const val WarpCompletionThreshold = 0.95f

// Color palette for warp effect
private val ColorGold = Color(0xFFD4AF37)
private val ColorSilver = Color(0xFFC0C0C0)
private val ColorLightSilver = Color(0xFFE8E8E8)
private val ColorGainsboro = Color(0xFFDCDCDC)
private val ColorLightGray = Color(0xFFD3D3D3)
private val ColorGray = Color(0xFFBDBDBD)
private val ColorMediumSilver = Color(0xFFB8B8B8)
private val ColorNeutralGray = Color(0xFFCCCCCC)
private val ColorPaleGray = Color(0xFFE0E0E0)

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
