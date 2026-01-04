package tech.dokus.features.cashflow.presentation.cashflow.components.upload

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
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import tech.dokus.features.cashflow.presentation.cashflow.components.FlyingDocument
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// Animation durations
private const val DiskRotationDurationMs = 8000
private const val HorizonPulseDurationMs = 1500
private const val DistortionWaveDurationMs = 2000
private const val ActiveScaleDurationMs = 800
private const val FrameDelayMs = 16L
private const val FrameDeltaSeconds = 0.016f

// Animation values
private const val FullRotationDegrees = 360f
private const val HorizonPulseMin = 0.95f
private const val HorizonPulseMax = 1.05f
private const val ActiveScaleValue = 1.2f
private const val DefaultScale = 1f

// Particle generation
private const val AccretionParticleCount = 60
private const val ParticleColorModulo = 5
private const val ParticleDistanceMin = 0.4f
private const val ParticleDistanceRange = 0.5f
private const val ParticleSpeedMin = 0.5f
private const val ParticleSpeedRange = 1.5f
private const val ParticleSizeMin = 1f
private const val ParticleSizeRange = 3f

// Particle color hex values
private const val ParticleColorOrangeHex = 0xFFFF6B35
private const val ParticleColorAmberHex = 0xFFFFAA00
private const val ParticleColorRedHex = 0xFFFF4444
private const val ParticleColorDarkOrangeHex = 0xFFCC3300
private const val ParticleColorLightOrangeHex = 0xFFFFDD88

// Particle colors
private object ParticleColors {
    val Orange = Color(ParticleColorOrangeHex)
    val Amber = Color(ParticleColorAmberHex)
    val Red = Color(ParticleColorRedHex)
    val DarkOrange = Color(ParticleColorDarkOrangeHex)
    val LightOrange = Color(ParticleColorLightOrangeHex)
}

// Lensing ring constants
private const val LensingRingCount = 4
private const val LensingRingBaseRadius = 0.9f
private const val LensingRingRadiusStep = 0.15f
private const val LensingRingWaveStep = 0.25f
private const val LensingRingMaxAlpha = 0.15f
private const val LensingRingStrokeWidth = 1.5f

// Lensing ring color
private const val LensingRingColorHex = 0xFF4466AA
private object LensingRingColors {
    val Ring = Color(LensingRingColorHex)
}

// Accretion disk constants
private const val OrbitSpeedMultiplier = 60f
private const val DegreesToRadians = PI.toFloat() / 180f
private const val SpiralFactor = 0.1f
private const val DiskFlattenFactor = 0.3f
private const val FrontParticleAlpha = 0.8f
private const val BackParticleAlpha = 0.4f
private const val TrailAngleOffset = 0.3f
private const val TrailAlphaMultiplier = 0.3f
private const val TrailStrokeWidthMultiplier = 0.5f

// Event horizon constants
private const val EventHorizonRadiusFactor = 0.35f
private const val EventHorizonGlowMultiplier = 1.4f

// Event horizon color hex values
private const val EventHorizonInnerColorHex = 0xFFFF4400
private const val EventHorizonMiddleColorHex = 0xFFFF6600
private const val EventHorizonOuterColorHex = 0xFFFFAA00

// Event horizon colors
private object EventHorizonColors {
    val Inner = Color(EventHorizonInnerColorHex)
    val Middle = Color(EventHorizonMiddleColorHex)
    val Outer = Color(EventHorizonOuterColorHex)
}

// Void constants
private const val VoidEdgeAlpha = 0.95f
private const val VoidOuterAlpha = 0.8f

// Void color hex values
private const val VoidEdgeColorHex = 0xFF110808
private const val VoidOuterColorHex = 0xFF1a0505

// Void colors
private object VoidColors {
    val Edge = Color(VoidEdgeColorHex)
    val Outer = Color(VoidOuterColorHex)
}

// Core flicker constants
private const val CoreFlickerSpeed = 3f
private const val CoreFlickerMaxAlpha = 0.5f
private const val CoreWhiteAlphaMultiplier = 0.3f
private const val CoreAmberAlphaMultiplier = 0.1f
private const val CoreRadiusFactor = 0.15f

// Lensing arc constants
private const val LensingArcCount = 6
private const val LensingArcSpacing = 60f
private const val LensingArcRotationFactor = 0.5f
private const val LensingArcStartAngle = -30f
private const val LensingArcSweepAngle = 60f
private const val LensingArcAlpha = 0.15f
private const val LensingArcRadiusFactor = 1.5f
private const val LensingArcSizeFactor = 3f
private const val LensingArcStrokeWidth = 2f

// Lensing arc color hex value
private const val LensingArcColorHex = 0xFF6688CC

// Lensing arc color
private object LensingArcColors {
    val Arc = Color(LensingArcColorHex)
}

