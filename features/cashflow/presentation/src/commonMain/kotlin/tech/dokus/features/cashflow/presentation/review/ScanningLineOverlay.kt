package tech.dokus.features.cashflow.presentation.review

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp

@Composable
internal fun ScanningLineOverlay(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "scan")
    val progress = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanProgress"
    )

    val lineColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    val glowColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    val transparentColor = lineColor.copy(alpha = 0f)

    Canvas(modifier = modifier) {
        val y = progress.value * size.height
        val lineHeight = 2.dp.toPx()
        val glowHeight = 14.dp.toPx()

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(transparentColor, glowColor, transparentColor),
                startY = y - glowHeight / 2f,
                endY = y + glowHeight / 2f
            ),
            topLeft = Offset(0f, y - glowHeight / 2f),
            size = Size(size.width, glowHeight)
        )

        drawRect(
            color = lineColor,
            topLeft = Offset(0f, y - lineHeight / 2f),
            size = Size(size.width, lineHeight)
        )
    }
}
