@file:Suppress("TopLevelPropertyNaming")

package tech.dokus.foundation.aura.components.background

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.foundation.aura.style.dokusEffects
import tech.dokus.foundation.aura.style.isDark
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

private const val CubeGrid = 9
private const val CubeAngleRange = 360f
private const val CubeProjectionDepth = 3.8f
private const val CubeAnimationMs = 22000
private const val CubePulseMs = 5600
private const val GrainAnimationMs = 700
private const val GrainPoints = 850
private const val CubePointGoldChance = 0.35f
private const val CubePointEmphasisChance = 0.06f
private const val CubePointSizeMin = 0.4f
private const val CubePointSizeMax = 1.35f
private const val CubeScaleMin = 0.2f
private const val CubeScaleMax = 0.34f
private const val CubeOpacityDark = 0.72f
private const val CubeOpacityLight = 0.34f
private const val GrainDark = 0.028f
private const val GrainLight = 0.018f
private const val CubeSplitCenterX = 0.66f
private const val CubeCenteredCenterX = 0.50f
private const val CubeCenterY = 0.50f
private const val CubeYawSpeed = 0.55f
private const val CubePitchAmplitude = 0.24f
private const val CubePitchBase = 0.13f
private const val CubePointFlickerMin = 0.72f
private const val CubePointFlickerRange = 0.28f
private const val CubePointDepthAlphaMin = 0.45f
private const val CubePointDepthAlphaRange = 0.55f
private const val CubeRadialInner = 0.08f
private const val CubeRadialOuter = 0.62f
private const val Pi2 = 6.2831855f

/**
 * Visual scene presets for the onboarding C5 background.
 */
enum class OnboardingC5Scene {
    Split,
    Centered,
}

private data class CubePoint(
    val x: Float,
    val y: Float,
    val z: Float,
    val size: Float,
    val isGold: Boolean,
    val emphasis: Boolean,
    val phase: Float,
)

/**
 * C5 onboarding background: rotating deterministic cube field with light grain.
 *
 * This component is intentionally independent from [AmbientBackground] so onboarding can evolve
 * without changing document/home visual behavior.
 */
@Composable
fun OnboardingC5Background(
    modifier: Modifier = Modifier,
    scene: OnboardingC5Scene = OnboardingC5Scene.Split,
    showCube: Boolean = true,
    showGrain: Boolean = true,
) {
    val inspection = LocalInspectionMode.current
    val colors = MaterialTheme.colorScheme
    val effects = MaterialTheme.dokusEffects
    val isDark = colors.isDark

    val infinite = rememberInfiniteTransition(label = "onboarding-c5")
    val rotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = CubeAngleRange,
        animationSpec = infiniteRepeatable(
            animation = tween(CubeAnimationMs, easing = LinearEasing)
        ),
        label = "rotation"
    )
    val pulse by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(CubePulseMs, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val grainShift by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(GrainAnimationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "grain-shift"
    )

    val points = remember {
        generateCubePoints(random = Random(17))
    }
    val grain = remember {
        val grainRandom = Random(97)
        List(GrainPoints) {
            Offset(grainRandom.nextFloat(), grainRandom.nextFloat())
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = colors.background)

            val center = Offset(
                x = size.width * if (scene == OnboardingC5Scene.Split) CubeSplitCenterX else CubeCenteredCenterX,
                y = size.height * CubeCenterY
            )

            if (showCube && !inspection) {
                val minDim = min(size.width, size.height)
                val cubeScale = minDim * (CubeScaleMin + (CubeScaleMax - CubeScaleMin) * pulse)
                val yaw = rotation * CubeYawSpeed * (Math.PI / 180f).toFloat()
                val pitch =
                    CubePitchBase + sin(rotation * (Math.PI / 180f).toFloat() * 0.28f) * CubePitchAmplitude

                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            effects.ambientSweepColor.copy(alpha = if (isDark) CubeRadialInner else 0.06f),
                            Color.Transparent,
                        ),
                        center = center,
                        radius = minDim * CubeRadialOuter
                    )
                )

                points.forEach { point ->
                    val rotated = rotateY(point.x, point.y, point.z, yaw)
                    val pitched = rotateX(rotated.first, rotated.second, rotated.third, pitch)

                    val projected = projectPoint(
                        x = pitched.first,
                        y = pitched.second,
                        z = pitched.third,
                        focalLength = CubeProjectionDepth,
                    ) ?: return@forEach

                    val alphaDepth =
                        CubePointDepthAlphaMin + (projected.depth * CubePointDepthAlphaRange)
                    val flicker =
                        CubePointFlickerMin + sin(rotation * 0.05f + point.phase) * CubePointFlickerRange
                    val alpha =
                        alphaDepth * flicker * if (isDark) CubeOpacityDark else CubeOpacityLight

                    val color = if (point.isGold || point.emphasis) {
                        effects.ambientParticleGold
                    } else {
                        effects.ambientParticleNeutral
                    }

                    drawCircle(
                        color = color.copy(alpha = alpha),
                        radius = point.size * projected.depth * if (point.emphasis) 1.45f else 1f,
                        center = Offset(
                            x = center.x + projected.x * cubeScale,
                            y = center.y + projected.y * cubeScale,
                        )
                    )
                }
            }

            if (showGrain && !inspection) {
                val grainAlpha = if (isDark) GrainDark else GrainLight
                val jitterX = (grainShift - 0.5f) * size.width * 0.03f
                val jitterY = (0.5f - grainShift) * size.height * 0.04f
                grain.forEach { p ->
                    drawCircle(
                        color = (if (isDark) Color.White else Color.Black).copy(alpha = grainAlpha),
                        radius = 0.7f,
                        center = Offset(
                            x = ((p.x * size.width) + jitterX).coerceIn(0f, size.width),
                            y = ((p.y * size.height) + jitterY).coerceIn(0f, size.height)
                        )
                    )
                }
            }
        }
    }
}

