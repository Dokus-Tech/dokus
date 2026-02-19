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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalInspectionMode
import tech.dokus.foundation.aura.style.isDark
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// Ambient background: gradient orbs + connected particles + light sweep

// --- Orb constants ---
private const val OrbCount = 5
private const val OrbFadeRadius = 0.6f // Radial gradient fades to transparent at 60%

// Orb sizes (fraction of canvas min dimension)
private val OrbSizeFractions = floatArrayOf(0.45f, 0.55f, 0.50f, 0.60f, 0.40f)
// Orb drift durations (ms)
private val OrbDriftDurations = intArrayOf(26000, 22000, 30000, 28000, 32000)
// Orb starting positions (normalized 0-1)
private val OrbStartX = floatArrayOf(0.2f, 0.7f, 0.5f, 0.15f, 0.85f)
private val OrbStartY = floatArrayOf(0.3f, 0.2f, 0.7f, 0.6f, 0.5f)
// Orb drift range (fraction of canvas)
private val OrbDriftRangeX = floatArrayOf(0.15f, 0.12f, 0.10f, 0.14f, 0.11f)
private val OrbDriftRangeY = floatArrayOf(0.10f, 0.13f, 0.12f, 0.08f, 0.10f)
// Orb colors: true = amber, false = warm-gray
private val OrbIsAmber = booleanArrayOf(true, false, true, false, false)

// Opacity ranges
private const val OrbAlphaLightMin = 0.06f
private const val OrbAlphaLightMax = 0.09f
private const val OrbAlphaDarkMin = 0.03f
private const val OrbAlphaDarkMax = 0.06f

// Orb colors
private val OrbAmberLight = Color(0xFFB8860B)
private val OrbAmberDark = Color(0xFFD4A017)
private val OrbGrayLight = Color(0xFF9C958C)
private val OrbGrayDark = Color(0xFF3D3832)

// --- Particle constants ---
private const val ParticleCount = 40
private const val ParticleGoldChance = 0.30f
private const val ParticleConnectionDistance = 120f // dp
private const val ParticleSpeed = 0.3f // dp/sec base speed

// Particle sizes (dp)
private const val ParticleSizeMin = 0.5f
private const val ParticleSizeMax = 2f

// Particle opacity
private const val ParticleAlphaLightMin = 0.20f
private const val ParticleAlphaLightMax = 0.25f
private const val ParticleAlphaDarkMin = 0.12f
private const val ParticleAlphaDarkMax = 0.30f

// Connection line opacity
private const val ConnectionAlphaLight = 0.08f
private const val ConnectionAlphaDark = 0.06f
private const val ConnectionStrokeWidth = 0.5f // dp

// Particle colors
private val ParticleGoldColor = Color(0xFFB8860B)
private val ParticleNeutralColor = Color(0xFF9C8C7C)

// --- Light sweep constants ---
private const val SweepDurationMs = 16000
private const val SweepWidthFraction = 0.40f
private const val SweepAlphaLight = 0.06f
private const val SweepAlphaDark = 0.03f

// Math
private const val TwoPI = 6.2831853f

// --- Data classes ---

private data class AmbientParticle(
    var x: Float, // dp
    var y: Float, // dp
    val vx: Float, // dp/sec
    val vy: Float, // dp/sec
    val radius: Float, // dp
    val isGold: Boolean,
    val alpha: Float,
)

// --- Generation ---

private fun generateParticles(
    canvasWidthDp: Float,
    canvasHeightDp: Float,
    isDark: Boolean,
    random: Random,
): List<AmbientParticle> {
    val alphaMin = if (isDark) ParticleAlphaDarkMin else ParticleAlphaLightMin
    val alphaMax = if (isDark) ParticleAlphaDarkMax else ParticleAlphaLightMax

    return List(ParticleCount) {
        val isGold = random.nextFloat() < ParticleGoldChance
        AmbientParticle(
            x = random.nextFloat() * canvasWidthDp,
            y = random.nextFloat() * canvasHeightDp,
            vx = (random.nextFloat() - 0.5f) * 2f * ParticleSpeed,
            vy = (random.nextFloat() - 0.5f) * 2f * ParticleSpeed,
            radius = ParticleSizeMin + random.nextFloat() * (ParticleSizeMax - ParticleSizeMin),
            isGold = isGold,
            alpha = alphaMin + random.nextFloat() * (alphaMax - alphaMin),
        )
    }
}

/**
 * Ambient background: 5 gradient orbs drifting slowly, 40 connected particles, light sweep.
 *
 * Creates depth behind the floating glass windows. Theme-adaptive: warmer/bolder in light,
 * more subtle in dark.
 */
