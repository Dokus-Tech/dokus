package tech.dokus.features.banking.presentation.balances.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.banking_balances_legend_total
import tech.dokus.aura.resources.banking_balances_no_chart_data
import tech.dokus.aura.resources.banking_balances_timeline_subtitle
import tech.dokus.aura.resources.banking_balances_timeline_title
import tech.dokus.domain.model.BalanceHistoryResponse
import tech.dokus.domain.model.BankAccountSummary
import tech.dokus.domain.model.BankConnectionDto
import tech.dokus.features.banking.presentation.balances.mvi.BalanceTimeRange
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.app.state.isError
import tech.dokus.foundation.app.state.isLoading
import tech.dokus.foundation.app.state.isSuccess
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.charts.DokusLineChart
import tech.dokus.foundation.aura.components.charts.LineChartSeries
import tech.dokus.foundation.aura.components.common.DokusErrorContent
import tech.dokus.foundation.aura.components.common.ShimmerBox
import tech.dokus.foundation.aura.components.tabs.DokusTab
import tech.dokus.foundation.aura.components.text.formatEuroCurrency
import tech.dokus.foundation.aura.components.tabs.DokusTabs
import tech.dokus.foundation.aura.components.text.Amt
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.textMuted

private val ChartHeight = 200.dp

// Palette for per-account lines
private val AccountColors = listOf(
    Color(0xFFD4A843),
    Color(0xFF5B9BD5),
    Color(0xFF70AD47),
    Color(0xFFFFC000),
    Color(0xFFED7D31),
)

@Composable
internal fun BalanceTimelineCard(
    summary: DokusState<BankAccountSummary>,
    balanceHistory: DokusState<BalanceHistoryResponse>,
    connections: DokusState<List<BankConnectionDto>>,
    timeRange: BalanceTimeRange,
    onTimeRangeChange: (BalanceTimeRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = remember {
        BalanceTimeRange.entries.map { range ->
            DokusTab(id = range.id, label = range.id)
        }
    }

    DokusCardSurface(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Constraints.Spacing.large),
            verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.large),
        ) {
            // Header: title + total balance on left, time range tabs on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xxSmall)) {
                    Text(
                        text = stringResource(Res.string.banking_balances_timeline_title),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    if (summary.isSuccess()) {
                        val accountCount = summary.data.accountCount
                        Text(
                            text = stringResource(
                                Res.string.banking_balances_timeline_subtitle,
                                formatEuroCurrency(summary.data.totalBalance.minor / 100.0),
                                accountCount,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.textMuted,
                        )
                    }
                }

                DokusTabs(
                    tabs = tabs,
                    activeId = timeRange.id,
                    onTabSelected = { id ->
                        BalanceTimeRange.entries.find { it.id == id }?.let(onTimeRangeChange)
                    },
                )
            }

            // Chart area
            when {
                balanceHistory.isLoading() -> {
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(ChartHeight),
                    )
                }
                balanceHistory.isError() -> {
                    DokusErrorContent(
                        exception = balanceHistory.exception,
                        retryHandler = balanceHistory.retryHandler,
                        compact = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(ChartHeight),
                    )
                }
                balanceHistory.isSuccess() -> {
                    val response = balanceHistory.data
                    val chartData = buildChartData(response)

                    if (chartData.series.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(ChartHeight),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(Res.string.banking_balances_no_chart_data),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.textMuted,
                            )
                        }
                    } else {
                        DokusLineChart(
                            series = chartData.series,
                            xLabels = chartData.xLabels,
                            yLabels = chartData.yLabels,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(ChartHeight),
                        )
                    }
                }
            }

            // Legend row
            if (balanceHistory.isSuccess() && balanceHistory.data.series.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.large),
                ) {
                    balanceHistory.data.series.forEachIndexed { index, accountSeries ->
                        val color = AccountColors[index % AccountColors.size]
                        LegendItem(label = accountSeries.accountName, color = color)
                    }
                    if (balanceHistory.data.totalSeries.isNotEmpty()) {
                        LegendItem(
                            label = stringResource(Res.string.banking_balances_legend_total),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            dashed = true,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(
    label: String,
    color: Color,
    dashed: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier.height(Constraints.Spacing.xxSmall),
        ) {
            val strokeWidth = 2.dp.toPx()
            val width = Constraints.Spacing.large.toPx()
            if (dashed) {
                val dashEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                    floatArrayOf(4f, 3f),
                )
                drawLine(
                    color = color,
                    start = androidx.compose.ui.geometry.Offset(0f, size.height / 2f),
                    end = androidx.compose.ui.geometry.Offset(width, size.height / 2f),
                    strokeWidth = strokeWidth,
                    pathEffect = dashEffect,
                )
            } else {
                drawLine(
                    color = color,
                    start = androidx.compose.ui.geometry.Offset(0f, size.height / 2f),
                    end = androidx.compose.ui.geometry.Offset(width, size.height / 2f),
                    strokeWidth = strokeWidth,
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.textMuted,
        )
    }
}

private data class ChartData(
    val series: List<LineChartSeries>,
    val xLabels: List<String>,
    val yLabels: List<String>,
)

private fun buildChartData(response: BalanceHistoryResponse): ChartData {
    val allPoints = response.series.flatMap { s -> s.points.map { it.balance.minor } } +
        response.totalSeries.map { it.balance.minor }

    if (allPoints.isEmpty()) return ChartData(emptyList(), emptyList(), emptyList())

    val minVal = allPoints.min().toFloat()
    val maxVal = allPoints.max().toFloat()
    val range = (maxVal - minVal).coerceAtLeast(1f)

    val lineChartSeries = response.series.mapIndexed { index, accountSeries ->
        val color = AccountColors[index % AccountColors.size]
        LineChartSeries(
            label = accountSeries.accountName,
            color = color,
            points = accountSeries.points.map { ((it.balance.minor - minVal) / range) },
        )
    }

    val totalLine = if (response.totalSeries.isNotEmpty()) {
        LineChartSeries(
            label = "Total",
            color = Color(0xFF8B8B8B),
            points = response.totalSeries.map { ((it.balance.minor - minVal) / range) },
            dashed = true,
        )
    } else null

    val allSeries = lineChartSeries + listOfNotNull(totalLine)

    // X labels: first and last dates from total series (or first account series)
    val dateSeries = response.totalSeries.ifEmpty {
        response.series.firstOrNull()?.points ?: emptyList()
    }
    val xLabels = if (dateSeries.size >= 2) {
        listOf(
            dateSeries.first().date.toString(),
            dateSeries.last().date.toString(),
        )
    } else emptyList()

    // Y labels: min and max
    val yLabels = listOf(
        formatCompactCurrency(minVal / 100f),
        formatCompactCurrency(maxVal / 100f),
    )

    return ChartData(allSeries, xLabels, yLabels)
}

private fun formatCompactCurrency(value: Float): String {
    val absValue = kotlin.math.abs(value)
    return when {
        absValue >= 1_000_000 -> "\u20AC${(value / 1_000_000).let { "%.1f".format(it) }}M"
        absValue >= 1_000 -> "\u20AC${(value / 1_000).let { "%.0f".format(it) }}k"
        else -> "\u20AC${"%.0f".format(value)}"
    }
}
