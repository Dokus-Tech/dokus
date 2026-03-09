package tech.dokus.features.banking.presentation.balances.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.banking_balances_accounts_title
import tech.dokus.aura.resources.banking_balances_col_account
import tech.dokus.aura.resources.banking_balances_col_balance
import tech.dokus.aura.resources.banking_balances_col_last_sync
import tech.dokus.aura.resources.banking_balances_col_status
import tech.dokus.aura.resources.banking_balances_col_type
import tech.dokus.aura.resources.banking_balances_empty_subtitle
import tech.dokus.aura.resources.banking_balances_empty_title
import tech.dokus.aura.resources.banking_balances_status_inactive
import tech.dokus.aura.resources.banking_balances_status_synced
import tech.dokus.domain.enums.BankAccountType
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.model.BankConnectionDto
import tech.dokus.features.banking.presentation.balances.components.BalanceTimelineCard
import tech.dokus.features.banking.presentation.balances.components.BalancesSkeleton
import tech.dokus.features.banking.presentation.balances.components.BalancesStatsRow
import tech.dokus.features.banking.presentation.balances.components.MissingDocumentsCallout
import tech.dokus.features.banking.presentation.balances.components.formatRelativeTime
import tech.dokus.features.banking.presentation.balances.mvi.BalancesIntent
import tech.dokus.features.banking.presentation.balances.mvi.BalancesState
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.app.state.isError
import tech.dokus.foundation.app.state.isLoading
import tech.dokus.foundation.app.state.isSuccess
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.common.DokusEmptyState
import tech.dokus.foundation.aura.components.common.DokusErrorContent
import tech.dokus.foundation.aura.components.layout.DokusHeaderColumn
import tech.dokus.foundation.aura.components.layout.DokusTableHeader
import tech.dokus.foundation.aura.components.layout.DokusTableColumnSpec
import tech.dokus.foundation.aura.components.layout.DokusTableCell
import tech.dokus.foundation.aura.components.layout.DokusTableRow
import tech.dokus.foundation.aura.components.text.Amt
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.aura.local.ScreenSize
import tech.dokus.foundation.aura.local.isLarge
import tech.dokus.foundation.aura.style.positionPositive
import tech.dokus.foundation.aura.style.textMuted
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

// =============================================================================
// Table column specs
// =============================================================================

