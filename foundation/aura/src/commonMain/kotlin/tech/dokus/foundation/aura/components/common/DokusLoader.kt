@file:Suppress("TopLevelPropertyNaming")

package tech.dokus.foundation.aura.components.common

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.foundation.aura.local.LocalReduceMotion
import tech.dokus.foundation.aura.style.dokusEffects
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ============================================================================
// DOKUS LOADER — Rotating surface-particle cube
// ============================================================================

// Rotation
private const val RotationMs = 12_000
private const val PitchBase = 0.20f
private const val PitchAmplitude = 0.15f

// 3D projection
private const val FocalLength = 3.7f

// Particle visuals
private const val GoldChance = 0.18f
private const val BrightChance = 0.04f
private const val ParticleSizeMin = 0.42f
private const val ParticleSizeMax = 1.35f
private const val DepthAlphaMin = 0.44f
private const val DepthAlphaRange = 0.56f
private const val BaseFlicker = 0.94f
private const val FlickerAmplitude = 0.06f

// Math
private const val Pi2 = 6.2831855f

// Reduced motion fallback
private const val ReducedMotionPulseMin = 0.3f
private const val ReducedMotionPulseMax = 0.8f
private const val ReducedMotionPulseDurationMs = 1500
private const val ReducedMotionDotScale = 0.06f
private const val ReducedMotionDotSpacingScale = 0.15f

private enum class ParticleTone { Neutral, Gold, Bright }

private data class LoaderParticle(
    val x: Float,
    val y: Float,
    val z: Float,
    val size: Float,
    val tone: ParticleTone,
    val phase: Float,
)

/**
 * Standard sizes for the Dokus loading animation.
 *
 * Enforces design consistency — screens must pick one of these rather than arbitrary sizes.
 */
enum class DokusLoaderSize(internal val canvasDp: Dp, internal val grid: Int) {
    /** 24.dp — inline next to text, dropdowns, list-loading indicators. */
    Small(canvasDp = 24.dp, grid = 4),

    /** 72.dp — section or panel loading. */
    Medium(canvasDp = 72.dp, grid = 6),

    /** 120.dp — full-screen / page loading. */
    Large(canvasDp = 120.dp, grid = 8),
}

/**
 * Dokus branded loading animation — a rotating surface-particle cube.
 *
 * Theme-adaptive with accessibility fallbacks.
 *
 * @param modifier Modifier for layout
 * @param size Standard loader size (default [DokusLoaderSize.Large])
 * @param speed Animation speed multiplier (affects rotation speed)
 */
@Composable
fun DokusLoader(
    modifier: Modifier = Modifier,
    size: DokusLoaderSize = DokusLoaderSize.Large,
    speed: Float = 1f,
) {
    val canvasSize = size.canvasDp
    if (LocalInspectionMode.current) {
        StaticLoaderPlaceholder(modifier = modifier, size = canvasSize)
        return
    }

    if (LocalReduceMotion.current) {
        ReducedMotionLoader(modifier = modifier, size = canvasSize)
        return
    }

    val effects = MaterialTheme.dokusEffects
    val colorNeutral = effects.ambientParticleNeutral
    val colorGold = effects.ambientParticleGold
    val isDark = effects.loaderBaseAlpha > 0.75f // dark theme has higher base alpha
    val baseOpacity = if (isDark) 0.78f else 0.40f

    val particles = remember(size.grid) {
        generateSurfaceParticles(size.grid, Random(seed = size.grid))
    }

    val infinite = rememberInfiniteTransition(label = "dokus-loader")
    val rotationProgress by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (RotationMs / speed).toInt(),
                easing = LinearEasing,
            ),
        ),
        label = "rotation",
    )

    Canvas(modifier = modifier.size(canvasSize)) {
        val centerX = this.size.width / 2f
        val centerY = this.size.height / 2f
        val cubeScale = this.size.minDimension * 0.30f

        val yaw = rotationProgress * Pi2
        val pitch = PitchBase + sin(rotationProgress * Pi2 * 0.77f) * PitchAmplitude

        val cosYaw = cos(yaw)
        val sinYaw = sin(yaw)
        val cosPitch = cos(pitch)
        val sinPitch = sin(pitch)

        for (p in particles) {
            // Y-axis rotation
            val rx = p.x * cosYaw + p.z * sinYaw
            val ry1 = p.y
            val rz1 = -p.x * sinYaw + p.z * cosYaw

            // X-axis pitch
            val ry = ry1 * cosPitch - rz1 * sinPitch
            val rz = ry1 * sinPitch + rz1 * cosPitch

            // Perspective projection
            val denominator = rz + FocalLength
            if (denominator <= 0.1f) continue

            val projScale = FocalLength / denominator
            val screenX = centerX + rx * projScale * cubeScale
            val screenY = centerY + ry * projScale * cubeScale

            val depth = projScale.coerceIn(0f, 1.4f)

            val color = when (p.tone) {
                ParticleTone.Neutral -> colorNeutral
                ParticleTone.Gold -> colorGold
                ParticleTone.Bright -> effects.brightParticle
            }

            val depthAlpha = DepthAlphaMin + depth * DepthAlphaRange
            val flicker = BaseFlicker + sin(rotationProgress * Pi2 * 2.1f + p.phase) * FlickerAmplitude
            val toneBoost = when (p.tone) {
                ParticleTone.Neutral -> 0.86f
                ParticleTone.Gold -> 1.08f
                ParticleTone.Bright -> 1.30f
            }
            val alpha = (baseOpacity * depthAlpha * flicker * toneBoost).coerceIn(0f, 1f)

            drawCircle(
                color = color.copy(alpha = alpha),
                radius = p.size * depth * if (p.tone == ParticleTone.Bright) 1.18f else 1f,
                center = Offset(screenX, screenY),
            )
        }
    }
}

/**
 * Generate surface-only particles on the 6 faces of a unit cube, deduplicated at shared edges.
 */
private fun generateSurfaceParticles(grid: Int, random: Random): List<LoaderParticle> {
    val axisValues = (0 until grid).map { i ->
        val t = i / (grid - 1f)
        -1f + 2f * t
    }

    val seen = mutableSetOf<Triple<Int, Int, Int>>()
    val particles = mutableListOf<LoaderParticle>()

    fun addParticle(x: Float, y: Float, z: Float) {
        val key = Triple((x * 1000).toInt(), (y * 1000).toInt(), (z * 1000).toInt())
        if (!seen.add(key)) return

        val tone = when {
            random.nextFloat() < BrightChance -> ParticleTone.Bright
            random.nextFloat() < GoldChance -> ParticleTone.Gold
            else -> ParticleTone.Neutral
        }

        particles += LoaderParticle(
            x = x, y = y, z = z,
            size = ParticleSizeMin + random.nextFloat() * (ParticleSizeMax - ParticleSizeMin),
            tone = tone,
            phase = random.nextFloat() * Pi2,
        )
    }

    axisValues.forEach { u ->
        axisValues.forEach { v ->
            addParticle(1f, u, v)
            addParticle(-1f, u, v)
            addParticle(u, 1f, v)
            addParticle(u, -1f, v)
            addParticle(u, v, 1f)
            addParticle(u, v, -1f)
        }
    }

    return particles
}

@Preview
@Composable
private fun DokusLoaderPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        DokusLoader(size = DokusLoaderSize.Medium)
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
