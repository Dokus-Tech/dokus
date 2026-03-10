package tech.dokus.features.banking.presentation.balances.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.banking_balances_accounts_title
import tech.dokus.aura.resources.banking_balances_connect
import tech.dokus.aura.resources.banking_balances_empty_subtitle
import tech.dokus.aura.resources.banking_balances_empty_title
import tech.dokus.aura.resources.banking_balances_upload
import tech.dokus.foundation.aura.style.borderAmber
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.features.banking.presentation.balances.components.BalanceTimelineCard
import tech.dokus.features.banking.presentation.balances.components.BalancesSkeleton
import tech.dokus.features.banking.presentation.balances.components.BalancesStatsRow
import tech.dokus.features.banking.presentation.balances.components.DesktopAccountsTable
import tech.dokus.features.banking.presentation.balances.components.MissingDocumentsCallout
import tech.dokus.features.banking.presentation.balances.components.MobileAccountsList
import tech.dokus.features.banking.presentation.balances.mvi.BalancesIntent
import tech.dokus.features.banking.presentation.balances.mvi.BalancesState
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.app.state.isError
import tech.dokus.foundation.app.state.isLoading
import tech.dokus.foundation.app.state.isSuccess
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.common.DokusEmptyState
import tech.dokus.foundation.aura.components.common.DokusErrorContent
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.aura.local.ScreenSize
import tech.dokus.foundation.aura.local.isLarge
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

// =============================================================================
// Screen
// =============================================================================

@Composable
internal fun BalancesScreen(
    state: BalancesState,
    onIntent: (BalancesIntent) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier,
) {
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier,
    ) {
        BalancesContent(state = state, onIntent = onIntent)
    }
}

@Composable
private fun BalancesContent(
    state: BalancesState,
    onIntent: (BalancesIntent) -> Unit,
) {
    val isLargeScreen = LocalScreenSize.isLarge

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = Constraints.Spacing.large,
                end = Constraints.Spacing.large,
                top = Constraints.Spacing.large,
            ),
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.large),
        contentPadding = PaddingValues(bottom = Constraints.Spacing.large),
    ) {
        // Stats row
        if (state.summary.isSuccess()) {
            item {
                BalancesStatsRow(
                    summary = state.summary.data,
                    transactionSummary = if (state.transactionSummary.isSuccess()) {
                        state.transactionSummary.data
                    } else null,
                )
            }
        }

        // Warning callout
        if (state.summary.isSuccess() && state.summary.data.unmatchedCount > 0) {
            item {
                MissingDocumentsCallout(count = state.summary.data.unmatchedCount)
            }
        }

        // Balance timeline card
        item {
            BalanceTimelineCard(
                summary = state.summary,
                balanceHistory = state.balanceHistory,
                timeRange = state.timeRange,
                onTimeRangeChange = { onIntent(BalancesIntent.SetTimeRange(it)) },
            )
        }

        // Accounts section header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.banking_balances_accounts_title),
                    style = MaterialTheme.typography.titleSmall,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small)) {
                    SmallActionChip(
                        text = stringResource(Res.string.banking_balances_upload),
                        onClick = { onIntent(BalancesIntent.UploadStatement) },
                    )
                    SmallActionChip(
                        text = "+ ${stringResource(Res.string.banking_balances_connect)}",
                        onClick = { onIntent(BalancesIntent.ConnectAccount) },
                        accent = true,
                    )
                }
            }
        }

        // Accounts content
        item {
            DokusCardSurface(modifier = Modifier.fillMaxWidth()) {
                val accountData = state.accounts.lastData
                val isLoading = state.accounts.isLoading()

                when {
                    accountData == null && isLoading -> {
                        BalancesSkeleton()
                    }
                    state.accounts.isError() -> {
                        DokusErrorContent(
                            exception = state.accounts.exception,
                            retryHandler = state.accounts.retryHandler,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                        )
                    }
                    accountData != null && accountData.isEmpty() -> {
                        DokusEmptyState(
                            title = stringResource(Res.string.banking_balances_empty_title),
                            subtitle = stringResource(Res.string.banking_balances_empty_subtitle),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Constraints.Spacing.xxLarge),
                        )
                    }
                    accountData != null -> {
                        if (isLargeScreen) {
                            DesktopAccountsTable(accounts = accountData)
                        } else {
                            MobileAccountsList(accounts = accountData)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallActionChip(
    text: String,
    onClick: () -> Unit,
    accent: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val borderColor = if (accent) colorScheme.borderAmber else colorScheme.outlineVariant
    val textColor = if (accent) colorScheme.primary else colorScheme.onSurfaceVariant

    Surface(
        modifier = modifier
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(Constraints.Spacing.xSmall),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(Constraints.Stroke.thin, borderColor),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = textColor,
            modifier = Modifier.padding(
                horizontal = Constraints.Spacing.medium,
                vertical = Constraints.Spacing.xSmall,
            ),
        )
    }
}

// =============================================================================
// Previews
// =============================================================================

@Preview
@Composable
private fun BalancesScreenLoadingPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        BalancesScreen(
            state = BalancesState.initial,
            onIntent = {},
        )
    }
}

@Preview(name = "Balances Desktop - Success", widthDp = 1366, heightDp = 900)
@Composable
private fun BalancesScreenDesktopSuccessPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        CompositionLocalProvider(LocalScreenSize provides ScreenSize.LARGE) {
            BalancesScreen(
                state = previewBalancesState(),
                onIntent = {},
            )
        }
    }
}

@Preview(name = "Balances Desktop - Error", widthDp = 1366, heightDp = 900)
@Composable
private fun BalancesScreenDesktopErrorPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        CompositionLocalProvider(LocalScreenSize provides ScreenSize.LARGE) {
            BalancesScreen(
                state = BalancesState(
                    accounts = DokusState.error(
                        exception = DokusException.ConnectionError(),
                        retryHandler = {},
                    ),
                    summary = DokusState.error(
                        exception = DokusException.ConnectionError(),
                        retryHandler = {},
                    ),
                    transactionSummary = DokusState.error(
                        exception = DokusException.ConnectionError(),
                        retryHandler = {},
                    ),
                    balanceHistory = DokusState.error(
                        exception = DokusException.ConnectionError(),
                        retryHandler = {},
                    ),
                ),
                onIntent = {},
            )
        }
    }
}

@Preview(name = "Balances Mobile - Success", widthDp = 390, heightDp = 844)
@Composable
private fun BalancesScreenMobileSuccessPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        BalancesScreen(
            state = previewBalancesState(),
            onIntent = {},
        )
    }
}
