package ai.dokus.app.cashflow.screens

import ai.dokus.app.cashflow.components.AppDownloadQrDialog
import ai.dokus.app.cashflow.components.DocumentUploadList
import ai.dokus.app.cashflow.components.DocumentUploadZone
import ai.dokus.app.cashflow.components.UploadIcon
import ai.dokus.app.cashflow.components.rememberDocumentFilePicker
import ai.dokus.app.cashflow.manager.DocumentUploadManager
import ai.dokus.app.cashflow.model.DocumentDeletionHandle
import ai.dokus.app.cashflow.model.DocumentUploadTask
import ai.dokus.app.cashflow.viewmodel.AddDocumentAction
import ai.dokus.app.cashflow.viewmodel.AddDocumentContainer
import ai.dokus.app.cashflow.viewmodel.AddDocumentIntent
import ai.dokus.app.cashflow.viewmodel.AddDocumentState
import ai.dokus.foundation.design.components.common.PTopAppBar
import ai.dokus.foundation.design.constrains.padding
import ai.dokus.foundation.design.local.LocalScreenSize
import ai.dokus.foundation.domain.model.DocumentDto
import ai.dokus.foundation.navigation.local.LocalNavController
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pro.respawn.flowmvi.api.IntentReceiver
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.foundation.app.mvi.container

/**
 * Add a document screen for uploading and processing documents/invoices.
 *
 * This screen is used on mobile devices. On desktop, the CashflowScreen
 * shows a sidebar instead of navigating to this screen.
 *
 * Features:
 * - Multiple upload zones (camera and file picker)
 * - Real-time upload progress tracking
 * - Upload list with cancel, retry, and delete actions
 * - QR code dialog for mobile app download
 */
@Composable
internal fun AddDocumentScreen(
    container: AddDocumentContainer = container()
) {
    val navController = LocalNavController.current
    val layoutDirection = LocalLayoutDirection.current
    val isLarge = LocalScreenSize.current.isLarge

    var isQrDialogOpen by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberDocumentFilePicker { files ->
        if (files.isNotEmpty()) container.store.intent(AddDocumentIntent.Upload(files))
    }

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            AddDocumentAction.LaunchFilePicker -> filePickerLauncher.launch()
            AddDocumentAction.NavigateBack -> navController.navigateUp()
        }
    }

    // Collect additional state flows from the container
    val uploadTasks by container.uploadTasks.collectAsState()
    val uploadedDocuments by container.uploadedDocuments.collectAsState()
    val deletionHandles by container.deletionHandles.collectAsState()

    val isUploading = state is AddDocumentState.Uploading

    Scaffold { contentPadding ->
        Box(Modifier.padding(contentPadding, layoutDirection)) {
            if (isLarge) {
                with(container.store) {
                    DesktopLayout(
                        onUploadFile = { intent(AddDocumentIntent.SelectFile) },
                        isUploading = isUploading,
                        uploadTasks = uploadTasks,
                        uploadedDocuments = uploadedDocuments,
                        deletionHandles = deletionHandles,
                        uploadManager = container.provideUploadManager(),
                        onShowQrCode = { isQrDialogOpen = true }
                    )
                }
            } else {
                // Mobile layout with upload zones and upload list
                with(container.store) {
                    MobileLayout(
                        onUploadFile = { intent(AddDocumentIntent.SelectFile) },
                        onUploadCamera = { /* TODO: Implement camera upload */ },
                        isUploading = isUploading,
                        uploadTasks = uploadTasks,
                        uploadedDocuments = uploadedDocuments,
                        deletionHandles = deletionHandles,
                        uploadManager = container.provideUploadManager()
                    )
                }
            }

            // QR code dialog
            AppDownloadQrDialog(
                isVisible = isQrDialogOpen,
                onDismiss = { isQrDialogOpen = false }
            )
        }
    }
}

