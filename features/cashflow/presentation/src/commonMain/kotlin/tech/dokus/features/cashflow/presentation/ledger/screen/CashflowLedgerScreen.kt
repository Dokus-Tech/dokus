package tech.dokus.features.cashflow.presentation.ledger.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tech.dokus.domain.Money
import tech.dokus.domain.enums.CashflowDirection
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.model.CashflowEntry
import tech.dokus.features.cashflow.presentation.ledger.components.CashflowEntryRow
import tech.dokus.features.cashflow.presentation.ledger.mvi.CashflowFilters
import tech.dokus.features.cashflow.presentation.ledger.mvi.CashflowLedgerIntent
import tech.dokus.features.cashflow.presentation.ledger.mvi.CashflowLedgerState
import tech.dokus.features.cashflow.presentation.ledger.mvi.DateRangeFilter
import tech.dokus.foundation.aura.components.common.DokusErrorContent

@Composable
internal fun CashflowLedgerScreen(
    state: CashflowLedgerState,
    onIntent: (CashflowLedgerIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        when (state) {
            is CashflowLedgerState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is CashflowLedgerState.Content -> {
                CashflowLedgerContent(
                    state = state,
                    onIntent = onIntent
                )
            }

            is CashflowLedgerState.Error -> {
                DokusErrorContent(
                    exception = state.exception,
                    retryHandler = state.retryHandler,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun CashflowLedgerContent(
    state: CashflowLedgerState.Content,
    onIntent: (CashflowLedgerIntent) -> Unit
) {
    val listState = rememberLazyListState()

    // Trigger load more when near bottom
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            lastVisibleItem >= totalItems - 5 &&
                state.entries.hasMorePages &&
                !state.entries.isLoadingMore
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            onIntent(CashflowLedgerIntent.LoadMore)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Summary header
        CashflowSummaryHeader(
            totalIn = state.totalIn,
            totalOut = state.totalOut
        )

        // Filters
        CashflowFiltersBar(
            filters = state.filters,
            onDateRangeChange = { onIntent(CashflowLedgerIntent.UpdateDateRangeFilter(it)) },
            onDirectionChange = { onIntent(CashflowLedgerIntent.UpdateDirectionFilter(it)) },
            onStatusChange = { onIntent(CashflowLedgerIntent.UpdateStatusFilter(it)) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Entries list
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = state.entries.data,
                key = { it.id.toString() }
            ) { entry ->
                CashflowEntryRow(
                    entry = entry,
                    isHighlighted = entry.id == state.highlightedEntryId,
                    onClick = { onIntent(CashflowLedgerIntent.OpenEntry(entry)) }
                )
            }

            if (state.entries.isLoadingMore) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            if (state.entries.data.isEmpty() && !state.entries.isLoadingMore) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No entries found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CashflowSummaryHeader(
    totalIn: Long,
    totalOut: Long,
    modifier: Modifier = Modifier
) {
    val net = totalIn - totalOut
    val netColor = when {
        net > 0 -> MaterialTheme.colorScheme.tertiary
        net < 0 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Expected In",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = Money(totalIn).toDisplayString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Expected Out",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = Money(totalOut).toDisplayString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Net",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = Money(net).toDisplayString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = netColor
            )
        }
    }
}

@Composable
private fun CashflowFiltersBar(
    filters: CashflowFilters,
    onDateRangeChange: (DateRangeFilter) -> Unit,
    onDirectionChange: (CashflowDirection?) -> Unit,
    onStatusChange: (CashflowEntryStatus?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Date range filters
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = filters.dateRange == DateRangeFilter.ThisMonth,
                onClick = { onDateRangeChange(DateRangeFilter.ThisMonth) },
                label = { Text("This Month") }
            )
            FilterChip(
                selected = filters.dateRange == DateRangeFilter.Next3Months,
                onClick = { onDateRangeChange(DateRangeFilter.Next3Months) },
                label = { Text("Next 3 Months") }
            )
            FilterChip(
                selected = filters.dateRange == DateRangeFilter.AllTime,
                onClick = { onDateRangeChange(DateRangeFilter.AllTime) },
                label = { Text("All") }
            )
        }

        // Direction and status filters
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = filters.direction == null,
                onClick = { onDirectionChange(null) },
                label = { Text("All") }
            )
            FilterChip(
                selected = filters.direction == CashflowDirection.In,
                onClick = {
                    onDirectionChange(
                        if (filters.direction == CashflowDirection.In) null else CashflowDirection.In
                    )
                },
                label = { Text("In") }
            )
            FilterChip(
                selected = filters.direction == CashflowDirection.Out,
                onClick = {
                    onDirectionChange(
                        if (filters.direction == CashflowDirection.Out) null else CashflowDirection.Out
                    )
                },
                label = { Text("Out") }
            )
            FilterChip(
                selected = filters.status == CashflowEntryStatus.Open,
                onClick = {
                    onStatusChange(
                        if (filters.status == CashflowEntryStatus.Open) null else CashflowEntryStatus.Open
                    )
                },
                label = { Text("Open") }
            )
            FilterChip(
                selected = filters.status == CashflowEntryStatus.Paid,
                onClick = {
                    onStatusChange(
                        if (filters.status == CashflowEntryStatus.Paid) null else CashflowEntryStatus.Paid
                    )
                },
                label = { Text("Paid") }
            )
        }
    }
}
