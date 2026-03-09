package tech.dokus.foundation.aura.components.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

private const val AnimationDurationMs = 800
private const val LabelAlpha = 0.6f
private const val GridAlpha = 0.3f
private const val StrokeWidthDp = 2f

@Immutable
data class LineChartSeries(
    val label: String,
    val color: Color,
    val points: List<Float>,
    val dashed: Boolean = false,
)

@Composable
fun DokusLineChart(
    series: List<LineChartSeries>,
    xLabels: List<String>,
    yLabels: List<String>,
    modifier: Modifier = Modifier,
) {
    if (series.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = LabelAlpha)
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = GridAlpha)

    val progress = remember { Animatable(0f) }
    LaunchedEffect(series) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = AnimationDurationMs),
        )
    }

    val resolvedLabelStyle = labelStyle.copy(color = labelColor)

    // Pre-measure y-labels to determine left inset
    val yLabelLayouts = remember(yLabels, resolvedLabelStyle) {
        yLabels.map { label -> textMeasurer.measure(label, resolvedLabelStyle) }
    }
    val xLabelLayouts = remember(xLabels, resolvedLabelStyle) {
        xLabels.map { label -> textMeasurer.measure(label, resolvedLabelStyle) }
    }

    Canvas(modifier = modifier) {
        val yLabelWidth = yLabelLayouts.maxOfOrNull { it.size.width.toFloat() } ?: 0f
        val leftPadding = yLabelWidth + Constraints.Spacing.small.toPx()
        val bottomPadding = (xLabelLayouts.maxOfOrNull { it.size.height.toFloat() } ?: 0f) +
            Constraints.Spacing.xSmall.toPx()

        val chartLeft = leftPadding
        val chartRight = size.width
        val chartTop = 0f
        val chartBottom = size.height - bottomPadding
        val chartWidth = chartRight - chartLeft
        val chartHeight = chartBottom - chartTop

        if (chartWidth <= 0f || chartHeight <= 0f) return@Canvas

        // Draw horizontal grid lines (one per y-label, evenly spaced)
        if (yLabels.isNotEmpty()) {
            val gridStroke = Constraints.Stroke.thin.toPx()
            for (i in yLabels.indices) {
                val fraction = if (yLabels.size == 1) 0.5f else i.toFloat() / (yLabels.size - 1)
                val y = chartBottom - fraction * chartHeight
                drawLine(
                    color = gridColor,
                    start = Offset(chartLeft, y),
                    end = Offset(chartRight, y),
                    strokeWidth = gridStroke,
                )
            }
        }

        // Draw y-axis labels
        for (i in yLabelLayouts.indices) {
            val fraction = if (yLabels.size == 1) 0.5f else i.toFloat() / (yLabels.size - 1)
            val y = chartBottom - fraction * chartHeight
            val layout = yLabelLayouts[i]
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(0f, y - layout.size.height / 2f),
            )
        }

        // Draw x-axis labels (start and end only)
        if (xLabelLayouts.isNotEmpty()) {
            val xLabelY = chartBottom + Constraints.Spacing.xSmall.toPx()

            // First label, left-aligned
            drawText(
                textLayoutResult = xLabelLayouts.first(),
                topLeft = Offset(chartLeft, xLabelY),
            )

            // Last label, right-aligned (if more than one)
            if (xLabelLayouts.size > 1) {
                val lastLayout = xLabelLayouts.last()
                drawText(
                    textLayoutResult = lastLayout,
                    topLeft = Offset(chartRight - lastLayout.size.width, xLabelY),
                )
            }
        }

        // Draw series lines
        val strokeWidth = StrokeWidthDp.dp.toPx()
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f))
        val pathMeasure = PathMeasure()

        for (s in series) {
            if (s.points.size < 2) continue

            val fullPath = Path()
            val stepX = chartWidth / (s.points.size - 1)

            for (i in s.points.indices) {
                val x = chartLeft + i * stepX
                val y = chartBottom - s.points[i].coerceIn(0f, 1f) * chartHeight
                if (i == 0) fullPath.moveTo(x, y) else fullPath.lineTo(x, y)
            }

            // Animate using PathMeasure
            pathMeasure.setPath(fullPath, false)
            val totalLength = pathMeasure.length
            val animatedLength = totalLength * progress.value

            if (animatedLength > 0f) {
                val animatedPath = Path()
                pathMeasure.getSegment(0f, animatedLength, animatedPath, true)

                val style = Stroke(
                    width = strokeWidth,
                    pathEffect = if (s.dashed) dashEffect else null,
                )
                drawPath(
                    path = animatedPath,
                    color = s.color,
                    style = style,
                )
            }
        }
    }
}

// region Previews

@Preview
@Composable
private fun DokusLineChartPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        DokusLineChart(
            series = listOf(
                LineChartSeries(
                    label = "KBC",
                    color = Color(0xFFD4A843),
                    points = listOf(0.6f, 0.62f, 0.65f, 0.63f, 0.7f, 0.72f, 0.75f, 0.73f, 0.78f, 0.8f),
                ),
                LineChartSeries(
                    label = "Belfius",
                    color = Color(0xFF8B8B8B),
                    points = listOf(0.2f, 0.2f, 0.19f, 0.2f, 0.2f, 0.19f, 0.2f, 0.2f, 0.19f, 0.2f),
                ),
                LineChartSeries(
                    label = "Total",
                    color = Color(0xFF8B8B8B),
                    points = listOf(0.8f, 0.82f, 0.84f, 0.83f, 0.9f, 0.91f, 0.95f, 0.93f, 0.97f, 1.0f),
                    dashed = true,
                ),
            ),
            xLabels = listOf("Feb 4", "Mar 4"),
            yLabels = listOf("\u20AC12k", "\u20AC18k"),
            modifier = Modifier.fillMaxWidth().height(200.dp),
        )
    }
}

// endregion
