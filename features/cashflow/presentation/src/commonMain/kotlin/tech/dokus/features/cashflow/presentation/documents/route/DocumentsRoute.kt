package tech.dokus.features.cashflow.presentation.documents.route

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Upload
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
import org.jetbrains.compose.resources.stringResource
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.documents_subtitle
import tech.dokus.aura.resources.documents_upload
import tech.dokus.aura.resources.nav_documents
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.features.cashflow.mvi.AddDocumentContainer
import tech.dokus.features.cashflow.presentation.cashflow.components.AppDownloadQrDialog
import tech.dokus.features.cashflow.presentation.cashflow.components.DocumentUploadSidebar
import tech.dokus.features.cashflow.presentation.cashflow.components.fileDropTarget
import tech.dokus.features.cashflow.presentation.documents.mvi.DocumentsAction
import tech.dokus.features.cashflow.presentation.documents.mvi.DocumentsContainer
import tech.dokus.features.cashflow.presentation.documents.mvi.DocumentsIntent
import tech.dokus.features.cashflow.presentation.documents.mvi.DocumentsState
import tech.dokus.features.cashflow.presentation.documents.screen.DocumentsScreen
import tech.dokus.features.cashflow.presentation.review.route.toRouteFilterToken
import tech.dokus.foundation.app.mvi.container
import tech.dokus.foundation.app.network.ConnectionSnackbarEffect
import tech.dokus.foundation.app.shell.HomeShellTopBarAction
import tech.dokus.foundation.app.shell.HomeShellTopBarConfig
import tech.dokus.foundation.app.shell.HomeShellTopBarMode
import tech.dokus.foundation.app.shell.RegisterHomeShellTopBar
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.navigation.destinations.CashFlowDestination
import tech.dokus.navigation.local.LocalNavController
import tech.dokus.navigation.navigateTo

private const val HOME_ROUTE_DOCUMENTS = "documents"

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
    val uploadManager = remember(uploadContainer) { uploadContainer.provideUploadManager() }

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
                navController.navigateTo(toDocumentReviewDestination(action))
            }
            is DocumentsAction.ShowError -> {
                pendingError = action.error
            }
        }
    }
    val onIntent = remember(documentsContainer) {
        { intent: DocumentsIntent ->
            documentsContainer.store.intent(intent)
        }
    }
    val onDocumentsChanged = remember(documentsContainer) {
        {
            documentsContainer.store.intent(DocumentsIntent.ExternalDocumentsChanged)
        }
    }
    val onUploadClick = remember {
        {
            isUploadSidebarVisible = true
        }
    }

    val title = stringResource(Res.string.nav_documents)
    val subtitle = stringResource(Res.string.documents_subtitle)
    val uploadContentDescription = stringResource(Res.string.documents_upload)
    val topBarConfig = remember(
        title,
        subtitle,
        uploadContentDescription,
        onUploadClick
    ) {
        HomeShellTopBarConfig(
            mode = HomeShellTopBarMode.Title(
                title = title,
                subtitle = subtitle
            ),
            actions = listOf(
                HomeShellTopBarAction.Icon(
                    icon = Icons.Default.Upload,
                    contentDescription = uploadContentDescription,
                    onClick = onUploadClick
                )
            )
        )
    }
    RegisterHomeShellTopBar(
        route = HOME_ROUTE_DOCUMENTS,
        config = topBarConfig
    )

    ConnectionSnackbarEffect(snackbarHostState)

    val refreshRequired = backStackEntry
        ?.savedStateHandle
        ?.get<Boolean>(DOCUMENTS_REFRESH_REQUIRED_RESULT_KEY) == true

    LaunchedEffect(refreshRequired) {
        handleSavedStateDocumentsRefresh(
            refreshRequired = refreshRequired,
            clearRefreshResult = {
                backStackEntry?.savedStateHandle?.remove<Boolean>(DOCUMENTS_REFRESH_REQUIRED_RESULT_KEY)
            },
            onRefreshRequested = onDocumentsChanged,
        )
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
                        uploadManager.enqueueFiles(files)
                    }
                }
            )
    ) {
        DocumentsScreen(
            state = state,
            snackbarHostState = snackbarHostState,
            onIntent = onIntent,
            onUploadClick = onUploadClick
        )

        DocumentsUploadOverlay(
            uploadContainer = uploadContainer,
            isVisible = isUploadSidebarVisible,
            onDismiss = { isUploadSidebarVisible = false },
            uploadManager = uploadManager,
            onDocumentsChanged = onDocumentsChanged,
            onShowQrCode = { isQrDialogVisible = true },
        )
    }

    // QR code dialog for mobile app download
    AppDownloadQrDialog(
        isVisible = isQrDialogVisible,
        onDismiss = { isQrDialogVisible = false }
    )
}

@Composable
private fun DocumentsUploadOverlay(
    uploadContainer: AddDocumentContainer,
    isVisible: Boolean,
    uploadManager: tech.dokus.features.cashflow.presentation.cashflow.model.manager.DocumentUploadManager,
    onDismiss: () -> Unit,
    onDocumentsChanged: () -> Unit,
    onShowQrCode: () -> Unit,
) {
    val uploadTasks by uploadContainer.uploadTasks.collectAsState()
    val uploadedDocuments by uploadContainer.uploadedDocuments.collectAsState()
    val deletionHandles by uploadContainer.deletionHandles.collectAsState()

    LaunchedEffect(uploadedDocuments.size) {
        if (uploadedDocuments.isNotEmpty()) {
            onDocumentsChanged()
        }
    }

    DocumentUploadSidebar(
        isVisible = isVisible,
        onDismiss = onDismiss,
        tasks = uploadTasks,
        documents = uploadedDocuments,
        deletionHandles = deletionHandles,
        uploadManager = uploadManager,
        onShowQrCode = onShowQrCode
    )
}

internal fun toDocumentReviewDestination(
    action: DocumentsAction.NavigateToDocumentReview,
): CashFlowDestination.DocumentReview {
    return CashFlowDestination.DocumentReview(
        documentId = action.documentId.toString(),
        sourceFilter = action.sourceFilter.toRouteFilterToken(),
        sourceSort = action.sourceSort.token,
    )
}
