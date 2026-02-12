package tech.dokus.features.cashflow.presentation.documents.route

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.features.cashflow.mvi.AddDocumentContainer
import tech.dokus.features.cashflow.presentation.cashflow.components.AppDownloadQrDialog
import tech.dokus.features.cashflow.presentation.cashflow.components.DocumentUploadSidebar
import tech.dokus.features.cashflow.presentation.cashflow.components.fileDropTarget
import tech.dokus.features.cashflow.presentation.documents.mvi.DocumentsAction
import tech.dokus.features.cashflow.presentation.documents.mvi.DocumentsContainer
import tech.dokus.features.cashflow.presentation.documents.mvi.DocumentsIntent
import tech.dokus.features.cashflow.presentation.documents.screen.DocumentsScreen
import tech.dokus.foundation.app.mvi.container
import tech.dokus.foundation.app.network.ConnectionSnackbarEffect
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.navigation.destinations.CashFlowDestination
import tech.dokus.navigation.local.LocalNavController
import tech.dokus.navigation.navigateTo

@Composable
internal fun DocumentsRoute(
    documentsContainer: DocumentsContainer = container(),
    uploadContainer: AddDocumentContainer = container(),
) {
    val navController = LocalNavController.current
    val backStackEntry by navController.currentBackStackEntryAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingError by remember { mutableStateOf<DokusException?>(null) }
    var isUploadSidebarVisible by remember { mutableStateOf(false) }
    var isQrDialogVisible by remember { mutableStateOf(false) }

    val errorMessage = pendingError?.localized

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            pendingError = null
        }
    }

    val state by documentsContainer.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            is DocumentsAction.NavigateToDocumentReview -> {
                navController.navigateTo(CashFlowDestination.DocumentReview(action.documentId.toString()))
            }
            is DocumentsAction.ShowError -> {
                pendingError = action.error
            }
        }
    }

    // Upload sidebar state
    val uploadTasks by uploadContainer.uploadTasks.collectAsState()
    val uploadedDocuments by uploadContainer.uploadedDocuments.collectAsState()
    val deletionHandles by uploadContainer.deletionHandles.collectAsState()

    ConnectionSnackbarEffect(snackbarHostState)

    LaunchedEffect(Unit) {
        documentsContainer.store.intent(DocumentsIntent.Refresh)
    }

    val refreshRequired = backStackEntry
        ?.savedStateHandle
        ?.get<Boolean>(DOCUMENTS_REFRESH_REQUIRED_RESULT_KEY) == true

    LaunchedEffect(refreshRequired) {
        if (!refreshRequired) return@LaunchedEffect
        backStackEntry?.savedStateHandle?.remove<Boolean>(DOCUMENTS_REFRESH_REQUIRED_RESULT_KEY)
        documentsContainer.store.intent(DocumentsIntent.Refresh)
    }

    // Refresh documents when an upload completes
    LaunchedEffect(uploadedDocuments.size) {
        if (uploadedDocuments.isNotEmpty()) {
            documentsContainer.store.intent(DocumentsIntent.Refresh)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .fileDropTarget(
                onDragStateChange = { isDragging ->
                    if (isDragging) {
                        isUploadSidebarVisible = true
                    }
                },
                onFilesDropped = { files ->
                    if (files.isNotEmpty()) {
                        uploadContainer.provideUploadManager().enqueueFiles(files)
                    }
                }
            )
    ) {
        DocumentsScreen(
            state = state,
            snackbarHostState = snackbarHostState,
            onIntent = { documentsContainer.store.intent(it) },
            onUploadClick = { isUploadSidebarVisible = true }
        )

        // Upload sidebar overlay
        DocumentUploadSidebar(
            isVisible = isUploadSidebarVisible,
            onDismiss = { isUploadSidebarVisible = false },
            tasks = uploadTasks,
            documents = uploadedDocuments,
            deletionHandles = deletionHandles,
            uploadManager = uploadContainer.provideUploadManager(),
            onShowQrCode = { isQrDialogVisible = true }
        )
    }

    // QR code dialog for mobile app download
    AppDownloadQrDialog(
        isVisible = isQrDialogVisible,
        onDismiss = { isQrDialogVisible = false }
    )
}
