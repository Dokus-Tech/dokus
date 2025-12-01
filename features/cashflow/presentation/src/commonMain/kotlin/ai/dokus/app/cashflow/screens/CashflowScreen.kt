package ai.dokus.app.cashflow.screens

import ai.dokus.app.cashflow.components.FinancialDocumentTable
import ai.dokus.app.cashflow.components.VatSummaryCard
import ai.dokus.app.cashflow.components.VatSummaryData
import ai.dokus.app.cashflow.components.needingConfirmation
import ai.dokus.app.cashflow.viewmodel.CashflowViewModel
import ai.dokus.app.core.state.DokusState
import ai.dokus.foundation.design.components.CashflowType
import ai.dokus.foundation.design.components.CashflowTypeBadge
import ai.dokus.foundation.design.components.PButton
import ai.dokus.foundation.design.components.PButtonVariant
import ai.dokus.foundation.design.components.PIconPosition
import ai.dokus.foundation.design.components.common.Breakpoints
import ai.dokus.foundation.design.components.common.DokusErrorContent
import ai.dokus.foundation.design.components.common.PSearchFieldCompact
import ai.dokus.foundation.design.components.common.PTopAppBarSearchAction
import ai.dokus.foundation.domain.enums.InvoiceStatus
import ai.dokus.foundation.domain.model.FinancialDocumentDto
import ai.dokus.foundation.domain.model.PaginatedResponse
import ai.dokus.foundation.navigation.destinations.CashFlowDestination
import ai.dokus.foundation.navigation.local.LocalNavController
import ai.dokus.foundation.navigation.navigateTo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel

/**
 * The main cashflow screen showing financial documents table and VAT summary.
 * Responsive layout that adapts to mobile and desktop screen sizes.
 */
