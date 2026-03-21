package tech.dokus.features.cashflow.presentation.overview.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.CircleCheck
import com.composables.icons.lucide.CreditCard
import com.composables.icons.lucide.FileText
import com.composables.icons.lucide.Lucide
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_action_mark_paid
import tech.dokus.aura.resources.cashflow_action_record_payment
import tech.dokus.aura.resources.cashflow_action_view_document
import tech.dokus.aura.resources.cashflow_empty_history
import tech.dokus.aura.resources.cashflow_empty_history_in
import tech.dokus.aura.resources.cashflow_empty_history_out
import tech.dokus.aura.resources.cashflow_empty_overdue
import tech.dokus.aura.resources.cashflow_empty_overdue_in
import tech.dokus.aura.resources.cashflow_empty_overdue_out
import tech.dokus.aura.resources.cashflow_empty_upcoming
import tech.dokus.aura.resources.cashflow_empty_upcoming_hint
import tech.dokus.aura.resources.cashflow_empty_upcoming_in
import tech.dokus.aura.resources.cashflow_empty_upcoming_out
import tech.dokus.domain.model.CashflowEntryDto
import tech.dokus.features.cashflow.presentation.common.components.pagination.rememberLoadMoreTrigger
import tech.dokus.features.cashflow.presentation.common.components.table.DokusTableDivider
import tech.dokus.features.cashflow.presentation.common.components.table.DokusTableSurface
import tech.dokus.features.cashflow.presentation.overview.components.CashFlowOverviewHeaderRow
import tech.dokus.features.cashflow.presentation.overview.components.CashFlowOverviewMobileRow
import tech.dokus.features.cashflow.presentation.overview.components.CashFlowOverviewTableRow
import tech.dokus.features.cashflow.presentation.overview.components.CashflowSummarySection
import tech.dokus.features.cashflow.presentation.overview.components.CashflowViewModeFilter
import tech.dokus.features.cashflow.presentation.overview.mvi.CashFlowOverviewIntent
import tech.dokus.features.cashflow.presentation.overview.mvi.CashFlowOverviewState
import tech.dokus.features.cashflow.presentation.overview.mvi.CashflowViewMode
import tech.dokus.features.cashflow.presentation.overview.mvi.DirectionFilter
import tech.dokus.foundation.app.state.isError
import tech.dokus.foundation.app.state.isLoading
import tech.dokus.foundation.aura.components.common.DokusEmptyState
import tech.dokus.foundation.aura.components.common.DokusErrorBanner
import tech.dokus.foundation.aura.components.common.DokusLoader
import tech.dokus.foundation.aura.components.common.DokusLoaderSize
import tech.dokus.foundation.aura.components.common.MonthSeparatorRow
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