private object BalancesTableColumns {
    val Account = DokusTableColumnSpec(weight = 1f)
    val Type = DokusTableColumnSpec(width = 90.dp)
    val Balance = DokusTableColumnSpec(width = 140.dp)
    val LastSync = DokusTableColumnSpec(width = 90.dp)
    val Status = DokusTableColumnSpec(width = 80.dp)
}

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
                connections = state.connections,
                timeRange = state.timeRange,
                onTimeRangeChange = { onIntent(BalancesIntent.SetTimeRange(it)) },
            )
        }

        // Accounts section header
        item {
            AccountsSectionHeader()
        }

        // Accounts content
        item {
            DokusCardSurface(modifier = Modifier.fillMaxWidth()) {
                val connectionData = state.connections.lastData
                val isLoading = state.connections.isLoading()

                when {
                    connectionData == null && isLoading -> {
                        BalancesSkeleton()
                    }
                    state.connections.isError() -> {
                        DokusErrorContent(
                            exception = state.connections.exception,
                            retryHandler = state.connections.retryHandler,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                        )
                    }
                    connectionData != null && connectionData.isEmpty() -> {
                        DokusEmptyState(
                            title = stringResource(Res.string.banking_balances_empty_title),
                            subtitle = stringResource(Res.string.banking_balances_empty_subtitle),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Constraints.Spacing.xxLarge),
                        )
                    }
                    connectionData != null -> {
                        if (isLargeScreen) {
                            DesktopAccountsTable(connections = connectionData)
                        } else {
                            MobileAccountsList(connections = connectionData)
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// Section header
// =============================================================================

@Composable
private fun AccountsSectionHeader(
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(Res.string.banking_balances_accounts_title),
        style = MaterialTheme.typography.titleSmall,
        modifier = modifier,
    )
}

// =============================================================================
// Desktop: accounts table
// =============================================================================

@Composable
private fun DesktopAccountsTable(
    connections: List<BankConnectionDto>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        DokusTableHeader(
            columns = listOf(
                DokusHeaderColumn(
                    label = stringResource(Res.string.banking_balances_col_account),
                    weight = 1f,
                ),
                DokusHeaderColumn(
                    label = stringResource(Res.string.banking_balances_col_type),
                    width = 90.dp,
                ),
                DokusHeaderColumn(
                    label = stringResource(Res.string.banking_balances_col_balance),
                    width = 140.dp,
                ),
                DokusHeaderColumn(
                    label = stringResource(Res.string.banking_balances_col_last_sync),
                    width = 90.dp,
                ),
                DokusHeaderColumn(
                    label = stringResource(Res.string.banking_balances_col_status),
                    width = 80.dp,
                ),
            ),
        )

        connections.forEachIndexed { index, connection ->
            AccountTableRow(connection = connection)
            if (index < connections.size - 1) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun AccountTableRow(
    connection: BankConnectionDto,
    modifier: Modifier = Modifier,
) {
    DokusTableRow(
        modifier = modifier,
        minHeight = Constraints.Height.input,
        contentPadding = PaddingValues(horizontal = Constraints.Spacing.large),
    ) {
        // Account: name + IBAN
        DokusTableCell(BalancesTableColumns.Account) {
            Column(verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xxSmall)) {
                Text(
                    text = connection.accountName ?: connection.institutionName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
                connection.iban?.let { iban ->
                    Text(
                        text = formatIban(iban.value),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.textMuted,
                        maxLines = 1,
                    )
                }
            }
        }

        // Type badge
        DokusTableCell(BalancesTableColumns.Type) {
            connection.accountType?.let { type ->
                AccountTypeBadge(type = type)
            }
        }

        // Balance + provider
        DokusTableCell(BalancesTableColumns.Balance) {
            Column(verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xxSmall)) {
                connection.balance?.let { balance ->
                    Amt(minorUnits = balance.minor)
                }
                Text(
                    text = connection.provider.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textMuted,
                    maxLines = 1,
                )
            }
        }

        // Last sync
        DokusTableCell(BalancesTableColumns.LastSync) {
            connection.lastSyncedAt?.let { syncedAt ->
                Text(
                    text = formatRelativeTime(syncedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textMuted,
                    maxLines = 1,
                )
            }
        }

        // Status
        DokusTableCell(BalancesTableColumns.Status) {
            StatusIndicator(isActive = connection.isActive)
        }
    }
}

// =============================================================================
// Mobile: card list
// =============================================================================

@Composable
private fun MobileAccountsList(
    connections: List<BankConnectionDto>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        connections.forEachIndexed { index, connection ->
            MobileAccountCard(connection = connection)
            if (index < connections.size - 1) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun MobileAccountCard(
    connection: BankConnectionDto,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(Constraints.Spacing.large),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xxSmall),
        ) {
            Text(
                text = connection.accountName ?: connection.institutionName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
            connection.iban?.let { iban ->
                Text(
                    text = formatIban(iban.value),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textMuted,
                    maxLines = 1,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                connection.accountType?.let { type ->
                    AccountTypeBadge(type = type)
                }
                StatusIndicator(isActive = connection.isActive)
            }
        }

        connection.balance?.let { balance ->
            Amt(minorUnits = balance.minor)
        }
    }
}

// =============================================================================
// Shared components
// =============================================================================

@Composable
private fun AccountTypeBadge(
    type: BankAccountType,
    modifier: Modifier = Modifier,
) {
    val (backgroundColor, textColor) = remember(type) {
        when (type) {
            BankAccountType.Checking -> Color(0xFF3B82F6).copy(alpha = 0.12f) to Color(0xFF3B82F6)
            BankAccountType.Savings -> Color(0xFF10B981).copy(alpha = 0.12f) to Color(0xFF10B981)
            BankAccountType.CreditCard -> Color(0xFFF59E0B).copy(alpha = 0.12f) to Color(0xFFF59E0B)
            BankAccountType.Business -> Color(0xFF8B5CF6).copy(alpha = 0.12f) to Color(0xFF8B5CF6)
            BankAccountType.Investment -> Color(0xFFEC4899).copy(alpha = 0.12f) to Color(0xFFEC4899)
        }
    }

    Box(
        modifier = modifier
            .background(
                color = backgroundColor,
                shape = MaterialTheme.shapes.extraSmall,
            )
            .padding(
                horizontal = Constraints.Spacing.small,
                vertical = Constraints.Spacing.xxSmall,
            ),
    ) {
        Text(
            text = type.name,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun StatusIndicator(
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(Constraints.StatusDot.size)
                .clip(CircleShape)
                .background(
                    if (isActive) {
                        MaterialTheme.colorScheme.positionPositive
                    } else {
                        MaterialTheme.colorScheme.textMuted
                    }
                ),
        )
        Text(
            text = stringResource(
                if (isActive) Res.string.banking_balances_status_synced
                else Res.string.banking_balances_status_inactive
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.textMuted,
            maxLines = 1,
        )
    }
}

/**
 * Formats an IBAN with spaces every 4 characters for display.
 */
private fun formatIban(iban: String): String {
    val clean = iban.replace(" ", "")
    return clean.chunked(4).joinToString(" ")
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
                    connections = DokusState.error(
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