// Document physics constants
private const val SpinAngleMultiplier = 720f
private const val StretchMultiplier = 2f
private const val ScaleReduction = 0.8f
private const val MinScale = 0.1f
private const val RedshiftGreenFactor = 0.7f
private const val RedshiftBlueFactor = 0.9f
private const val TrailBaseLength = 30f
private const val TrailMaxAddition = 80f
private const val TrailStrokeBase = 4f
private const val TrailGlowAlphaFactor = 0.2f
private const val TrailGlowAlphaOuter = 0.4f

// Trail color hex values
private const val TrailInnerColorHex = 0xFFFF6600
private const val TrailOuterColorHex = 0xFFFFAA00

// Trail colors
private object TrailColors {
    val Inner = Color(TrailInnerColorHex)
    val Outer = Color(TrailOuterColorHex)
}

// Document icon constants
private const val IconBaseWidth = 32f
private const val IconBaseHeight = 40f
private const val FoldAlphaMultiplier = 0.8f
private const val IconHalfDivisor = 2f
private const val IconQuarterDivisor = 4f

// Heat glow constants
private const val HeatGlowThreshold = 0.3f
private const val HeatGlowIntensityDivisor = 0.7f
private const val HeatGlowAlphaInner = 0.3f
private const val HeatGlowAlphaOuter = 0.15f
private const val HeatGlowRadius = 40f

// Heat glow color hex values
private const val HeatGlowInnerColorHex = 0xFFFF4400
private const val HeatGlowOuterColorHex = 0xFFFF6600

// Heat glow colors
private object HeatGlowColors {
    val Inner = Color(HeatGlowInnerColorHex)
    val Outer = Color(HeatGlowOuterColorHex)
}

// Canvas size
private val CanvasSize = 320.dp

/**
 * Particle being pulled into the black hole.
 */
private data class AccretionParticle(
    val angle: Float,
    val distance: Float,
    val speed: Float,
    val size: Float,
    val color: Color,
    val orbitOffset: Float
)

/**
 * The black hole vortex with event horizon and accretion disk.
 *
 * Renders a visually stunning black hole effect with:
 * - Gravitational lensing rings
 * - Rotating accretion disk with particles
 * - Pulsing event horizon
 * - Inner singularity glow
 *
 * @param isActive Whether the black hole is in active state (scales up when true)
 * @param modifier Modifier to apply to the vortex
 */
