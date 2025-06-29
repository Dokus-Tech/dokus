package ai.thepredict.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import thepredict.application.ui.generated.resources.Res
import thepredict.application.ui.generated.resources.background_gradient
import kotlin.random.Random

private data class Dot(
    var position: Offset,
    var velocity: Offset,
    val radius: Float,
    val blur: Dp,
    val color: Color
)

@Composable
fun BackgroundGradientAnimated(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val blurRadius by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 98f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = ""
    )

    Box(modifier.fillMaxSize()) {
        Image(
            painter = painterResource(Res.drawable.background_gradient),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(blurRadius.dp)
        )

        val dotCount = 12
        val dots = remember {
            List(dotCount) {
                Dot(
                    position = Offset(Random.nextFloat() * 2000f, Random.nextFloat() * 2000f),
                    velocity = Offset(
                        (Random.nextFloat() - 0.5f) * 2.0f, // Range: [-1.0, 1.0]
                        (Random.nextFloat() - 0.5f) * 2.0f
                    ),
                    radius = Random.nextFloat() * 48f + 32f,
                    blur = (Random.nextFloat() * 24f + 12f).dp,
                    color = Color.White.copy(alpha = 0.11f + Random.nextFloat() * 0.22f)
                )
            }
        }
        val boxSize = remember { mutableStateOf(Offset.Zero) }

        // Animate dots
        LaunchedEffect(Unit) {
            while (true) {
                withFrameNanos { _ ->
                    dots.forEach { dot ->
                        dot.position = Offset(
                            dot.position.x + dot.velocity.x,
                            dot.position.y + dot.velocity.y
                        )
                        // Bounce logic
                        val maxX = boxSize.value.x
                        val maxY = boxSize.value.y
                        if (dot.position.x < 0f || dot.position.x > maxX) {
                            dot.velocity = Offset(-dot.velocity.x, dot.velocity.y)
                        }
                        if (dot.position.y < 0f || dot.position.y > maxY) {
                            dot.velocity = Offset(dot.velocity.x, -dot.velocity.y)
                        }
                    }
                }
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .blur(24.dp) // Global blur for "soft" dots
                .onSizeChanged {
                    boxSize.value = Offset(it.width.toFloat(), it.height.toFloat())
                }
        ) {
            dots.forEach { dot ->
                drawCircle(
                    color = dot.color,
                    radius = dot.radius,
                    center = dot.position
                )
            }
        }
    }
}