package ai.dokus.app.cashflow.screens

import ai.dokus.app.cashflow.components.AppDownloadQrDialog
import ai.dokus.app.cashflow.components.CashflowHeaderActions
import ai.dokus.app.cashflow.components.CashflowHeaderSearch
import ai.dokus.app.cashflow.components.DesktopCashflowContent
import ai.dokus.app.cashflow.components.DocumentUploadSidebar
import ai.dokus.app.cashflow.components.DroppedFile
import ai.dokus.app.cashflow.components.FlyingDocument
import ai.dokus.app.cashflow.components.MobileCashflowContent
import ai.dokus.app.cashflow.components.SpaceUploadOverlay
import ai.dokus.app.cashflow.components.fileDropTarget
import ai.dokus.app.cashflow.components.isDragDropSupported
import ai.dokus.app.cashflow.viewmodel.CashflowViewModel
import ai.dokus.foundation.design.components.common.PTopAppBarSearchAction
import ai.dokus.foundation.design.local.LocalScreenSize
import ai.dokus.foundation.navigation.destinations.CashFlowDestination
import ai.dokus.foundation.navigation.local.LocalNavController
import ai.dokus.foundation.navigation.navigateTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.koin.compose.viewmodel.koinViewModel
import tech.dokus.foundation.app.network.ConnectionSnackbarEffect
import kotlin.random.Random

/**
 * The main cashflow screen showing financial documents table with summary cards.
 *
 * Desktop layout matching Figma design:
 * - Top row: VAT Summary | Business Health | Pending Documents
 * - Sort dropdown
 * - Full-width documents table
 *
 * Mobile layout:
 * - Sort dropdown
 * - Documents list only (summary cards shown on Dashboard)
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

    // Snackbar for connection status changes
    val snackbarHostState = remember { SnackbarHostState() }
    ConnectionSnackbarEffect(snackbarHostState)

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
                                flyingDocuments.clear()
                                val timestamp = Random.nextLong()
                                files.forEachIndexed { index, file ->
                                    flyingDocuments.add(
                                        FlyingDocument(
                                            id = "${file.name}_${timestamp}_$index",
                                            name = file.name,
                                            startX = 0.5f,
                                            startY = 0.8f,
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
                        CashflowHeaderSearch(
                            searchQuery = searchQuery,
                            onSearchQueryChange = viewModel::updateSearchQuery,
                            isSearchExpanded = searchExpanded,
                            isLargeScreen = isLargeScreen,
                            onExpandSearch = { isSearchExpanded = true }
                        )
                    },
                    actions = {
                        CashflowHeaderActions(
                            onUploadClick = {
                                if (isLargeScreen) {
                                    viewModel.openSidebar()
                                } else {
                                    navController.navigateTo(CashFlowDestination.AddDocument)
                                }
                            },
                            onCreateInvoiceClick = {
                                navController.navigateTo(CashFlowDestination.CreateInvoice)
                            }
                        )
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background
        ) { contentPadding ->
            if (isLargeScreen) {
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
