package ai.dokus.app.cashflow.screens

import ai.dokus.app.cashflow.components.AppDownloadQrDialog
import ai.dokus.app.cashflow.components.BusinessHealthCard
import ai.dokus.app.cashflow.components.BusinessHealthData
import ai.dokus.app.cashflow.components.DocumentSortOption
import ai.dokus.app.cashflow.components.DocumentUploadSidebar
import ai.dokus.app.cashflow.components.DroppedFile
import ai.dokus.app.cashflow.components.FinancialDocumentList
import ai.dokus.app.cashflow.components.FinancialDocumentTable
import ai.dokus.app.cashflow.components.FlyingDocument
import ai.dokus.app.cashflow.components.PendingDocumentsCard
import ai.dokus.app.cashflow.components.SortDropdown
import ai.dokus.app.cashflow.components.SpaceUploadOverlay
import ai.dokus.app.cashflow.components.VatSummaryCard
import ai.dokus.app.cashflow.components.VatSummaryData
import ai.dokus.app.cashflow.components.fileDropTarget
import ai.dokus.app.cashflow.components.isDragDropSupported
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.runtime.mutableStateListOf
import kotlin.random.Random
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Search
import compose.icons.feathericons.UploadCloud
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

    // Search expansion state for mobile
    var isSearchExpanded by rememberSaveable { mutableStateOf(isLargeScreen) }
    val searchExpanded = isLargeScreen || isSearchExpanded

    // Space upload overlay state
    var isSpaceOverlayVisible by remember { mutableStateOf(false) }
    var isDraggingOverScreen by remember { mutableStateOf(false) }
    val flyingDocuments = remember { mutableStateListOf<FlyingDocument>() }
    var pendingDroppedFiles by remember { mutableStateOf<List<DroppedFile>>(emptyList()) }

    LaunchedEffect(viewModel) {
        viewModel.refresh()
    }

    // Reset mobile search expansion when rotating to large screen (desktop)
    LaunchedEffect(isLargeScreen) {
        if (isLargeScreen) isSearchExpanded = false
    }

    // Screen-level drop target - shows space overlay when user drags files
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (isDragDropSupported && isLargeScreen) {
                    Modifier.fileDropTarget(
                        onDragStateChange = { dragging ->
                            isDraggingOverScreen = dragging
                            if (dragging) {
                                isSpaceOverlayVisible = true
                            } else if (flyingDocuments.isEmpty()) {
                                // Only hide if no flying documents (user cancelled drag)
                                isSpaceOverlayVisible = false
                            }
                        },
                        onFilesDropped = { files ->
                            if (files.isNotEmpty()) {
                                // Store files for later upload
                                pendingDroppedFiles = files

                                // Create flying documents from screen center
                                // (we'll calculate proper positions based on drop location)
                                flyingDocuments.clear()
                                val timestamp = kotlin.random.Random.nextLong()
                                files.forEachIndexed { index, file ->
                                    flyingDocuments.add(
                                        FlyingDocument(
                                            id = "${file.name}_${timestamp}_$index",
                                            name = file.name,
                                            startX = 0.5f, // Will be set relative to screen
                                            startY = 0.8f, // Start from bottom area
                                            targetAngle = Random.nextFloat() * 360f
                                        )
                                    )
                                }
                            }
                        }
                    )
                } else {
                    Modifier
                }
            )
    ) {
        Scaffold(
            topBar = {
                PTopAppBarSearchAction(
                    searchContent = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Show search icon button on mobile when search is collapsed
                            if (!isLargeScreen && !searchExpanded) {
                                IconButton(
                                    onClick = { isSearchExpanded = true },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = FeatherIcons.Search,
                                        contentDescription = "Search"
                                    )
                                }
                            }

                            // Animated search field
                            AnimatedVisibility(
                                visible = searchExpanded,
                                enter = expandHorizontally(expandFrom = Alignment.Start) + fadeIn(),
                                exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut()
                            ) {
                                PSearchFieldCompact(
                                    value = searchQuery,
                                    onValueChange = viewModel::updateSearchQuery,
                                    placeholder = "Search...",
                                    modifier = if (isLargeScreen) Modifier else Modifier.fillMaxWidth()
                                )
                            }
                        }
                    },
                    actions = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Upload icon button (secondary action - drag & drop is primary)
                            IconButton(
                                onClick = {
                                    if (isLargeScreen) {
                                        viewModel.openSidebar()
                                    } else {
                                        navController.navigateTo(CashFlowDestination.AddDocument)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = FeatherIcons.UploadCloud,
                                    contentDescription = "Upload document",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Create Invoice button (primary action)
                            PButton(
                                text = "Create Invoice",
                                variant = PButtonVariant.Outline,
                                icon = Icons.Default.Add,
                                iconPosition = PIconPosition.Trailing,
                                onClick = {
                                    navController.navigateTo(CashFlowDestination.CreateInvoice)
                                }
                            )
                        }
                    }
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { contentPadding ->
            if (isLargeScreen) {
                // Desktop layout with summary cards
                DesktopCashflowContent(
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
            } else {
                // Mobile layout - only documents list
                MobileCashflowContent(
                    documentsState = documentsState,
                    sortOption = sortOption,
                    contentPadding = contentPadding,
                    onSortOptionSelected = viewModel::updateSortOption,
                    onDocumentClick = { /* TODO: Navigate to document detail */ },
                    onLoadMore = viewModel::loadNextPage
                )
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

        // Space upload overlay (futuristic drag-and-drop effect)
        if (isDragDropSupported && isLargeScreen) {
            SpaceUploadOverlay(
                isVisible = isSpaceOverlayVisible,
                isDragging = isDraggingOverScreen,
                flyingDocuments = flyingDocuments.toList(),
                onAnimationComplete = {
                    // Upload the files after animation
                    if (pendingDroppedFiles.isNotEmpty()) {
                        viewModel.provideUploadManager().enqueueFiles(pendingDroppedFiles)
                        pendingDroppedFiles = emptyList()
                    }

                    // Clear flying documents and hide overlay
                    flyingDocuments.clear()
                    isSpaceOverlayVisible = false

                    // Open sidebar to show uploaded files
                    viewModel.openSidebar()
                }
            )
        }
    }
}

/**
 * Desktop cashflow content with Figma-matching layout.
 * Shows summary cards (VAT, Business Health, Pending Documents) + documents table.
 * Each section handles its own loading/error state independently.
 */
@Composable
private fun DesktopCashflowContent(
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
 * Mobile cashflow content showing only documents list.
 * No summary cards - those are displayed in the Dashboard on mobile.
 */
@Composable
private fun MobileCashflowContent(
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
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        state = listState
    ) {
        // Sort dropdown
        item {
            SortDropdown(
                selectedOption = sortOption,
                onOptionSelected = onSortOptionSelected
            )
        }

        // Documents list section
        item {
            MobileDocumentsSection(
                state = documentsState,
                onDocumentClick = onDocumentClick
            )
        }

        // Bottom padding
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Mobile documents list section with its own loading/error handling.
 */
@Composable
private fun MobileDocumentsSection(
    state: DokusState<PaginationState<FinancialDocumentDto>>,
    onDocumentClick: (FinancialDocumentDto) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when (state) {
            is DokusState.Loading, is DokusState.Idle -> {
                MobileDocumentsListSkeleton()
            }

            is DokusState.Success -> {
                val paginationState = state.data
                if (paginationState.data.isEmpty()) {
                    EmptyDocumentsState()
                } else {
                    FinancialDocumentList(
                        documents = paginationState.data,
                        onDocumentClick = onDocumentClick,
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
                        .padding(vertical = 32.dp),
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
 * Skeleton for mobile documents list during loading.
 */
@Composable
private fun MobileDocumentsListSkeleton() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(6) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShimmerLine(
                    modifier = Modifier.weight(1f),
                    height = 16.dp
                )
                Spacer(modifier = Modifier.width(16.dp))
                ShimmerLine(
                    modifier = Modifier.width(60.dp),
                    height = 16.dp
                )
                Spacer(modifier = Modifier.width(16.dp))
                ShimmerLine(
                    modifier = Modifier.width(70.dp),
                    height = 22.dp
                )
            }
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
