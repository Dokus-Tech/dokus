package tech.dokus.app.screens.accountant

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_close
import tech.dokus.aura.resources.cancel
import tech.dokus.aura.resources.console_back_to_clients
import tech.dokus.aura.resources.console_client_detail_access_granted
import tech.dokus.aura.resources.console_client_detail_completeness
import tech.dokus.aura.resources.console_client_detail_payables
import tech.dokus.aura.resources.console_client_detail_peppol
import tech.dokus.aura.resources.console_client_detail_receivables
import tech.dokus.aura.resources.console_client_detail_requests
import tech.dokus.aura.resources.console_clients_add_client
import tech.dokus.aura.resources.console_clients_add_client_dialog_title
import tech.dokus.aura.resources.console_clients_add_client_email
import tech.dokus.aura.resources.console_clients_column_cashflow
import tech.dokus.aura.resources.console_clients_column_client
import tech.dokus.aura.resources.console_clients_column_completeness
import tech.dokus.aura.resources.console_clients_column_docs
import tech.dokus.aura.resources.console_clients_column_last_activity
import tech.dokus.aura.resources.console_clients_column_peppol
import tech.dokus.aura.resources.console_clients_column_period
import tech.dokus.aura.resources.console_clients_column_reqs
import tech.dokus.aura.resources.console_clients_column_vat
import tech.dokus.aura.resources.console_clients_count
import tech.dokus.aura.resources.console_clients_empty
import tech.dokus.aura.resources.console_clients_empty_all
import tech.dokus.aura.resources.console_clients_filter_all
import tech.dokus.aura.resources.console_clients_filter_critical
import tech.dokus.aura.resources.console_clients_filter_gaps
import tech.dokus.aura.resources.console_clients_filter_peppol_issues
import tech.dokus.aura.resources.console_clients_search_placeholder
import tech.dokus.aura.resources.console_no_documents_yet
import tech.dokus.aura.resources.console_requests_period_label
import tech.dokus.domain.DisplayName
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.FirmId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.DocumentDto
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.model.auth.ConsoleClientSummary
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.app.state.isError
import tech.dokus.foundation.app.state.isLoading
import tech.dokus.foundation.app.state.isSuccess
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.PPrimaryButton
import tech.dokus.foundation.aura.components.badges.DocumentSource as BadgeDocumentSource
import tech.dokus.foundation.aura.components.badges.SourceBadge
import tech.dokus.app.screens.accountant.components.ConsoleClientsSkeleton
import tech.dokus.foundation.aura.components.common.DokusErrorContent
import tech.dokus.foundation.aura.components.common.PSearchFieldCompact
import tech.dokus.foundation.aura.components.dialog.DokusDialog
import tech.dokus.foundation.aura.components.dialog.DokusDialogAction
import tech.dokus.foundation.aura.components.fields.PTextFieldStandard
import tech.dokus.foundation.aura.components.layout.DokusTableCell
import tech.dokus.foundation.aura.components.layout.DokusTableColumnSpec
import tech.dokus.foundation.aura.components.layout.DokusTableRow
import tech.dokus.foundation.aura.components.status.StatusDot
import tech.dokus.foundation.aura.components.status.StatusDotType
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper
import kotlin.math.absoluteValue

private enum class ClientFilter {
    All, PeppolIssues, Gaps, Critical
}

private data class ConsoleClientInsight(
    val peppolLabel: String,
    val peppolStatus: StatusDotType,
    val docsCount: Int,
    val completenessLabel: String,
    val completenessStatus: StatusDotType,
    val requestsCount: Int,
    val lastActivityLabel: String,
    val cashflowLabel: String,
    val cashflowStatus: StatusDotType,
    val periodLabel: String,
    val periodStatus: StatusDotType,
    val hasPeppolIssues: Boolean,
    val hasGaps: Boolean,
    val isCritical: Boolean,
)

private data class ConsoleDocumentRow(
    val id: String,
    val date: String,
    val number: String,
    val counterparty: String,
    val amount: String,
    val vatType: String,
    val origin: BadgeDocumentSource,
    val statusLabel: String,
    val statusType: StatusDotType,
)

