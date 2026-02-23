@file:Suppress("TopLevelPropertyNaming")

package tech.dokus.foundation.aura.components.background

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalInspectionMode
import tech.dokus.foundation.aura.style.isDark
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private const val FaceGrid = 9
private const val FaceCount = 6
private const val ParticleCount = FaceGrid * FaceGrid * FaceCount

private const val CubeProjectionDepth = 3.9f
private const val CubeCenterX = 0.5f
private const val CubePitchRad = 0.296706f // 17 degrees
private const val CubeRotationSecondsPerTurn = 50f

private const val ScatterDistanceMin = 0.48f
private const val ScatterDistanceMax = 0.92f
private const val WobbleDistanceMin = 0.03f
private const val WobbleDistanceMax = 0.09f
private const val DelayScale = 0.64f
private const val DelayJitter = 0.04f

private const val ParticleSizeMin = 0.42f
private const val ParticleSizeMax = 1.05f
private const val GoldChance = 0.20f
private const val BrightChance = 0.0f

private const val WireframeGhostAlphaDark = 0.06f
private const val WireframeGhostAlphaLight = 0.04f
private const val WireframeSolidAlphaDark = 0.34f
private const val WireframeSolidAlphaLight = 0.24f

private const val BackgroundDarkHex = 0xFF070605
private const val TwoPI = 6.2831855f

enum class BootstrapBackgroundLayout {
    Mobile,
    Desktop,
}

private data class BootstrapBackgroundSpec(
    val cubeWidthFraction: Float,
    val centerY: Float,
    val scatterMultiplier: Float,
)

private val MobileBackgroundSpec = BootstrapBackgroundSpec(
    cubeWidthFraction = 0.35f,
    centerY = 0.34f,
    scatterMultiplier = 0.78f,
)

private val DesktopBackgroundSpec = BootstrapBackgroundSpec(
    cubeWidthFraction = 0.42f,
    centerY = 0.44f,
    scatterMultiplier = 0.92f,
)

private enum class BootstrapParticleTone {
    Neutral,
    Gold,
    Bright,
}

private data class BootstrapParticle(
    val homeX: Float,
    val homeY: Float,
    val homeZ: Float,
    val outwardX: Float,
    val outwardY: Float,
    val outwardZ: Float,
    val tangentX: Float,
    val tangentY: Float,
    val tangentZ: Float,
    val delay: Float,
    val scatterDistance: Float,
    val wobbleDistance: Float,
    val size: Float,
    val phase: Float,
    val tone: BootstrapParticleTone,
)

private data class BootstrapProjectedPoint(
    val x: Float,
    val y: Float,
    val depth: Float,
)

private data class RenderedParticle(
    val x: Float,
    val y: Float,
    val depth: Float,
    val radius: Float,
    val color: Color,
    val alpha: Float,
)

private val CubeVertices = listOf(
    Triple(-1f, -1f, -1f),
    Triple(1f, -1f, -1f),
    Triple(1f, 1f, -1f),
    Triple(-1f, 1f, -1f),
    Triple(-1f, -1f, 1f),
    Triple(1f, -1f, 1f),
    Triple(1f, 1f, 1f),
    Triple(-1f, 1f, 1f),
)

