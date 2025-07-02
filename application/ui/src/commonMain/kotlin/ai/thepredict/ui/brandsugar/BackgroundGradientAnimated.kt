package ai.thepredict.ui.brandsugar

import BackgroundAnimationViewModel
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import thepredict.application.ui.generated.resources.Res
import thepredict.application.ui.generated.resources.background_gradient

@Composable
fun BackgroundGradientAnimated(
    modifier: Modifier = Modifier,
    animationViewModel: BackgroundAnimationViewModel
) {
    DisposableEffect(Unit) {
        animationViewModel.start()
        onDispose {
            animationViewModel.stop()
        }
    }
    val state by animationViewModel.state.collectAsState()

    Box(modifier.fillMaxSize()) {
        Image(
            painter = painterResource(Res.drawable.background_gradient),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(state.blurProgress.dp)
        )

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .blur(24.dp)
                .onSizeChanged {
                    animationViewModel.setBoxSize(Offset(it.width.toFloat(), it.height.toFloat()))
                }
        ) {
            state.dots.forEach { dot ->
                drawCircle(
                    color = dot.color,
                    radius = dot.radius,
                    center = dot.position
                )
            }
        }
    }
}