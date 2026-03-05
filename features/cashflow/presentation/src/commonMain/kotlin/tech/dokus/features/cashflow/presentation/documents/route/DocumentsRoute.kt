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
import tech.dokus.aura.resources.documents_back_to_clients
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
import tech.dokus.foundation.app.shell.LocalUserAccessContext
import tech.dokus.foundation.app.shell.RegisterHomeShellTopBar
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.navigation.destinations.CashFlowDestination
import tech.dokus.navigation.destinations.HomeDestination
import tech.dokus.navigation.local.LocalNavController
import tech.dokus.navigation.navigateTo
import tech.dokus.navigation.navigateToTopLevelTab

private const val HOME_ROUTE_DOCUMENTS = "documents"

@Composable
internal fun DocumentsRoute(
    documentsContainer: DocumentsContainer = container(),
    uploadContainer: AddDocumentContainer = container(),
) {
    val accessContext = LocalUserAccessContext.current
    val isAccountantReadOnly = accessContext.isBookkeeperConsoleDrillDown
    val showBackToClients = accessContext.isBookkeeperConsoleDrillDown
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
    val onUploadClick = remember(isAccountantReadOnly) {
        {
            if (!isAccountantReadOnly) {
                isUploadSidebarVisible = true
            }
        }
    }
    val onBackToClientsClick = remember(showBackToClients) {
        {
            if (showBackToClients) {
                navController.navigateToTopLevelTab(HomeDestination.ConsoleClients)
            }
        }
    }

    val title = stringResource(Res.string.nav_documents)
    val subtitle = stringResource(Res.string.documents_subtitle)
    val uploadContentDescription = stringResource(Res.string.documents_upload)
    val backToClientsLabel = stringResource(Res.string.documents_back_to_clients)
    val topBarConfig = remember(
        title,
        subtitle,
        uploadContentDescription,
        onUploadClick,
        backToClientsLabel,
        onBackToClientsClick,
        isAccountantReadOnly,
        showBackToClients,
    ) {
        val actions = buildList {
            if (!isAccountantReadOnly) {
                add(
                    HomeShellTopBarAction.Icon(
                        icon = Icons.Default.Upload,
                        contentDescription = uploadContentDescription,
                        onClick = onUploadClick,
                    )
                )
            }
            if (showBackToClients) {
                add(
                    HomeShellTopBarAction.Text(
                        label = backToClientsLabel,
                        onClick = onBackToClientsClick,
                    )
                )
            }
        }
        HomeShellTopBarConfig(
            mode = HomeShellTopBarMode.Title(
                title = title,
                subtitle = subtitle,
            ),
            actions = actions,
        )
    }
    RegisterHomeShellTopBar(
        route = HOME_ROUTE_DOCUMENTS,
        config = topBarConfig,
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

    val dropTargetModifier = if (!isAccountantReadOnly) {
        Modifier.fileDropTarget(
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
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(dropTargetModifier)
    ) {
        DocumentsScreen(
            state = state,
            snackbarHostState = snackbarHostState,
            onIntent = onIntent,
            onUploadClick = onUploadClick,
            isUploadEnabled = !isAccountantReadOnly,
            showBackToClients = showBackToClients,
            onBackToClientsClick = onBackToClientsClick,
        )

        if (!isAccountantReadOnly) {
            DocumentsUploadOverlay(
                uploadContainer = uploadContainer,
                isVisible = isUploadSidebarVisible,
                onDismiss = { isUploadSidebarVisible = false },
                uploadManager = uploadManager,
                onDocumentsChanged = onDocumentsChanged,
                onShowQrCode = { isQrDialogVisible = true },
            )
        }
    }

    if (!isAccountantReadOnly) {
        AppDownloadQrDialog(
            isVisible = isQrDialogVisible,
            onDismiss = { isQrDialogVisible = false },
        )
    }
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
        onShowQrCode = onShowQrCode,
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
