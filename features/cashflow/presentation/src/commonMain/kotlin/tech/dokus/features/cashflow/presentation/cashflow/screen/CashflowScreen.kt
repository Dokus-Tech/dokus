package tech.dokus.features.cashflow.presentation.cashflow.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tech.dokus.foundation.aura.components.navigation.UserPreferencesMenu
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.DocumentDto
import tech.dokus.features.cashflow.mvi.CashflowIntent
import tech.dokus.features.cashflow.mvi.CashflowState
import tech.dokus.features.cashflow.presentation.cashflow.components.AppDownloadQrDialog
import tech.dokus.features.cashflow.presentation.cashflow.components.CashflowHeaderActions
import tech.dokus.features.cashflow.presentation.cashflow.components.CashflowHeaderSearch
import tech.dokus.features.cashflow.presentation.cashflow.components.DesktopCashflowContent
import tech.dokus.features.cashflow.presentation.cashflow.components.DocumentSortOption
import tech.dokus.features.cashflow.presentation.cashflow.components.DocumentUploadSidebar
import tech.dokus.features.cashflow.presentation.cashflow.components.DroppedFile
import tech.dokus.features.cashflow.presentation.cashflow.components.FlyingDocument
import tech.dokus.features.cashflow.presentation.cashflow.components.MobileCashflowContent
import tech.dokus.features.cashflow.presentation.cashflow.components.SpaceUploadOverlay
import tech.dokus.features.cashflow.presentation.cashflow.components.fileDropTarget
import tech.dokus.features.cashflow.presentation.cashflow.components.isDragDropSupported
import tech.dokus.features.cashflow.presentation.cashflow.model.DocumentDeletionHandle
import tech.dokus.features.cashflow.presentation.cashflow.model.DocumentUploadTask
import tech.dokus.features.cashflow.presentation.cashflow.model.manager.DocumentUploadManager
import tech.dokus.foundation.app.network.rememberIsOnline
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.aura.components.common.PTopAppBarSearchAction
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.aura.local.isLarge
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
    state: CashflowState,
    uploadTasks: List<DocumentUploadTask>,
    uploadedDocuments: Map<String, DocumentDto>,
    deletionHandles: Map<String, DocumentDeletionHandle>,
    uploadManager: DocumentUploadManager,
    snackbarHostState: SnackbarHostState,
    onIntent: (CashflowIntent) -> Unit,
    onNavigateToAddDocument: () -> Unit,
    onNavigateToCreateInvoice: () -> Unit,
    onNavigateToDocumentReview: (DocumentId) -> Unit
) {
    val isLargeScreen = LocalScreenSize.isLarge
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
                            onSearchQueryChange = { onIntent(CashflowIntent.UpdateSearchQuery(it)) },
                            isSearchExpanded = searchExpanded,
                            isLargeScreen = isLargeScreen,
                            onExpandSearch = { isSearchExpanded = true }
                        )
                    },
                    actions = {
                        CashflowHeaderActions(
                            onUploadClick = {
                                if (isLargeScreen) {
                                    onIntent(CashflowIntent.OpenSidebar)
                                } else {
                                    onNavigateToAddDocument()
                                }
                            },
                            onCreateInvoiceClick = {
                                onNavigateToCreateInvoice()
                            }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        UserPreferencesMenu()
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
                            pendingDocumentsState = DokusState.loading(),
                            sortOption = sortOption,
                            contentPadding = contentPadding,
                            onSortOptionSelected = { onIntent(CashflowIntent.UpdateSortOption(it)) },
                            onDocumentClick = { /* TODO: Navigate to document detail */ },
                            onMoreClick = { /* TODO: Show context menu */ },
                            onLoadMore = { onIntent(CashflowIntent.LoadMore) },
                            onPendingDocumentClick = { doc ->
                                onNavigateToDocumentReview(doc.document.id)
                            },
                            onPendingLoadMore = { onIntent(CashflowIntent.LoadMorePendingDocuments) },
                            isOnline = isOnline
                        )
                    } else {
                        MobileCashflowContent(
                            documentsState = DokusState.loading(),
                            sortOption = sortOption,
                            contentPadding = contentPadding,
                            onSortOptionSelected = { onIntent(CashflowIntent.UpdateSortOption(it)) },
                            onDocumentClick = { /* TODO: Navigate to document detail */ },
                            onLoadMore = { onIntent(CashflowIntent.LoadMore) }
                        )
                    }
                }

                is CashflowState.Content -> {
                    val content = state as CashflowState.Content
                    if (isLargeScreen) {
                        DesktopCashflowContent(
                            documentsState = DokusState.success(content.documents),
                            vatSummaryState = content.vatSummaryState,
                            pendingDocumentsState = content.pendingDocumentsState,
                            sortOption = content.sortOption,
                            contentPadding = contentPadding,
                            onSortOptionSelected = { onIntent(CashflowIntent.UpdateSortOption(it)) },
                            onDocumentClick = { /* TODO: Navigate to document detail */ },
                            onMoreClick = { /* TODO: Show context menu */ },
                            onLoadMore = { onIntent(CashflowIntent.LoadMore) },
                            onPendingDocumentClick = { doc ->
                                onNavigateToDocumentReview(doc.document.id)
                            },
                            onPendingLoadMore = { onIntent(CashflowIntent.LoadMorePendingDocuments) },
                            isOnline = isOnline
                        )
                    } else {
                        MobileCashflowContent(
                            documentsState = DokusState.success(content.documents),
                            sortOption = content.sortOption,
                            contentPadding = contentPadding,
                            onSortOptionSelected = { onIntent(CashflowIntent.UpdateSortOption(it)) },
                            onDocumentClick = { /* TODO: Navigate to document detail */ },
                            onLoadMore = { onIntent(CashflowIntent.LoadMore) }
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
                            pendingDocumentsState = DokusState.error(error.exception, error.retryHandler),
                            sortOption = sortOption,
                            contentPadding = contentPadding,
                            onSortOptionSelected = { onIntent(CashflowIntent.UpdateSortOption(it)) },
                            onDocumentClick = { /* TODO: Navigate to document detail */ },
                            onMoreClick = { /* TODO: Show context menu */ },
                            onLoadMore = { onIntent(CashflowIntent.LoadMore) },
                            onPendingDocumentClick = { doc ->
                                onNavigateToDocumentReview(doc.document.id)
                            },
                            onPendingLoadMore = { onIntent(CashflowIntent.LoadMorePendingDocuments) },
                            isOnline = isOnline
                        )
                    } else {
                        MobileCashflowContent(
                            documentsState = DokusState.error(error.exception, error.retryHandler),
                            sortOption = sortOption,
                            contentPadding = contentPadding,
                            onSortOptionSelected = { onIntent(CashflowIntent.UpdateSortOption(it)) },
                            onDocumentClick = { /* TODO: Navigate to document detail */ },
                            onLoadMore = { onIntent(CashflowIntent.LoadMore) }
                        )
                    }
                }
            }
        }

        // Upload sidebar (desktop only)
        DocumentUploadSidebar(
            isVisible = isSidebarOpen,
            onDismiss = { onIntent(CashflowIntent.CloseSidebar) },
            tasks = uploadTasks,
            documents = uploadedDocuments,
            deletionHandles = deletionHandles,
            uploadManager = uploadManager,
            onShowQrCode = { onIntent(CashflowIntent.ShowQrDialog) }
        )

        // QR code dialog
        AppDownloadQrDialog(
            isVisible = isQrDialogOpen,
            onDismiss = { onIntent(CashflowIntent.HideQrDialog) }
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
                        uploadManager.enqueueFiles(pendingDroppedFiles)
                        pendingDroppedFiles = emptyList()
                    }

                    // Clear flying documents and hide overlay
                    flyingDocuments.clear()
                    isSpaceOverlayVisible = false

                    // Open sidebar to show uploaded files
                    onIntent(CashflowIntent.OpenSidebar)
                }
            )
        }
    }
}
