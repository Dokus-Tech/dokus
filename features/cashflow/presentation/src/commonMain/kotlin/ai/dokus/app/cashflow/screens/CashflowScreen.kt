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
import ai.dokus.foundation.design.components.common.ShimmerLine
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
    val documentsState by viewModel.state.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val navController = LocalNavController.current

    // Sidebar and dialog state
    val isSidebarOpen by viewModel.isSidebarOpen.collectAsState()
    val isQrDialogOpen by viewModel.isQrDialogOpen.collectAsState()
    val uploadTasks by viewModel.uploadTasks.collectAsState()
    val uploadedDocuments by viewModel.uploadedDocuments.collectAsState()
    val deletionHandles by viewModel.deletionHandles.collectAsState()

    // Individual section states (each loads independently)
    val vatSummaryState by viewModel.vatSummaryState.collectAsState()
    val businessHealthState by viewModel.businessHealthState.collectAsState()
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
            // Always show the screen structure - each section handles its own loading state
            CashflowContent(
                documentsState = documentsState,
                vatSummaryState = vatSummaryState,
                businessHealthState = businessHealthState,
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
 * Main cashflow content with Figma-matching layout.
 * Each section handles its own loading/error state independently.
 */
@Composable
private fun CashflowContent(
    documentsState: DokusState<PaginationState<FinancialDocumentDto>>,
    vatSummaryState: DokusState<VatSummaryData>,
    businessHealthState: DokusState<BusinessHealthData>,
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

    // Extract pagination state for infinite scroll (if available)
    val paginationState = (documentsState as? DokusState.Success)?.data

    // Infinite scroll trigger
    LaunchedEffect(listState, paginationState?.hasMorePages, paginationState?.isLoadingMore) {
        if (paginationState == null) return@LaunchedEffect
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
        // Top row: Summary cards (each handles its own loading state)
        item {
            SummaryCardsRow(
                vatSummaryState = vatSummaryState,
                businessHealthState = businessHealthState,
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

        // Documents table (handles its own loading/error state)
        item {
            DocumentsTableSection(
                state = documentsState,
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
 * Each card handles its own loading/error state independently.
 */
@Composable
private fun SummaryCardsRow(
    vatSummaryState: DokusState<VatSummaryData>,
    businessHealthState: DokusState<BusinessHealthData>,
    pendingDocumentsState: DokusState<PaginationState<DocumentProcessingDto>>,
    onPendingDocumentClick: (DocumentProcessingDto) -> Unit,
    onPendingLoadMore: () -> Unit
) {
    // Fixed height for the row - LazyColumn doesn't support intrinsic measurements
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Left column: VAT Summary + Business Health stacked vertically
        Column(
            modifier = Modifier
                .weight(3f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // VAT Summary Card at top (handles its own loading/error)
            VatSummaryCard(
                state = vatSummaryState,
                modifier = Modifier.fillMaxWidth()
            )

            // Business Health Card below - fills remaining space (handles its own loading/error)
            BusinessHealthCard(
                state = businessHealthState,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 120.dp)
                    .weight(1f)
            )
        }

        // Right side: Pending Documents Card (handles its own loading/error)
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
 * Documents table section with its own loading/error handling.
 */
@Composable
private fun DocumentsTableSection(
    state: DokusState<PaginationState<FinancialDocumentDto>>,
    onDocumentClick: (FinancialDocumentDto) -> Unit,
    onMoreClick: (FinancialDocumentDto) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when (state) {
            is DokusState.Loading, is DokusState.Idle -> {
                // Show loading skeleton for documents table
                DocumentsTableSkeleton()
            }

            is DokusState.Success -> {
                val paginationState = state.data
                if (paginationState.data.isEmpty()) {
                    EmptyDocumentsState()
                } else {
                    FinancialDocumentTable(
                        documents = paginationState.data,
                        onDocumentClick = onDocumentClick,
                        onMoreClick = onMoreClick,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (paginationState.isLoadingMore) {
                    LoadingMoreIndicator()
                }
            }

            is DokusState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    DokusErrorContent(
                        exception = state.exception,
                        retryHandler = state.retryHandler
                    )
                }
            }
        }
    }
}

/**
 * Skeleton for documents table during loading.
 */
@Composable
private fun DocumentsTableSkeleton() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Table header skeleton
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            repeat(5) {
                ShimmerLine(
                    modifier = Modifier.weight(1f),
                    height = 14.dp
                )
            }
        }

        // Table rows skeleton
        repeat(5) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                repeat(5) {
                    ShimmerLine(
                        modifier = Modifier.weight(1f),
                        height = 16.dp
                    )
                }
            }
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