/**
 * Desktop layout with upload zone and upload list.
 * Note: On desktop, the sidebar in CashflowScreen is the primary upload interface.
 * This screen is a fallback if navigated to directly.
 */
@Composable
private fun IntentReceiver<AddDocumentIntent>.DesktopLayout(
    onUploadFile: () -> Unit,
    isUploading: Boolean,
    uploadTasks: List<DocumentUploadTask>,
    uploadedDocuments: Map<String, DocumentDto>,
    deletionHandles: Map<String, DocumentDeletionHandle>,
    uploadManager: DocumentUploadManager,
    onShowQrCode: () -> Unit
) {
    var isDragging by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { PTopAppBar("Add a new document") },
        containerColor = MaterialTheme.colorScheme.background
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Upload Documents",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "To import an image or scan a document for your invoice, make sure the file is clear and in a compatible format.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            DocumentUploadZone(
                isDragging = isDragging,
                onClick = onUploadFile,
                onDragStateChange = { isDragging = it },
                onFilesDropped = { intent(AddDocumentIntent.Upload(it)) },
                isUploading = isUploading,
                modifier = Modifier.fillMaxWidth(0.5f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(onClick = onShowQrCode) {
                Text(
                    text = "Don't have the application? Click here",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Uploads(
                tasks = uploadTasks,
                documents = uploadedDocuments,
                deletionHandles = deletionHandles,
                uploadManager = uploadManager,
                modifierText = Modifier.fillMaxWidth(0.5f),
                modifierList = Modifier.fillMaxWidth(0.5f)
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Mobile layout with stacked upload zones and upload list.
 */
@Composable
private fun IntentReceiver<AddDocumentIntent>.MobileLayout(
    onUploadFile: () -> Unit,
    onUploadCamera: () -> Unit,
    isUploading: Boolean,
    uploadTasks: List<DocumentUploadTask>,
    uploadedDocuments: Map<String, DocumentDto>,
    deletionHandles: Map<String, DocumentDeletionHandle>,
    uploadManager: DocumentUploadManager
) {
    var isCameraDragging by remember { mutableStateOf(false) }
    var isFileDragging by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { PTopAppBar("Add a new document") },
        containerColor = MaterialTheme.colorScheme.background
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Camera upload zone
            DocumentUploadZone(
                isDragging = isCameraDragging,
                onClick = onUploadCamera,
                onDragStateChange = { isCameraDragging = it },
                onFilesDropped = { intent(AddDocumentIntent.Upload(it)) },
                isUploading = isUploading,
                title = "Upload with camera",
                icon = UploadIcon.Camera,
                modifier = Modifier.fillMaxWidth()
            )

            // File upload zone
            DocumentUploadZone(
                isDragging = isFileDragging,
                onClick = onUploadFile,
                onDragStateChange = { isFileDragging = it },
                onFilesDropped = { intent(AddDocumentIntent.Upload(it)) },
                isUploading = isUploading,
                title = "Select file",
                icon = UploadIcon.Document,
                modifier = Modifier.fillMaxWidth()
            )

            Uploads(
                tasks = uploadTasks,
                documents = uploadedDocuments,
                deletionHandles = deletionHandles,
                uploadManager = uploadManager,
                modifierList = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}


@Composable
private fun Uploads(
    tasks: List<DocumentUploadTask>,
    documents: Map<String, DocumentDto>,
    deletionHandles: Map<String, DocumentDeletionHandle>,
    uploadManager: DocumentUploadManager,
    modifierText: Modifier = Modifier,
    modifierList: Modifier = Modifier,
) {
    if (tasks.isNotEmpty()) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Uploads",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = modifierText
        )

        Spacer(modifier = Modifier.height(8.dp))

        DocumentUploadList(
            tasks = tasks,
            documents = documents,
            deletionHandles = deletionHandles,
            uploadManager = uploadManager,
            modifier = modifierList
        )
    }
}
