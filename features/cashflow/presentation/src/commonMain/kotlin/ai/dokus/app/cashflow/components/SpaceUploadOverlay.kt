package ai.dokus.app.cashflow.components

import ai.dokus.foundation.design.components.background.EnhancedFloatingBubbles
import ai.dokus.foundation.design.components.background.SpotlightEffect
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * Data class representing a flying document in the upload animation.
 */
data class FlyingDocument(
    val id: String,
    val name: String,
    val startX: Float,
    val startY: Float,
    val targetAngle: Float, // Direction to fly
    val progress: Animatable<Float, *> = Animatable(0f)
)

/**
 * Futuristic upload overlay matching the workspace select/create style.
 * Uses EnhancedFloatingBubbles and SpotlightEffect for consistent visual language.
 *
 * @param isVisible Whether the overlay is visible
 * @param isDragging Whether files are currently being dragged
 * @param flyingDocuments List of documents currently flying in the animation
 * @param onAnimationComplete Called when all documents have flown away
 * @param modifier Optional modifier
 */
@Composable
fun SpaceUploadOverlay(
    isVisible: Boolean,
    isDragging: Boolean,
    flyingDocuments: List<FlyingDocument>,
    onAnimationComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Track animation completion
    LaunchedEffect(flyingDocuments) {
        if (flyingDocuments.isNotEmpty()) {
            // Wait for all documents to complete their flight
            flyingDocuments.forEach { doc ->
                doc.progress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 1200,
                        easing = FastOutSlowInEasing
                    )
                )
            }
            delay(300) // Brief pause after animation
            onAnimationComplete()
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(400)),
        exit = fadeOut(tween(600)),
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Matching workspace style background
            EnhancedFloatingBubbles()

            // Golden spotlight from top
            SpotlightEffect()

            // Central upload portal (golden/mystical style)
            UploadPortal(
                isActive = isDragging,
                modifier = Modifier.align(Alignment.Center)
            )

            // Drop zone text
            if (isDragging) {
                DropZonePrompt(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Flying documents
            FlyingDocumentsLayer(
                documents = flyingDocuments
            )
        }
    }
}

/**
 * Golden mystical upload portal matching the workspace style.
 */
@Composable
private fun UploadPortal(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "uploadPortal")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing)
        ),
        label = "portalRotation"
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "portalPulse"
    )

    val breathe by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    val activeScale by animateFloatAsState(
        targetValue = if (isActive) 1.3f else 1f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "activeScale"
    )

    // Portal colors matching the workspace theme
    val goldenCore = Color(0xFFD4AF37)
    val goldenLight = Color(0xFFFFE4A0)
    val purple = Color(0xFF9370DB)
    val turquoise = Color(0xFF40E0D0)

    Canvas(modifier = modifier.size(280.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = size.minDimension / 2f * pulse * activeScale

        // Outer mystical rings with prismatic colors
        for (i in 0..4) {
            val ringRadius = maxRadius * (0.6f + i * 0.1f)
            val ringAlpha = (0.3f - i * 0.05f) * breathe

            rotate(degrees = rotation * (if (i % 2 == 0) 1f else -0.5f) + i * 15f, pivot = center) {
                // Golden ring
                drawCircle(
                    color = goldenCore.copy(alpha = ringAlpha),
                    radius = ringRadius,
                    center = center,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                )

                // Purple accent ring (offset)
                drawCircle(
                    color = purple.copy(alpha = ringAlpha * 0.5f),
                    radius = ringRadius * 0.95f,
                    center = Offset(center.x + 3f, center.y + 3f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
                )
            }
        }

        // Spinning energy arcs (golden)
        for (i in 0..5) {
            val arcAngle = rotation * 1.5f + i * 60f
            rotate(degrees = arcAngle, pivot = center) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            goldenLight.copy(alpha = 0.4f * breathe),
                            goldenCore.copy(alpha = 0.6f * breathe),
                            goldenLight.copy(alpha = 0.4f * breathe),
                            Color.Transparent
                        ),
                        center = center
                    ),
                    startAngle = 0f,
                    sweepAngle = 30f,
                    useCenter = false,
                    topLeft = Offset(center.x - maxRadius * 0.5f, center.y - maxRadius * 0.5f),
                    size = androidx.compose.ui.geometry.Size(maxRadius, maxRadius),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                )
            }
        }

        // Inner portal gradient (the mystical void)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF1a1a2e).copy(alpha = 0.95f), // Deep void center
                    Color(0xFF16213e).copy(alpha = 0.8f),
                    purple.copy(alpha = 0.4f),
                    goldenCore.copy(alpha = 0.2f),
                    Color.Transparent
                ),
                center = center,
                radius = maxRadius * 0.45f
            ),
            center = center,
            radius = maxRadius * 0.45f
        )

        // Bright golden core
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.9f * breathe),
                    goldenLight.copy(alpha = 0.6f * breathe),
                    goldenCore.copy(alpha = 0.3f * breathe),
                    Color.Transparent
                ),
                center = center,
                radius = maxRadius * 0.15f
            ),
            center = center,
            radius = maxRadius * 0.15f
        )

        // Crystal particles orbiting (matching EnhancedFloatingBubbles style)
        for (i in 0..15) {
            val particleAngle = (rotation * 2f + i * 24f) * 0.017453f
            val particleDistance = maxRadius * (0.35f + (i % 4) * 0.08f)
            val particlePos = Offset(
                center.x + cos(particleAngle) * particleDistance,
                center.y + sin(particleAngle) * particleDistance
            )

            // Prismatic particle glow
            val particleColor = when (i % 4) {
                0 -> goldenCore
                1 -> purple
                2 -> turquoise
                else -> goldenLight
            }

            drawCircle(
                color = particleColor.copy(alpha = 0.6f * breathe),
                radius = 3f,
                center = particlePos
            )

            // Tiny highlight
            drawCircle(
                color = Color.White.copy(alpha = 0.8f),
                radius = 1f,
                center = particlePos
            )
        }
    }
}