@Composable
fun BlackHoleVortex(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "blackHole")

    // Slow rotation for the accretion disk
    val diskRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = FullRotationDegrees,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = DiskRotationDurationMs, easing = LinearEasing)
        ),
        label = "diskRotation"
    )

    // Pulsing event horizon
    val horizonPulse by infiniteTransition.animateFloat(
        initialValue = HorizonPulseMin,
        targetValue = HorizonPulseMax,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = HorizonPulseDurationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "horizonPulse"
    )

    // Gravitational distortion waves
    val distortionWave by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = DistortionWaveDurationMs, easing = LinearEasing)
        ),
        label = "distortion"
    )

    val activeScale by animateFloatAsState(
        targetValue = if (isActive) ActiveScaleValue else DefaultScale,
        animationSpec = tween(ActiveScaleDurationMs, easing = FastOutSlowInEasing),
        label = "activeScale"
    )

    // Generate accretion particles
    val accretionParticles = remember {
        mutableStateListOf<AccretionParticle>().apply {
            repeat(AccretionParticleCount) { i ->
                add(
                    AccretionParticle(
                        angle = Random.nextFloat() * FullRotationDegrees,
                        distance = ParticleDistanceMin + Random.nextFloat() * ParticleDistanceRange,
                        speed = ParticleSpeedMin + Random.nextFloat() * ParticleSpeedRange,
                        size = ParticleSizeMin + Random.nextFloat() * ParticleSizeRange,
                        color = when (i % ParticleColorModulo) {
                            0 -> ParticleColors.Orange
                            1 -> ParticleColors.Amber
                            2 -> ParticleColors.Red
                            3 -> ParticleColors.DarkOrange
                            else -> ParticleColors.LightOrange
                        },
                        orbitOffset = Random.nextFloat() * FullRotationDegrees
                    )
                )
            }
        }
    }

    // Animate particles being sucked in
    var time by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(FrameDelayMs)
            time += FrameDeltaSeconds
        }
    }

    Canvas(modifier = modifier.size(CanvasSize)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = size.minDimension / 2f * activeScale

        // Outer gravitational lensing rings (subtle distortion effect)
        for (i in 0 until LensingRingCount) {
            val ringRadius = maxRadius * (LensingRingBaseRadius + i * LensingRingRadiusStep)
            val waveOffset = ((distortionWave + i * LensingRingWaveStep) % 1f)
            val ringAlpha = (LensingRingMaxAlpha - waveOffset * LensingRingMaxAlpha).coerceAtLeast(0f)

            drawCircle(
                color = LensingRingColors.Ring.copy(alpha = ringAlpha),
                radius = ringRadius * (1f + waveOffset * SpiralFactor),
                center = center,
                style = Stroke(width = LensingRingStrokeWidth)
            )
        }

        // Accretion disk (swirling matter)
        rotate(degrees = diskRotation, pivot = center) {
            accretionParticles.forEachIndexed { index, particle ->
                val orbitTime = time * particle.speed
                val currentAngle =
                    (particle.angle + particle.orbitOffset + orbitTime * OrbitSpeedMultiplier) % FullRotationDegrees
                val angleRad = currentAngle * DegreesToRadians

                // Particles spiral inward slowly
                val spiralFactor = ((sin(orbitTime + index) + 1f) / 2f) * SpiralFactor
                val currentDistance = particle.distance - spiralFactor

                // Elliptical orbit for disk effect (flattened)
                val x = center.x + cos(angleRad) * maxRadius * currentDistance
                val y = center.y + sin(angleRad) * maxRadius * currentDistance * DiskFlattenFactor

                // Particles in front (bottom half) are brighter
                val depthAlpha = if (sin(angleRad) > 0) FrontParticleAlpha else BackParticleAlpha

                // Motion blur trail
                val trailAngle = angleRad - TrailAngleOffset
                val trailX = center.x + cos(trailAngle) * maxRadius * currentDistance
                val trailY = center.y + sin(trailAngle) * maxRadius * currentDistance * DiskFlattenFactor

                drawLine(
                    color = particle.color.copy(alpha = depthAlpha * TrailAlphaMultiplier),
                    start = Offset(trailX, trailY),
                    end = Offset(x, y),
                    strokeWidth = particle.size * TrailStrokeWidthMultiplier
                )

                drawCircle(
                    color = particle.color.copy(alpha = depthAlpha),
                    radius = particle.size,
                    center = Offset(x, y)
                )
            }
        }

        // Event horizon glow (red/orange edge)
        val eventHorizonRadius = maxRadius * EventHorizonRadiusFactor * horizonPulse
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Transparent,
                    EventHorizonColors.Inner.copy(alpha = HeatGlowAlphaInner),
                    EventHorizonColors.Middle.copy(alpha = CoreFlickerMaxAlpha),
                    EventHorizonColors.Outer.copy(alpha = HeatGlowAlphaInner),
                    Color.Transparent
                ),
                center = center,
                radius = eventHorizonRadius * EventHorizonGlowMultiplier
            ),
            center = center,
            radius = eventHorizonRadius * EventHorizonGlowMultiplier
        )

        // The void (absolute black center)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Black,
                    Color.Black,
                    VoidColors.Edge.copy(alpha = VoidEdgeAlpha),
                    VoidColors.Outer.copy(alpha = VoidOuterAlpha),
                    Color.Transparent
                ),
                center = center,
                radius = eventHorizonRadius
            ),
            center = center,
            radius = eventHorizonRadius
        )

        // Inner singularity glow (tiny bright core sometimes visible)
        val coreFlicker = (sin(time * CoreFlickerSpeed) + 1f) / 2f * CoreFlickerMaxAlpha
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = coreFlicker * CoreWhiteAlphaMultiplier),
                    EventHorizonColors.Outer.copy(alpha = coreFlicker * CoreAmberAlphaMultiplier),
                    Color.Transparent
                ),
                center = center,
                radius = eventHorizonRadius * CoreRadiusFactor
            ),
            center = center,
            radius = eventHorizonRadius * CoreRadiusFactor
        )

        // Gravitational lensing arcs (light bending around black hole)
        for (i in 0 until LensingArcCount) {
            val arcAngle = diskRotation * LensingArcRotationFactor + i * LensingArcSpacing
            rotate(degrees = arcAngle, pivot = center) {
                drawArc(
                    color = LensingArcColors.Arc.copy(alpha = LensingArcAlpha),
                    startAngle = LensingArcStartAngle,
                    sweepAngle = LensingArcSweepAngle,
                    useCenter = false,
                    topLeft = Offset(
                        center.x - eventHorizonRadius * LensingArcRadiusFactor,
                        center.y - eventHorizonRadius * LensingArcRadiusFactor
                    ),
                    size = androidx.compose.ui.geometry.Size(
                        eventHorizonRadius * LensingArcSizeFactor,
                        eventHorizonRadius * LensingArcSizeFactor
                    ),
                    style = Stroke(width = LensingArcStrokeWidth)
                )
            }
        }
    }
}

