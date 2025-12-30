package tech.dokus.app.screens
import tech.dokus.foundation.aura.constrains.withContentPadding
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.under_development_subtitle
import tech.dokus.aura.resources.under_development_title
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun UnderDevelopmentScreen() {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    val progress1 = remember { Animatable(0f) }
    val progress2 = remember { Animatable(0f) }
    val progress3 = remember { Animatable(0f) }
    val particleProgress = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Staggered shape animations
        progress1.animateTo(
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    LaunchedEffect(Unit) {
        progress2.animateTo(
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2500, delayMillis = 300, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    LaunchedEffect(Unit) {
        progress3.animateTo(
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2800, delayMillis = 600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    LaunchedEffect(Unit) {
        particleProgress.animateTo(
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(4000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        )
    }

    LaunchedEffect(Unit) {
        titleAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(1000, delayMillis = 300, easing = FastOutSlowInEasing)
        )
    }

    LaunchedEffect(Unit) {
        subtitleAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(1000, delayMillis = 600, easing = FastOutSlowInEasing)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .withContentPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            // Custom Canvas Animation
            Canvas(
                modifier = Modifier.size(280.dp)
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val centerX = canvasWidth / 2
                val centerY = canvasHeight / 2

                // Background grid pattern (subtle)
                val gridSpacing = 30f
                for (i in 0..canvasWidth.toInt() step gridSpacing.toInt()) {
                    drawLine(
                        color = surfaceVariant.copy(alpha = 0.2f),
                        start = Offset(i.toFloat(), 0f),
                        end = Offset(i.toFloat(), canvasHeight),
                        strokeWidth = 1f
                    )
                }
                for (i in 0..canvasHeight.toInt() step gridSpacing.toInt()) {
                    drawLine(
                        color = surfaceVariant.copy(alpha = 0.2f),
                        start = Offset(0f, i.toFloat()),
                        end = Offset(canvasWidth, i.toFloat()),
                        strokeWidth = 1f
                    )
                }

                // Animated particles orbiting
                val particleCount = 12
                val orbitRadius = 100f
                for (i in 0 until particleCount) {
                    val angle = (2 * PI * i / particleCount) + (particleProgress.value * 2 * PI)
                    val x = centerX + (orbitRadius * cos(angle)).toFloat()
                    val y = centerY + (orbitRadius * sin(angle)).toFloat()
                    val particleAlpha =
                        0.4f + (sin(angle + particleProgress.value * 2 * PI) * 0.4f).toFloat()

                    drawCircle(
                        color = primaryColor.copy(alpha = particleAlpha),
                        radius = 3f,
                        center = Offset(x, y)
                    )
                }

                // Central hexagon outline (animated)
                val hexRadius = 80f
                val hexPath = Path().apply {
                    for (i in 0..6) {
                        val angle = (PI / 3 * i) - PI / 2
                        val x = centerX + (hexRadius * cos(angle) * progress1.value).toFloat()
                        val y = centerY + (hexRadius * sin(angle) * progress1.value).toFloat()
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                }
                drawPath(
                    path = hexPath,
                    color = primaryColor,
                    style = Stroke(width = 3f, cap = StrokeCap.Round)
                )

                // Inner triangle (rotating)
                rotate(degrees = particleProgress.value * 360f, pivot = Offset(centerX, centerY)) {
                    val triangleRadius = 50f * progress2.value
                    val trianglePath = Path().apply {
                        for (i in 0..3) {
                            val angle = (2 * PI / 3 * i) - PI / 2
                            val x = centerX + (triangleRadius * cos(angle)).toFloat()
                            val y = centerY + (triangleRadius * sin(angle)).toFloat()
                            if (i == 0) moveTo(x, y) else lineTo(x, y)
                        }
                    }
                    drawPath(
                        path = trianglePath,
                        color = primaryColor.copy(alpha = 0.6f),
                        style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                    )
                }

                // Outer square (pulsing)
                val squareSize = 140f * progress3.value
                drawRect(
                    color = primaryColor.copy(alpha = 0.3f),
                    topLeft = Offset(centerX - squareSize / 2, centerY - squareSize / 2),
                    size = Size(squareSize, squareSize),
                    style = Stroke(width = 2f, cap = StrokeCap.Round)
                )

                // Central dot
                drawCircle(
                    color = primaryColor,
                    radius = 8f,
                    center = Offset(centerX, centerY)
                )

                // Connecting lines (animated)
                val lineProgress = progress1.value
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0f),
                            primaryColor.copy(alpha = 0.8f)
                        ),
                        start = Offset(centerX, centerY),
                        end = Offset(centerX + 60f * lineProgress, centerY)
                    ),
                    start = Offset(centerX, centerY),
                    end = Offset(centerX + 60f * lineProgress, centerY),
                    strokeWidth = 2f,
                    cap = StrokeCap.Round
                )

                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0f),
                            primaryColor.copy(alpha = 0.8f)
                        ),
                        start = Offset(centerX, centerY),
                        end = Offset(centerX - 60f * lineProgress, centerY)
                    ),
                    start = Offset(centerX, centerY),
                    end = Offset(centerX - 60f * lineProgress, centerY),
                    strokeWidth = 2f,
                    cap = StrokeCap.Round
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Title
            Text(
                text = stringResource(Res.string.under_development_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(titleAlpha.value)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Subtitle
            Text(
                text = stringResource(Res.string.under_development_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(subtitleAlpha.value)
            )
        }
    }
}
