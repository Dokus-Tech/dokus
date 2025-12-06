package ai.dokus.app.cashflow.screens

import ai.dokus.app.cashflow.components.AppDownloadQrDialog
import ai.dokus.app.cashflow.components.BusinessHealthCard
import ai.dokus.app.cashflow.components.BusinessHealthData
import ai.dokus.app.cashflow.components.DocumentSortOption
import ai.dokus.app.cashflow.components.DocumentUploadSidebar
import ai.dokus.app.cashflow.components.FinancialDocumentTable
import ai.dokus.app.cashflow.components.PendingDocumentsCard
import ai.dokus.app.cashflow.components.SortDropdown
import ai.dokus.app.cashflow.components.VatSummaryCard
import ai.dokus.app.cashflow.components.VatSummaryData
import ai.dokus.app.cashflow.viewmodel.CashflowViewModel
import ai.dokus.app.core.state.DokusState
import ai.dokus.foundation.design.components.PButton
import ai.dokus.foundation.design.components.PButtonVariant
import ai.dokus.foundation.design.components.PIconPosition
import ai.dokus.foundation.design.components.common.DokusErrorContent
import ai.dokus.foundation.design.components.common.PSearchFieldCompact
import ai.dokus.foundation.design.components.common.PTopAppBarSearchAction
import ai.dokus.foundation.design.local.LocalScreenSize
import ai.dokus.foundation.domain.model.DocumentProcessingDto
import ai.dokus.foundation.domain.model.FinancialDocumentDto
import ai.dokus.foundation.domain.model.common.PaginationState
import ai.dokus.foundation.navigation.destinations.CashFlowDestination
import ai.dokus.foundation.navigation.local.LocalNavController
import ai.dokus.foundation.navigation.navigateTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import org.koin.compose.viewmodel.koinViewModel

/**
 * The main cashflow screen showing financial documents table with summary cards.
 *
 * Desktop layout matching Figma design:
 * - Top row: VAT Summary | Business Health | Pending Documents
 * - Sort dropdown
 * - Full-width documents table
 */
