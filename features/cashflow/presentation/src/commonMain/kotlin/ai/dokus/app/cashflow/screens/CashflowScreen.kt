package ai.dokus.app.cashflow.screens

import ai.dokus.app.cashflow.components.AppDownloadQrDialog
import ai.dokus.app.cashflow.components.CashflowHeaderActions
import ai.dokus.app.cashflow.components.CashflowHeaderSearch
import ai.dokus.app.cashflow.components.DesktopCashflowContent
import ai.dokus.app.cashflow.components.DocumentSortOption
import ai.dokus.app.cashflow.components.DocumentUploadSidebar
import ai.dokus.app.cashflow.components.DroppedFile
import ai.dokus.app.cashflow.components.FlyingDocument
import ai.dokus.app.cashflow.components.MobileCashflowContent
import ai.dokus.app.cashflow.components.SpaceUploadOverlay
import ai.dokus.app.cashflow.components.fileDropTarget
import ai.dokus.app.cashflow.components.isDragDropSupported
import ai.dokus.app.cashflow.viewmodel.CashflowAction
import ai.dokus.app.cashflow.viewmodel.CashflowContainer
import ai.dokus.app.cashflow.viewmodel.CashflowIntent
import ai.dokus.app.cashflow.viewmodel.CashflowSuccess
import ai.dokus.app.cashflow.viewmodel.CashflowState
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_invoice_create_success
import tech.dokus.foundation.aura.components.common.PTopAppBarSearchAction
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.aura.local.isLarge
import tech.dokus.navigation.destinations.CashFlowDestination
import tech.dokus.navigation.local.LocalNavController
import tech.dokus.navigation.navigateTo
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
import org.jetbrains.compose.resources.stringResource
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.foundation.app.mvi.container
import tech.dokus.foundation.app.network.ConnectionSnackbarEffect
import tech.dokus.foundation.app.network.rememberIsOnline
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.domain.exceptions.DokusException
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
    container: CashflowContainer = container(),
) {
    val navController = LocalNavController.current
    val isLargeScreen = LocalScreenSize.isLarge
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingSuccess by remember { mutableStateOf<CashflowSuccess?>(null) }
    var pendingError by remember { mutableStateOf<DokusException?>(null) }

    val successMessage = pendingSuccess?.let { success ->
        when (success) {
            CashflowSuccess.InvoiceCreated -> stringResource(Res.string.cashflow_invoice_create_success)
        }
    }
    val errorMessage = pendingError?.localized

    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            snackbarHostState.showSnackbar(successMessage)
            pendingSuccess = null
        }
    }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            pendingError = null
        }
    }

    // Subscribe to state and handle actions
    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            is CashflowAction.NavigateToDocument -> {
                // TODO: Navigate to document detail
            }
            is CashflowAction.NavigateToCreateInvoice -> {
                navController.navigateTo(CashFlowDestination.CreateInvoice)
            }
            is CashflowAction.NavigateToAddDocument -> {
                navController.navigateTo(CashFlowDestination.AddDocument)
            }
            is CashflowAction.NavigateToSettings -> {
                // TODO: Navigate to settings
            }
            is CashflowAction.ShowError -> {
                pendingError = action.error
            }
            is CashflowAction.ShowSuccess -> {
                pendingSuccess = action.success
            }
        }
    }

    // Upload manager flows (still exposed via container)
    val uploadTasks by container.uploadTasks.collectAsState()
    val uploadedDocuments by container.uploadedDocuments.collectAsState()
    val deletionHandles by container.deletionHandles.collectAsState()

    // Trigger initial data load
    LaunchedEffect(Unit) {
        container.store.intent(CashflowIntent.Refresh)
    }

    // Snackbar for connection status changes
    ConnectionSnackbarEffect(snackbarHostState)

    // Check connection status for offline UI
    val isOnline = rememberIsOnline()

    // Search expansion state for mobile
    var isSearchExpanded by rememberSaveable { mutableStateOf(isLargeScreen) }

    // Reset mobile search expansion when rotating to large screen (desktop)
    LaunchedEffect(isLargeScreen) {
        if (isLargeScreen) isSearchExpanded = false
    }

    // Space upload overlay state
    var isSpaceOverlayVisible by remember { mutableStateOf(false) }
    var isDraggingOverScreen by remember { mutableStateOf(false) }
    val flyingDocuments = remember { mutableStateListOf<FlyingDocument>() }
    var pendingDroppedFiles by remember { mutableStateOf<List<DroppedFile>>(emptyList()) }

    // Extract state values
    val contentState = state as? CashflowState.Content
    val searchQuery = contentState?.searchQuery ?: ""
    val sortOption = contentState?.sortOption ?: DocumentSortOption.Default
    val isSidebarOpen = contentState?.isSidebarOpen ?: false
    val isQrDialogOpen = contentState?.isQrDialogOpen ?: false
    val searchExpanded = isLargeScreen || isSearchExpanded

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
                            onSearchQueryChange = { container.store.intent(CashflowIntent.UpdateSearchQuery(it)) },
                            isSearchExpanded = searchExpanded,
                            isLargeScreen = isLargeScreen,
                            onExpandSearch = { isSearchExpanded = true }
                        )
                    },
                    actions = {
                        CashflowHeaderActions(
                            onUploadClick = {
                                if (isLargeScreen) {
                                    container.store.intent(CashflowIntent.OpenSidebar)
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
            when (state) {
                is CashflowState.Loading -> {
                    // Show loading state - use empty pagination states
                    if (isLargeScreen) {
                        DesktopCashflowContent(
                            documentsState = DokusState.loading(),
                            vatSummaryState = DokusState.loading(),
                            businessHealthState = DokusState.loading(),
                            pendingDocumentsState = DokusState.loading(),
                            sortOption = sortOption,
                            contentPadding = contentPadding,
                            onSortOptionSelected = { container.store.intent(CashflowIntent.UpdateSortOption(it)) },
                            onDocumentClick = { /* TODO: Navigate to document detail */ },
                            onMoreClick = { /* TODO: Show context menu */ },
                            onLoadMore = { container.store.intent(CashflowIntent.LoadMore) },
                            onPendingDocumentClick = { doc ->
                                navController.navigateTo(CashFlowDestination.DocumentReview(doc.document.id.toString()))
                            },
                            onPendingLoadMore = { container.store.intent(CashflowIntent.LoadMorePendingDocuments) },
                            isOnline = isOnline
                        )
                    } else {
                        MobileCashflowContent(
                            documentsState = DokusState.loading(),
                            sortOption = sortOption,
                            contentPadding = contentPadding,
                            onSortOptionSelected = { container.store.intent(CashflowIntent.UpdateSortOption(it)) },
                            onDocumentClick = { /* TODO: Navigate to document detail */ },
                            onLoadMore = { container.store.intent(CashflowIntent.LoadMore) }
                        )
                    }
                }

                is CashflowState.Content -> {
                    val content = state as CashflowState.Content
                    if (isLargeScreen) {
                        DesktopCashflowContent(
                            documentsState = DokusState.success(content.documents),
                            vatSummaryState = content.vatSummaryState,
                            businessHealthState = content.businessHealthState,
                            pendingDocumentsState = content.pendingDocumentsState,
                            sortOption = content.sortOption,
                            contentPadding = contentPadding,
                            onSortOptionSelected = { container.store.intent(CashflowIntent.UpdateSortOption(it)) },
                            onDocumentClick = { /* TODO: Navigate to document detail */ },
                            onMoreClick = { /* TODO: Show context menu */ },
                            onLoadMore = { container.store.intent(CashflowIntent.LoadMore) },
                            onPendingDocumentClick = { doc ->
                                navController.navigateTo(CashFlowDestination.DocumentReview(doc.document.id.toString()))
                            },
                            onPendingLoadMore = { container.store.intent(CashflowIntent.LoadMorePendingDocuments) },
                            isOnline = isOnline
                        )
                    } else {
                        MobileCashflowContent(
                            documentsState = DokusState.success(content.documents),
                            sortOption = content.sortOption,
                            contentPadding = contentPadding,
                            onSortOptionSelected = { container.store.intent(CashflowIntent.UpdateSortOption(it)) },
                            onDocumentClick = { /* TODO: Navigate to document detail */ },
                            onLoadMore = { container.store.intent(CashflowIntent.LoadMore) }
                        )
                    }
                }

                is CashflowState.Error -> {
                    val error = state as CashflowState.Error
                    // TODO: Show error state with retry
                    if (isLargeScreen) {
                        DesktopCashflowContent(
                            documentsState = DokusState.error(error.exception, error.retryHandler),
                            vatSummaryState = DokusState.error(error.exception, error.retryHandler),
                            businessHealthState = DokusState.error(error.exception, error.retryHandler),
                            pendingDocumentsState = DokusState.error(error.exception, error.retryHandler),
                            sortOption = sortOption,
                            contentPadding = contentPadding,
                            onSortOptionSelected = { container.store.intent(CashflowIntent.UpdateSortOption(it)) },
                            onDocumentClick = { /* TODO: Navigate to document detail */ },
                            onMoreClick = { /* TODO: Show context menu */ },
                            onLoadMore = { container.store.intent(CashflowIntent.LoadMore) },
                            onPendingDocumentClick = { doc ->
                                navController.navigateTo(CashFlowDestination.DocumentReview(doc.document.id.toString()))
                            },
                            onPendingLoadMore = { container.store.intent(CashflowIntent.LoadMorePendingDocuments) },
                            isOnline = isOnline
                        )
                    } else {
                        MobileCashflowContent(
                            documentsState = DokusState.error(error.exception, error.retryHandler),
                            sortOption = sortOption,
                            contentPadding = contentPadding,
                            onSortOptionSelected = { container.store.intent(CashflowIntent.UpdateSortOption(it)) },
                            onDocumentClick = { /* TODO: Navigate to document detail */ },
                            onLoadMore = { container.store.intent(CashflowIntent.LoadMore) }
                        )
                    }
                }
            }
        }

        // Upload sidebar (desktop only)
        DocumentUploadSidebar(
            isVisible = isSidebarOpen,
            onDismiss = { container.store.intent(CashflowIntent.CloseSidebar) },
            tasks = uploadTasks,
            documents = uploadedDocuments,
            deletionHandles = deletionHandles,
            uploadManager = container.provideUploadManager(),
            onShowQrCode = { container.store.intent(CashflowIntent.ShowQrDialog) }
        )

        // QR code dialog
        AppDownloadQrDialog(
            isVisible = isQrDialogOpen,
            onDismiss = { container.store.intent(CashflowIntent.HideQrDialog) }
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
                        container.provideUploadManager().enqueueFiles(pendingDroppedFiles)
                        pendingDroppedFiles = emptyList()
                    }

                    // Clear flying documents and hide overlay
                    flyingDocuments.clear()
                    isSpaceOverlayVisible = false

                    // Open sidebar to show uploaded files
                    container.store.intent(CashflowIntent.OpenSidebar)
                }
            )
        }
    }
}
