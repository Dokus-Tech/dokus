@file:Suppress("TopLevelPropertyNaming")

package tech.dokus.foundation.aura.components.common

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.withFrameNanos
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import tech.dokus.foundation.aura.local.LocalReduceMotion
import tech.dokus.foundation.aura.style.dokusEffects
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

// ============================================================================
// DOKUS LOADER — Chaos-to-cube particle morph animation
// ============================================================================

// Animation cycle timing
private const val CycleDurationSec = 8f

// Cycle phase boundaries (fractions of total cycle)
private const val FormingEnd = 0.20f
private const val HoldingEnd = 0.55f
private const val DissolvingEnd = 0.75f

// 3D projection
private const val FocalLength = 4.0f

// Rotation
private const val BaseRotationSpeed = 0.4f
private const val FormedRotationBoost = 1.5f
private const val XTiltBase = 0.3f
private const val XOscillationAmplitude = 0.08f
private const val XOscillationSpeed = 1.2f

// Particle sphere (chaos state)
private const val SphereRadiusMin = 0.8f
private const val SphereRadiusMax = 1.4f

// Cube grid
private const val CubeHalfSize = 0.65f

// Breathing wave (formed state)
private const val BreathAmplitude = 0.025f
private const val BreathSpeed = 3f
private const val BreathSpatialFreq = 0.8f

// Visual
private const val MinParticleAlpha = 0.15f
private const val MaxParticleAlpha = 0.95f
private const val BaseParticleSizeDp = 2.2f
private const val DepthScaleMin = 0.4f
private const val DepthAlphaMin = 0.3f
private const val MinPerspDivisor = 0.1f

// Math
private const val TwoPI = 6.2831853f

// Reduced motion fallback
private const val ReducedMotionPulseMin = 0.3f
private const val ReducedMotionPulseMax = 0.8f
private const val ReducedMotionPulseDurationMs = 1500
private const val ReducedMotionDotScale = 0.06f
private const val ReducedMotionDotSpacingScale = 0.15f

/**
 * A single particle with chaos (sphere) and cube-grid target positions.
 */
private data class LoaderParticle(
    val chaosX: Float,
    val chaosY: Float,
    val chaosZ: Float,
    val cubeX: Float,
    val cubeY: Float,
    val cubeZ: Float,
    val colorWeight: Float,
    val phaseOffset: Float,
)

/**
 * Theme-resolved color palette for the loader.
 */
private data class LoaderColors(
    val primary: Color,
    val secondary: Color,
    val accent: Color,
    val baseAlpha: Float,
)

/**
 * Generates particles with chaos positions (sphere) and cube target positions (grid).
 */
private fun generateParticles(count: Int, random: Random): List<LoaderParticle> {
    // Cube grid: find the nearest cube root for grid dimensions
    val gridN = kotlin.math.ceil(count.toFloat().pow(1f / 3f)).toInt().coerceAtLeast(2)
    val spacing = (CubeHalfSize * 2f) / (gridN - 1).coerceAtLeast(1)

    return List(count) { i ->
        // Chaos: random point in sphere using spherical coordinates with cube-root radius
        val u = random.nextFloat()
        val radius = SphereRadiusMin + (SphereRadiusMax - SphereRadiusMin) * u.pow(1f / 3f)
        val theta = random.nextFloat() * TwoPI
        val phi = kotlin.math.acos(2f * random.nextFloat() - 1f)
        val chaosX = radius * sin(phi) * cos(theta)
        val chaosY = radius * sin(phi) * sin(theta)
        val chaosZ = radius * cos(phi)

        // Cube: grid position (wraps when count > gridN^3)
        val ix = i % gridN
        val iy = (i / gridN) % gridN
        val iz = (i / (gridN * gridN)) % gridN
        val cubeX = -CubeHalfSize + ix * spacing
        val cubeY = -CubeHalfSize + iy * spacing
        val cubeZ = -CubeHalfSize + iz * spacing

        // Color weight: 80% low (zinc), 15% mid, 5% high (gold)
        val roll = random.nextFloat()
        val colorWeight = when {
            roll < 0.80f -> random.nextFloat() * 0.2f
            roll < 0.95f -> 0.3f + random.nextFloat() * 0.3f
            else -> 0.7f + random.nextFloat() * 0.3f
        }

        LoaderParticle(
            chaosX = chaosX, chaosY = chaosY, chaosZ = chaosZ,
            cubeX = cubeX, cubeY = cubeY, cubeZ = cubeZ,
            colorWeight = colorWeight,
            phaseOffset = random.nextFloat() * TwoPI,
        )
    }
}

