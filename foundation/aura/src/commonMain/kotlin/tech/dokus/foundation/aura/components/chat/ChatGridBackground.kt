package tech.dokus.foundation.aura.components.chat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

private const val SmallGridSize = 24f
private const val LargeGridSize = 120f
private const val SmallStrokeAlpha = 0.03f
private const val LargeStrokeAlpha = 0.06f
private const val StrokeWidth = 0.5f
private const val RadialGradientInnerStop = 0.2f
private const val RadialGradientOuterAlpha = 0.92f

/**
 * Amber-tinted grid background with radial gradient overlay.
 * Matches the v29 Intelligence chat screen background.
 *
 * Draws a small grid (24px) with faint amber strokes and a large grid (120px)
 * with slightly stronger strokes, then overlays a radial gradient that fades
 * to the surface color at the edges.
 */
@Composable
fun ChatGridBackground(
    modifier: Modifier = Modifier,
) {
    val amber = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.background

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val smallColor = amber.copy(alpha = SmallStrokeAlpha)
            val largeColor = amber.copy(alpha = LargeStrokeAlpha)

            // Small grid
            var x = 0f
            while (x <= w) {
                drawLine(smallColor, Offset(x, 0f), Offset(x, h), StrokeWidth)
                x += SmallGridSize
            }
            var y = 0f
            while (y <= h) {
                drawLine(smallColor, Offset(0f, y), Offset(w, y), StrokeWidth)
                y += SmallGridSize
            }

            // Large grid overlay
            x = 0f
            while (x <= w) {
                drawLine(largeColor, Offset(x, 0f), Offset(x, h), StrokeWidth)
                x += LargeGridSize
            }
            y = 0f
            while (y <= h) {
                drawLine(largeColor, Offset(0f, y), Offset(w, y), StrokeWidth)
                y += LargeGridSize
            }
        }

        // Radial gradient overlay — transparent center, surface edges
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            surface.copy(alpha = RadialGradientOuterAlpha),
                        ),
                        radius = Float.POSITIVE_INFINITY,
                    )
                )
        )
    }
}

@Preview
@Composable
private fun ChatGridBackgroundPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        ChatGridBackground()
    }
}
