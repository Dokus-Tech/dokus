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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.foundation.aura.style.dokusEffects
import tech.dokus.foundation.aura.style.isDark
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private const val CubeGrid = 9
private const val CycleMs = 14_000
private const val RotationMs = 38_000
private const val GrainAnimationMs = 700
private const val GrainPoints = 850
private const val CubeProjectionDepth = 3.7f
private const val CubeSplitCenterX = 0.66f
private const val CubeCenteredCenterX = 0.50f
private const val CubeCenterY = 0.50f
private const val CubePitchBase = 0.14f
private const val CubePitchAmplitude = 0.20f
private const val CubeScaleSplit = 0.30f
private const val CubeScaleCentered = 0.25f
private const val CubeRadialInnerDark = 0.07f
private const val CubeRadialInnerLight = 0.05f
private const val CubeRadialOuter = 0.62f
private const val ParticleGoldChance = 0.18f
private const val ParticleBrightChance = 0.04f
private const val ParticleSizeMin = 0.42f
private const val ParticleSizeMax = 1.35f
private const val ParticleDriftMin = 0.56f
private const val ParticleDriftMax = 1.12f
private const val ParticleWobbleMin = 0.04f
private const val ParticleWobbleMax = 0.16f
private const val ParticleDepthAlphaMin = 0.44f
private const val ParticleDepthAlphaRange = 0.56f
private const val ParticleOpacityDark = 0.78f
private const val ParticleOpacityLight = 0.40f
private const val ParticleScatterFade = 0.74f
private const val ParticleBaseFlicker = 0.94f
private const val ParticleFlickerAmplitude = 0.06f
private const val WireframeAlphaDark = 0.30f
private const val WireframeAlphaLight = 0.18f
private const val GrainDark = 0.028f
private const val GrainLight = 0.018f
private const val Pi2 = 6.2831855f
private const val DissolveEnd = 0.26f
private const val ScatterHoldEnd = 0.50f
private const val ReformEnd = 0.76f

/**
 * Visual scene presets for the onboarding background.
 */
enum class OnboardingScene {
    Split,
    Centered,
}

private enum class ParticleTone {
    Neutral,
    Gold,
    Bright,
}

private data class CubeParticle(
    val x: Float,
    val y: Float,
    val z: Float,
    val size: Float,
    val tone: ParticleTone,
    val phase: Float,
    val releaseDelay: Float,
    val driftDistance: Float,
    val wobbleDistance: Float,
    val driftDirectionX: Float,
    val driftDirectionY: Float,
    val driftDirectionZ: Float,
    val tangentDirectionX: Float,
    val tangentDirectionY: Float,
    val tangentDirectionZ: Float,
)

private data class ScatterState(
    val explode: Float,
    val dissolving: Boolean,
    val reforming: Boolean,
)

/**
 * Onboarding background: a rotating particle cube that periodically dissolves and reforms.
 */