/** Cubic ease in-out. */
private fun cubicEaseInOut(t: Float): Float {
    return if (t < 0.5f) 4f * t * t * t
    else 1f - (-2f * t + 2f).let { it * it * it } / 2f
}

/** Linearly interpolate between two colors. */
private fun lerpColor(a: Color, b: Color, fraction: Float): Color =
    androidx.compose.ui.graphics.lerp(a, b, fraction)

/**
 * Standard sizes for the Dokus loading animation.
 *
 * Enforces design consistency — screens must pick one of these rather than arbitrary sizes.
 */
enum class DokusLoaderSize(internal val canvasDp: Dp, internal val particles: Int) {
    /** 24.dp — inline next to text, dropdowns, list-loading indicators. */
    Small(canvasDp = 24.dp, particles = 100),

    /** 72.dp — section or panel loading. */
    Medium(canvasDp = 72.dp, particles = 200),

    /** 120.dp — full-screen / page loading. */
    Large(canvasDp = 120.dp, particles = 400),
}

/**
 * Dokus branded loading animation — particles morph from chaos (sphere) into an ordered cube.
 *
 * Reinforces the "chaos to order" brand metaphor. Theme-adaptive with accessibility fallbacks.
 *
 * @param modifier Modifier for layout
 * @param size Standard loader size (default [DokusLoaderSize.Large])
 * @param speed Animation speed multiplier
 */