private val CubeEdges = listOf(
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

@Composable
fun BootstrapBackground(
    progress: Float,
    layout: BootstrapBackgroundLayout = BootstrapBackgroundLayout.Desktop,
    modifier: Modifier = Modifier,
) {
    val inspection = LocalInspectionMode.current
    val isDark = MaterialTheme.colorScheme.isDark
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val adaptiveBackground = MaterialTheme.colorScheme.background
    val spec = remember(layout) {
        when (layout) {
            BootstrapBackgroundLayout.Mobile -> MobileBackgroundSpec
            BootstrapBackgroundLayout.Desktop -> DesktopBackgroundSpec
        }
    }
    val particles = remember(layout) { generateParticles(Random(83), spec.scatterMultiplier) }

    var elapsedSeconds by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(inspection) {
        if (inspection) return@LaunchedEffect
        var lastFrameNanos = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                if (lastFrameNanos != 0L) {
                    elapsedSeconds += (frameNanos - lastFrameNanos) / 1_000_000_000f
                }
                lastFrameNanos = frameNanos
            }
        }
    }

    val clampedProgress = progress.coerceIn(0f, 1f)
    val baseColor = if (isDark) Color(BackgroundDarkHex) else adaptiveBackground

    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(color = baseColor)

        val center = Offset(
            x = size.width * CubeCenterX,
            y = size.height * spec.centerY,
        )
        val scale = size.width * spec.cubeWidthFraction * 0.5f

        val timeSeconds = if (inspection) 0f else elapsedSeconds
        val yaw = (timeSeconds / CubeRotationSecondsPerTurn) * TwoPI
        val pitch = CubePitchRad

        val progressEased = easeInOutCubic(clampedProgress)
        val wireframeGhostAlpha = (1f - progressEased) * if (isDark) WireframeGhostAlphaDark else WireframeGhostAlphaLight
        val wireframeSolidAlpha = progressEased * if (isDark) WireframeSolidAlphaDark else WireframeSolidAlphaLight

        val neutralParticleColor = if (isDark) onSurface.copy(alpha = 0.78f) else onSurface.copy(alpha = 0.56f)
        val brightParticleColor = if (isDark) Color.White else onSurface

        drawCubeWireframe(
            center = center,
            scale = scale,
            yaw = yaw,
            pitch = pitch,
            color = onSurface,
            alpha = wireframeGhostAlpha,
            strokeScale = 0.92f,
        )

        drawCubeWireframe(
            center = center,
            scale = scale,
            yaw = yaw,
            pitch = pitch,
            color = primary,
            alpha = wireframeSolidAlpha,
            strokeScale = 1.12f,
        )

        val rendered = ArrayList<RenderedParticle>(ParticleCount)
        for (particle in particles) {
            val localProgress = delayedProgress(clampedProgress, particle.delay)
            val formation = easeInOutCubic(localProgress)
            val scatter = 1f - formation

            val wobble = sin((timeSeconds * 1.4f) + particle.phase) * particle.wobbleDistance * scatter
            val drift = particle.scatterDistance * scatter

            val x = particle.homeX + (particle.outwardX * drift) + (particle.tangentX * wobble)
            val y = particle.homeY + (particle.outwardY * drift) + (particle.tangentY * wobble)
            val z = particle.homeZ + (particle.outwardZ * drift) + (particle.tangentZ * wobble)

            val yRotated = rotateY(x, y, z, yaw)
            val xRotated = rotateX(yRotated.first, yRotated.second, yRotated.third, pitch)
            val projected = projectPoint(
                x = xRotated.first,
                y = xRotated.second,
                z = xRotated.third,
                focalLength = CubeProjectionDepth,
            ) ?: continue

            val toneColor = when (particle.tone) {
                BootstrapParticleTone.Neutral -> neutralParticleColor
                BootstrapParticleTone.Gold -> primary
                BootstrapParticleTone.Bright -> brightParticleColor
            }
            val toneBoost = when (particle.tone) {
                BootstrapParticleTone.Neutral -> 0.92f
                BootstrapParticleTone.Gold -> 1.08f
                BootstrapParticleTone.Bright -> 1.24f
            }
            val baseAlpha = if (isDark) 0.72f else 0.52f
            val depthAlpha = 0.34f + (projected.depth * 0.66f)
            val scatterAlpha = 0.70f + (scatter * 0.30f)
            val alpha = (baseAlpha * depthAlpha * scatterAlpha * toneBoost).coerceIn(0f, 1f)

            rendered += RenderedParticle(
                x = center.x + (projected.x * scale),
                y = center.y + (projected.y * scale),
                depth = projected.depth,
                radius = particle.size,
                color = toneColor,
                alpha = alpha,
            )
        }

        rendered
            .sortedBy { it.depth }
            .forEach { particle ->
                drawCircle(
                    color = particle.color.copy(alpha = particle.alpha),
                    radius = particle.radius * density * (0.92f + particle.depth * 0.48f),
                    center = Offset(particle.x, particle.y),
                )
            }
    }
}