private sealed interface CashflowDisplayRow {
    data class EntryRow(val entry: CashflowEntryDto) : CashflowDisplayRow
    data class MonthHeader(val year: Int, val month: Int) : CashflowDisplayRow
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CashFlowOverviewScreen(
    state: CashFlowOverviewState,
    onIntent: (CashFlowOverviewIntent) -> Unit,
    onCreateInvoiceClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isLargeScreen = LocalScreenSize.current.isLarge
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier
    ) {
        CashFlowOverviewContent(
            state = state,
            onIntent = onIntent,
            onCreateInvoiceClick = onCreateInvoiceClick
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CashFlowOverviewContent(
    state: CashFlowOverviewState,
    onIntent: (CashFlowOverviewIntent) -> Unit,
    onCreateInvoiceClick: (() -> Unit)? = null,
) {
    val listState = rememberLazyListState()
    val isLargeScreen = LocalScreenSize.current.isLarge
    val entriesData = state.entries.lastData?.data ?: emptyList()
    val isRefreshing = state.entries.isLoading()

    // Trigger load more when near bottom
    val shouldLoadMore = rememberLoadMoreTrigger(
        listState = listState,
        hasMore = state.entries.lastData?.hasMorePages ?: false,
        isLoading = state.entries.isLoading(),
        buffer = 5
    )

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            onIntent(CashFlowOverviewIntent.LoadMore)
        }
    }

    // Build display rows with month separators (History mode only)
    val displayRows: List<CashflowDisplayRow> = remember(entriesData, state.filters.viewMode) {
        if (state.filters.viewMode != CashflowViewMode.History) {
            entriesData.map<_, CashflowDisplayRow> { CashflowDisplayRow.EntryRow(it) }
        } else {
            // Sort by display date to ensure monotonic month grouping.
            // Server sorts by paidAt DESC but entries with null paidAt fall back to eventDate,
            // which can break month ordering.
            val sorted = entriesData.sortedByDescending { it.paidAt?.date ?: it.eventDate }
            buildList {
                var lastYear = -1
                var lastMonth = -1
                for (entry in sorted) {
                    val date: LocalDate = entry.paidAt?.date ?: entry.eventDate
                    if (date.year != lastYear || date.month.number != lastMonth) {
                        add(CashflowDisplayRow.MonthHeader(date.year, date.month.number))
                        lastYear = date.year
                        lastMonth = date.month.number
                    }
                    add(CashflowDisplayRow.EntryRow(entry))
                }
            }
        }
    }

    // Derive spark data from visible entries (up to 8 amounts)
    val sparkData = remember(entriesData) {
        entriesData
            .take(8)
            .map { kotlin.math.abs(it.amountGross.toDouble()) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Summary hero card
            CashflowSummarySection(
                summary = state.summary,
                viewMode = state.filters.viewMode,
                sparkData = sparkData,
            )

            // Filter tabs
            CashflowViewModeFilter(
                viewMode = state.filters.viewMode,
                direction = state.filters.direction,
                onViewModeChange = { onIntent(CashFlowOverviewIntent.SetViewMode(it)) },
                onDirectionChange = { onIntent(CashFlowOverviewIntent.SetDirectionFilter(it)) },
                onCreateInvoiceClick = if (isLargeScreen) onCreateInvoiceClick else null,
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                // Table surface (data only)
                DokusTableSurface(
                    modifier = Modifier.fillMaxSize(),
                    header = if (isLargeScreen) {
                        { CashFlowOverviewHeaderRow() }
                    } else {
                        null
                    }
                ) {
                    // Table body OR loading/error/empty state
                    if (entriesData.isEmpty() && state.entries.isError()) {
                        val error = state.entries
                        DokusErrorBanner(
                            exception = error.exception,
                            retryHandler = error.retryHandler,
                            modifier = Modifier.padding(16.dp),
                        )
                    } else if (entriesData.isEmpty() && isRefreshing) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            DokusLoader(size = DokusLoaderSize.Small)
                        }
                    } else if (entriesData.isEmpty() && !isRefreshing) {
                        // Context-aware empty state based on current filters
                        val emptyStateTitle = getEmptyStateTitle(
                            viewMode = state.filters.viewMode,
                            direction = state.filters.direction
                        )
                        // Hint only shown for Upcoming + All
                        val emptyStateHint = if (
                            state.filters.viewMode == CashflowViewMode.Upcoming &&
                            state.filters.direction == DirectionFilter.All
                        ) {
                            stringResource(Res.string.cashflow_empty_upcoming_hint)
                        } else {
                            null
                        }

                        DokusEmptyState(
                            title = emptyStateTitle,
                            subtitle = emptyStateHint,
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
                                items = displayRows,
                                key = { _, row ->
                                    when (row) {
                                        is CashflowDisplayRow.EntryRow -> row.entry.id.toString()
                                        is CashflowDisplayRow.MonthHeader -> "month-${row.year}-${row.month}"
                                    }
                                }
                            ) { index, row ->
                                when (row) {
                                    is CashflowDisplayRow.MonthHeader -> {
                                        MonthSeparatorRow(year = row.year, month = row.month)
                                    }
                                    is CashflowDisplayRow.EntryRow -> {
                                        val entry = row.entry
                                        if (isLargeScreen) {
                                            CashFlowOverviewTableRow(
                                                entry = entry,
                                                viewMode = state.filters.viewMode,
                                                isHighlighted = entry.id == state.highlightedEntryId,
                                                showActionsMenu = state.actionsEntryId == entry.id,
                                                onClick = { onIntent(CashFlowOverviewIntent.OpenEntry(entry)) },
                                                onShowActions = { onIntent(CashFlowOverviewIntent.ShowRowActions(entry.id)) },
                                                onHideActions = { onIntent(CashFlowOverviewIntent.HideRowActions) },
                                                onRecordPayment = { onIntent(CashFlowOverviewIntent.RecordPaymentFor(entry.id)) },
                                                onMarkAsPaid = { onIntent(CashFlowOverviewIntent.MarkAsPaidQuick(entry.id)) },
                                                onViewDocument = { onIntent(CashFlowOverviewIntent.ViewDocumentFor(entry)) }
                                            )
                                        } else {
                                            CashFlowOverviewMobileRow(
                                                entry = entry,
                                                viewMode = state.filters.viewMode,
                                                onClick = { onIntent(CashFlowOverviewIntent.OpenEntry(entry)) },
                                                onShowActions = { onIntent(CashFlowOverviewIntent.ShowRowActions(entry.id)) }
                                            )
                                        }

                                        // Dividers only between entry rows
                                        val nextRow = displayRows.getOrNull(index + 1)
                                        if (nextRow is CashflowDisplayRow.EntryRow) {
                                            DokusTableDivider()
                                        }
                                    }
                                }
                            }

                            if (isRefreshing) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        DokusLoader(size = DokusLoaderSize.Small)
                                    }
                                }
                            }
                        }
                    }
                }

                if (isRefreshing && entriesData.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        DokusLoader(size = DokusLoaderSize.Small)
                    }
                }
            }
        }

        // Mobile actions bottom sheet
        val actionsEntry = entriesData.find { it.id == state.actionsEntryId }
        if (!isLargeScreen && actionsEntry != null) {
            val sheetState = rememberModalBottomSheetState()
            ModalBottomSheet(
                onDismissRequest = { onIntent(CashFlowOverviewIntent.HideRowActions) },
                sheetState = sheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                ) {
                    // Payment actions only in Upcoming mode
                    if (state.filters.viewMode != CashflowViewMode.History) {
                        // Record payment
                        MobileActionItem(
                            icon = Lucide.CreditCard,
                            label = stringResource(Res.string.cashflow_action_record_payment),
                            onClick = { onIntent(CashFlowOverviewIntent.RecordPaymentFor(actionsEntry.id)) }
                        )
                        // Mark as paid
                        MobileActionItem(
                            icon = Lucide.CircleCheck,
                            label = stringResource(Res.string.cashflow_action_mark_paid),
                            onClick = { onIntent(CashFlowOverviewIntent.MarkAsPaidQuick(actionsEntry.id)) }
                        )
                    }
                    // View document (all modes)
                    MobileActionItem(
                        icon = Lucide.FileText,
                        label = stringResource(Res.string.cashflow_action_view_document),
                        onClick = { onIntent(CashFlowOverviewIntent.ViewDocumentFor(actionsEntry)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MobileActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Returns the context-aware empty state message based on current filters.
 */
@Composable
private fun getEmptyStateTitle(
    viewMode: CashflowViewMode,
    direction: DirectionFilter
): String = when (viewMode) {
    CashflowViewMode.Upcoming -> when (direction) {
        DirectionFilter.All -> stringResource(Res.string.cashflow_empty_upcoming)
        DirectionFilter.In -> stringResource(Res.string.cashflow_empty_upcoming_in)
        DirectionFilter.Out -> stringResource(Res.string.cashflow_empty_upcoming_out)
    }

    CashflowViewMode.Overdue -> when (direction) {
        DirectionFilter.All -> stringResource(Res.string.cashflow_empty_overdue)
        DirectionFilter.In -> stringResource(Res.string.cashflow_empty_overdue_in)
        DirectionFilter.Out -> stringResource(Res.string.cashflow_empty_overdue_out)
    }

    CashflowViewMode.History -> when (direction) {
        DirectionFilter.All -> stringResource(Res.string.cashflow_empty_history)
        DirectionFilter.In -> stringResource(Res.string.cashflow_empty_history_in)
        DirectionFilter.Out -> stringResource(Res.string.cashflow_empty_history_out)
    }
}

@Preview
@Composable
private fun CashFlowOverviewScreenPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        CashFlowOverviewScreen(
            state = CashFlowOverviewState.initial,
            onIntent = {},
        )
    }
}
