package ai.dokus.app.cashflow.screens

import ai.dokus.app.cashflow.components.AppDownloadQrDialog
import ai.dokus.app.cashflow.components.DocumentUploadSidebar
import ai.dokus.app.cashflow.components.FinancialDocumentTable
import ai.dokus.app.cashflow.components.PendingDocumentsCard
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
import ai.dokus.foundation.design.local.LocalScreenSize
import ai.dokus.foundation.domain.enums.InvoiceStatus
import ai.dokus.foundation.domain.model.FinancialDocumentDto
import ai.dokus.foundation.domain.model.MediaDto
import ai.dokus.foundation.domain.model.common.PaginationState
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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import org.koin.compose.viewmodel.koinViewModel

/**
 * The main cashflow screen showing financial documents table and VAT summary.
 * Responsive layout that adapts to mobile and desktop screen sizes.
 *
 * On desktop, clicking "Add new document" opens a sidebar for uploading.
 * On mobile, it navigates to the AddDocumentScreen.
 */
@Composable
internal fun CashflowScreen(
    viewModel: CashflowViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val navController = LocalNavController.current

    // Sidebar and dialog state
    val isSidebarOpen by viewModel.isSidebarOpen.collectAsState()
    val isQrDialogOpen by viewModel.isQrDialogOpen.collectAsState()
    val uploadTasks by viewModel.uploadTasks.collectAsState()
    val uploadedDocuments by viewModel.uploadedDocuments.collectAsState()
    val deletionHandles by viewModel.deletionHandles.collectAsState()

    // Pending documents state with local pagination
    val pendingDocumentsState by viewModel.pendingDocumentsState.collectAsState()
    val pendingCurrentPage by viewModel.pendingCurrentPage.collectAsState()

    // Use LocalScreenSize for reliable screen size detection
    val isLargeScreen = LocalScreenSize.current.isLarge

    LaunchedEffect(viewModel) {
        viewModel.refresh()
    }

    // Main content with sidebar overlay
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                PTopAppBarSearchAction(
                    searchContent = {
                        PSearchFieldCompact(
                            value = searchQuery,
                            onValueChange = viewModel::updateSearchQuery,
                            placeholder = "Search..."
                        )
                    },
                    actions = {
                        PButton(
                            text = "Add new document",
                            variant = PButtonVariant.Outline,
                            icon = Icons.Default.Add,
                            iconPosition = PIconPosition.Trailing,
                            onClick = {
                                if (isLargeScreen) {
                                    // Desktop: Open sidebar
                                    viewModel.openSidebar()
                                } else {
                                    // Mobile: Navigate to AddDocumentScreen
                                    navController.navigateTo(CashFlowDestination.AddDocument)
                                }
                            }
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
                    // Compute pending documents pagination with derivedStateOf for performance
                    val pendingPaginationData by remember(pendingDocumentsState, pendingCurrentPage) {
                        derivedStateOf {
                            val allDocs = (pendingDocumentsState as? DokusState.Success)?.data ?: emptyList()
                            val pageSize = PENDING_PAGE_SIZE
                            val totalPages = if (allDocs.isEmpty()) 1 else ((allDocs.size - 1) / pageSize) + 1
                            val start = pendingCurrentPage * pageSize
                            val end = minOf(start + pageSize, allDocs.size)
                            val currentDocs = if (start < allDocs.size) {
                                allDocs.subList(start, end)
                            } else {
                                emptyList()
                            }
                            PendingPaginationData(
                                documents = currentDocs,
                                isLoading = pendingDocumentsState is DokusState.Loading,
                                hasPreviousPage = pendingCurrentPage > 0,
                                hasNextPage = pendingCurrentPage < totalPages - 1
                            )
                        }
                    }

                    SuccessContent(
                        paginationState = currentState.data,
                        vatSummaryData = VatSummaryData.empty,
                        pendingDocuments = pendingPaginationData.documents,
                        isPendingLoading = pendingPaginationData.isLoading,
                        hasPendingPreviousPage = pendingPaginationData.hasPreviousPage,
                        hasPendingNextPage = pendingPaginationData.hasNextPage,
                        contentPadding = contentPadding,
                        onDocumentClick = { document ->
                            // TODO: Navigate to document detail
                        },
                        onMoreClick = { document ->
                            // TODO: Show context menu
                        },
                        onLoadMore = viewModel::loadNextPage,
                        onPendingDocumentClick = { media ->
                            // TODO: Navigate to document edit/confirmation screen
                        },
                        onPendingPreviousPage = viewModel::pendingDocumentsPreviousPage,
                        onPendingNextPage = viewModel::pendingDocumentsNextPage
                    )
                }

                is DokusState.Error -> {
                    DokusErrorContent(currentState.exception, currentState.retryHandler)
                }
            }
        }

        // Upload sidebar (desktop only)
        DocumentUploadSidebar(
            isVisible = isSidebarOpen,
            onDismiss = viewModel::closeSidebar,
            tasks = uploadTasks,
            documents = uploadedDocuments,
            deletionHandles = deletionHandles,
            uploadManager = viewModel.provideUploadManager(),
            onShowQrCode = viewModel::showQrDialog
        )

        // QR code dialog
        AppDownloadQrDialog(
            isVisible = isQrDialogOpen,
            onDismiss = viewModel::hideQrDialog
        )
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
    paginationState: PaginationState<FinancialDocumentDto>,
    vatSummaryData: VatSummaryData,
    pendingDocuments: List<MediaDto>,
    isPendingLoading: Boolean,
    hasPendingPreviousPage: Boolean,
    hasPendingNextPage: Boolean,
    contentPadding: PaddingValues,
    onDocumentClick: (FinancialDocumentDto) -> Unit,
    onMoreClick: (FinancialDocumentDto) -> Unit,
    onLoadMore: () -> Unit,
    onPendingDocumentClick: (MediaDto) -> Unit,
    onPendingPreviousPage: () -> Unit,
    onPendingNextPage: () -> Unit
) {
    // Use BoxWithConstraints to determine layout based on screen size
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        val isLargeScreen = maxWidth >= Breakpoints.LARGE.dp

        if (isLargeScreen) {
            // Desktop layout: Two columns with table on left, VAT summary + pending on right
            DesktopLayout(
                paginationState = paginationState,
                vatSummaryData = vatSummaryData,
                pendingDocuments = pendingDocuments,
                isPendingLoading = isPendingLoading,
                hasPendingPreviousPage = hasPendingPreviousPage,
                hasPendingNextPage = hasPendingNextPage,
                onDocumentClick = onDocumentClick,
                onMoreClick = onMoreClick,
                onLoadMore = onLoadMore,
                onPendingDocumentClick = onPendingDocumentClick,
                onPendingPreviousPage = onPendingPreviousPage,
                onPendingNextPage = onPendingNextPage
            )
        } else {
            // Mobile layout: Single column with scrollable content
            MobileLayout(
                paginationState = paginationState,
                vatSummaryData = vatSummaryData,
                pendingDocuments = pendingDocuments,
                isPendingLoading = isPendingLoading,
                hasPendingPreviousPage = hasPendingPreviousPage,
                hasPendingNextPage = hasPendingNextPage,
                onDocumentClick = onDocumentClick,
                onLoadMore = onLoadMore,
                onPendingDocumentClick = onPendingDocumentClick,
                onPendingPreviousPage = onPendingPreviousPage,
                onPendingNextPage = onPendingNextPage
            )
        }
    }
}