private val ClientColumnSpec = DokusTableColumnSpec(weight = 2f)
private val VatColumnSpec = DokusTableColumnSpec(weight = 1.5f)
private val PeppolColumnSpec = DokusTableColumnSpec(weight = 1f)
private val DocsColumnSpec = DokusTableColumnSpec(weight = 0.7f)
private val CompletenessColumnSpec = DokusTableColumnSpec(weight = 1.2f)
private val RequestsColumnSpec = DokusTableColumnSpec(weight = 0.9f)
private val LastActivityColumnSpec = DokusTableColumnSpec(weight = 1.1f)
private val CashflowColumnSpec = DokusTableColumnSpec(weight = 1f)
private val PeriodColumnSpec = DokusTableColumnSpec(weight = 1f)

private val DetailDateColumnSpec = DokusTableColumnSpec(weight = 0.9f)
private val DetailNumberColumnSpec = DokusTableColumnSpec(weight = 1.4f)
private val DetailCounterpartyColumnSpec = DokusTableColumnSpec(weight = 1.8f)
private val DetailAmountColumnSpec = DokusTableColumnSpec(weight = 1f)
private val DetailVatTypeColumnSpec = DokusTableColumnSpec(weight = 1f)
private val DetailOriginColumnSpec = DokusTableColumnSpec(weight = 0.9f)
private val DetailStatusColumnSpec = DokusTableColumnSpec(weight = 1f)

