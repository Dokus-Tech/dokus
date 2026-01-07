package tech.dokus.features.cashflow.presentation.cashflow.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.model.FinancialDocumentDto
import tech.dokus.domain.model.common.PaginationState
import tech.dokus.foundation.app.state.DokusState

// Desktop layout constants
private val DesktopContentPadding = 24.dp
private val DesktopItemSpacing = 24.dp

// Mobile layout constants
private val MobileContentPadding = 16.dp
private val MobileItemSpacing = 16.dp

// Infinite scroll trigger threshold
private const val InfiniteScrollThreshold = 5

/**
 * Desktop cashflow content with Figma-matching layout.
 * Shows summary cards (VAT, Pending Documents) + documents table.
 * Each section handles its own loading/error state independently.
 *
 * @param documentsState State for financial documents with pagination
 * @param vatSummaryState State for VAT summary card
 * @param pendingDocumentsState State for pending documents card
 * @param sortOption Currently selected sort option
 * @param contentPadding Scaffold content padding to apply
 * @param onSortOptionSelected Callback when sort option changes
 * @param onDocumentClick Callback when a document is clicked
 * @param onMoreClick Callback when document more button is clicked
 * @param onLoadMore Callback for infinite scroll pagination
 * @param onPendingDocumentClick Callback when pending document is clicked
 * @param onPendingLoadMore Callback for pending documents pagination
 */
@Composable
fun DesktopCashflowContent(
    documentsState: DokusState<PaginationState<FinancialDocumentDto>>,
    vatSummaryState: DokusState<VatSummaryData>,
    pendingDocumentsState: DokusState<PaginationState<DocumentRecordDto>>,
    sortOption: DocumentSortOption,
    contentPadding: PaddingValues,
    onSortOptionSelected: (DocumentSortOption) -> Unit,
    onDocumentClick: (FinancialDocumentDto) -> Unit,
    onMoreClick: (FinancialDocumentDto) -> Unit,
    onLoadMore: () -> Unit,
    onPendingDocumentClick: (DocumentRecordDto) -> Unit,
    onPendingLoadMore: () -> Unit,
    isOnline: Boolean = true
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
                (last + 1) > (total - InfiniteScrollThreshold) &&
                    paginationState.hasMorePages &&
                    !paginationState.isLoadingMore
            }
            .collect { onLoadMore() }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(DesktopContentPadding),
        verticalArrangement = Arrangement.spacedBy(DesktopItemSpacing),
        state = listState
    ) {
        // Top row: Summary cards (each handles its own loading state)
        item {
            CashflowSummarySection(
                vatSummaryState = vatSummaryState,
                pendingDocumentsState = pendingDocumentsState,
                onPendingDocumentClick = onPendingDocumentClick,
                onPendingLoadMore = onPendingLoadMore,
                isOnline = isOnline
            )
        }

        // Sort/filter controls
        item {
            CashflowFilters(
                selectedSortOption = sortOption,
                onSortOptionSelected = onSortOptionSelected
            )
        }

        // Documents table (handles its own loading/error state)
        item {
            CashflowDocumentsTableSection(
                state = documentsState,
                onDocumentClick = onDocumentClick,
                onMoreClick = onMoreClick
            )
        }

        // Bottom padding
        item {
            Spacer(modifier = Modifier.height(DesktopContentPadding))
        }
    }
}

/**
 * Mobile cashflow content showing only documents list.
 * No summary cards - those are displayed in the Dashboard on mobile.
 *
 * @param documentsState State for financial documents with pagination
 * @param sortOption Currently selected sort option
 * @param contentPadding Scaffold content padding to apply
 * @param onSortOptionSelected Callback when sort option changes
 * @param onDocumentClick Callback when a document is clicked
 * @param onLoadMore Callback for infinite scroll pagination
 */
@Composable
fun MobileCashflowContent(
    documentsState: DokusState<PaginationState<FinancialDocumentDto>>,
    sortOption: DocumentSortOption,
    contentPadding: PaddingValues,
    onSortOptionSelected: (DocumentSortOption) -> Unit,
    onDocumentClick: (FinancialDocumentDto) -> Unit,
    onLoadMore: () -> Unit
) {
    val listState = rememberLazyListState()

    // Extract pagination state for infinite scroll
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
                (last + 1) > (total - InfiniteScrollThreshold) &&
                    paginationState.hasMorePages &&
                    !paginationState.isLoadingMore
            }
            .collect { onLoadMore() }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(MobileContentPadding),
        verticalArrangement = Arrangement.spacedBy(MobileItemSpacing),
        state = listState
    ) {
        // Sort/filter controls (mobile layout)
        item {
            CashflowFiltersMobile(
                selectedSortOption = sortOption,
                onSortOptionSelected = onSortOptionSelected
            )
        }

        // Documents list section
        item {
            CashflowMobileDocumentsSection(
                state = documentsState,
                onDocumentClick = onDocumentClick
            )
        }

        // Bottom padding
        item {
            Spacer(modifier = Modifier.height(MobileContentPadding))
        }
    }
}