/**
 * Layer that renders documents being pulled into the black hole with gravitational physics.
 *
 * Each document is animated with:
 * - Gravitational acceleration toward center
 * - Spaghettification (stretching as it approaches singularity)
 * - Redshift color effect
 * - Heat glow from friction with accretion disk
 *
 * @param documents List of flying documents to render
 */
@Composable
fun GravitationalDocumentsLayer(
    documents: List<FlyingDocument>
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)

        documents.forEach { doc ->
            val progress = doc.progress.value

            // Gravitational physics: accelerating fall with spaghettification
            val startPos = Offset(doc.startX * size.width, doc.startY * size.height)
            val distanceToCenter = sqrt(
                (center.x - startPos.x).pow(2) + (center.y - startPos.y).pow(2)
            )

            // Position along gravitational pull (accelerating curve already in animation)
            val currentDistance = distanceToCenter * (1f - progress)
            val directionX = (center.x - startPos.x) / distanceToCenter
            val directionY = (center.y - startPos.y) / distanceToCenter

            val x = startPos.x + directionX * (distanceToCenter - currentDistance)
            val y = startPos.y + directionY * (distanceToCenter - currentDistance)

            // Spaghettification: document stretches as it approaches singularity
            val stretchFactor = 1f + progress.pow(2) * StretchMultiplier
            val squishFactor = 1f / (1f + progress.pow(2))

            // Rotation as it falls in (spinning faster near center)
            val spinAngle = doc.targetAngle + progress.pow(2) * SpinAngleMultiplier

            // Scale down as it approaches event horizon
            val scale = (1f - progress * ScaleReduction).coerceAtLeast(MinScale)

            // Redshift effect: color shifts to red as it approaches event horizon
            val redshift = progress.pow(2)
            val docColor = Color(
                red = 1f,
                green = 1f - redshift * RedshiftGreenFactor,
                blue = 1f - redshift * RedshiftBlueFactor,
                alpha = (1f - progress * RedshiftGreenFactor).coerceAtLeast(MinScale)
            )

            // Gravitational glow trail
            val trailLength = TrailBaseLength + progress * TrailMaxAddition
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        TrailColors.Inner.copy(alpha = TrailGlowAlphaFactor * (1f - progress)),
                        TrailColors.Outer.copy(alpha = TrailGlowAlphaOuter * (1f - progress))
                    ),
                    start = Offset(
                        x - directionX * trailLength,
                        y - directionY * trailLength
                    ),
                    end = Offset(x, y)
                ),
                start = Offset(
                    x - directionX * trailLength,
                    y - directionY * trailLength
                ),
                end = Offset(x, y),
                strokeWidth = TrailStrokeBase * scale * squishFactor
            )

            // Document icon (with spaghettification stretch)
            val iconWidth = IconBaseWidth * scale * squishFactor
            val iconHeight = IconBaseHeight * scale * stretchFactor

            rotate(degrees = spinAngle, pivot = Offset(x, y)) {
                // Document body
                val iconPath = Path().apply {
                    moveTo(x - iconWidth / IconHalfDivisor, y - iconHeight / IconHalfDivisor)
                    lineTo(x + iconWidth / IconQuarterDivisor, y - iconHeight / IconHalfDivisor)
                    lineTo(x + iconWidth / IconHalfDivisor, y - iconHeight / IconQuarterDivisor)
                    lineTo(x + iconWidth / IconHalfDivisor, y + iconHeight / IconHalfDivisor)
                    lineTo(x - iconWidth / IconHalfDivisor, y + iconHeight / IconHalfDivisor)
                    close()
                }

                drawPath(
                    path = iconPath,
                    color = docColor
                )

                // Document fold
                val foldPath = Path().apply {
                    moveTo(x + iconWidth / IconQuarterDivisor, y - iconHeight / IconHalfDivisor)
                    lineTo(x + iconWidth / IconQuarterDivisor, y - iconHeight / IconQuarterDivisor)
                    lineTo(x + iconWidth / IconHalfDivisor, y - iconHeight / IconQuarterDivisor)
                    close()
                }
                drawPath(
                    path = foldPath,
                    color = TrailColors.Outer.copy(alpha = docColor.alpha * FoldAlphaMultiplier)
                )
            }

            // Heat glow around document (friction with accretion disk)
            if (progress > HeatGlowThreshold) {
                val glowIntensity = (progress - HeatGlowThreshold) / HeatGlowIntensityDivisor
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            HeatGlowColors.Inner.copy(alpha = glowIntensity * HeatGlowAlphaInner),
                            HeatGlowColors.Outer.copy(alpha = glowIntensity * HeatGlowAlphaOuter),
                            Color.Transparent
                        ),
                        center = Offset(x, y),
                        radius = HeatGlowRadius * scale
                    ),
                    center = Offset(x, y),
                    radius = HeatGlowRadius * scale
                )
            }
        }
    }
}