@Composable
fun OnboardingBackground(
    modifier: Modifier = Modifier,
    scene: OnboardingScene = OnboardingScene.Split,
    showCube: Boolean = true,
    showGrain: Boolean = true,
) {
    val inspection = LocalInspectionMode.current
    val colors = MaterialTheme.colorScheme
    val effects = MaterialTheme.dokusEffects
    val isDark = colors.isDark

    val infinite = rememberInfiniteTransition(label = "onboarding-background")
    val cycleProgress by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(CycleMs, easing = LinearEasing)
        ),
        label = "cycle-progress"
    )
    val rotationProgress by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(RotationMs, easing = LinearEasing)
        ),
        label = "rotation-progress"
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

    val scatterState = remember(cycleProgress) {
        computeScatterState(cycleProgress)
    }
    val particles = remember {
        generateCubeParticles(random = Random(17))
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
                x = size.width * if (scene == OnboardingScene.Split) CubeSplitCenterX else CubeCenteredCenterX,
                y = size.height * CubeCenterY,
            )

            if (showCube && !inspection) {
                val minDim = min(size.width, size.height)
                val cubeScale = minDim * if (scene == OnboardingScene.Split) CubeScaleSplit else CubeScaleCentered
                val yaw = rotationProgress * Pi2
                val pitch = CubePitchBase + sin(rotationProgress * Pi2 * 0.77f) * CubePitchAmplitude
                val ghostAlpha = easeInOutCubic(scatterState.explode) * if (isDark) WireframeAlphaDark else WireframeAlphaLight

                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            effects.ambientSweepColor.copy(alpha = if (isDark) CubeRadialInnerDark else CubeRadialInnerLight),
                            Color.Transparent,
                        ),
                        center = center,
                        radius = minDim * CubeRadialOuter,
                    )
                )

                if (ghostAlpha > 0.001f) {
                    drawCubeWireframe(
                        center = center,
                        scale = cubeScale,
                        yaw = yaw,
                        pitch = pitch,
                        color = effects.ambientParticleNeutral,
                        alpha = ghostAlpha,
                    )
                }

                particles.forEach { particle ->
                    val scatterAmount = particleScatterAmount(
                        scatterState = scatterState,
                        releaseDelay = particle.releaseDelay,
                    )
                    val wobble = sin(rotationProgress * Pi2 * 0.93f + particle.phase) * particle.wobbleDistance * scatterAmount
                    val drift = particle.driftDistance * scatterAmount

                    val driftedX = particle.x + (particle.driftDirectionX * drift) + (particle.tangentDirectionX * wobble)
                    val driftedY = particle.y + (particle.driftDirectionY * drift) + (particle.tangentDirectionY * wobble)
                    val driftedZ = particle.z + (particle.driftDirectionZ * drift) + (particle.tangentDirectionZ * wobble)

                    val rotated = rotateY(driftedX, driftedY, driftedZ, yaw)
                    val pitched = rotateX(rotated.first, rotated.second, rotated.third, pitch)
                    val projected = projectPoint(
                        x = pitched.first,
                        y = pitched.second,
                        z = pitched.third,
                        focalLength = CubeProjectionDepth,
                    ) ?: return@forEach

                    val color = when (particle.tone) {
                        ParticleTone.Neutral -> effects.ambientParticleNeutral
                        ParticleTone.Gold -> effects.ambientParticleGold
                        ParticleTone.Bright -> if (isDark) Color.White else Color(0xFF1F1F1F)
                    }

                    val depthAlpha = ParticleDepthAlphaMin + (projected.depth * ParticleDepthAlphaRange)
                    val flickerAmplitude = ParticleFlickerAmplitude * (1f - scatterState.explode * 0.8f)
                    val flicker = ParticleBaseFlicker + sin(rotationProgress * Pi2 * 2.1f + particle.phase) * flickerAmplitude
                    val scatterFade = 1f - (scatterAmount * ParticleScatterFade)
                    val toneBoost = when (particle.tone) {
                        ParticleTone.Neutral -> 0.86f
                        ParticleTone.Gold -> 1.08f
                        ParticleTone.Bright -> 1.30f
                    }
                    val alpha = (
                        (if (isDark) ParticleOpacityDark else ParticleOpacityLight) *
                            depthAlpha *
                            flicker *
                            scatterFade *
                            toneBoost
                        ).coerceIn(0f, 1f)

                    drawCircle(
                        color = color.copy(alpha = alpha),
                        radius = particle.size * projected.depth * if (particle.tone == ParticleTone.Bright) 1.18f else 1f,
                        center = Offset(
                            x = center.x + projected.x * cubeScale,
                            y = center.y + projected.y * cubeScale,
                        ),
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
                            y = ((p.y * size.height) + jitterY).coerceIn(0f, size.height),
                        ),
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

private fun computeScatterState(cycleProgress: Float): ScatterState {
    val t = cycleProgress.coerceIn(0f, 1f)

    return when {
        t < DissolveEnd -> ScatterState(
            explode = easeInOutCubic(t / DissolveEnd),
            dissolving = true,
            reforming = false,
        )

        t < ScatterHoldEnd -> ScatterState(
            explode = 1f,
            dissolving = false,
            reforming = false,
        )

        t < ReformEnd -> {
            val reformProgress = (t - ScatterHoldEnd) / (ReformEnd - ScatterHoldEnd)
            ScatterState(
                explode = 1f - easeInOutCubic(reformProgress),
                dissolving = false,
                reforming = true,
            )
        }

        else -> ScatterState(
            explode = 0f,
            dissolving = false,
            reforming = false,
        )
    }
}

private fun particleScatterAmount(
    scatterState: ScatterState,
    releaseDelay: Float,
): Float {
    if (!scatterState.dissolving && !scatterState.reforming) {
        return scatterState.explode
    }

    return if (scatterState.dissolving) {
        easeInOutCubic(applyDelay(scatterState.explode, releaseDelay))
    } else {
        val reformProgress = 1f - scatterState.explode
        val delayed = applyDelay(reformProgress, releaseDelay * 0.85f)
        1f - easeInOutCubic(delayed)
    }
}

private fun applyDelay(progress: Float, delay: Float): Float {
    val normalizedDelay = delay.coerceIn(0f, 0.95f)
    if (progress <= normalizedDelay) return 0f
    return ((progress - normalizedDelay) / (1f - normalizedDelay)).coerceIn(0f, 1f)
}

private fun easeInOutCubic(value: Float): Float {
    val t = value.coerceIn(0f, 1f)
    return if (t < 0.5f) {
        4f * t * t * t
    } else {
        1f - ((-2f * t + 2f).let { it * it * it } / 2f)
    }
}

private fun DrawScope.drawCubeWireframe(
    center: Offset,
    scale: Float,
    yaw: Float,
    pitch: Float,
    color: Color,
    alpha: Float,
) {
    val vertices = listOf(
        Triple(-1f, -1f, -1f),
        Triple(1f, -1f, -1f),
        Triple(1f, 1f, -1f),
        Triple(-1f, 1f, -1f),
        Triple(-1f, -1f, 1f),
        Triple(1f, -1f, 1f),
        Triple(1f, 1f, 1f),
        Triple(-1f, 1f, 1f),
    )
    val edges = listOf(
        0 to 1,
        1 to 2,
        2 to 3,
        3 to 0,
        4 to 5,
        5 to 6,
        6 to 7,
        7 to 4,
        0 to 4,
        1 to 5,
        2 to 6,
        3 to 7,
    )

    val projected = vertices.map { vertex ->
        val rotated = rotateY(vertex.first, vertex.second, vertex.third, yaw)
        val pitched = rotateX(rotated.first, rotated.second, rotated.third, pitch)
        projectPoint(
            x = pitched.first,
            y = pitched.second,
            z = pitched.third,
            focalLength = CubeProjectionDepth,
        )
    }

    val stroke = max(1f, min(size.width, size.height) * 0.00115f)

    edges.forEach { (start, end) ->
        val p1 = projected[start] ?: return@forEach
        val p2 = projected[end] ?: return@forEach
        val depthAlpha = ((p1.depth + p2.depth) * 0.5f).coerceIn(0.25f, 1f)

        drawLine(
            color = color.copy(alpha = (alpha * depthAlpha).coerceIn(0f, 1f)),
            start = Offset(
                x = center.x + (p1.x * scale),
                y = center.y + (p1.y * scale),
            ),
            end = Offset(
                x = center.x + (p2.x * scale),
                y = center.y + (p2.y * scale),
            ),
            strokeWidth = stroke,
        )
    }
}

private fun projectPoint(
    x: Float,
    y: Float,
    z: Float,
    focalLength: Float,
): ProjectedPoint? {
    val denominator = z + focalLength
    if (denominator <= 0.1f) return null

    val scale = focalLength / denominator
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

private fun generateCubeParticles(random: Random): List<CubeParticle> {
    val axisValues = (0 until CubeGrid).map { index ->
        val t = index / (CubeGrid - 1f)
        -1f + (2f * t)
    }

    val particles = mutableListOf<CubeParticle>()

    fun addParticle(x: Float, y: Float, z: Float) {
        val edgePriority = computeEdgePriority(x, y, z)
        val releaseDelay = ((1f - edgePriority) * 0.30f + random.nextFloat() * 0.05f)
            .coerceIn(0f, 0.36f)

        val outward = normalizeVector(
            x + randomCentered(random) * 0.25f,
            y + randomCentered(random) * 0.25f,
            z + randomCentered(random) * 0.25f,
        )
        val tangent = normalizeVector(
            outward.second - outward.third + randomCentered(random) * 0.20f,
            outward.third - outward.first + randomCentered(random) * 0.20f,
            outward.first - outward.second + randomCentered(random) * 0.20f,
        )

        val tone = when {
            random.nextFloat() < ParticleBrightChance -> ParticleTone.Bright
            random.nextFloat() < ParticleGoldChance -> ParticleTone.Gold
            else -> ParticleTone.Neutral
        }

        particles += CubeParticle(
            x = x,
            y = y,
            z = z,
            size = ParticleSizeMin + random.nextFloat() * (ParticleSizeMax - ParticleSizeMin),
            tone = tone,
            phase = random.nextFloat() * Pi2,
            releaseDelay = releaseDelay,
            driftDistance = ParticleDriftMin + random.nextFloat() * (ParticleDriftMax - ParticleDriftMin) + (edgePriority * 0.26f),
            wobbleDistance = ParticleWobbleMin + random.nextFloat() * (ParticleWobbleMax - ParticleWobbleMin),
            driftDirectionX = outward.first,
            driftDirectionY = outward.second,
            driftDirectionZ = outward.third,
            tangentDirectionX = tangent.first,
            tangentDirectionY = tangent.second,
            tangentDirectionZ = tangent.third,
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
        .distinctBy { particle ->
            Triple(
                (particle.x * 1000).toInt(),
                (particle.y * 1000).toInt(),
                (particle.z * 1000).toInt(),
            )
        }
}

private fun computeEdgePriority(x: Float, y: Float, z: Float): Float {
    val ax = abs(x)
    val ay = abs(y)
    val az = abs(z)
    val first = max(ax, max(ay, az))
    val third = min(ax, min(ay, az))
    val second = ax + ay + az - first - third
    return second.coerceIn(0f, 1f)
}

private fun normalizeVector(x: Float, y: Float, z: Float): Triple<Float, Float, Float> {
    val length = max(0.0001f, sqrt((x * x) + (y * y) + (z * z)))
    return Triple(x / length, y / length, z / length)
}

private fun randomCentered(random: Random): Float {
    return (random.nextFloat() * 2f) - 1f
}

@Preview
@Composable
private fun OnboardingBackgroundSplitPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        OnboardingBackground(scene = OnboardingScene.Split)
    }
}

@Preview
@Composable
private fun OnboardingBackgroundCenteredPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        OnboardingBackground(scene = OnboardingScene.Centered)
    }
}