/**
 * Prompt text shown when dragging files.
 */
@Composable
private fun DropZonePrompt(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "promptPulse")

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "textPulse"
    )

    // Golden text matching the theme
    val goldenColor = Color(0xFFD4AF37)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(160.dp))

        Text(
            text = "Release to Upload",
            style = MaterialTheme.typography.headlineMedium,
            color = goldenColor.copy(alpha = alpha),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Your document will enter the portal",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = alpha * 0.8f),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Layer that renders flying documents with golden trail effect.
 */
@Composable
private fun FlyingDocumentsLayer(
    documents: List<FlyingDocument>
) {
    // Colors matching the theme
    val goldenCore = Color(0xFFD4AF37)
    val goldenLight = Color(0xFFFFE4A0)
    val purple = Color(0xFF9370DB)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)

        documents.forEach { doc ->
            val progress = doc.progress.value

            // Calculate position: start -> center -> spiral into portal
            val x: Float
            val y: Float
            val scale: Float
            val alpha: Float

            if (progress < 0.5f) {
                // Phase 1: Move to center
                val phase = progress * 2f
                val easedPhase = 1f - (1f - phase).pow(3) // Ease out cubic
                x = doc.startX * size.width + (center.x - doc.startX * size.width) * easedPhase
                y = doc.startY * size.height + (center.y - doc.startY * size.height) * easedPhase
                scale = 1f - phase * 0.2f
                alpha = 1f
            } else {
                // Phase 2: Spiral into portal and disappear
                val phase = (progress - 0.5f) * 2f
                val spiralAngle = doc.targetAngle + phase * 540f // Spin 1.5 rotations
                val spiralRadius = (1f - phase) * 80f
                val easedPhase = phase.pow(2)

                x = center.x + cos(spiralAngle * 0.017453f) * spiralRadius
                y = center.y + sin(spiralAngle * 0.017453f) * spiralRadius
                scale = (1f - phase) * 0.8f
                alpha = 1f - easedPhase
            }

            if (alpha > 0.01f && scale > 0.01f) {
                // Golden glow trail
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            goldenLight.copy(alpha = alpha * 0.4f),
                            goldenCore.copy(alpha = alpha * 0.2f),
                            purple.copy(alpha = alpha * 0.1f),
                            Color.Transparent
                        ),
                        center = Offset(x, y),
                        radius = 50f * scale
                    ),
                    center = Offset(x, y),
                    radius = 50f * scale
                )

                // Document icon
                val iconSize = 36f * scale
                val iconPath = Path().apply {
                    moveTo(x - iconSize / 2, y - iconSize / 2)
                    lineTo(x + iconSize / 4, y - iconSize / 2)
                    lineTo(x + iconSize / 2, y - iconSize / 4)
                    lineTo(x + iconSize / 2, y + iconSize / 2)
                    lineTo(x - iconSize / 2, y + iconSize / 2)
                    close()
                }

                // Document body with golden tint
                drawPath(
                    path = iconPath,
                    color = Color.White.copy(alpha = alpha * 0.95f)
                )

                // Document fold corner
                val foldPath = Path().apply {
                    moveTo(x + iconSize / 4, y - iconSize / 2)
                    lineTo(x + iconSize / 4, y - iconSize / 4)
                    lineTo(x + iconSize / 2, y - iconSize / 4)
                    close()
                }
                drawPath(
                    path = foldPath,
                    color = goldenLight.copy(alpha = alpha * 0.8f)
                )

                // Energy trail (golden)
                if (progress > 0.1f) {
                    val trailLength = 40f * progress
                    val trailAngle = if (progress < 0.5f) {
                        kotlin.math.atan2(y - doc.startY * size.height, x - doc.startX * size.width)
                    } else {
                        (doc.targetAngle + (progress - 0.5f) * 540f - 90f) * 0.017453f
                    }

                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                goldenCore.copy(alpha = alpha * 0.3f),
                                goldenLight.copy(alpha = alpha * 0.5f)
                            ),
                            start = Offset(
                                x - cos(trailAngle) * trailLength,
                                y - sin(trailAngle) * trailLength
                            ),
                            end = Offset(x, y)
                        ),
                        start = Offset(
                            x - cos(trailAngle) * trailLength,
                            y - sin(trailAngle) * trailLength
                        ),
                        end = Offset(x, y),
                        strokeWidth = 3f * scale
                    )
                }
            }
        }
    }
}
