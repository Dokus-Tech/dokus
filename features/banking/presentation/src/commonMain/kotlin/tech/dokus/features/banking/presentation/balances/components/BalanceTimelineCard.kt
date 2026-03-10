package tech.dokus.features.banking.presentation.balances.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.banking_balances_legend_total
import tech.dokus.aura.resources.banking_balances_no_chart_data
import tech.dokus.aura.resources.banking_balances_timeline_subtitle
import tech.dokus.aura.resources.banking_balances_timeline_title
import tech.dokus.domain.Money
import tech.dokus.domain.ids.BankAccountId
import tech.dokus.domain.model.AccountBalanceSeries
import tech.dokus.domain.model.BalanceHistoryPoint
import tech.dokus.domain.model.BalanceHistoryResponse
import tech.dokus.domain.model.BankAccountSummary
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
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.aura.style.textMuted
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

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
            // Header: title + total balance, time range tabs
            val isLargeScreen = LocalScreenSize.current.isLarge
            if (isLargeScreen) {
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
                            Text(
                                text = stringResource(
                                    Res.string.banking_balances_timeline_subtitle,
                                    formatEuroCurrency(summary.data.totalBalance.toDouble()),
                                    summary.data.accountCount,
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
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
                ) {
                    Text(
                        text = stringResource(Res.string.banking_balances_timeline_title),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    if (summary.isSuccess()) {
                        Text(
                            text = stringResource(
                                Res.string.banking_balances_timeline_subtitle,
                                formatEuroCurrency(summary.data.totalBalance.toDouble()),
                                summary.data.accountCount,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.textMuted,
                        )
                    }
                    DokusTabs(
                        tabs = tabs,
                        activeId = timeRange.id,
                        onTabSelected = { id ->
                            BalanceTimeRange.entries.find { it.id == id }?.let(onTimeRangeChange)
                        },
                    )
                }
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
                    val chartData = buildChartSeries(balanceHistory.data)

                    if (chartData.isEmpty()) {
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
                            series = chartData,
                            xLabels = buildXLabels(balanceHistory.data),
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
            modifier = Modifier
                .width(Constraints.Spacing.large)
                .height(Constraints.Spacing.xxSmall),
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

private val ShortMonthNames = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

private fun buildXLabels(response: BalanceHistoryResponse): List<String> {
    val dateSeries = response.totalSeries.ifEmpty {
        response.series.firstOrNull()?.points ?: emptyList()
    }
    return dateSeries.map { point ->
        val month = ShortMonthNames[point.date.month.ordinal]
        "$month ${point.date.day}"
    }
}

private fun buildChartSeries(response: BalanceHistoryResponse): List<LineChartSeries> {
    if (response.series.isEmpty() && response.totalSeries.isEmpty()) {
        return emptyList()
    }

    val lineChartSeries = response.series.mapIndexed { index, accountSeries ->
        val color = AccountColors[index % AccountColors.size]
        LineChartSeries(
            label = accountSeries.accountName,
            color = color,
            points = accountSeries.points.map { it.balance.toDouble().toFloat() },
        )
    }

    val totalLine = if (response.totalSeries.isNotEmpty()) {
        LineChartSeries(
            label = "Total",
            color = Color(0xFF8B8B8B),
            points = response.totalSeries.map { it.balance.toDouble().toFloat() },
            dashed = true,
        )
    } else null

    return lineChartSeries + listOfNotNull(totalLine)
}

// =============================================================================
// Previews
// =============================================================================

private val PreviewSummary = BankAccountSummary(
    totalBalance = Money(1778042),
    accountCount = 2,
    unmatchedCount = 3,
    totalUnresolvedAmount = Money(842050),
    matchedThisPeriod = 12,
    lastSyncedAt = null,
)

private val PreviewBalanceHistory = BalanceHistoryResponse(
    series = listOf(
        AccountBalanceSeries(
            accountId = BankAccountId.generate(),
            accountName = "KBC Business",
            points = listOf(
                BalanceHistoryPoint(LocalDate(2026, 2, 7), Money(1200000)),
                BalanceHistoryPoint(LocalDate(2026, 2, 15), Money(1180000)),
                BalanceHistoryPoint(LocalDate(2026, 2, 23), Money(1320000)),
                BalanceHistoryPoint(LocalDate(2026, 3, 3), Money(1350000)),
                BalanceHistoryPoint(LocalDate(2026, 3, 7), Money(1438042)),
            ),
        ),
    ),
    totalSeries = listOf(
        BalanceHistoryPoint(LocalDate(2026, 2, 7), Money(1540000)),
        BalanceHistoryPoint(LocalDate(2026, 2, 15), Money(1520000)),
        BalanceHistoryPoint(LocalDate(2026, 2, 23), Money(1660000)),
        BalanceHistoryPoint(LocalDate(2026, 3, 3), Money(1690000)),
        BalanceHistoryPoint(LocalDate(2026, 3, 7), Money(1778042)),
    ),
)

@Preview(name = "Balance Timeline Card", widthDp = 800)
@Composable
private fun BalanceTimelineCardPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        BalanceTimelineCard(
            summary = DokusState.success(PreviewSummary),
            balanceHistory = DokusState.success(PreviewBalanceHistory),
            timeRange = BalanceTimeRange.ThirtyDays,
            onTimeRangeChange = {},
        )
    }
}
