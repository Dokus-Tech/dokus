package tech.dokus.app.screens.accountant

import androidx.compose.foundation.clickable
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
import tech.dokus.aura.resources.console_clients_empty_all
import tech.dokus.aura.resources.console_clients_search_placeholder
import tech.dokus.domain.DisplayName
import tech.dokus.domain.asbtractions.RetryHandler
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.FirmId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.DocumentDto
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.model.auth.ConsoleClientSummary
import tech.dokus.foundation.app.state.DokusState
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
                    if (state.selectedClientTenantId == null) {
                        ClientsListContent(
                            state = state,
                            onIntent = onIntent,
                        )
                    } else {
                        ClientDocumentsContent(
                            state = state,
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
            val emptyText = if (state.clients.isEmpty()) {
                stringResource(Res.string.console_clients_empty_all)
            } else {
                stringResource(Res.string.console_clients_empty)
            }
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = emptyText,
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
                            DokusTableRow(
                                onClick = {
                                    onIntent(ConsoleClientsIntent.SelectClient(client.tenantId))
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
                    DokusCardSurface(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onIntent(ConsoleClientsIntent.SelectClient(client.tenantId)) },
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

@Composable
private fun ClientDocumentsContent(
    state: ConsoleClientsState.Content,
    onIntent: (ConsoleClientsIntent) -> Unit,
) {
    val selectedClient = state.clients.firstOrNull { it.tenantId == state.selectedClientTenantId }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.medium),
    ) {
        Text(
            text = "< Back to clients",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { onIntent(ConsoleClientsIntent.BackToClients) },
        )

        if (selectedClient != null) {
            Text(
                text = selectedClient.companyName.value,
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = selectedClient.vatNumber?.value ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        when (val documentsState = state.documentsState) {
            is DokusState.Loading -> DokusLoader()
            is DokusState.Error -> DokusErrorContent(
                exception = documentsState.exception,
                retryHandler = documentsState.retryHandler
            )
            is DokusState.Success -> {
                if (documentsState.data.isEmpty()) {
                    Text(
                        text = "No documents yet.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    DokusCardSurface(modifier = Modifier.fillMaxWidth()) {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(documentsState.data, key = { it.document.id.toString() }) { record ->
                                DokusTableRow(
                                    onClick = {
                                        onIntent(
                                            ConsoleClientsIntent.OpenDocument(
                                                record.document.id.toString()
                                            )
                                        )
                                    },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                        horizontal = Constraints.Spacing.medium,
                                        vertical = Constraints.Spacing.small,
                                    ),
                                ) {
                                    DokusTableCell(column = DokusTableColumnSpec(2f)) {
                                        Text(record.document.filename)
                                    }
                                    DokusTableCell(column = DokusTableColumnSpec(1f)) {
                                        Text(record.draft?.documentStatus?.name ?: "PENDING")
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            }
            else -> Unit
        }

        state.selectedDocument?.let { document ->
            DokusCardSurface(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Constraints.Spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = document.document.filename,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Status: ${document.draft?.documentStatus ?: DocumentStatus.NeedsReview}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "Type: ${document.draft?.documentType ?: "Unknown"}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun ConsoleClientsScreenLoadingPreview(
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

@Preview(name = "Console Clients Mobile", widthDp = 390, heightDp = 844)
@Composable
private fun ConsoleClientsScreenMobileContentPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        ConsoleClientsScreen(
            state = ConsoleClientsState.Content(
                firmId = FirmId("00000000-0000-0000-0000-000000000111"),
                clients = previewClients(),
            ),
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
            state = ConsoleClientsState.Content(
                firmId = FirmId("00000000-0000-0000-0000-000000000111"),
                clients = previewClients(),
            ),
            snackbarHostState = SnackbarHostState(),
            onIntent = {},
        )
    }
}

@Preview(name = "Console Clients Empty All Desktop", widthDp = 1366, heightDp = 900)
@Composable
private fun ConsoleClientsScreenDesktopEmptyAllPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        ConsoleClientsScreen(
            state = ConsoleClientsState.Content(
                firmId = FirmId("00000000-0000-0000-0000-000000000111"),
                clients = emptyList(),
            ),
            snackbarHostState = SnackbarHostState(),
            onIntent = {},
        )
    }
}

@Preview(name = "Console Clients Empty Filter Desktop", widthDp = 1366, heightDp = 900)
@Composable
private fun ConsoleClientsScreenDesktopEmptyFilterPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        ConsoleClientsScreen(
            state = ConsoleClientsState.Content(
                firmId = FirmId("00000000-0000-0000-0000-000000000111"),
                clients = previewClients(),
                query = "not-found-client",
            ),
            snackbarHostState = SnackbarHostState(),
            onIntent = {},
        )
    }
}

@Preview(name = "Console Client Documents Desktop", widthDp = 1366, heightDp = 900)
@Composable
private fun ConsoleClientDocumentsDesktopPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        ConsoleClientsScreen(
            state = ConsoleClientsState.Content(
                firmId = FirmId("00000000-0000-0000-0000-000000000111"),
                clients = previewClients(),
                selectedClientTenantId = TenantId("00000000-0000-0000-0000-000000000001"),
                documentsState = DokusState.success(previewDocumentRecords()),
            ),
            snackbarHostState = SnackbarHostState(),
            onIntent = {},
        )
    }
}

@Preview(name = "Console Client Documents Empty Desktop", widthDp = 1366, heightDp = 900)
@Composable
private fun ConsoleClientDocumentsEmptyDesktopPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        ConsoleClientsScreen(
            state = ConsoleClientsState.Content(
                firmId = FirmId("00000000-0000-0000-0000-000000000111"),
                clients = previewClients(),
                selectedClientTenantId = TenantId("00000000-0000-0000-0000-000000000001"),
                documentsState = DokusState.success(emptyList()),
            ),
            snackbarHostState = SnackbarHostState(),
            onIntent = {},
        )
    }
}

@Preview(name = "Console Client Documents Selected Desktop", widthDp = 1366, heightDp = 900)
@Composable
private fun ConsoleClientDocumentsSelectedDesktopPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    val documents = previewDocumentRecords()
    TestWrapper(parameters) {
        ConsoleClientsScreen(
            state = ConsoleClientsState.Content(
                firmId = FirmId("00000000-0000-0000-0000-000000000111"),
                clients = previewClients(),
                selectedClientTenantId = TenantId("00000000-0000-0000-0000-000000000001"),
                documentsState = DokusState.success(documents),
                selectedDocument = documents.first(),
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
            state = ConsoleClientsState.Error(
                exception = DokusException.Validation.InvalidDisplayName,
                retryHandler = RetryHandler {},
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
            uploadedAt = kotlinx.datetime.LocalDateTime(2026, 2, 14, 9, 0),
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
            source = tech.dokus.domain.enums.DocumentSource.Upload,
            uploadedAt = kotlinx.datetime.LocalDateTime(2026, 2, 15, 11, 30),
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
            vatNumber = null,
        ),
    )
}
