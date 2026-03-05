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
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.documents_subtitle
import tech.dokus.aura.resources.nav_documents
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.DocumentId
import tech.dokus.features.cashflow.mvi.AddDocumentContainer
import tech.dokus.features.cashflow.presentation.cashflow.components.fileDropTarget
import tech.dokus.features.cashflow.presentation.cashflow.components.rememberDocumentFilePicker
import tech.dokus.features.cashflow.presentation.cashflow.model.UploadStatus
import tech.dokus.features.cashflow.presentation.documents.components.DocumentsAddDocumentSheet
import tech.dokus.features.cashflow.presentation.documents.components.computeNeedsAttention
import tech.dokus.features.cashflow.presentation.documents.model.buildDocumentsLocalUploadRows
import tech.dokus.features.cashflow.presentation.documents.mvi.DocumentsAction
import tech.dokus.features.cashflow.presentation.documents.mvi.DocumentsContainer
import tech.dokus.features.cashflow.presentation.documents.mvi.DocumentsIntent
import tech.dokus.features.cashflow.presentation.documents.mvi.DocumentsState
import tech.dokus.features.cashflow.presentation.documents.screen.DocumentsScreen
import tech.dokus.features.cashflow.presentation.review.route.toRouteFilterToken
import tech.dokus.features.cashflow.usecases.GetDocumentRecordUseCase
import tech.dokus.foundation.app.mvi.container
import tech.dokus.foundation.app.network.ConnectionSnackbarEffect
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
    getDocumentRecord: GetDocumentRecordUseCase = koinInject(),
) {
    val navController = LocalNavController.current
    val backStackEntry by navController.currentBackStackEntryAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingError by remember { mutableStateOf<DokusException?>(null) }
    var isDraggingOverTable by remember { mutableStateOf(false) }
    var isAddDocumentSheetVisible by remember { mutableStateOf(false) }
    var knownNonAttentionDocumentIds by remember { mutableStateOf<Set<DocumentId>>(emptySet()) }

    val uploadManager = remember(uploadContainer) { uploadContainer.provideUploadManager() }
    val uploadTasks by uploadContainer.uploadTasks.collectAsState()
    val uploadedDocuments by uploadContainer.uploadedDocuments.collectAsState()

    val filePickerLauncher = rememberDocumentFilePicker { files ->
        if (files.isNotEmpty()) {
            uploadManager.enqueueFiles(files)
        }
    }

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
    val onUploadClick = remember(filePickerLauncher) {
        {
            filePickerLauncher.launch()
        }
    }

    val completedDocumentIds = remember(uploadTasks, uploadedDocuments) {
        uploadTasks.mapNotNull { task ->
            if (task.status == UploadStatus.COMPLETED) {
                task.documentId ?: uploadedDocuments[task.id]?.id
            } else {
                null
            }
        }.toSet()
    }

    LaunchedEffect(completedDocumentIds) {
        if (completedDocumentIds.isEmpty()) {
            knownNonAttentionDocumentIds = emptySet()
            return@LaunchedEffect
        }

        knownNonAttentionDocumentIds = knownNonAttentionDocumentIds.intersect(completedDocumentIds)
        val unresolvedIds = completedDocumentIds - knownNonAttentionDocumentIds
        if (unresolvedIds.isEmpty()) return@LaunchedEffect

        unresolvedIds.forEach { documentId ->
            getDocumentRecord(documentId)
                .getOrNull()
                ?.let { document ->
                    if (!computeNeedsAttention(document)) {
                        knownNonAttentionDocumentIds = knownNonAttentionDocumentIds + documentId
                    }
                }
        }
    }

    val contentState = state as? DocumentsState.Content
    val localUploadRows = remember(
        contentState?.filter,
        contentState?.documents?.data,
        uploadTasks,
        uploadedDocuments,
        knownNonAttentionDocumentIds
    ) {
        val cs = contentState ?: return@remember emptyList()

        buildDocumentsLocalUploadRows(
            filter = cs.filter,
            uploadTasks = uploadTasks,
            uploadedDocuments = uploadedDocuments,
            remoteDocuments = cs.documents.data,
            knownNonAttentionDocumentIds = knownNonAttentionDocumentIds
        )
    }

    // Keep list synchronized so local rows are replaced by server rows ASAP.
    LaunchedEffect(uploadedDocuments.size) {
        if (uploadedDocuments.isNotEmpty()) {
            onDocumentsChanged()
        }
    }

    val title = stringResource(Res.string.nav_documents)
    val subtitle = stringResource(Res.string.documents_subtitle)
    val topBarConfig = remember(title, subtitle) {
        HomeShellTopBarConfig(
            mode = HomeShellTopBarMode.Title(
                title = title,
                subtitle = subtitle
            ),
            actions = emptyList()
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
                    isDraggingOverTable = isDragging
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
            localUploadRows = localUploadRows,
            isDesktopDropTargetActive = isDraggingOverTable,
            snackbarHostState = snackbarHostState,
            onIntent = onIntent,
            onUploadClick = onUploadClick,
            onMobileFabClick = { isAddDocumentSheetVisible = true },
            onRetryLocalUpload = { taskId -> uploadManager.retryUpload(taskId) },
            onDismissLocalUpload = { taskId -> uploadManager.cancelUpload(taskId) }
        )

        DocumentsAddDocumentSheet(
            isVisible = isAddDocumentSheetVisible,
            onDismiss = { isAddDocumentSheetVisible = false },
            onUploadFile = {
                isAddDocumentSheetVisible = false
                filePickerLauncher.launch()
            }
        )
    }
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
