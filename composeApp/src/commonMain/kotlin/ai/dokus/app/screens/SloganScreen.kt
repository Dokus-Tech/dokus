package ai.dokus.app.screens

import ai.dokus.foundation.design.tooling.PreviewParameters
import ai.dokus.foundation.design.tooling.PreviewParametersProvider
import ai.dokus.foundation.design.tooling.TestWrapper
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.app_name
import ai.dokus.app.resources.generated.app_slogan
import ai.dokus.app.resources.generated.copyright
import ai.dokus.app.resources.generated.develop_by
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SloganScreen() {
    // Animation states for different stages
    val pulseWave1 = remember { Animatable(0f) }
    val pulseWave2 = remember { Animatable(0f) }
    val pulseWave3 = remember { Animatable(0f) }

    val appNameAlpha = remember { Animatable(0f) }
    val appNameScale = remember { Animatable(0.5f) }

    val sloganLine1Alpha = remember { Animatable(0f) }
    val sloganLine1OffsetY = remember { Animatable(30f) }

    val sloganLine2Alpha = remember { Animatable(0f) }
    val sloganLine2OffsetY = remember { Animatable(30f) }

    val sloganLine3Alpha = remember { Animatable(0f) }
    val sloganLine3OffsetY = remember { Animatable(30f) }

    val creditsAlpha = remember { Animatable(0f) }

    val gridProgress = remember { Animatable(0f) }
    val particleFlow = remember { Animatable(0f) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    LaunchedEffect(Unit) {
        // Stage 1: Pulse waves (0-1.5s)
        launch {
            pulseWave1.animateTo(
                targetValue = 1f,
                animationSpec = tween(1500, easing = FastOutSlowInEasing)
            )
        }
        launch {
            delay(300)
            pulseWave2.animateTo(
                targetValue = 1f,
                animationSpec = tween(1200, easing = FastOutSlowInEasing)
            )
        }
        launch {
            delay(600)
            pulseWave3.animateTo(
                targetValue = 1f,
                animationSpec = tween(900, easing = FastOutSlowInEasing)
            )
        }

        // Stage 2: App name (1.0-2.0s)
        delay(1000)
        launch {
            appNameAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(800, easing = FastOutSlowInEasing)
            )
        }
        launch {
            appNameScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }

        // Stage 3: Slogan lines (2.5-4.5s)
        delay(1500)
        launch {
            sloganLine1Alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(600, easing = FastOutSlowInEasing)
            )
        }
        launch {
            sloganLine1OffsetY.animateTo(
                targetValue = 0f,
                animationSpec = tween(600, easing = FastOutSlowInEasing)
            )
        }

        delay(400)
        launch {
            sloganLine2Alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(600, easing = FastOutSlowInEasing)
            )
        }
        launch {
            sloganLine2OffsetY.animateTo(
                targetValue = 0f,
                animationSpec = tween(600, easing = FastOutSlowInEasing)
            )
        }

        delay(400)
        launch {
            sloganLine3Alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(600, easing = FastOutSlowInEasing)
            )
        }
        launch {
            sloganLine3OffsetY.animateTo(
                targetValue = 0f,
                animationSpec = tween(600, easing = FastOutSlowInEasing)
            )
        }

        // Stage 4: Credits (4.5s)
        delay(600)
        creditsAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(1000, easing = LinearEasing)
        )
    }

    // Continuous background animations
    LaunchedEffect(Unit) {
        gridProgress.animateTo(
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(10000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        )
    }

    LaunchedEffect(Unit) {
        particleFlow.animateTo(
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(8000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        )
    }

    val sloganLines = stringResource(Res.string.app_slogan).split("\n")

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Animated background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val centerX = canvasWidth / 2
            val centerY = canvasHeight / 2

            // Gradient background
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        surfaceColor,
                        surfaceColor.copy(alpha = 0.8f)
                    ),
                    center = Offset(centerX, centerY),
                    radius = canvasWidth * 1.5f
                )
            )

            // Animated grid network
            val gridSpacing = 80f
            val gridOffset = (gridProgress.value * gridSpacing) % gridSpacing

            // Vertical lines
            var x = gridOffset
            while (x < canvasWidth) {
                val alpha =
                    0.05f + (sin((x / canvasWidth) * PI + gridProgress.value * 2 * PI) * 0.03f).toFloat()
                drawLine(
                    color = primaryColor.copy(alpha = alpha),
                    start = Offset(x, 0f),
                    end = Offset(x, canvasHeight),
                    strokeWidth = 1.5f
                )
                x += gridSpacing
            }

            // Horizontal lines
            var y = gridOffset
            while (y < canvasHeight) {
                val alpha =
                    0.05f + (cos((y / canvasHeight) * PI + gridProgress.value * 2 * PI) * 0.03f).toFloat()
                drawLine(
                    color = primaryColor.copy(alpha = alpha),
                    start = Offset(0f, y),
                    end = Offset(canvasWidth, y),
                    strokeWidth = 1.5f
                )
                y += gridSpacing
            }

            // Flowing particles (data nodes)
            val particleCount = 30
            for (i in 0 until particleCount) {
                val baseAngle = (2 * PI * i / particleCount)
                val radius =
                    (canvasWidth * 0.3f) + (sin(baseAngle * 3 + particleFlow.value * 2 * PI) * 50f).toFloat()
                val angle = baseAngle + particleFlow.value * 2 * PI

                val px = centerX + (radius * cos(angle)).toFloat()
                val py = centerY + (radius * sin(angle)).toFloat()

                val particleAlpha =
                    0.3f + (sin(angle * 2 + particleFlow.value * 4 * PI) * 0.3f).toFloat()

                drawCircle(
                    color = primaryColor.copy(alpha = particleAlpha),
                    radius = 4f,
                    center = Offset(px, py)
                )

                // Connection lines between nearby particles
                if (i % 3 == 0) {
                    val nextIdx = (i + 1) % particleCount
                    val nextAngle = (2 * PI * nextIdx / particleCount) + particleFlow.value * 2 * PI
                    val nextRadius =
                        (canvasWidth * 0.3f) + (sin(nextAngle * 3 + particleFlow.value * 2 * PI) * 50f).toFloat()
                    val nextPx = centerX + (nextRadius * cos(nextAngle)).toFloat()
                    val nextPy = centerY + (nextRadius * sin(nextAngle)).toFloat()

                    drawLine(
                        color = primaryColor.copy(alpha = 0.15f),
                        start = Offset(px, py),
                        end = Offset(nextPx, nextPy),
                        strokeWidth = 1f
                    )
                }
            }

            // Pulse waves from center (heartbeat effect)
            listOf(
                pulseWave1.value to 0.6f,
                pulseWave2.value to 0.5f,
                pulseWave3.value to 0.4f
            ).forEach { (progress, maxAlpha) ->
                if (progress > 0f) {
                    val waveRadius = canvasWidth * 0.4f * progress
                    val waveAlpha = maxAlpha * (1f - progress)

                    drawCircle(
                        color = primaryColor.copy(alpha = waveAlpha),
                        radius = waveRadius,
                        center = Offset(centerX, centerY),
                        style = Stroke(width = 3f)
                    )
                }
            }

            // Central pulse indicator
            val pulseSize = 12f + (sin(particleFlow.value * 2 * PI * 2) * 4f).toFloat()
            drawCircle(
                color = primaryColor,
                radius = pulseSize,
                center = Offset(centerX, centerY)
            )
            drawCircle(
                color = primaryColor.copy(alpha = 0.3f),
                radius = pulseSize * 2f,
                center = Offset(centerX, centerY)
            )
        }

        // Content overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(60.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App name with scale and fade animation
            Text(
                text = stringResource(Res.string.app_name),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 96.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 8.sp
                ),
                color = primaryColor,
                modifier = Modifier
                    .alpha(appNameAlpha.value)
                    .scale(appNameScale.value)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Slogan lines with staggered entrance
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (sloganLines.size >= 3) {
                    Text(
                        text = sloganLines[0],
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 2.sp
                        ),
                        color = onSurfaceColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .alpha(sloganLine1Alpha.value)
                            .offset(y = sloganLine1OffsetY.value.dp)
                    )

                    Text(
                        text = sloganLines[1],
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Normal,
                            letterSpacing = 1.sp
                        ),
                        color = onSurfaceColor.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .alpha(sloganLine2Alpha.value)
                            .offset(y = sloganLine2OffsetY.value.dp)
                    )

                    Text(
                        text = sloganLines[2],
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Normal,
                            letterSpacing = 1.sp
                        ),
                        color = onSurfaceColor.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .alpha(sloganLine3Alpha.value)
                            .offset(y = sloganLine3OffsetY.value.dp)
                    )
                }
            }
        }

        // Credits at bottom
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.alpha(creditsAlpha.value)
            ) {
                Text(
                    text = stringResource(Res.string.develop_by),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp
                    ),
                    color = onSurfaceColor.copy(alpha = 0.6f)
                )
                Text(
                    text = stringResource(Res.string.copyright),
                    style = MaterialTheme.typography.bodySmall,
                    color = onSurfaceColor.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Preview
@Composable
private fun SloganPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        SloganScreen()
    }
}
