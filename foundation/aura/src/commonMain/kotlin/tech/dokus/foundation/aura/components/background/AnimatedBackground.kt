@file:Suppress("TopLevelPropertyNaming") // Using PascalCase for animation/color constants (Kotlin convention)

package tech.dokus.foundation.aura.components.background

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import tech.dokus.foundation.aura.style.dokusEffects
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

// --- Light sweep constants ---
private const val SweepDurationMs = 16000
private const val SweepWidthFraction = 0.40f

// Math
private const val TwoPI = 6.2831853f

// --- Data classes ---

private class AmbientParticle(
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
    val effects = MaterialTheme.dokusEffects
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
    var particlesInitialized by remember { mutableStateOf(false) }

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
        if (particlesRef.isEmpty() || !particlesInitialized) {
            particlesRef.clear()
            particlesRef.addAll(generateParticles(wDp, hDp, isDark, Random(42)))
            particlesInitialized = true
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
                effects.ambientOrbAmber
            } else {
                effects.ambientOrbNeutral
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
        // Fixed timestep approximation: recomposition is driven by withFrameNanos (~60fps),
        // so using a constant dt avoids jitter from variable frame deltas on particle positions.
        val dtFrame = 1f / 60f
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
                        effects.ambientParticleGold.copy(alpha = lineAlpha)
                    } else {
                        effects.ambientParticleNeutral.copy(alpha = lineAlpha)
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
            val color = if (p.isGold) effects.ambientParticleGold else effects.ambientParticleNeutral
            drawCircle(
                color = color.copy(alpha = p.alpha),
                radius = p.radius * density,
                center = Offset(p.x * density, p.y * density)
            )
        }

        // ── Layer 3: Light Sweep ──
        val sweepStart = sweepProgress * (w + w * SweepWidthFraction * 2) - w * SweepWidthFraction
        val sweepEnd = sweepStart + w * SweepWidthFraction

        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    effects.ambientSweepColor.copy(alpha = effects.ambientSweepAlpha),
                    Color.Transparent
                ),
                startX = sweepStart,
                endX = sweepEnd
            ),
            size = size
        )
    }
}


