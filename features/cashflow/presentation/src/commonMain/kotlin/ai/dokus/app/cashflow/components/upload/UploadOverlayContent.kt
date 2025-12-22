package ai.dokus.app.cashflow.components.upload

import ai.dokus.app.cashflow.components.FlyingDocument
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

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
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing)
        ),
        label = "diskRotation"
    )

    // Pulsing event horizon
    val horizonPulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "horizonPulse"
    )

    // Gravitational distortion waves
    val distortionWave by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing)
        ),
        label = "distortion"
    )

    val activeScale by animateFloatAsState(
        targetValue = if (isActive) 1.2f else 1f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "activeScale"
    )

    // Generate accretion particles
    val accretionParticles = remember {
        mutableStateListOf<AccretionParticle>().apply {
            repeat(60) { i ->
                add(
                    AccretionParticle(
                        angle = Random.nextFloat() * 360f,
                        distance = 0.4f + Random.nextFloat() * 0.5f,
                        speed = 0.5f + Random.nextFloat() * 1.5f,
                        size = 1f + Random.nextFloat() * 3f,
                        color = when (i % 5) {
                            0 -> Color(0xFFFF6B35) // Orange
                            1 -> Color(0xFFFFAA00) // Amber
                            2 -> Color(0xFFFF4444) // Red
                            3 -> Color(0xFFCC3300) // Dark orange
                            else -> Color(0xFFFFDD88) // Light orange
                        },
                        orbitOffset = Random.nextFloat() * 360f
                    )
                )
            }
        }
    }

    // Animate particles being sucked in
    var time by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(16)
            time += 0.016f
        }
    }

    Canvas(modifier = modifier.size(320.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = size.minDimension / 2f * activeScale

        // Outer gravitational lensing rings (subtle distortion effect)
        for (i in 0..3) {
            val ringRadius = maxRadius * (0.9f + i * 0.15f)
            val waveOffset = ((distortionWave + i * 0.25f) % 1f)
            val ringAlpha = (0.15f - waveOffset * 0.15f).coerceAtLeast(0f)

            drawCircle(
                color = Color(0xFF4466AA).copy(alpha = ringAlpha),
                radius = ringRadius * (1f + waveOffset * 0.1f),
                center = center,
                style = Stroke(width = 1.5f)
            )
        }

        // Accretion disk (swirling matter)
        rotate(degrees = diskRotation, pivot = center) {
            accretionParticles.forEachIndexed { index, particle ->
                val orbitTime = time * particle.speed
                val currentAngle = (particle.angle + particle.orbitOffset + orbitTime * 60f) % 360f
                val angleRad = currentAngle * (PI.toFloat() / 180f)

                // Particles spiral inward slowly
                val spiralFactor = ((sin(orbitTime + index) + 1f) / 2f) * 0.1f
                val currentDistance = particle.distance - spiralFactor

                // Elliptical orbit for disk effect (flattened)
                val x = center.x + cos(angleRad) * maxRadius * currentDistance
                val y = center.y + sin(angleRad) * maxRadius * currentDistance * 0.3f // Flatten

                // Particles in front (bottom half) are brighter
                val depthAlpha = if (sin(angleRad) > 0) 0.8f else 0.4f

                // Motion blur trail
                val trailAngle = angleRad - 0.3f
                val trailX = center.x + cos(trailAngle) * maxRadius * currentDistance
                val trailY = center.y + sin(trailAngle) * maxRadius * currentDistance * 0.3f

                drawLine(
                    color = particle.color.copy(alpha = depthAlpha * 0.3f),
                    start = Offset(trailX, trailY),
                    end = Offset(x, y),
                    strokeWidth = particle.size * 0.5f
                )

                drawCircle(
                    color = particle.color.copy(alpha = depthAlpha),
                    radius = particle.size,
                    center = Offset(x, y)
                )
            }
        }

        // Event horizon glow (red/orange edge)
        val eventHorizonRadius = maxRadius * 0.35f * horizonPulse
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFFFF4400).copy(alpha = 0.3f),
                    Color(0xFFFF6600).copy(alpha = 0.5f),
                    Color(0xFFFFAA00).copy(alpha = 0.3f),
                    Color.Transparent
                ),
                center = center,
                radius = eventHorizonRadius * 1.4f
            ),
            center = center,
            radius = eventHorizonRadius * 1.4f
        )

        // The void (absolute black center)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Black,
                    Color.Black,
                    Color(0xFF110808).copy(alpha = 0.95f),
                    Color(0xFF1a0505).copy(alpha = 0.8f),
                    Color.Transparent
                ),
                center = center,
                radius = eventHorizonRadius
            ),
            center = center,
            radius = eventHorizonRadius
        )

        // Inner singularity glow (tiny bright core sometimes visible)
        val coreFlicker = (sin(time * 3f) + 1f) / 2f * 0.5f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = coreFlicker * 0.3f),
                    Color(0xFFFFAA00).copy(alpha = coreFlicker * 0.1f),
                    Color.Transparent
                ),
                center = center,
                radius = eventHorizonRadius * 0.15f
            ),
            center = center,
            radius = eventHorizonRadius * 0.15f
        )

        // Gravitational lensing arcs (light bending around black hole)
        for (i in 0..5) {
            val arcAngle = diskRotation * 0.5f + i * 60f
            rotate(degrees = arcAngle, pivot = center) {
                drawArc(
                    color = Color(0xFF6688CC).copy(alpha = 0.15f),
                    startAngle = -30f,
                    sweepAngle = 60f,
                    useCenter = false,
                    topLeft = Offset(
                        center.x - eventHorizonRadius * 1.5f,
                        center.y - eventHorizonRadius * 1.5f
                    ),
                    size = androidx.compose.ui.geometry.Size(
                        eventHorizonRadius * 3f,
                        eventHorizonRadius * 3f
                    ),
                    style = Stroke(width = 2f)
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
            val stretchFactor = 1f + progress.pow(2) * 2f
            val squishFactor = 1f / (1f + progress.pow(2))

            // Rotation as it falls in (spinning faster near center)
            val spinAngle = doc.targetAngle + progress.pow(2) * 720f

            // Scale down as it approaches event horizon
            val scale = (1f - progress * 0.8f).coerceAtLeast(0.1f)

            // Redshift effect: color shifts to red as it approaches event horizon
            val redshift = progress.pow(2)
            val docColor = Color(
                red = 1f,
                green = 1f - redshift * 0.7f,
                blue = 1f - redshift * 0.9f,
                alpha = (1f - progress * 0.7f).coerceAtLeast(0.1f)
            )

            // Gravitational glow trail
            val trailLength = 30f + progress * 80f
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFFFF6600).copy(alpha = 0.2f * (1f - progress)),
                        Color(0xFFFFAA00).copy(alpha = 0.4f * (1f - progress))
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
                strokeWidth = 4f * scale * squishFactor
            )

            // Document icon (with spaghettification stretch)
            val iconWidth = 32f * scale * squishFactor
            val iconHeight = 40f * scale * stretchFactor

            rotate(degrees = spinAngle, pivot = Offset(x, y)) {
                // Document body
                val iconPath = Path().apply {
                    moveTo(x - iconWidth / 2, y - iconHeight / 2)
                    lineTo(x + iconWidth / 4, y - iconHeight / 2)
                    lineTo(x + iconWidth / 2, y - iconHeight / 4)
                    lineTo(x + iconWidth / 2, y + iconHeight / 2)
                    lineTo(x - iconWidth / 2, y + iconHeight / 2)
                    close()
                }

                drawPath(
                    path = iconPath,
                    color = docColor
                )

                // Document fold
                val foldPath = Path().apply {
                    moveTo(x + iconWidth / 4, y - iconHeight / 2)
                    lineTo(x + iconWidth / 4, y - iconHeight / 4)
                    lineTo(x + iconWidth / 2, y - iconHeight / 4)
                    close()
                }
                drawPath(
                    path = foldPath,
                    color = Color(0xFFFFAA00).copy(alpha = docColor.alpha * 0.8f)
                )
            }

            // Heat glow around document (friction with accretion disk)
            if (progress > 0.3f) {
                val glowIntensity = (progress - 0.3f) / 0.7f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFF4400).copy(alpha = glowIntensity * 0.3f),
                            Color(0xFFFF6600).copy(alpha = glowIntensity * 0.15f),
                            Color.Transparent
                        ),
                        center = Offset(x, y),
                        radius = 40f * scale
                    ),
                    center = Offset(x, y),
                    radius = 40f * scale
                )
            }
        }
    }
}