@Composable
internal fun ConsoleClientsScreen(
    state: ConsoleClientsState,
    snackbarHostState: SnackbarHostState,
    onIntent: (ConsoleClientsIntent) -> Unit,
    onAddClientClick: () -> Unit = {},
    initialShowAddClientDialog: Boolean = false,
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
            val clientsState = state.clients
            when {
                clientsState.isLoading() -> {
                    ConsoleClientsSkeleton(modifier = Modifier.fillMaxSize())
                }

                clientsState.isError() -> {
                    DokusErrorContent(
                        exception = clientsState.exception,
                        retryHandler = clientsState.retryHandler,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                clientsState.isSuccess() -> {
                    if (state.selectedClientTenantId == null) {
                        ClientsListContent(
                            state = state,
                            clients = clientsState.data,
                            onIntent = onIntent,
                            onAddClientClick = onAddClientClick,
                            initialShowAddClientDialog = initialShowAddClientDialog,
                        )
                    } else {
                        ClientDetailContent(
                            state = state,
                            clients = clientsState.data,
                            onIntent = onIntent,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClientsListContent(
    state: ConsoleClientsState,
    clients: List<ConsoleClientSummary>,
    onIntent: (ConsoleClientsIntent) -> Unit,
    onAddClientClick: () -> Unit,
    initialShowAddClientDialog: Boolean,
) {
    var showAddClientDialog by remember { mutableStateOf(initialShowAddClientDialog) }
    var addClientEmail by remember { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf(ClientFilter.All) }

    val clientsWithInsights = remember(clients) {
        clients.mapIndexed { index, client ->
            client to insightForClient(client = client, index = index)
        }
    }
    val filteredByQuery = state.filteredClients.toSet()
    val baseRows = remember(clientsWithInsights, filteredByQuery) {
        clientsWithInsights.filter { (client, _) -> client in filteredByQuery }
    }
    val visibleRows = remember(baseRows, selectedFilter) {
        baseRows.filter { (_, insight) ->
            when (selectedFilter) {
                ClientFilter.All -> true
                ClientFilter.PeppolIssues -> insight.hasPeppolIssues
                ClientFilter.Gaps -> insight.hasGaps
                ClientFilter.Critical -> insight.isCritical
            }
        }
    }
    val filterCounts = remember(clientsWithInsights) {
        mapOf(
            ClientFilter.All to clientsWithInsights.size,
            ClientFilter.PeppolIssues to clientsWithInsights.count { it.second.hasPeppolIssues },
            ClientFilter.Gaps to clientsWithInsights.count { it.second.hasGaps },
            ClientFilter.Critical to clientsWithInsights.count { it.second.isCritical },
        )
    }
    val periodLabel = stringResource(Res.string.console_requests_period_label)

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.medium),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PSearchFieldCompact(
                value = state.query,
                onValueChange = { onIntent(ConsoleClientsIntent.UpdateQuery(it)) },
                placeholder = stringResource(Res.string.console_clients_search_placeholder),
                onClear = { onIntent(ConsoleClientsIntent.UpdateQuery("")) },
                modifier = Modifier.weight(1f),
            )
            ConsoleFilterChip(
                label = stringResource(Res.string.console_clients_filter_all),
                count = filterCounts[ClientFilter.All] ?: 0,
                selected = selectedFilter == ClientFilter.All,
                onClick = { selectedFilter = ClientFilter.All },
            )
            ConsoleFilterChip(
                label = stringResource(Res.string.console_clients_filter_peppol_issues),
                count = filterCounts[ClientFilter.PeppolIssues] ?: 0,
                selected = selectedFilter == ClientFilter.PeppolIssues,
                onClick = { selectedFilter = ClientFilter.PeppolIssues },
            )
            ConsoleFilterChip(
                label = stringResource(Res.string.console_clients_filter_gaps),
                count = filterCounts[ClientFilter.Gaps] ?: 0,
                selected = selectedFilter == ClientFilter.Gaps,
                onClick = { selectedFilter = ClientFilter.Gaps },
            )
            ConsoleFilterChip(
                label = stringResource(Res.string.console_clients_filter_critical),
                count = filterCounts[ClientFilter.Critical] ?: 0,
                selected = selectedFilter == ClientFilter.Critical,
                onClick = { selectedFilter = ClientFilter.Critical },
            )
            PPrimaryButton(
                text = stringResource(Res.string.console_clients_add_client),
                onClick = {
                    showAddClientDialog = true
                    onAddClientClick()
                },
            )
        }

        if (visibleRows.isEmpty()) {
            val emptyText = if (clients.isEmpty()) {
                stringResource(Res.string.console_clients_empty_all)
            } else {
                stringResource(Res.string.console_clients_empty)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = emptyText,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            DokusCardSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    DokusTableRow(contentPadding = PaddingValues(horizontal = Constraints.Spacing.medium)) {
                        HeaderCell(Res.string.console_clients_column_client, ClientColumnSpec)
                        HeaderCell(Res.string.console_clients_column_vat, VatColumnSpec)
                        HeaderCell(Res.string.console_clients_column_peppol, PeppolColumnSpec)
                        HeaderCell(Res.string.console_clients_column_docs, DocsColumnSpec)
                        HeaderCell(Res.string.console_clients_column_completeness, CompletenessColumnSpec)
                        HeaderCell(Res.string.console_clients_column_reqs, RequestsColumnSpec)
                        HeaderCell(Res.string.console_clients_column_last_activity, LastActivityColumnSpec)
                        HeaderCell(Res.string.console_clients_column_cashflow, CashflowColumnSpec)
                        HeaderCell(Res.string.console_clients_column_period, PeriodColumnSpec)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(
                            items = visibleRows,
                            key = { (client, _) -> client.tenantId.toString() }
                        ) { (client, insight) ->
                            DokusTableRow(
                                onClick = { onIntent(ConsoleClientsIntent.SelectClient(client.tenantId)) },
                                contentPadding = PaddingValues(horizontal = Constraints.Spacing.medium),
                            ) {
                                DokusTableCell(column = ClientColumnSpec) {
                                    Text(
                                        text = client.companyName.value,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                                DokusTableCell(column = VatColumnSpec) {
                                    Text(
                                        text = client.vatNumber?.formatted ?: "—",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                DokusTableCell(column = PeppolColumnSpec) {
                                    StatusWithDot(
                                        label = insight.peppolLabel,
                                        status = insight.peppolStatus,
                                    )
                                }
                                DokusTableCell(column = DocsColumnSpec) {
                                    Text(
                                        text = insight.docsCount.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                                DokusTableCell(column = CompletenessColumnSpec) {
                                    StatusWithDot(
                                        label = insight.completenessLabel,
                                        status = insight.completenessStatus,
                                    )
                                }
                                DokusTableCell(column = RequestsColumnSpec) {
                                    RequestsBadge(count = insight.requestsCount)
                                }
                                DokusTableCell(column = LastActivityColumnSpec) {
                                    Text(
                                        text = insight.lastActivityLabel,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                DokusTableCell(column = CashflowColumnSpec) {
                                    StatusWithDot(
                                        label = insight.cashflowLabel,
                                        status = insight.cashflowStatus,
                                    )
                                }
                                DokusTableCell(column = PeriodColumnSpec) {
                                    StatusWithDot(
                                        label = insight.periodLabel,
                                        status = insight.periodStatus,
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }

        Text(
            text = "${stringResource(Res.string.console_clients_count, visibleRows.size)} · $periodLabel",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (showAddClientDialog) {
        DokusDialog(
            onDismissRequest = { showAddClientDialog = false },
            title = stringResource(Res.string.console_clients_add_client_dialog_title),
            content = {
                PTextFieldStandard(
                    fieldName = stringResource(Res.string.console_clients_add_client_email),
                    value = addClientEmail,
                    onValueChange = { addClientEmail = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            primaryAction = DokusDialogAction(
                text = stringResource(Res.string.action_close),
                onClick = { showAddClientDialog = false },
            ),
            secondaryAction = DokusDialogAction(
                text = stringResource(Res.string.cancel),
                onClick = { showAddClientDialog = false },
            ),
        )
    }
}

@Composable
private fun ClientDetailContent(
    state: ConsoleClientsState,
    clients: List<ConsoleClientSummary>,
    onIntent: (ConsoleClientsIntent) -> Unit,
) {
    val selectedClient = clients.firstOrNull { it.tenantId == state.selectedClientTenantId }
    if (selectedClient == null) {
        onIntent(ConsoleClientsIntent.BackToClients)
        return
    }

    val selectedIndex = clients.indexOfFirst { it.tenantId == selectedClient.tenantId }.coerceAtLeast(0)
    val insight = remember(selectedClient.tenantId) { insightForClient(selectedClient, selectedIndex) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.medium),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
            ) {
                Text(
                    text = "‹ ${stringResource(Res.string.console_back_to_clients)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onIntent(ConsoleClientsIntent.BackToClients) },
                )
                Text(
                    text = "/",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = selectedClient.companyName.value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = selectedClient.vatNumber?.formatted ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
            ) {
                Text(
                    text = stringResource(Res.string.console_client_detail_access_granted),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                DokusCardSurface(accent = true) {
                    Text(
                        text = state.firmName.orEmpty(),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small)) {
            SummaryCard(
                title = stringResource(Res.string.console_client_detail_receivables),
                value = "€${formatEuro(8400 + selectedIndex * 350)}",
                subtitle = "None overdue",
                statusType = StatusDotType.Confirmed,
                modifier = Modifier.weight(1f),
            )
            SummaryCard(
                title = stringResource(Res.string.console_client_detail_payables),
                value = "€${formatEuro(3200 + selectedIndex * 260)}",
                subtitle = "None overdue",
                statusType = StatusDotType.Confirmed,
                modifier = Modifier.weight(1f),
            )
            SummaryCard(
                title = stringResource(Res.string.console_client_detail_peppol),
                value = insight.peppolLabel,
                subtitle = "No anomalies detected",
                statusType = insight.peppolStatus,
                modifier = Modifier.weight(1f),
            )
            SummaryCard(
                title = stringResource(Res.string.console_client_detail_requests),
                value = insight.requestsCount.toString(),
                subtitle = if (insight.requestsCount == 0) "All clear" else "Needs attention",
                statusType = if (insight.requestsCount == 0) StatusDotType.Confirmed else StatusDotType.Warning,
                modifier = Modifier.weight(1f),
            )
        }

        DokusCardSurface(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Constraints.Spacing.medium),
                verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
            ) {
                Text(
                    text = stringResource(Res.string.console_client_detail_completeness),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                CompletenessRow("Purchase invoices", "11 received", StatusDotType.Confirmed)
                CompletenessRow("Sales invoices", "3 sent", StatusDotType.Confirmed)
                CompletenessRow("Bank statements", "Missing — last January 31", StatusDotType.Error, showRequest = true)
                CompletenessRow("Credit card statements", "Missing — last January 15", StatusDotType.Error, showRequest = true)
                CompletenessRow("Receipts", "3 uploaded", StatusDotType.Confirmed)
            }
        }

        when (val documentsState = state.documentsState) {
            is DokusState.Loading -> {
                ConsoleClientsSkeleton(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
            }

            is DokusState.Error -> {
                DokusErrorContent(
                    exception = documentsState.exception,
                    retryHandler = documentsState.retryHandler,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            is DokusState.Success -> {
                val documentRows = remember(documentsState.data) {
                    documentsState.data.mapIndexed { index, record ->
                        record.toConsoleDocumentRow(index)
                    }
                }
                if (documentRows.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(Res.string.console_no_documents_yet),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    DocumentsOverviewTable(
                        rows = documentRows,
                        onRowClick = { documentId ->
                            onIntent(ConsoleClientsIntent.OpenDocument(documentId))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                }
            }

            else -> Unit
        }
    }
}

@Composable
private fun DocumentsOverviewTable(
    rows: List<ConsoleDocumentRow>,
    onRowClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    DokusCardSurface(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Constraints.Spacing.medium),
                horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PSearchFieldCompact(
                    value = "",
                    onValueChange = {},
                    placeholder = "Counterparty or number...",
                    onClear = {},
                    modifier = Modifier.weight(1f),
                )
                DetailFilterPill("All")
                DetailFilterPill("Purchase")
                DetailFilterPill("Sales")
                DetailFilterPill("Receipt")
                DetailFilterPill("Bank")
                DetailFilterPill("Any")
                DetailFilterPill("Confirmed")
                DetailFilterPill("Review")
                Text(
                    text = rows.size.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            DokusTableRow(contentPadding = PaddingValues(horizontal = Constraints.Spacing.medium)) {
                HeaderCellRaw("DATE", DetailDateColumnSpec)
                HeaderCellRaw("NUMBER", DetailNumberColumnSpec)
                HeaderCellRaw("COUNTERPARTY", DetailCounterpartyColumnSpec)
                HeaderCellRaw("AMOUNT", DetailAmountColumnSpec)
                HeaderCellRaw("VATTYPE", DetailVatTypeColumnSpec)
                HeaderCellRaw("ORIGIN", DetailOriginColumnSpec)
                HeaderCellRaw("STATUS", DetailStatusColumnSpec)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(rows, key = { it.id }) { row ->
                    DokusTableRow(
                        onClick = { onRowClick(row.id) },
                        contentPadding = PaddingValues(horizontal = Constraints.Spacing.medium),
                    ) {
                        DokusTableCell(column = DetailDateColumnSpec) {
                            Text(row.date, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        DokusTableCell(column = DetailNumberColumnSpec) {
                            Text(row.number, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        DokusTableCell(column = DetailCounterpartyColumnSpec) {
                            Text(row.counterparty, style = MaterialTheme.typography.bodyLarge)
                        }
                        DokusTableCell(column = DetailAmountColumnSpec) {
                            Text(row.amount, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                        }
                        DokusTableCell(column = DetailVatTypeColumnSpec) {
                            Text(row.vatType, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        DokusTableCell(column = DetailOriginColumnSpec) {
                            SourceBadge(source = row.origin, compact = false)
                        }
                        DokusTableCell(column = DetailStatusColumnSpec) {
                            StatusWithDot(label = row.statusLabel, status = row.statusType)
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    subtitle: String,
    statusType: StatusDotType,
    modifier: Modifier = Modifier,
) {
    DokusCardSurface(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Constraints.Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (statusType == StatusDotType.Confirmed || statusType == StatusDotType.Warning || statusType == StatusDotType.Error) {
                StatusWithDot(
                    label = value,
                    status = statusType,
                    textStyle = MaterialTheme.typography.headlineSmall,
                    emphasize = true,
                )
            } else {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CompletenessRow(
    label: String,
    statusText: String,
    statusType: StatusDotType,
    showRequest: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        StatusWithDot(
            label = label,
            status = statusType,
            textStyle = MaterialTheme.typography.bodyLarge,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
        ) {
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = if (statusType == StatusDotType.Error) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            if (showRequest) {
                DokusCardSurface(accent = true) {
                    Text(
                        text = "Request",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun ConsoleFilterChip(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    DokusCardSurface(
        accent = selected,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DetailFilterPill(text: String) {
    DokusCardSurface {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RowScope.HeaderCell(
    resourceId: StringResource,
    spec: DokusTableColumnSpec
) {
    DokusTableCell(column = spec) {
        Text(
            text = stringResource(resourceId).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RowScope.HeaderCellRaw(
    text: String,
    spec: DokusTableColumnSpec
) {
    DokusTableCell(column = spec) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun StatusWithDot(
    label: String,
    status: StatusDotType,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    emphasize: Boolean = false,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        StatusDot(type = status)
        Text(
            text = label,
            style = textStyle,
            fontWeight = if (emphasize) FontWeight.SemiBold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RequestsBadge(count: Int) {
    if (count <= 0) {
        Text(
            text = "—",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    DokusCardSurface(accent = true) {
        Text(
            text = count.toString(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

private fun insightForClient(client: ConsoleClientSummary, index: Int): ConsoleClientInsight {
    val seed = (client.companyName.value.hashCode() + (index * 31)).absoluteValue
    val docsCount = 3 + (seed % 30)
    val hasPeppolIssues = seed % 5 == 0
    val hasGaps = seed % 4 == 0
    val isCritical = seed % 7 == 0
    val requestsCount = if (hasGaps || isCritical) 1 + (seed % 3) else 0
    val lastActivity = listOf("2 min ago", "1 hr ago", "3 hrs ago", "Yesterday", "2 days ago")
    return ConsoleClientInsight(
        peppolLabel = if (hasPeppolIssues) "Problem" else "OK",
        peppolStatus = if (hasPeppolIssues) StatusDotType.Error else StatusDotType.Confirmed,
        docsCount = docsCount,
        completenessLabel = when {
            isCritical -> "Critical"
            hasGaps -> "Gaps"
            else -> "Complete"
        },
        completenessStatus = when {
            isCritical -> StatusDotType.Error
            hasGaps -> StatusDotType.Warning
            else -> StatusDotType.Confirmed
        },
        requestsCount = requestsCount,
        lastActivityLabel = lastActivity[seed % lastActivity.size],
        cashflowLabel = when {
            isCritical -> "Critical"
            hasGaps -> "Attention"
            else -> "Healthy"
        },
        cashflowStatus = when {
            isCritical -> StatusDotType.Error
            hasGaps -> StatusDotType.Warning
            else -> StatusDotType.Confirmed
        },
        periodLabel = if (hasGaps) "Gaps" else "Complete",
        periodStatus = if (hasGaps) StatusDotType.Warning else StatusDotType.Confirmed,
        hasPeppolIssues = hasPeppolIssues,
        hasGaps = hasGaps,
        isCritical = isCritical,
    )
}

private fun DocumentRecordDto.toConsoleDocumentRow(index: Int): ConsoleDocumentRow {
    val counterparty = listOf(
        "CloudSoft NV",
        "DataStream BV",
        "Client ABC",
        "Office Supply NV",
        "Startup XY",
        "AWS EU",
        "Lunch meeting",
        "KBC",
        "Telenet NV",
    )
    val baseAmount = 45 + ((index + 1) * 89)
    val major = 100 + baseAmount
    val cents = ((index * 13) % 100).toString().padStart(2, '0')
    val status = draft?.documentStatus ?: DocumentStatus.NeedsReview
    return ConsoleDocumentRow(
        id = document.id.toString(),
        date = formatDate(document.uploadedAt),
        number = document.filename.substringBeforeLast('.').uppercase(),
        counterparty = counterparty[index % counterparty.size],
        amount = "€$major,$cents",
        vatType = when (index % 4) {
            0 -> "purchase"
            1 -> "sales"
            2 -> "receipt"
            else -> "bank"
        },
        origin = if (document.effectiveOrigin == tech.dokus.domain.enums.DocumentSource.Peppol) {
            BadgeDocumentSource.Peppol
        } else {
            BadgeDocumentSource.Pdf
        },
        statusLabel = statusDisplay(status),
        statusType = statusDot(status),
    )
}

private fun formatDate(dateTime: LocalDateTime): String {
    val month = when (dateTime.month) {
        Month.JANUARY -> "Jan"
        Month.FEBRUARY -> "Feb"
        Month.MARCH -> "Mar"
        Month.APRIL -> "Apr"
        Month.MAY -> "May"
        Month.JUNE -> "Jun"
        Month.JULY -> "Jul"
        Month.AUGUST -> "Aug"
        Month.SEPTEMBER -> "Sep"
        Month.OCTOBER -> "Oct"
        Month.NOVEMBER -> "Nov"
        Month.DECEMBER -> "Dec"
    }
    return "$month ${dateTime.day}"
}

private fun statusDisplay(status: DocumentStatus): String = when (status) {
    DocumentStatus.Confirmed -> "Confirmed"
    DocumentStatus.NeedsReview -> "Review"
    DocumentStatus.Rejected -> "Rejected"
}

private fun statusDot(status: DocumentStatus): StatusDotType = when (status) {
    DocumentStatus.Confirmed -> StatusDotType.Confirmed
    DocumentStatus.NeedsReview -> StatusDotType.Warning
    DocumentStatus.Rejected -> StatusDotType.Error
}

private fun formatEuro(value: Int): String {
    val grouped = value.toString().reversed().chunked(3).joinToString(".").reversed()
    return "$grouped,00"
}

@Preview
@Composable
private fun ConsoleClientsScreenLoadingPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        ConsoleClientsScreen(
            state = ConsoleClientsState(),
            snackbarHostState = SnackbarHostState(),
            onIntent = {},
        )
    }
}

@Preview(name = "Console Clients Desktop", widthDp = 1366, heightDp = 900)
@Composable
private fun ConsoleClientsScreenDesktopContentPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        ConsoleClientsScreen(
            state = ConsoleClientsState(
                firmId = FirmId("00000000-0000-0000-0000-000000000111"),
                firmName = "Kantoor Boonen",
                clients = DokusState.success(previewClients()),
            ),
            snackbarHostState = SnackbarHostState(),
            onIntent = {},
        )
    }
}

@Preview(name = "Console Client Detail Desktop", widthDp = 1366, heightDp = 900)
@Composable
private fun ConsoleClientDetailDesktopPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        ConsoleClientsScreen(
            state = ConsoleClientsState(
                firmId = FirmId("00000000-0000-0000-0000-000000000111"),
                firmName = "Kantoor Boonen",
                clients = DokusState.success(previewClients()),
                selectedClientTenantId = TenantId("00000000-0000-0000-0000-000000000001"),
                documentsState = DokusState.success(previewDocumentRecords()),
            ),
            snackbarHostState = SnackbarHostState(),
            onIntent = {},
        )
    }
}

@Preview
@Composable
private fun ConsoleClientsScreenErrorPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        ConsoleClientsScreen(
            state = ConsoleClientsState(
                clients = DokusState.error(
                    exception = DokusException.Validation.InvalidDisplayName,
                    retryHandler = {},
                ),
            ),
            snackbarHostState = SnackbarHostState(),
            onIntent = {},
        )
    }
}

private fun previewDocumentRecords(): List<DocumentRecordDto> = listOf(
    DocumentRecordDto(
        document = DocumentDto(
            id = tech.dokus.domain.ids.DocumentId("00000000-0000-0000-0000-000000000041"),
            tenantId = TenantId("00000000-0000-0000-0000-000000000001"),
            filename = "INV-2026-0188.pdf",
            contentType = "application/pdf",
            sizeBytes = 123_000,
            storageKey = "docs/1.pdf",
            source = tech.dokus.domain.enums.DocumentSource.Upload,
            uploadedAt = LocalDateTime(2026, 2, 14, 9, 0),
        ),
        draft = null,
        latestIngestion = null,
        confirmedEntity = null,
    ),
    DocumentRecordDto(
        document = DocumentDto(
            id = tech.dokus.domain.ids.DocumentId("00000000-0000-0000-0000-000000000042"),
            tenantId = TenantId("00000000-0000-0000-0000-000000000001"),
            filename = "PUR-2026-0042.pdf",
            contentType = "application/pdf",
            sizeBytes = 98_000,
            storageKey = "docs/2.pdf",
            source = tech.dokus.domain.enums.DocumentSource.Peppol,
            uploadedAt = LocalDateTime(2026, 2, 15, 11, 30),
        ),
        draft = null,
        latestIngestion = null,
        confirmedEntity = null,
    ),
)

private fun previewClients(): List<ConsoleClientSummary> {
    return listOf(
        ConsoleClientSummary(
            tenantId = TenantId("00000000-0000-0000-0000-000000000001"),
            companyName = DisplayName("Invoid BV"),
            vatNumber = VatNumber("BE0792140667"),
        ),
        ConsoleClientSummary(
            tenantId = TenantId("00000000-0000-0000-0000-000000000002"),
            companyName = DisplayName("PixelForge BV"),
            vatNumber = VatNumber("BE0456789123"),
        ),
        ConsoleClientSummary(
            tenantId = TenantId("00000000-0000-0000-0000-000000000003"),
            companyName = DisplayName("Atelier Gent"),
            vatNumber = VatNumber("BE0890123456"),
        ),
    )
}
