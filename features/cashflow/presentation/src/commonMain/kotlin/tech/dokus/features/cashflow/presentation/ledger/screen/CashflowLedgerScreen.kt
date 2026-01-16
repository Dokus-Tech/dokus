package tech.dokus.features.cashflow.presentation.ledger.screen

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_ledger_empty_hint
import tech.dokus.aura.resources.cashflow_ledger_empty_title
import tech.dokus.features.cashflow.presentation.common.components.empty.DokusEmptyState
import tech.dokus.features.cashflow.presentation.common.components.pagination.rememberLoadMoreTrigger
import tech.dokus.features.cashflow.presentation.common.components.table.DokusTableDivider
import tech.dokus.features.cashflow.presentation.common.components.table.DokusTableSurface
import tech.dokus.features.cashflow.presentation.ledger.components.CashflowDetailPane
import tech.dokus.features.cashflow.presentation.ledger.components.CashflowLedgerHeaderRow
import tech.dokus.features.cashflow.presentation.ledger.components.CashflowLedgerMobileRow
import tech.dokus.features.cashflow.presentation.ledger.components.CashflowLedgerTableRow
import tech.dokus.features.cashflow.presentation.ledger.components.CashflowSummarySection
import tech.dokus.features.cashflow.presentation.ledger.components.CashflowViewModeFilter
import tech.dokus.features.cashflow.presentation.ledger.mvi.CashflowLedgerIntent
import tech.dokus.features.cashflow.presentation.ledger.mvi.CashflowLedgerState
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
            header = null
        ) {
            // Summary section (replaces old status bar)
            CashflowSummarySection(
                summary = state.summary,
                viewMode = state.filters.viewMode
            )

            DokusTableDivider()

            // View mode and direction filters (replaces old 3-row filters)
            CashflowViewModeFilter(
                viewMode = state.filters.viewMode,
                direction = state.filters.direction,
                onViewModeChange = { onIntent(CashflowLedgerIntent.SetViewMode(it)) },
                onDirectionChange = { onIntent(CashflowLedgerIntent.SetDirectionFilter(it)) }
            )

            DokusTableDivider()

            // Table header (desktop only)
            if (isLargeScreen) {
                CashflowLedgerHeaderRow()
                DokusTableDivider()
            }

            // Table body OR empty state
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
            onPaymentAmountTextChange = { onIntent(CashflowLedgerIntent.UpdatePaymentAmountText(it)) },
            onPaymentNoteChange = { onIntent(CashflowLedgerIntent.UpdatePaymentNote(it)) },
            onSubmitPayment = { onIntent(CashflowLedgerIntent.SubmitPayment) },
            onTogglePaymentOptions = { onIntent(CashflowLedgerIntent.TogglePaymentOptions) },
            onQuickMarkAsPaid = { onIntent(CashflowLedgerIntent.QuickMarkAsPaid) },
            onCancelPaymentOptions = { onIntent(CashflowLedgerIntent.CancelPaymentOptions) },
            onOpenDocument = { onIntent(CashflowLedgerIntent.OpenDocument(it)) }
        )
    }
}