@Composable
internal fun CashflowScreen(
    viewModel: CashflowViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val navController = LocalNavController.current
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(viewModel) {
        viewModel.loadCashflowPage()
    }

    Scaffold(
        topBar = {
            PTopAppBarSearchAction(
                searchContent = {
                    PSearchFieldCompact(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = "Search..."
                    )
                },
                actions = {
                    PButton(
                        text = "Add new document",
                        variant = PButtonVariant.Outline,
                        icon = Icons.Default.Add,
                        iconPosition = PIconPosition.Trailing,
                        onClick = { navController.navigateTo(CashFlowDestination.AddDocument) }
                    )
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { contentPadding ->
        // Main content based on state
        when (val currentState = state) {
            is DokusState.Loading -> {
                LoadingContent(contentPadding)
            }

            is DokusState.Success -> {
                SuccessContent(
                    page = currentState.data,
                    vatSummaryData = VatSummaryData.empty,
                    contentPadding = contentPadding,
                    onDocumentClick = { document ->
                        // TODO: Navigate to document detail
                    },
                    onMoreClick = { document ->
                        // TODO: Show context menu
                    },
                    onNextPage = viewModel::loadNextPage,
                    onPreviousPage = viewModel::loadPreviousPage
                )
            }

            is DokusState.Error -> {
                DokusErrorContent(currentState.exception, currentState.retryHandler)
            }
        }
    }
}

/**
 * Loading state content with a centered progress indicator.
 */
@Composable
private fun LoadingContent(
    contentPadding: PaddingValues
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

/**
 * Success state content with responsive layout for financial documents and VAT summary.
 * Adapts layout based on screen width.
 */
@Composable
private fun SuccessContent(
    page: PaginatedResponse<FinancialDocumentDto>,
    vatSummaryData: VatSummaryData,
    contentPadding: PaddingValues,
    onDocumentClick: (FinancialDocumentDto) -> Unit,
    onMoreClick: (FinancialDocumentDto) -> Unit,
    onNextPage: () -> Unit,
    onPreviousPage: () -> Unit
) {
    // Use BoxWithConstraints to determine layout based on screen size
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        val isLargeScreen = maxWidth >= Breakpoints.LARGE.dp

        if (isLargeScreen) {
            // Desktop layout: Two columns with table on left, VAT summary on right
            DesktopLayout(
                page = page,
                vatSummaryData = vatSummaryData,
                onDocumentClick = onDocumentClick,
                onMoreClick = onMoreClick,
                onNextPage = onNextPage,
                onPreviousPage = onPreviousPage
            )
        } else {
            // Mobile layout: Single column with scrollable content
            MobileLayout(
                page = page,
                vatSummaryData = vatSummaryData,
                onDocumentClick = onDocumentClick,
                onMoreClick = onMoreClick,
                onNextPage = onNextPage,
                onPreviousPage = onPreviousPage
            )
        }
    }
}

/**
 * Desktop layout with a two-column structure.
 * Left: Financial documents table
 * Right: VAT summary card (sticky)
 */
@Composable
private fun DesktopLayout(
    page: PaginatedResponse<FinancialDocumentDto>,
    vatSummaryData: VatSummaryData,
    onDocumentClick: (FinancialDocumentDto) -> Unit,
    onMoreClick: (FinancialDocumentDto) -> Unit,
    onNextPage: () -> Unit,
    onPreviousPage: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Left column: Financial documents table
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Section title
            item {
                Text(
                    text = "Financial Documents",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Documents needing confirmation section
            val pendingDocuments = page.items.needingConfirmation()
            if (pendingDocuments.isNotEmpty()) {
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Needs Confirmation",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        FinancialDocumentTable(
                            documents = pendingDocuments,
                            onDocumentClick = onDocumentClick,
                            onMoreClick = onMoreClick,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // All documents section
            item {
                Text(
                    text = "All Documents",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            item {
                if (page.items.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No financial documents yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    FinancialDocumentTable(
                        documents = page.items,
                        onDocumentClick = onDocumentClick,
                        onMoreClick = onMoreClick,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                PaginationControls(
                    page = page,
                    onNextPage = onNextPage,
                    onPreviousPage = onPreviousPage
                )
            }

            // Add some bottom padding
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Right column: VAT Summary Card (fixed width)
        Box(
            modifier = Modifier.width(360.dp)
        ) {
            VatSummaryCard(
                vatAmount = vatSummaryData.vatAmount,
                netAmount = vatSummaryData.netAmount,
                predictedNetAmount = vatSummaryData.predictedNetAmount,
                quarterInfo = vatSummaryData.quarterInfo,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Mobile layout with single-column scrollable content.
 * Stacks VAT summary above the document table.
 */
@Composable
private fun MobileLayout(
    page: PaginatedResponse<FinancialDocumentDto>,
    vatSummaryData: VatSummaryData,
    onDocumentClick: (FinancialDocumentDto) -> Unit,
    onMoreClick: (FinancialDocumentDto) -> Unit,
    onNextPage: () -> Unit,
    onPreviousPage: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // VAT Summary Card at top
        item {
            VatSummaryCard(
                vatAmount = vatSummaryData.vatAmount,
                netAmount = vatSummaryData.netAmount,
                predictedNetAmount = vatSummaryData.predictedNetAmount,
                quarterInfo = vatSummaryData.quarterInfo,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Section title
        item {
            Text(
                text = "Financial Documents",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        // Documents needing confirmation section
        val pendingDocuments = page.items.needingConfirmation()
        if (pendingDocuments.isNotEmpty()) {
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Needs Confirmation",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // On mobile, show a more compact list view
                    MobileDocumentList(
                        documents = pendingDocuments,
                        onDocumentClick = onDocumentClick
                    )
                }
            }
        }

        // All documents section
        item {
            Text(
                text = "All Documents",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (page.items.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No financial documents yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(page.items) { document ->
                MobileDocumentCard(
                    document = document,
                    onClick = { onDocumentClick(document) }
                )
            }
        }

        item {
            PaginationControls(
                page = page,
                onNextPage = onNextPage,
                onPreviousPage = onPreviousPage
            )
        }

        // Add bottom padding for navigation bar
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PaginationControls(
    page: PaginatedResponse<FinancialDocumentDto>,
    onNextPage: () -> Unit,
    onPreviousPage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val start = if (page.total == 0L) 0 else page.offset + 1
    val end = if (page.total == 0L) 0 else minOf(page.offset + page.items.size, page.total.toInt())

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Showing $start-$end of ${page.total}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PButton(
                text = "Previous",
                variant = PButtonVariant.Outline,
                isEnabled = page.offset > 0,
                onClick = onPreviousPage
            )
            PButton(
                text = "Next",
                variant = PButtonVariant.Default,
                isEnabled = page.hasMore,
                onClick = onNextPage
            )
        }
    }
}

/**
 * Compact list view for mobile showing document items.
 */
@Composable
private fun MobileDocumentList(
    documents: List<FinancialDocumentDto>,
    onDocumentClick: (FinancialDocumentDto) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        documents.forEach { document ->
            MobileDocumentCard(
                document = document,
                onClick = { onDocumentClick(document) }
            )
        }
    }
}

/**
 * Compact card for mobile showing a single document.
 */
@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
@Composable
private fun MobileDocumentCard(
    document: FinancialDocumentDto,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side: Document info
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Alert indicator if needed
                    val needsAlert = when (document) {
                        is FinancialDocumentDto.InvoiceDto -> document.status == InvoiceStatus.Sent || document.status == InvoiceStatus.Overdue
                        is FinancialDocumentDto.ExpenseDto -> false
                        is FinancialDocumentDto.BillDto -> false
                    }
                    if (needsAlert) {
                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .height(6.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.error,
                                    shape = CircleShape
                                )
                        )
                    }

                    val documentNumber = when (document) {
                        is FinancialDocumentDto.InvoiceDto -> document.invoiceNumber.toString()
                        is FinancialDocumentDto.ExpenseDto -> "EXP-${document.id.value}"
                        is FinancialDocumentDto.BillDto -> document.invoiceNumber ?: "BILL-${document.id.value}"
                    }
                    Text(
                        text = documentNumber,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Contact/merchant name
                val contactName = when (document) {
                    is FinancialDocumentDto.InvoiceDto -> "Name Surname" // TODO: Get from client
                    is FinancialDocumentDto.ExpenseDto -> document.merchant
                    is FinancialDocumentDto.BillDto -> document.supplierName
                }
                Text(
                    text = contactName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Amount and date
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "€${document.amount.value}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = document.date.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Right side: Type badge
            CashflowTypeBadge(
                type = when (document) {
                    is FinancialDocumentDto.InvoiceDto -> CashflowType.CashIn
                    is FinancialDocumentDto.ExpenseDto -> CashflowType.CashOut
                    is FinancialDocumentDto.BillDto -> CashflowType.CashOut
                }
            )
        }
    }
}