private data class ProjectedPoint(
    val x: Float,
    val y: Float,
    val depth: Float,
)

private fun projectPoint(
    x: Float,
    y: Float,
    z: Float,
    focalLength: Float,
): ProjectedPoint? {
    val perspectiveDenominator = z + focalLength
    if (perspectiveDenominator <= 0.1f) return null

    val scale = focalLength / perspectiveDenominator
    return ProjectedPoint(
        x = x * scale,
        y = y * scale,
        depth = scale.coerceIn(0f, 1.4f),
    )
}

private fun rotateY(x: Float, y: Float, z: Float, angle: Float): Triple<Float, Float, Float> {
    val c = cos(angle)
    val s = sin(angle)
    return Triple(
        x * c + z * s,
        y,
        -x * s + z * c,
    )
}

private fun rotateX(x: Float, y: Float, z: Float, angle: Float): Triple<Float, Float, Float> {
    val c = cos(angle)
    val s = sin(angle)
    return Triple(
        x,
        y * c - z * s,
        y * s + z * c,
    )
}

private fun generateCubePoints(random: Random): List<CubePoint> {
    val axisValues = (0 until CubeGrid).map { index ->
        val t = index / (CubeGrid - 1f)
        -1f + (2f * t)
    }

    val points = mutableListOf<CubePoint>()

    fun addPoint(x: Float, y: Float, z: Float) {
        points += CubePoint(
            x = x,
            y = y,
            z = z,
            size = CubePointSizeMin + random.nextFloat() * (CubePointSizeMax - CubePointSizeMin),
            isGold = random.nextFloat() < CubePointGoldChance,
            emphasis = random.nextFloat() < CubePointEmphasisChance,
            phase = random.nextFloat() * Pi2,
        )
    }

    axisValues.forEach { u ->
        axisValues.forEach { v ->
            addPoint(1f, u, v)
            addPoint(-1f, u, v)
            addPoint(u, 1f, v)
            addPoint(u, -1f, v)
            addPoint(u, v, 1f)
            addPoint(u, v, -1f)
        }
    }

    // Remove obvious duplicates generated on shared edges/corners.
    return points
        .distinctBy { point ->
            Triple(
                (point.x * 1000).toInt(),
                (point.y * 1000).toInt(),
                (point.z * 1000).toInt(),
            )
        }
}

@Preview
@Composable
private fun OnboardingC5BackgroundSplitPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        OnboardingC5Background(scene = OnboardingC5Scene.Split)
    }
}

@Preview
@Composable
private fun OnboardingC5BackgroundCenteredPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        OnboardingC5Background(scene = OnboardingC5Scene.Centered)
    }
}