@Composable
fun AmbientBackground(modifier: Modifier = Modifier) {
    if (LocalInspectionMode.current) return

    val isDark = MaterialTheme.colorScheme.isDark
    val infiniteTransition = rememberInfiniteTransition(label = "ambient")

    // Orb drift animations (each orb has its own cycle)
    val orbOffsets = Array(OrbCount) { i ->
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = OrbDriftDurations[i], easing = LinearEasing)
            ),
            label = "orb$i"
        )
    }

    // Light sweep
    val sweepProgress by infiniteTransition.animateFloat(
        initialValue = -SweepWidthFraction,
        targetValue = 1f + SweepWidthFraction,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SweepDurationMs, easing = LinearEasing)
        ),
        label = "sweep"
    )

    // Particle animation driven by frame time
    var elapsedSec by remember { mutableFloatStateOf(0f) }
    // Store particles in a mutable list for position updates
    val particlesRef = remember { mutableListOf<AmbientParticle>() }
    var particlesInitialized by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        var lastNanos = 0L
        while (true) {
            withFrameNanos { nanos ->
                if (lastNanos != 0L) {
                    val dt = (nanos - lastNanos) / 1_000_000_000f
                    elapsedSec += dt
                }
                lastNanos = nanos
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val density = this.density
        val wDp = w / density
        val hDp = h / density

        // Initialize particles on first draw (need canvas size)
        if (particlesRef.isEmpty() || particlesInitialized == 0f) {
            particlesRef.clear()
            particlesRef.addAll(generateParticles(wDp, hDp, isDark, Random(42)))
            particlesInitialized = 1f
        }

        // ── Layer 1: Gradient Orbs ──
        val orbAlphaMin = if (isDark) OrbAlphaDarkMin else OrbAlphaLightMin
        val orbAlphaMax = if (isDark) OrbAlphaDarkMax else OrbAlphaLightMax
        val minDim = size.minDimension

        for (i in 0 until OrbCount) {
            val t = orbOffsets[i].value
            // Sine-based drift for smooth looping
            val driftX = sin(t * TwoPI) * OrbDriftRangeX[i]
            val driftY = cos(t * TwoPI * 1.3f) * OrbDriftRangeY[i]
            val cx = (OrbStartX[i] + driftX) * w
            val cy = (OrbStartY[i] + driftY) * h
            val orbRadius = OrbSizeFractions[i] * minDim

            val orbColor = if (OrbIsAmber[i]) {
                if (isDark) OrbAmberDark else OrbAmberLight
            } else {
                if (isDark) OrbGrayDark else OrbGrayLight
            }
            val orbAlpha = orbAlphaMin + (orbAlphaMax - orbAlphaMin) * ((i.toFloat() / OrbCount))

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        orbColor.copy(alpha = orbAlpha),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = orbRadius
                ),
                center = Offset(cx, cy),
                radius = orbRadius
            )
        }

        // ── Layer 2: Connected Particles ──
        // Update positions based on elapsed time (simple velocity bounce)
        val dtFrame = 1f / 60f // approximate, smoothed by withFrameNanos driving recomposition
        val connectionDistPx = ParticleConnectionDistance * density
        val connectionAlpha = if (isDark) ConnectionAlphaDark else ConnectionAlphaLight

        for (p in particlesRef) {
            p.x += p.vx * dtFrame
            p.y += p.vy * dtFrame
            // Bounce off edges
            if (p.x < 0f) p.x = -p.x
            if (p.x > wDp) p.x = 2f * wDp - p.x
            if (p.y < 0f) p.y = -p.y
            if (p.y > hDp) p.y = 2f * hDp - p.y
        }

        // Draw connection lines
        for (i in particlesRef.indices) {
            val a = particlesRef[i]
            val ax = a.x * density
            val ay = a.y * density
            for (j in i + 1 until particlesRef.size) {
                val b = particlesRef[j]
                val bx = b.x * density
                val by = b.y * density
                val dx = ax - bx
                val dy = ay - by
                val dist = sqrt(dx * dx + dy * dy)
                if (dist < connectionDistPx) {
                    val lineAlpha = (1f - dist / connectionDistPx) * connectionAlpha
                    val lineColor = if (a.isGold || b.isGold) {
                        ParticleGoldColor.copy(alpha = lineAlpha)
                    } else {
                        ParticleNeutralColor.copy(alpha = lineAlpha)
                    }
                    drawLine(
                        color = lineColor,
                        start = Offset(ax, ay),
                        end = Offset(bx, by),
                        strokeWidth = ConnectionStrokeWidth * density
                    )
                }
            }
        }

        // Draw particles
        for (p in particlesRef) {
            val color = if (p.isGold) ParticleGoldColor else ParticleNeutralColor
            drawCircle(
                color = color.copy(alpha = p.alpha),
                radius = p.radius * density,
                center = Offset(p.x * density, p.y * density)
            )
        }

        // ── Layer 3: Light Sweep ──
        val sweepAlpha = if (isDark) SweepAlphaDark else SweepAlphaLight
        val sweepColor = if (isDark) OrbAmberDark else Color.White
        val sweepStart = sweepProgress * (w + w * SweepWidthFraction * 2) - w * SweepWidthFraction
        val sweepEnd = sweepStart + w * SweepWidthFraction

        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    sweepColor.copy(alpha = sweepAlpha),
                    Color.Transparent
                ),
                startX = sweepStart,
                endX = sweepEnd
            ),
            size = size
        )
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
private const val DegreesToRadians = 0.017453f

// Warp effect thresholds
private const val WarpPhase1End = 0.3f
private const val WarpPhase2Start = 0.2f
private const val WarpPhase3Start = 0.5f
private const val WarpFadeStart = 0.7f
private const val WarpCompletionThreshold = 0.95f

// Color palette for warp effect
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
    if (LocalInspectionMode.current) return
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
                color = listOf(
                    Color.White,
                    ColorLightSilver,
                    ColorGainsboro,
                    ColorLightGray
                ).random()
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
                    val ringAlpha =
                        ringProgress * (1f - ring * WarpTunnelRingAlphaFade) * WarpTunnelRingAlphaMax

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
                val flashAlpha =
                    ((tunnelPhase - WarpFlashThreshold) / WarpFlashRange) * WarpFlashAlpha
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
