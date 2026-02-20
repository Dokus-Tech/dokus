package tech.dokus.features.cashflow.presentation.documents.route

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Upload
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import org.jetbrains.compose.resources.stringResource
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.documents_upload
import tech.dokus.aura.resources.search_placeholder
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
import tech.dokus.foundation.aura.local.LocalScreenSize
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
    val isLargeScreen = LocalScreenSize.current.isLarge
    var isSearchExpanded by rememberSaveable { mutableStateOf(isLargeScreen) }

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

    LaunchedEffect(isLargeScreen) {
        if (isLargeScreen) {
            isSearchExpanded = true
        }
    }

    val searchPlaceholder = stringResource(Res.string.search_placeholder)
    val uploadContentDescription = stringResource(Res.string.documents_upload)
    val searchQuery = (state as? DocumentsState.Content)?.searchQuery.orEmpty()
    val onSearchQueryChange = remember(documentsContainer) {
        { query: String ->
            documentsContainer.store.intent(DocumentsIntent.UpdateSearchQuery(query))
        }
    }
    val onClearSearchQuery = remember(documentsContainer) {
        {
            documentsContainer.store.intent(DocumentsIntent.UpdateSearchQuery(""))
        }
    }
    val onExpandSearch = remember {
        {
            isSearchExpanded = true
        }
    }
    val onUploadActionClick = remember {
        {
            isUploadSidebarVisible = true
        }
    }
    val topBarConfig = remember(
        searchQuery,
        searchPlaceholder,
        uploadContentDescription,
        isSearchExpanded,
        onSearchQueryChange,
        onClearSearchQuery,
        onExpandSearch,
        onUploadActionClick
    ) {
        HomeShellTopBarConfig(
            mode = HomeShellTopBarMode.Search(
                query = searchQuery,
                placeholder = searchPlaceholder,
                onQueryChange = onSearchQueryChange,
                onClear = onClearSearchQuery,
                isSearchExpanded = isSearchExpanded,
                onExpandSearch = onExpandSearch
            ),
            actions = listOf(
                HomeShellTopBarAction.Icon(
                    icon = Icons.Default.Upload,
                    contentDescription = uploadContentDescription,
                    onClick = onUploadActionClick
                )
            )
        )
    }
    RegisterHomeShellTopBar(
        route = HOME_ROUTE_DOCUMENTS,
        config = topBarConfig
    )

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

internal fun toDocumentReviewDestination(
    action: DocumentsAction.NavigateToDocumentReview,
): CashFlowDestination.DocumentReview {
    return CashFlowDestination.DocumentReview(
        documentId = action.documentId.toString(),
        sourceFilter = action.sourceFilter.toRouteFilterToken(),
        sourceSearch = action.sourceSearch,
        sourceSort = action.sourceSort.token,
    )
}
