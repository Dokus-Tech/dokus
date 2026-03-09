package tech.dokus.foundation.aura.components.charts

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.multiplatform.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.multiplatform.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.multiplatform.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.multiplatform.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.multiplatform.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.multiplatform.cartesian.data.LineCartesianLayerModel
import com.patrykandpatrick.vico.multiplatform.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.multiplatform.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.multiplatform.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.multiplatform.common.Fill
import com.patrykandpatrick.vico.multiplatform.common.component.LineComponent
import com.patrykandpatrick.vico.multiplatform.common.component.TextComponent
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

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
    modifier: Modifier = Modifier,
) {
    if (series.isEmpty()) return

    val model = remember(series) {
        val layerModel = LineCartesianLayerModel(
            series.map { s ->
                s.points.mapIndexed { index, y ->
                    LineCartesianLayerModel.Entry(index, y)
                }
            }
        )
        CartesianChartModel(layerModel)
    }

    val lines = remember(series) {
        series.map { s ->
            LineCartesianLayer.Line(
                fill = LineCartesianLayer.LineFill.single(Fill(s.color)),
                stroke = if (s.dashed) {
                    LineCartesianLayer.LineStroke.Dashed(thickness = 1.5.dp)
                } else {
                    LineCartesianLayer.LineStroke.Continuous(thickness = 2.dp)
                },
            )
        }
    }

    val markerLabel = remember { TextComponent() }
    val marker = rememberDefaultCartesianMarker(label = markerLabel)

    // Minimal gridline style
    val guidelineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    val guideline = LineComponent(Fill(guidelineColor), thickness = 0.5.dp)

    // Compact Euro formatter for y-axis
    val yFormatter = remember {
        CartesianValueFormatter { _, value, _ ->
            when {
                value >= 1_000_000 -> "\u20AC${(value / 1_000_000).toInt()}M"
                value >= 1_000 -> "\u20AC${(value / 1_000).toInt()}k"
                else -> "\u20AC${value.toInt()}"
            }
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            LineCartesianLayer(
                lineProvider = LineCartesianLayer.LineProvider.series(lines),
            ),
            startAxis = VerticalAxis.rememberStart(
                guideline = guideline,
                valueFormatter = yFormatter,
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                guideline = null,
            ),
            marker = marker,
        ),
        model = model,
        modifier = modifier,
    )
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
                    points = listOf(12000f, 12300f, 11800f, 12500f, 13200f, 13800f, 13500f, 14380f),
                ),
                LineChartSeries(
                    label = "Belfius",
                    color = Color(0xFF8B8B8B),
                    points = listOf(3400f, 3400f, 3400f, 3400f, 3400f, 3400f, 3400f, 3400f),
                ),
                LineChartSeries(
                    label = "Total",
                    color = Color(0xFF8B8B8B),
                    points = listOf(15400f, 15700f, 15200f, 15900f, 16600f, 17200f, 16900f, 17780f),
                    dashed = true,
                ),
            ),
            modifier = Modifier.fillMaxWidth().height(200.dp),
        )
    }
}

// endregion
