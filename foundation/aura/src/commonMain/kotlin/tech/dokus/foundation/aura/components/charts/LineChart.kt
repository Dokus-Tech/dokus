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
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

private const val AnimationDurationMs = 800
private const val LabelAlpha = 0.6f
private const val GridAlpha = 0.3f
private const val StrokeWidthDp = 2f
private const val YTickCount = 4
private const val XLabelMaxCount = 6

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
    xLabels: List<String> = emptyList(),
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

    // Compute y-axis ticks from data
    val yTicks = remember(series) {
        val allPoints = series.flatMap { it.points }
        val minY = allPoints.minOrNull() ?: 0f
        val maxY = allPoints.maxOrNull() ?: 1f
        computeNiceTicks(minY, maxY, YTickCount)
    }

    val yLabelLayouts = remember(yTicks, resolvedLabelStyle) {
        yTicks.map { value -> textMeasurer.measure(formatEuroLabel(value), resolvedLabelStyle) }
    }

    // Select a subset of x-labels if too many
    val visibleXIndices = remember(xLabels) {
        if (xLabels.size <= XLabelMaxCount) {
            xLabels.indices.toList()
        } else {
            val step = (xLabels.size - 1).toFloat() / (XLabelMaxCount - 1)
            (0 until XLabelMaxCount).map { (it * step).roundToInt().coerceAtMost(xLabels.size - 1) }
        }
    }

    val xLabelLayouts = remember(xLabels, visibleXIndices, resolvedLabelStyle) {
        visibleXIndices.map { index ->
            index to textMeasurer.measure(xLabels.getOrElse(index) { "" }, resolvedLabelStyle)
        }
    }

    Canvas(modifier = modifier) {
        val yLabelWidth = yLabelLayouts.maxOfOrNull { it.size.width.toFloat() } ?: 0f
        val yLabelHeight = yLabelLayouts.maxOfOrNull { it.size.height.toFloat() } ?: 0f
        val leftPadding = yLabelWidth + Constraints.Spacing.small.toPx()
        val topPadding = yLabelHeight / 2f
        val bottomPadding = (xLabelLayouts.maxOfOrNull { (_, l) -> l.size.height.toFloat() } ?: 0f) +
            Constraints.Spacing.xSmall.toPx()

        val chartLeft = leftPadding
        val chartRight = size.width
        val chartTop = topPadding
        val chartBottom = size.height - bottomPadding
        val chartWidth = chartRight - chartLeft
        val chartHeight = chartBottom - chartTop

        if (chartWidth <= 0f || chartHeight <= 0f) return@Canvas

        val tickMin = yTicks.firstOrNull() ?: 0f
        val tickMax = yTicks.lastOrNull() ?: 1f
        val tickRange = (tickMax - tickMin).coerceAtLeast(1f)

        // Draw horizontal grid lines + y-axis labels
        val gridStroke = Constraints.Stroke.thin.toPx()
        for (i in yTicks.indices) {
            val fraction = (yTicks[i] - tickMin) / tickRange
            val y = chartBottom - fraction * chartHeight

            drawLine(
                color = gridColor,
                start = Offset(chartLeft, y),
                end = Offset(chartRight, y),
                strokeWidth = gridStroke,
            )

            val layout = yLabelLayouts[i]
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(0f, y - layout.size.height / 2f),
            )
        }

        // Draw x-axis labels (evenly positioned based on their data index)
        if (xLabels.isNotEmpty()) {
            val xLabelY = chartBottom + Constraints.Spacing.xSmall.toPx()
            val maxIndex = (xLabels.size - 1).coerceAtLeast(1)

            for ((index, layout) in xLabelLayouts) {
                val xFraction = index.toFloat() / maxIndex
                val x = chartLeft + xFraction * chartWidth - layout.size.width / 2f
                val clampedX = x.coerceIn(chartLeft, chartRight - layout.size.width)
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(clampedX, xLabelY),
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
            val pointCount = s.points.size
            val stepX = chartWidth / (pointCount - 1)

            for (i in s.points.indices) {
                val x = chartLeft + i * stepX
                val normalized = ((s.points[i] - tickMin) / tickRange).coerceIn(0f, 1f)
                val y = chartBottom - normalized * chartHeight
                if (i == 0) fullPath.moveTo(x, y) else fullPath.lineTo(x, y)
            }

            // Animate path drawing
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

// =============================================================================
// Y-axis helpers
// =============================================================================

/**
 * Computes "nice" evenly-spaced tick values that cover the data range.
 * Returns ticks sorted ascending.
 */
private fun computeNiceTicks(minValue: Float, maxValue: Float, count: Int): List<Float> {
    if (minValue == maxValue) {
        val v = minValue
        return listOf(v * 0.9f, v, v * 1.1f)
    }

    val range = maxValue - minValue
    val roughStep = range / (count - 1)
    val magnitude = 10f.pow(floor(log10(roughStep)))
    val normalized = roughStep / magnitude

    val niceStep = when {
        normalized <= 1.5f -> magnitude
        normalized <= 3f -> 2f * magnitude
        normalized <= 7f -> 5f * magnitude
        else -> 10f * magnitude
    }

    val niceMin = floor(minValue / niceStep) * niceStep
    val niceMax = ceil(maxValue / niceStep) * niceStep

    val ticks = mutableListOf<Float>()
    var tick = niceMin
    while (tick <= niceMax + niceStep * 0.01f) {
        ticks.add(tick)
        tick += niceStep
    }
    return ticks
}

/**
 * Formats a value as a compact Euro label for the y-axis.
 */
private fun formatEuroLabel(value: Float): String = when {
    value == 0f -> " "
    value >= 1_000_000 -> "\u20AC${formatCompact(value / 1_000_000)}M"
    value >= 1_000 -> "\u20AC${formatCompact(value / 1_000)}k"
    else -> "\u20AC${value.toInt()}"
}

private fun formatCompact(value: Float): String {
    val rounded = (value * 10).roundToInt() / 10f
    return if (rounded == rounded.toInt().toFloat()) {
        rounded.toInt().toString()
    } else {
        rounded.toString()
    }
}

// =============================================================================
// Previews
// =============================================================================

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
                    points = listOf(12000f, 12300f, 11800f, 12500f, 13200f, 13800f, 13500f, 14380f),
                ),
                LineChartSeries(
                    label = "Belfius",
                    color = Color(0xFF5B9BD5),
                    points = listOf(3400f, 3400f, 3400f, 3400f, 3400f, 3400f, 3400f, 3400f),
                ),
                LineChartSeries(
                    label = "Total",
                    color = Color(0xFF8B8B8B),
                    points = listOf(15400f, 15700f, 15200f, 15900f, 16600f, 17200f, 16900f, 17780f),
                    dashed = true,
                ),
            ),
            xLabels = listOf("Feb 7", "Feb 11", "Feb 15", "Feb 19", "Feb 23", "Feb 27", "Mar 3", "Mar 7"),
            modifier = Modifier.fillMaxWidth().height(200.dp),
        )
    }
}
