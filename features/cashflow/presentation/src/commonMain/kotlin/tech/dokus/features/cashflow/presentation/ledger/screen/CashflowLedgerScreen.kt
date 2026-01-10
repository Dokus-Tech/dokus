package tech.dokus.features.cashflow.presentation.ledger.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_ledger_empty_hint
import tech.dokus.aura.resources.cashflow_ledger_empty_title
import tech.dokus.aura.resources.cashflow_ledger_filter_custom
import tech.dokus.aura.resources.cashflow_ledger_filter_next_3_months
import tech.dokus.aura.resources.cashflow_ledger_filter_this_month
import tech.dokus.aura.resources.cashflow_ledger_filter_in
import tech.dokus.aura.resources.cashflow_ledger_filter_out
import tech.dokus.aura.resources.cashflow_ledger_filter_open
import tech.dokus.aura.resources.cashflow_ledger_filter_paid
import tech.dokus.aura.resources.cashflow_ledger_filter_overdue
import tech.dokus.aura.resources.cashflow_ledger_filter_cancelled
import tech.dokus.aura.resources.documents_filter_all
import tech.dokus.domain.enums.CashflowDirection
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.features.cashflow.presentation.common.components.empty.DokusEmptyState
import tech.dokus.features.cashflow.presentation.common.components.filters.DokusFilterChipRow
import tech.dokus.features.cashflow.presentation.common.components.pagination.rememberLoadMoreTrigger
import tech.dokus.features.cashflow.presentation.common.components.table.DokusTableDivider
import tech.dokus.features.cashflow.presentation.common.components.table.DokusTableSurface
import tech.dokus.features.cashflow.presentation.ledger.components.CashflowDetailPane
import tech.dokus.features.cashflow.presentation.ledger.components.CashflowLedgerHeaderRow
import tech.dokus.features.cashflow.presentation.ledger.components.CashflowLedgerMobileRow
import tech.dokus.features.cashflow.presentation.ledger.components.CashflowLedgerOverview
import tech.dokus.features.cashflow.presentation.ledger.components.CashflowLedgerTableRow
import tech.dokus.features.cashflow.presentation.ledger.mvi.CashflowFilters
import tech.dokus.features.cashflow.presentation.ledger.mvi.CashflowLedgerIntent
import tech.dokus.features.cashflow.presentation.ledger.mvi.CashflowLedgerState
import tech.dokus.features.cashflow.presentation.ledger.mvi.DateRangeFilter
import tech.dokus.foundation.aura.components.common.DokusErrorContent
import tech.dokus.foundation.aura.local.LocalScreenSize

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
    val isLargeScreen = LocalScreenSize.current.isLarge

    // Trigger load more when near bottom
    val shouldLoadMore = rememberLoadMoreTrigger(
        listState = listState,
        hasMore = state.entries.hasMorePages,
        isLoading = state.entries.isLoadingMore,
        buffer = 5
    )

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            onIntent(CashflowLedgerIntent.LoadMore)
        }
    }

    // Resolve selected entry from list
    val selectedEntry = state.entries.data.find { it.id == state.selectedEntryId }

    Box(modifier = Modifier.fillMaxSize()) {
        // Single unified surface for all content
        DokusTableSurface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            header = null // We handle sections inside
        ) {
            // Overview section (status bar only)
            CashflowLedgerOverview(
                entries = state.entries.data,
                modifier = Modifier.padding(16.dp)
            )

            DokusTableDivider()

            // Filters section
            CashflowFiltersBar(
                filters = state.filters,
                onDateRangeChange = { onIntent(CashflowLedgerIntent.UpdateDateRangeFilter(it)) },
                onDirectionChange = { onIntent(CashflowLedgerIntent.UpdateDirectionFilter(it)) },
                onStatusChange = { onIntent(CashflowLedgerIntent.UpdateStatusFilter(it)) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            DokusTableDivider()

            // Table header (desktop only)
            if (isLargeScreen) {
                CashflowLedgerHeaderRow()
                DokusTableDivider()
            }

            // Table body OR empty state - both inside the surface
            if (state.entries.data.isEmpty() && !state.entries.isLoadingMore) {
                DokusEmptyState(
                    title = stringResource(Res.string.cashflow_ledger_empty_title),
                    subtitle = stringResource(Res.string.cashflow_ledger_empty_hint),
                    modifier = Modifier
                        .weight(1f)
                        .padding(32.dp)
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f)
                ) {
                    itemsIndexed(
                        items = state.entries.data,
                        key = { _, entry -> entry.id.toString() }
                    ) { index, entry ->
                        if (isLargeScreen) {
                            CashflowLedgerTableRow(
                                entry = entry,
                                isHighlighted = entry.id == state.highlightedEntryId,
                                onClick = { onIntent(CashflowLedgerIntent.OpenEntry(entry)) }
                            )
                        } else {
                            CashflowLedgerMobileRow(
                                entry = entry,
                                onClick = { onIntent(CashflowLedgerIntent.OpenEntry(entry)) }
                            )
                        }

                        if (index < state.entries.data.size - 1) {
                            DokusTableDivider()
                        }
                    }

                    if (state.entries.isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
        }

        // Detail pane overlay
        CashflowDetailPane(
            isVisible = selectedEntry != null,
            entry = selectedEntry,
            paymentFormState = state.paymentFormState,
            isFullScreen = !isLargeScreen,
            onDismiss = { onIntent(CashflowLedgerIntent.CloseDetailPane) },
            onPaymentDateChange = { onIntent(CashflowLedgerIntent.UpdatePaymentDate(it)) },
            onPaymentAmountChange = { onIntent(CashflowLedgerIntent.UpdatePaymentAmount(it)) },
            onPaymentNoteChange = { onIntent(CashflowLedgerIntent.UpdatePaymentNote(it)) },
            onSubmitPayment = { onIntent(CashflowLedgerIntent.SubmitPayment) },
            onOpenDocument = { onIntent(CashflowLedgerIntent.OpenDocument(it)) }
        )
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
        DokusFilterChipRow {
            FilterChip(
                selected = filters.dateRange == DateRangeFilter.ThisMonth,
                onClick = { onDateRangeChange(DateRangeFilter.ThisMonth) },
                label = { Text(stringResource(Res.string.cashflow_ledger_filter_this_month)) }
            )
            FilterChip(
                selected = filters.dateRange == DateRangeFilter.Next3Months,
                onClick = { onDateRangeChange(DateRangeFilter.Next3Months) },
                label = { Text(stringResource(Res.string.cashflow_ledger_filter_next_3_months)) }
            )
            FilterChip(
                selected = filters.dateRange == DateRangeFilter.AllTime,
                onClick = { },
                enabled = false,
                label = { Text(stringResource(Res.string.cashflow_ledger_filter_custom)) }
            )
        }

        // Direction and status filters
        DokusFilterChipRow {
            FilterChip(
                selected = filters.direction == null,
                onClick = { onDirectionChange(null) },
                label = { Text(stringResource(Res.string.documents_filter_all)) }
            )
            FilterChip(
                selected = filters.direction == CashflowDirection.In,
                onClick = {
                    onDirectionChange(
                        if (filters.direction == CashflowDirection.In) null else CashflowDirection.In
                    )
                },
                label = { Text(stringResource(Res.string.cashflow_ledger_filter_in)) }
            )
            FilterChip(
                selected = filters.direction == CashflowDirection.Out,
                onClick = {
                    onDirectionChange(
                        if (filters.direction == CashflowDirection.Out) null else CashflowDirection.Out
                    )
                },
                label = { Text(stringResource(Res.string.cashflow_ledger_filter_out)) }
            )
        }

        DokusFilterChipRow {
            FilterChip(
                selected = filters.status == null,
                onClick = { onStatusChange(null) },
                label = { Text(stringResource(Res.string.documents_filter_all)) }
            )
            FilterChip(
                selected = filters.status == CashflowEntryStatus.Open,
                onClick = {
                    onStatusChange(
                        if (filters.status == CashflowEntryStatus.Open) null else CashflowEntryStatus.Open
                    )
                },
                label = { Text(stringResource(Res.string.cashflow_ledger_filter_open)) }
            )
            FilterChip(
                selected = filters.status == CashflowEntryStatus.Paid,
                onClick = {
                    onStatusChange(
                        if (filters.status == CashflowEntryStatus.Paid) null else CashflowEntryStatus.Paid
                    )
                },
                label = { Text(stringResource(Res.string.cashflow_ledger_filter_paid)) }
            )
            FilterChip(
                selected = filters.status == CashflowEntryStatus.Overdue,
                onClick = {
                    onStatusChange(
                        if (filters.status == CashflowEntryStatus.Overdue) null else CashflowEntryStatus.Overdue
                    )
                },
                label = { Text(stringResource(Res.string.cashflow_ledger_filter_overdue)) }
            )
            FilterChip(
                selected = filters.status == CashflowEntryStatus.Cancelled,
                onClick = {
                    onStatusChange(
                        if (filters.status == CashflowEntryStatus.Cancelled) null else CashflowEntryStatus.Cancelled
                    )
                },
                label = { Text(stringResource(Res.string.cashflow_ledger_filter_cancelled)) }
            )
        }
    }
}