@Composable
internal fun CashflowScreen(
    viewModel: CashflowViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val navController = LocalNavController.current

    // Sidebar and dialog state
    val isSidebarOpen by viewModel.isSidebarOpen.collectAsState()
    val isQrDialogOpen by viewModel.isQrDialogOpen.collectAsState()
    val uploadTasks by viewModel.uploadTasks.collectAsState()
    val uploadedDocuments by viewModel.uploadedDocuments.collectAsState()
    val deletionHandles by viewModel.deletionHandles.collectAsState()

    // Pending documents state (includes loading, success with pagination, and error)
    val pendingDocumentsState by viewModel.pendingDocumentsState.collectAsState()

    val isLargeScreen = LocalScreenSize.current.isLarge

    LaunchedEffect(viewModel) {
        viewModel.refresh()
    }

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
                                    viewModel.openSidebar()
                                } else {
                                    navController.navigateTo(CashFlowDestination.AddDocument)
                                }
                            }
                        )
                    }
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { contentPadding ->
            when (val currentState = state) {
                is DokusState.Loading -> {
                    CashflowLoadingContent(contentPadding)
                }

                is DokusState.Success -> {
                    CashflowContent(
                        paginationState = currentState.data,
                        vatSummaryData = VatSummaryData.empty,
                        businessHealthData = BusinessHealthData.empty,
                        pendingDocumentsState = pendingDocumentsState,
                        sortOption = sortOption,
                        contentPadding = contentPadding,
                        onSortOptionSelected = viewModel::updateSortOption,
                        onDocumentClick = { /* TODO: Navigate to document detail */ },
                        onMoreClick = { /* TODO: Show context menu */ },
                        onLoadMore = viewModel::loadNextPage,
                        onPendingDocumentClick = { /* TODO: Navigate to document edit */ },
                        onPendingLoadMore = viewModel::pendingDocumentsLoadMore
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
 * Loading state content.
 */
@Composable
private fun CashflowLoadingContent(
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
 * Main cashflow content with Figma-matching layout.
 */
@Composable
private fun CashflowContent(
    paginationState: PaginationState<FinancialDocumentDto>,
    vatSummaryData: VatSummaryData,
    businessHealthData: BusinessHealthData,
    pendingDocumentsState: DokusState<PaginationState<DocumentProcessingDto>>,
    sortOption: DocumentSortOption,
    contentPadding: PaddingValues,
    onSortOptionSelected: (DocumentSortOption) -> Unit,
    onDocumentClick: (FinancialDocumentDto) -> Unit,
    onMoreClick: (FinancialDocumentDto) -> Unit,
    onLoadMore: () -> Unit,
    onPendingDocumentClick: (DocumentProcessingDto) -> Unit,
    onPendingLoadMore: () -> Unit
) {
    val listState = rememberLazyListState()

    // Infinite scroll trigger
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
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        state = listState
    ) {
        // Top row: Summary cards
        item {
            SummaryCardsRow(
                vatSummaryData = vatSummaryData,
                businessHealthData = businessHealthData,
                pendingDocumentsState = pendingDocumentsState,
                onPendingDocumentClick = onPendingDocumentClick,
                onPendingLoadMore = onPendingLoadMore
            )
        }

        // Sort dropdown row
        item {
            SortDropdown(
                selectedOption = sortOption,
                onOptionSelected = onSortOptionSelected
            )
        }

        // Documents table
        item {
            DocumentsSection(
                documents = paginationState.data,
                isLoadingMore = paginationState.isLoadingMore,
                onDocumentClick = onDocumentClick,
                onMoreClick = onMoreClick
            )
        }

        // Bottom padding
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Top row with summary cards matching Figma layout:
 * Left column: VAT Summary (top) + Business Health (bottom)
 * Right side: Cash flow (pending documents) card
 * Both columns have matching heights.
 */
@Composable
private fun SummaryCardsRow(
    vatSummaryData: VatSummaryData,
    businessHealthData: BusinessHealthData,
    pendingDocumentsState: DokusState<PaginationState<DocumentProcessingDto>>,
    onPendingDocumentClick: (DocumentProcessingDto) -> Unit,
    onPendingLoadMore: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Left column: VAT Summary + Business Health stacked vertically
        Column(
            modifier = Modifier
                .weight(3f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // VAT Summary Card at top
            VatSummaryCard(
                vatAmount = vatSummaryData.vatAmount,
                netAmount = vatSummaryData.netAmount,
                predictedNetAmount = vatSummaryData.predictedNetAmount,
                quarterInfo = vatSummaryData.quarterInfo,
                modifier = Modifier.fillMaxWidth()
            )

            // Business Health Card below - fills remaining space
            // defaultMinSize ensures it contributes to IntrinsicSize calculation
            // while weight(1f) allows it to expand to fill available space
            BusinessHealthCard(
                data = businessHealthData,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 120.dp)
                    .weight(1f)
            )
        }

        // Right side: Pending Documents Card - determines the row height
        PendingDocumentsCard(
            state = pendingDocumentsState,
            onDocumentClick = onPendingDocumentClick,
            onLoadMore = onPendingLoadMore,
            modifier = Modifier
                .weight(2f)
                .fillMaxHeight()
        )
    }
}

/**
 * Documents section with table and empty state.
 */
@Composable
private fun DocumentsSection(
    documents: List<FinancialDocumentDto>,
    isLoadingMore: Boolean,
    onDocumentClick: (FinancialDocumentDto) -> Unit,
    onMoreClick: (FinancialDocumentDto) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (documents.isEmpty()) {
            EmptyDocumentsState()
        } else {
            FinancialDocumentTable(
                documents = documents,
                onDocumentClick = onDocumentClick,
                onMoreClick = onMoreClick,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (isLoadingMore) {
            LoadingMoreIndicator()
        }
    }
}

/**
 * Empty state when no documents exist.
 */
@Composable
private fun EmptyDocumentsState() {
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

/**
 * Loading indicator for infinite scroll.
 */
@Composable
private fun LoadingMoreIndicator() {
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
