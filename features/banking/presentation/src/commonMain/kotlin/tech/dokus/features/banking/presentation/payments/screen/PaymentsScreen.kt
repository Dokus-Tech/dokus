package tech.dokus.features.banking.presentation.payments.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import org.jetbrains.compose.resources.stringResource
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.aura.local.ScreenSize
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.banking_empty_subtitle
import tech.dokus.aura.resources.banking_empty_title
import tech.dokus.features.banking.presentation.payments.components.PaymentFilterTabs
import tech.dokus.features.banking.presentation.payments.components.PaymentsSkeleton
import tech.dokus.features.banking.presentation.payments.components.TransactionRow
import tech.dokus.features.banking.presentation.payments.components.UnresolvedCallout
import tech.dokus.features.banking.presentation.payments.mvi.PaymentsIntent
import tech.dokus.features.banking.presentation.payments.mvi.PaymentsState
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.app.state.isError
import tech.dokus.foundation.app.state.isLoading
import tech.dokus.foundation.app.state.isSuccess
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.common.DokusEmptyState
import tech.dokus.foundation.aura.components.common.DokusErrorContent
import tech.dokus.foundation.aura.components.common.DokusLoader
import tech.dokus.foundation.aura.components.common.DokusLoaderSize
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

@Composable
internal fun PaymentsScreen(
    state: PaymentsState,
    onIntent: (PaymentsIntent) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier,
) {
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier,
    ) {
        PaymentsContent(
            state = state,
            onIntent = onIntent,
        )
    }
}

@Composable
private fun PaymentsContent(
    state: PaymentsState,
    onIntent: (PaymentsIntent) -> Unit,
) {
    val txData = state.transactions.lastData?.data ?: emptyList()
    val isRefreshing = state.transactions.isLoading()
    val listState = rememberLazyListState()

    // Load-more trigger
    val shouldLoadMore by remember(listState, state.transactions) {
        derivedStateOf {
            val hasMore = state.transactions.lastData?.hasMorePages ?: false
            if (!hasMore || isRefreshing) return@derivedStateOf false
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= listState.layoutInfo.totalItemsCount - 5
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onIntent(PaymentsIntent.LoadMore)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = Constraints.Spacing.large,
                end = Constraints.Spacing.large,
                top = Constraints.Spacing.large,
            ),
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.large),
    ) {
        // Unresolved callout (only when summary loaded)
        if (state.summary.isSuccess()) {
            val summary = state.summary.data
            if (summary.unmatchedCount + summary.needsReviewCount > 0) {
                UnresolvedCallout(
                    unresolvedCount = summary.unmatchedCount + summary.needsReviewCount,
                    unresolvedAmount = summary.totalUnresolvedAmount,
                )
            }
        }

        // Filter tabs
        PaymentFilterTabs(
            selectedTab = state.filterTab,
            summary = if (state.summary.isSuccess()) state.summary.data else null,
            onTabSelected = { onIntent(PaymentsIntent.SetFilterTab(it)) },
        )

        // Content area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(bottom = Constraints.Spacing.large),
        ) {
            DokusCardSurface(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    when {
                        // First load — no data yet
                        txData.isEmpty() && isRefreshing -> {
                            PaymentsSkeleton()
                        }
                        // Error with no data
                        txData.isEmpty() && state.transactions.isError() -> {
                            DokusErrorContent(
                                exception = state.transactions.exception,
                                retryHandler = state.transactions.retryHandler,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        // Empty after successful load
                        txData.isEmpty() && state.transactions.isSuccess() -> {
                            DokusEmptyState(
                                title = stringResource(Res.string.banking_empty_title),
                                subtitle = stringResource(Res.string.banking_empty_subtitle),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(Constraints.Spacing.xxLarge),
                            )
                        }
                        // Data loaded
                        else -> {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                itemsIndexed(
                                    items = txData,
                                    key = { _, tx -> tx.id.toString() },
                                ) { index, tx ->
                                    TransactionRow(
                                        transaction = tx,
                                        isSelected = tx.id == state.selectedTransactionId,
                                        onClick = {
                                            onIntent(PaymentsIntent.SelectTransaction(tx.id))
                                        },
                                        onIgnore = {
                                            onIntent(PaymentsIntent.IgnoreTransaction(tx.id))
                                        },
                                        onConfirm = {
                                            onIntent(PaymentsIntent.ConfirmMatch(tx.id))
                                        },
                                    )
                                    if (index < txData.size - 1) {
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant,
                                        )
                                    }
                                }

                                if (isRefreshing) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(Constraints.Spacing.large),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            DokusLoader(size = DokusLoaderSize.Small)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// Previews
// =============================================================================

@Preview
@Composable
private fun PaymentsScreenLoadingPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        PaymentsScreen(
            state = PaymentsState.initial,
            onIntent = {},
        )
    }
}

@Preview(name = "Payments Desktop — Success", widthDp = 1366, heightDp = 900)
@Composable
private fun PaymentsScreenDesktopSuccessPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        CompositionLocalProvider(LocalScreenSize provides ScreenSize.LARGE) {
            PaymentsScreen(
                state = previewPaymentsState(),
                onIntent = {},
            )
        }
    }
}

@Preview(name = "Payments Desktop — Error", widthDp = 1366, heightDp = 900)
@Composable
private fun PaymentsScreenDesktopErrorPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        CompositionLocalProvider(LocalScreenSize provides ScreenSize.LARGE) {
            PaymentsScreen(
                state = PaymentsState(
                    transactions = DokusState.error(
                        exception = DokusException.ConnectionError(),
                        retryHandler = {},
                    ),
                    summary = DokusState.error(
                        exception = DokusException.ConnectionError(),
                        retryHandler = {},
                    ),
                ),
                onIntent = {},
            )
        }
    }
}

@Preview(name = "Payments Mobile — Success", widthDp = 390, heightDp = 844)
@Composable
private fun PaymentsScreenMobileSuccessPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        PaymentsScreen(
            state = previewPaymentsState(),
            onIntent = {},
        )
    }
}
