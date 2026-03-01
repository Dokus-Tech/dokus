package tech.dokus.app.screens.accountant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.console_clients_column_company
import tech.dokus.aura.resources.console_clients_column_vat
import tech.dokus.aura.resources.console_clients_count
import tech.dokus.aura.resources.console_clients_empty
import tech.dokus.aura.resources.console_clients_search_placeholder
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.common.DokusErrorContent
import tech.dokus.foundation.aura.components.common.DokusLoader
import tech.dokus.foundation.aura.components.common.PSearchFieldCompact
import tech.dokus.foundation.aura.components.layout.DokusTableCell
import tech.dokus.foundation.aura.components.layout.DokusTableColumnSpec
import tech.dokus.foundation.aura.components.layout.DokusTableRow
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

private val CompanyColumnSpec = DokusTableColumnSpec(weight = 1.8f)
private val VatColumnSpec = DokusTableColumnSpec(weight = 1f)

@Composable
internal fun ConsoleClientsScreen(
    state: ConsoleClientsState,
    snackbarHostState: SnackbarHostState,
    onIntent: (ConsoleClientsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier,
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = Constraints.Spacing.medium)
                .padding(top = Constraints.Spacing.medium)
        ) {
            when (state) {
                ConsoleClientsState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        DokusLoader()
                    }
                }

                is ConsoleClientsState.Error -> {
                    DokusErrorContent(
                        exception = state.exception,
                        retryHandler = state.retryHandler,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                is ConsoleClientsState.Content -> {
                    ConsoleClientsContent(
                        state = state,
                        onIntent = onIntent,
                    )
                }
            }
        }
    }
}

@Composable
private fun ConsoleClientsContent(
    state: ConsoleClientsState.Content,
    onIntent: (ConsoleClientsIntent) -> Unit,
) {
    val isLargeScreen = LocalScreenSize.current.isLarge

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.medium),
    ) {
        PSearchFieldCompact(
            value = state.query,
            onValueChange = { onIntent(ConsoleClientsIntent.UpdateQuery(it)) },
            placeholder = stringResource(Res.string.console_clients_search_placeholder),
            onClear = {
                onIntent(ConsoleClientsIntent.UpdateQuery(""))
            }
        )

        Text(
            text = stringResource(Res.string.console_clients_count, state.filteredClients.size),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (state.filteredClients.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.console_clients_empty),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return
        }

        if (isLargeScreen) {
            DokusCardSurface(
                modifier = Modifier.fillMaxSize(),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    DokusTableRow(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = Constraints.Spacing.medium
                        ),
                    ) {
                        DokusTableCell(column = CompanyColumnSpec) {
                            Text(
                                text = stringResource(Res.string.console_clients_column_company),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        DokusTableCell(column = VatColumnSpec) {
                            Text(
                                text = stringResource(Res.string.console_clients_column_vat),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(
                            items = state.filteredClients,
                            key = { it.tenantId.toString() }
                        ) { client ->
                            val isSelecting = state.selectingTenantId == client.tenantId
                            DokusTableRow(
                                onClick = {
                                    if (!isSelecting && state.selectingTenantId == null) {
                                        onIntent(ConsoleClientsIntent.SelectClient(client.tenantId))
                                    }
                                },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                    horizontal = Constraints.Spacing.medium
                                ),
                            ) {
                                DokusTableCell(column = CompanyColumnSpec) {
                                    Text(
                                        text = client.companyName.value,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                                DokusTableCell(column = VatColumnSpec) {
                                    Text(
                                        text = client.vatNumber?.value ?: "—",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    items = state.filteredClients,
                    key = { it.tenantId.toString() }
                ) { client ->
                    val isSelecting = state.selectingTenantId == client.tenantId
                    DokusCardSurface(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            if (!isSelecting && state.selectingTenantId == null) {
                                onIntent(ConsoleClientsIntent.SelectClient(client.tenantId))
                            }
                        },
                        enabled = !isSelecting,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Constraints.Spacing.medium),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = client.companyName.value,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    text = client.vatNumber?.value ?: "—",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun ConsoleClientsScreenPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        ConsoleClientsScreen(
            state = ConsoleClientsState.Loading,
            snackbarHostState = SnackbarHostState(),
            onIntent = {},
        )
    }
}