private fun DrawScope.drawCubeWireframe(
    center: Offset,
    scale: Float,
    yaw: Float,
    pitch: Float,
    color: Color,
    alpha: Float,
    strokeScale: Float,
) {
    if (alpha <= 0.001f) return

    val projected = CubeVertices.map { vertex ->
        val yRotated = rotateY(vertex.first, vertex.second, vertex.third, yaw)
        val xRotated = rotateX(yRotated.first, yRotated.second, yRotated.third, pitch)
        projectPoint(
            x = xRotated.first,
            y = xRotated.second,
            z = xRotated.third,
            focalLength = CubeProjectionDepth,
        )
    }
    val stroke = max(1f, min(size.width, size.height) * 0.00122f) * strokeScale

    CubeEdges.forEach { (start, end) ->
        val p1 = projected[start] ?: return@forEach
        val p2 = projected[end] ?: return@forEach
        val depthAlpha = ((p1.depth + p2.depth) * 0.5f).coerceIn(0.24f, 1f)
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

private fun generateParticles(
    random: Random,
    scatterMultiplier: Float,
): List<BootstrapParticle> {
    val axisValues = (0 until FaceGrid).map { index ->
        val t = index / (FaceGrid - 1f)
        -1f + (2f * t)
    }
    val particles = ArrayList<BootstrapParticle>(ParticleCount)

    fun addParticle(x: Float, y: Float, z: Float) {
        val faceDistance = when {
            abs(x) == 1f -> sqrt((y * y + z * z) * 0.5f)
            abs(y) == 1f -> sqrt((x * x + z * z) * 0.5f)
            else -> sqrt((x * x + y * y) * 0.5f)
        }

        val delay = (faceDistance * DelayScale + random.nextFloat() * DelayJitter).coerceIn(0f, 0.76f)
        val outward = normalize(
            x + randomCentered(random) * 0.25f,
            y + randomCentered(random) * 0.25f,
            z + randomCentered(random) * 0.25f,
        )
        val tangent = orthogonal(outward, random)
        val toneRoll = random.nextFloat()
        val tone = when {
            toneRoll < BrightChance -> BootstrapParticleTone.Bright
            toneRoll < GoldChance + BrightChance -> BootstrapParticleTone.Gold
            else -> BootstrapParticleTone.Neutral
        }

        particles += BootstrapParticle(
            homeX = x,
            homeY = y,
            homeZ = z,
            outwardX = outward.first,
            outwardY = outward.second,
            outwardZ = outward.third,
            tangentX = tangent.first,
            tangentY = tangent.second,
            tangentZ = tangent.third,
            delay = delay,
            scatterDistance = (
                ScatterDistanceMin +
                    random.nextFloat() * (ScatterDistanceMax - ScatterDistanceMin) +
                    (faceDistance * 0.08f)
                ) * scatterMultiplier,
            wobbleDistance = WobbleDistanceMin + random.nextFloat() * (WobbleDistanceMax - WobbleDistanceMin),
            size = ParticleSizeMin + random.nextFloat() * (ParticleSizeMax - ParticleSizeMin),
            phase = random.nextFloat() * TwoPI,
            tone = tone,
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

private fun delayedProgress(progress: Float, delay: Float): Float {
    if (progress <= delay) return 0f
    return ((progress - delay) / (1f - delay)).coerceIn(0f, 1f)
}

private fun easeInOutCubic(value: Float): Float {
    val t = value.coerceIn(0f, 1f)
    return if (t < 0.5f) {
        4f * t * t * t
    } else {
        1f - ((-2f * t + 2f).let { it * it * it } / 2f)
    }
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

private fun projectPoint(
    x: Float,
    y: Float,
    z: Float,
    focalLength: Float,
): BootstrapProjectedPoint? {
    val denominator = z + focalLength
    if (denominator <= 0.1f) return null

    val scale = focalLength / denominator
    return BootstrapProjectedPoint(
        x = x * scale,
        y = y * scale,
        depth = scale.coerceIn(0f, 1.45f),
    )
}

private fun normalize(x: Float, y: Float, z: Float): Triple<Float, Float, Float> {
    val length = max(0.0001f, sqrt((x * x) + (y * y) + (z * z)))
    return Triple(x / length, y / length, z / length)
}

private fun orthogonal(
    forward: Triple<Float, Float, Float>,
    random: Random,
): Triple<Float, Float, Float> {
    val rx = randomCentered(random)
    val ry = randomCentered(random)
    val rz = randomCentered(random)
    var cross = cross(forward, Triple(rx, ry, rz))
    val nearZero = abs(cross.first) + abs(cross.second) + abs(cross.third) < 0.01f
    if (nearZero) {
        cross = cross(forward, Triple(0f, 1f, 0f))
    }
    return normalize(cross.first, cross.second, cross.third)
}

private fun cross(
    a: Triple<Float, Float, Float>,
    b: Triple<Float, Float, Float>,
): Triple<Float, Float, Float> {
    return Triple(
        a.second * b.third - a.third * b.second,
        a.third * b.first - a.first * b.third,
        a.first * b.second - a.second * b.first,
    )
}

private fun randomCentered(random: Random): Float {
    return (random.nextFloat() * 2f) - 1f
}