@Composable
fun DokusLoader(
    modifier: Modifier = Modifier,
    size: DokusLoaderSize = DokusLoaderSize.Large,
    speed: Float = 1f,
) {
    val canvasSize = size.canvasDp
    val particleCount = size.particles
    if (LocalInspectionMode.current) {
        StaticLoaderPlaceholder(modifier = modifier, size = canvasSize)
        return
    }

    if (LocalReduceMotion.current) {
        ReducedMotionLoader(modifier = modifier, size = canvasSize)
        return
    }

    val effects = MaterialTheme.dokusEffects
    val colors = remember(effects) {
        LoaderColors(
            primary = effects.loaderPrimary,
            secondary = effects.loaderSecondary,
            accent = effects.loaderAccent,
            baseAlpha = effects.loaderBaseAlpha,
        )
    }

    val particles = remember(particleCount) {
        generateParticles(particleCount, Random(seed = particleCount))
    }

    var elapsedSeconds by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(speed) {
        var lastFrameNanos = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                if (lastFrameNanos != 0L) {
                    val deltaSec = (frameNanos - lastFrameNanos) / 1_000_000_000f
                    elapsedSeconds += deltaSec * speed
                }
                lastFrameNanos = frameNanos
            }
        }
    }

    Canvas(modifier = modifier.size(canvasSize)) {
        val centerX = this.size.width / 2f
        val centerY = this.size.height / 2f
        val scale = this.size.minDimension / 2f

        // Cycle phase
        val cycleTime = elapsedSeconds % CycleDurationSec
        val cycleProgress = cycleTime / CycleDurationSec

        // Morph factor: 0 = chaos, 1 = cube
        val morphFactor = when {
            cycleProgress < FormingEnd ->
                cubicEaseInOut(cycleProgress / FormingEnd)
            cycleProgress < HoldingEnd -> 1f
            cycleProgress < DissolvingEnd ->
                1f - cubicEaseInOut((cycleProgress - HoldingEnd) / (DissolvingEnd - HoldingEnd))
            else -> 0f
        }

        // Rotation
        val formedBoost = morphFactor * morphFactor
        val totalYAngle = elapsedSeconds * BaseRotationSpeed + formedBoost * elapsedSeconds * FormedRotationBoost
        val xAngle = XTiltBase + sin(elapsedSeconds * XOscillationSpeed) * XOscillationAmplitude

        val sinY = sin(totalYAngle)
        val cosY = cos(totalYAngle)
        val sinX = sin(xAngle)
        val cosX = cos(xAngle)

        // Breathing wave intensity (active during hold, ramped)
        val breathIntensity = when {
            cycleProgress < FormingEnd -> morphFactor
            cycleProgress < HoldingEnd -> 1f
            cycleProgress < DissolvingEnd -> morphFactor
            else -> 0f
        }

        // Particle size scaled by canvas size
        val particleSizePx = BaseParticleSizeDp * density * (this.size.minDimension / 120.dp.toPx()).coerceIn(0.3f, 2f)

        for (p in particles) {
            // Interpolate chaos → cube
            val px = p.chaosX + (p.cubeX - p.chaosX) * morphFactor
            var py = p.chaosY + (p.cubeY - p.chaosY) * morphFactor
            val pz = p.chaosZ + (p.cubeZ - p.chaosZ) * morphFactor

            // Breathing wave
            if (breathIntensity > 0f) {
                val wave = sin(elapsedSeconds * BreathSpeed + px * BreathSpatialFreq + p.phaseOffset)
                py += wave * BreathAmplitude * breathIntensity
            }

            // Y-axis rotation
            val rx = px * cosY + pz * sinY
            val rz1 = -px * sinY + pz * cosY

            // X-axis tilt
            val ry = py * cosX - rz1 * sinX
            val rz2 = py * sinX + rz1 * cosX

            // Perspective projection
            val perspDivisor = rz2 + FocalLength
            if (perspDivisor <= MinPerspDivisor) continue

            val screenX = centerX + (rx * FocalLength / perspDivisor) * scale
            val screenY = centerY + (ry * FocalLength / perspDivisor) * scale

            // Depth-based sizing and alpha
            val depthFactor = (FocalLength / perspDivisor).coerceIn(DepthScaleMin, 1.5f)
            val radius = particleSizePx * depthFactor
            val depthAlpha = depthFactor.coerceIn(DepthAlphaMin, 1f)

            // Color: lerp secondary→primary by weight, shift toward accent when formed
            val baseColor = lerpColor(colors.secondary, colors.primary, p.colorWeight)
            val finalColor = lerpColor(baseColor, colors.accent, morphFactor * p.colorWeight)
            val alpha = (colors.baseAlpha * depthAlpha).coerceIn(MinParticleAlpha, MaxParticleAlpha)

            drawCircle(
                color = finalColor.copy(alpha = alpha),
                radius = radius,
                center = Offset(screenX, screenY),
            )
        }
    }
}

/**
 * Reduced-motion fallback: simple pulsing dots.
 */
@Composable
private fun ReducedMotionLoader(modifier: Modifier, size: Dp) {
    val color = MaterialTheme.dokusEffects.loaderPrimary

    val infinite = rememberInfiniteTransition(label = "DokusLoaderReduced")
    val alpha by infinite.animateFloat(
        initialValue = ReducedMotionPulseMin,
        targetValue = ReducedMotionPulseMax,
        animationSpec = infiniteRepeatable(
            animation = tween(ReducedMotionPulseDurationMs),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    Canvas(modifier = modifier.size(size)) {
        val dotRadius = this.size.minDimension * ReducedMotionDotScale
        val spacing = this.size.minDimension * ReducedMotionDotSpacingScale
        val cy = this.size.height / 2f
        val cx = this.size.width / 2f
        for (i in -1..1) {
            drawCircle(
                color = color.copy(alpha = alpha),
                radius = dotRadius,
                center = Offset(cx + i * spacing, cy),
            )
        }
    }
}

/**
 * Static placeholder for preview/inspection mode.
 */
@Composable
private fun StaticLoaderPlaceholder(modifier: Modifier, size: Dp) {
    val color = MaterialTheme.dokusEffects.loaderPrimary

    Canvas(modifier = modifier.size(size)) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        drawCircle(
            color = color.copy(alpha = 0.4f),
            radius = this.size.minDimension * 0.3f,
            center = Offset(cx, cy),
            style = Stroke(width = 1.5f),
        )
    }
}
