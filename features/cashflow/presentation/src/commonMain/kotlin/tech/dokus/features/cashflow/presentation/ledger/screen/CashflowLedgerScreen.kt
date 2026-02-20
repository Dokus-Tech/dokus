package tech.dokus.features.cashflow.presentation.ledger.screen

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Payment
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
import androidx.compose.ui.unit.dp
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
import tech.dokus.aura.resources.cashflow_title
import tech.dokus.features.cashflow.presentation.common.components.empty.DokusEmptyState
import tech.dokus.features.cashflow.presentation.common.components.pagination.rememberLoadMoreTrigger
import tech.dokus.features.cashflow.presentation.common.components.table.DokusTableDivider
import tech.dokus.features.cashflow.presentation.common.components.table.DokusTableSurface
import tech.dokus.features.cashflow.presentation.ledger.components.CashflowDetailPane
import tech.dokus.features.cashflow.presentation.ledger.components.CashflowLedgerHeaderRow
import tech.dokus.features.cashflow.presentation.ledger.components.CashflowLedgerMobileRow
import tech.dokus.features.cashflow.presentation.ledger.components.CashflowLedgerSkeleton
import tech.dokus.features.cashflow.presentation.ledger.components.CashflowLedgerTableRow
import tech.dokus.features.cashflow.presentation.ledger.components.CashflowSummarySection
import tech.dokus.features.cashflow.presentation.ledger.components.CashflowViewModeFilter
import tech.dokus.features.cashflow.presentation.ledger.mvi.CashflowLedgerIntent
import tech.dokus.features.cashflow.presentation.ledger.mvi.CashflowLedgerState
import tech.dokus.features.cashflow.presentation.ledger.mvi.CashflowViewMode
import tech.dokus.features.cashflow.presentation.ledger.mvi.DirectionFilter
import tech.dokus.foundation.aura.components.common.DokusErrorContent
import tech.dokus.foundation.aura.components.common.DokusLoader
import tech.dokus.foundation.aura.components.common.DokusLoaderSize
import tech.dokus.foundation.aura.components.text.MobilePageTitle
import tech.dokus.foundation.aura.local.LocalScreenSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CashflowLedgerScreen(
    state: CashflowLedgerState,
    onIntent: (CashflowLedgerIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val isLargeScreen = LocalScreenSize.current.isLarge
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier
    ) {
        Box(Modifier.fillMaxSize()) {
            when (state) {
                is CashflowLedgerState.Loading -> {
                    CashflowLedgerSkeleton(
                        showHeader = isLargeScreen,
                        rowCount = 5,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    )
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
}

@OptIn(ExperimentalMaterial3Api::class)
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

    // Derive spark data from visible entries (up to 8 amounts)
    val sparkData = remember(state.entries.data) {
        state.entries.data
            .take(8)
            .map { kotlin.math.abs(it.amountGross.toDouble()) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            MobilePageTitle(title = stringResource(Res.string.cashflow_title))

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
                onViewModeChange = { onIntent(CashflowLedgerIntent.SetViewMode(it)) },
                onDirectionChange = { onIntent(CashflowLedgerIntent.SetDirectionFilter(it)) },
            )

            // Table surface (data only)
            DokusTableSurface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                header = if (isLargeScreen) {
                    { CashflowLedgerHeaderRow() }
                } else null
            ) {
                // Table body OR empty state
                if (state.entries.data.isEmpty() && !state.entries.isLoadingMore) {
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
                            items = state.entries.data,
                            key = { _, entry -> entry.id.toString() }
                        ) { index, entry ->
                            if (isLargeScreen) {
                                CashflowLedgerTableRow(
                                    entry = entry,
                                    viewMode = state.filters.viewMode,
                                    isHighlighted = entry.id == state.highlightedEntryId,
                                    showActionsMenu = state.actionsEntryId == entry.id,
                                    onClick = { onIntent(CashflowLedgerIntent.OpenEntry(entry)) },
                                    onShowActions = { onIntent(CashflowLedgerIntent.ShowRowActions(entry.id)) },
                                    onHideActions = { onIntent(CashflowLedgerIntent.HideRowActions) },
                                    onRecordPayment = { onIntent(CashflowLedgerIntent.RecordPaymentFor(entry.id)) },
                                    onMarkAsPaid = { onIntent(CashflowLedgerIntent.MarkAsPaidQuick(entry.id)) },
                                    onViewDocument = { onIntent(CashflowLedgerIntent.ViewDocumentFor(entry)) }
                                )
                            } else {
                                CashflowLedgerMobileRow(
                                    entry = entry,
                                    viewMode = state.filters.viewMode,
                                    onClick = { onIntent(CashflowLedgerIntent.OpenEntry(entry)) },
                                    onShowActions = { onIntent(CashflowLedgerIntent.ShowRowActions(entry.id)) }
                                )
                            }

                            // Dividers only between rows
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
                                    DokusLoader(size = DokusLoaderSize.Small)
                                }
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
            onPaymentAmountTextChange = { onIntent(CashflowLedgerIntent.UpdatePaymentAmountText(it)) },
            onPaymentNoteChange = { onIntent(CashflowLedgerIntent.UpdatePaymentNote(it)) },
            onSubmitPayment = { onIntent(CashflowLedgerIntent.SubmitPayment) },
            onTogglePaymentOptions = { onIntent(CashflowLedgerIntent.TogglePaymentOptions) },
            onQuickMarkAsPaid = { onIntent(CashflowLedgerIntent.QuickMarkAsPaid) },
            onCancelPaymentOptions = { onIntent(CashflowLedgerIntent.CancelPaymentOptions) },
            onOpenDocument = { onIntent(CashflowLedgerIntent.OpenDocument(it)) }
        )

        // Mobile actions bottom sheet
        val actionsEntry = state.entries.data.find { it.id == state.actionsEntryId }
        if (!isLargeScreen && actionsEntry != null) {
            val sheetState = rememberModalBottomSheetState()
            ModalBottomSheet(
                onDismissRequest = { onIntent(CashflowLedgerIntent.HideRowActions) },
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
                            icon = Icons.Default.Payment,
                            label = stringResource(Res.string.cashflow_action_record_payment),
                            onClick = { onIntent(CashflowLedgerIntent.RecordPaymentFor(actionsEntry.id)) }
                        )
                        // Mark as paid
                        MobileActionItem(
                            icon = Icons.Default.CheckCircle,
                            label = stringResource(Res.string.cashflow_action_mark_paid),
                            onClick = { onIntent(CashflowLedgerIntent.MarkAsPaidQuick(actionsEntry.id)) }
                        )
                    }
                    // View document (all modes)
                    MobileActionItem(
                        icon = Icons.Default.Description,
                        label = stringResource(Res.string.cashflow_action_view_document),
                        onClick = { onIntent(CashflowLedgerIntent.ViewDocumentFor(actionsEntry)) }
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