/**
 * Desktop layout with a two-column structure.
 * Left: Financial documents table
 * Right: VAT summary card + Pending documents card (sticky)
 */
@Composable
private fun DesktopLayout(
    paginationState: PaginationState<FinancialDocumentDto>,
    vatSummaryData: VatSummaryData,
    pendingDocuments: List<MediaDto>,
    isPendingLoading: Boolean,
    hasPendingPreviousPage: Boolean,
    hasPendingNextPage: Boolean,
    onDocumentClick: (FinancialDocumentDto) -> Unit,
    onMoreClick: (FinancialDocumentDto) -> Unit,
    onLoadMore: () -> Unit,
    onPendingDocumentClick: (MediaDto) -> Unit,
    onPendingPreviousPage: () -> Unit,
    onPendingNextPage: () -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(listState, paginationState.hasMorePages, paginationState.isLoadingMore) {
        snapshotFlow {
            val info = listState.layoutInfo
            (info.visibleItemsInfo.lastOrNull()?.index ?: 0) to info.totalItemsCount
        }
            .distinctUntilChanged()
            .filter { (last, total) ->
                (last + 1) > (total - 5) &&
                        paginationState.hasMorePages &&
                        !paginationState.isLoadingMore
            }
            .collect { onLoadMore() }
    }

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
            verticalArrangement = Arrangement.spacedBy(24.dp),
            state = listState
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
            val pendingDocuments = paginationState.data.needingConfirmation()
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
                if (paginationState.data.isEmpty()) {
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
                        documents = paginationState.data,
                        onDocumentClick = onDocumentClick,
                        onMoreClick = onMoreClick,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (paginationState.isLoadingMore) {
                item {
                    LoadingMoreItem()
                }
            }

            // Add some bottom padding
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Right column: VAT Summary Card + Pending Documents (fixed width)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            VatSummaryCard(
                vatAmount = vatSummaryData.vatAmount,
                netAmount = vatSummaryData.netAmount,
                predictedNetAmount = vatSummaryData.predictedNetAmount,
                quarterInfo = vatSummaryData.quarterInfo,
                modifier = Modifier.weight(2f)
            )

            // Pending documents card - always show (displays empty state when no documents)
            PendingDocumentsCard(
                documents = pendingDocuments,
                isLoading = isPendingLoading,
                hasPreviousPage = hasPendingPreviousPage,
                hasNextPage = hasPendingNextPage,
                onDocumentClick = onPendingDocumentClick,
                onPreviousClick = onPendingPreviousPage,
                onNextClick = onPendingNextPage,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Mobile layout with single-column scrollable content.
 * Stacks VAT summary and pending documents above the document table.
 */
@Composable
private fun MobileLayout(
    paginationState: PaginationState<FinancialDocumentDto>,
    vatSummaryData: VatSummaryData,
    pendingDocuments: List<MediaDto>,
    isPendingLoading: Boolean,
    hasPendingPreviousPage: Boolean,
    hasPendingNextPage: Boolean,
    onDocumentClick: (FinancialDocumentDto) -> Unit,
    onLoadMore: () -> Unit,
    onPendingDocumentClick: (MediaDto) -> Unit,
    onPendingPreviousPage: () -> Unit,
    onPendingNextPage: () -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(listState, paginationState.hasMorePages, paginationState.isLoadingMore) {
        snapshotFlow {
            val info = listState.layoutInfo
            (info.visibleItemsInfo.lastOrNull()?.index ?: 0) to info.totalItemsCount
        }
            .distinctUntilChanged()
            .filter { (last, total) ->
                (last + 1) > (total - 5) &&
                        paginationState.hasMorePages &&
                        !paginationState.isLoadingMore
            }
            .collect { onLoadMore() }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        state = listState
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

        // Pending documents card - always show (displays empty state when no documents)
        item {
            PendingDocumentsCard(
                documents = pendingDocuments,
                isLoading = isPendingLoading,
                hasPreviousPage = hasPendingPreviousPage,
                hasNextPage = hasPendingNextPage,
                onDocumentClick = onPendingDocumentClick,
                onPreviousClick = onPendingPreviousPage,
                onNextClick = onPendingNextPage,
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
        val pendingDocuments = paginationState.data.needingConfirmation()
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

        if (paginationState.data.isEmpty()) {
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
            items(paginationState.data) { document ->
                MobileDocumentCard(
                    document = document,
                    onClick = { onDocumentClick(document) }
                )
            }
        }

        if (paginationState.isLoadingMore) {
            item {
                LoadingMoreItem()
            }
        }

        // Add bottom padding for navigation bar
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LoadingMoreItem() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator()
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

/**
 * Number of pending documents to display per page.
 * Matches the ViewModel's page size for consistency.
 */
private const val PENDING_PAGE_SIZE = 5

/**
 * Data class holding computed pending documents pagination state.
 * Used with derivedStateOf for efficient recomposition.
 */
private data class PendingPaginationData(
    val documents: List<MediaDto>,
    val isLoading: Boolean,
    val hasPreviousPage: Boolean,
    val hasNextPage: Boolean
)
