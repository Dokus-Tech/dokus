package tech.dokus.features.cashflow.presentation.cashflow.screen

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_add_document
import tech.dokus.aura.resources.upload_documents_title
import tech.dokus.aura.resources.upload_instructions
import tech.dokus.aura.resources.upload_no_app_hint
import tech.dokus.aura.resources.upload_select_file
import tech.dokus.aura.resources.upload_uploads_title
import tech.dokus.aura.resources.upload_with_camera
import tech.dokus.domain.model.DocumentDto
import tech.dokus.features.cashflow.mvi.AddDocumentIntent
import tech.dokus.features.cashflow.mvi.AddDocumentState
import tech.dokus.features.cashflow.presentation.cashflow.components.AppDownloadQrDialog
import tech.dokus.features.cashflow.presentation.cashflow.components.DocumentUploadList
import tech.dokus.features.cashflow.presentation.cashflow.components.DocumentUploadZone
import tech.dokus.features.cashflow.presentation.cashflow.components.UploadIcon
import tech.dokus.features.cashflow.presentation.cashflow.model.DocumentDeletionHandle
import tech.dokus.features.cashflow.presentation.cashflow.model.DocumentUploadTask
import tech.dokus.features.cashflow.presentation.cashflow.model.manager.DocumentUploadManager
import tech.dokus.foundation.aura.components.common.PTopAppBar
import tech.dokus.foundation.aura.constrains.withContentPadding
import tech.dokus.foundation.aura.local.LocalScreenSize

private val DesktopContentPadding = 32.dp
private val MobileContentPadding = 16.dp
private val SectionSpacing = 24.dp
private val SmallSpacing = 8.dp
private val MediumSpacing = 12.dp
private const val DesktopContentWidthFraction = 0.5f

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
    state: AddDocumentState,
    uploadTasks: List<DocumentUploadTask>,
    uploadedDocuments: Map<String, DocumentDto>,
    deletionHandles: Map<String, DocumentDeletionHandle>,
    uploadManager: DocumentUploadManager,
    onIntent: (AddDocumentIntent) -> Unit
) {
    val layoutDirection = LocalLayoutDirection.current
    val isLarge = LocalScreenSize.current.isLarge

    var isQrDialogOpen by remember { mutableStateOf(false) }

    val isUploading = state is AddDocumentState.Uploading

    Scaffold { contentPadding ->
        Box(Modifier.withContentPadding(contentPadding, layoutDirection)) {
            if (isLarge) {
                DesktopLayout(
                    onUploadFile = { onIntent(AddDocumentIntent.SelectFile) },
                    isUploading = isUploading,
                    uploadTasks = uploadTasks,
                    uploadedDocuments = uploadedDocuments,
                    deletionHandles = deletionHandles,
                    uploadManager = uploadManager,
                    onShowQrCode = { isQrDialogOpen = true },
                    onIntent = onIntent
                )
            } else {
                // Mobile layout with upload zones and upload list
                MobileLayout(
                    onUploadFile = { onIntent(AddDocumentIntent.SelectFile) },
                    onUploadCamera = { /* TODO: Implement camera upload */ },
                    isUploading = isUploading,
                    uploadTasks = uploadTasks,
                    uploadedDocuments = uploadedDocuments,
                    deletionHandles = deletionHandles,
                    uploadManager = uploadManager,
                    onIntent = onIntent
                )
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
private fun DesktopLayout(
    onUploadFile: () -> Unit,
    isUploading: Boolean,
    uploadTasks: List<DocumentUploadTask>,
    uploadedDocuments: Map<String, DocumentDto>,
    deletionHandles: Map<String, DocumentDeletionHandle>,
    uploadManager: DocumentUploadManager,
    onShowQrCode: () -> Unit,
    onIntent: (AddDocumentIntent) -> Unit
) {
    var isDragging by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { PTopAppBar(stringResource(Res.string.cashflow_add_document)) },
        containerColor = MaterialTheme.colorScheme.background
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(DesktopContentPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(Res.string.upload_documents_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(SmallSpacing))

            Text(
                text = stringResource(Res.string.upload_instructions),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(SectionSpacing))

            DocumentUploadZone(
                isDragging = isDragging,
                onClick = onUploadFile,
                onDragStateChange = { isDragging = it },
                onFilesDropped = { onIntent(AddDocumentIntent.Upload(it)) },
                isUploading = isUploading,
                modifier = Modifier.fillMaxWidth(DesktopContentWidthFraction)
            )

            Spacer(modifier = Modifier.height(MediumSpacing))

            TextButton(onClick = onShowQrCode) {
                Text(
                    text = stringResource(Res.string.upload_no_app_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Uploads(
                tasks = uploadTasks,
                documents = uploadedDocuments,
                deletionHandles = deletionHandles,
                uploadManager = uploadManager,
                modifierText = Modifier.fillMaxWidth(DesktopContentWidthFraction),
                modifierList = Modifier.fillMaxWidth(DesktopContentWidthFraction)
            )

            Spacer(modifier = Modifier.height(SectionSpacing))
        }
    }
}

/**
 * Mobile layout with stacked upload zones and upload list.
 */
@Composable
private fun MobileLayout(
    onUploadFile: () -> Unit,
    onUploadCamera: () -> Unit,
    isUploading: Boolean,
    uploadTasks: List<DocumentUploadTask>,
    uploadedDocuments: Map<String, DocumentDto>,
    deletionHandles: Map<String, DocumentDeletionHandle>,
    uploadManager: DocumentUploadManager,
    onIntent: (AddDocumentIntent) -> Unit
) {
    var isCameraDragging by remember { mutableStateOf(false) }
    var isFileDragging by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { PTopAppBar(stringResource(Res.string.cashflow_add_document)) },
        containerColor = MaterialTheme.colorScheme.background
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(MobileContentPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(MobileContentPadding)
        ) {
            // Camera upload zone
            DocumentUploadZone(
                isDragging = isCameraDragging,
                onClick = onUploadCamera,
                onDragStateChange = { isCameraDragging = it },
                onFilesDropped = { onIntent(AddDocumentIntent.Upload(it)) },
                isUploading = isUploading,
                title = stringResource(Res.string.upload_with_camera),
                icon = UploadIcon.Camera,
                modifier = Modifier.fillMaxWidth()
            )

            // File upload zone
            DocumentUploadZone(
                isDragging = isFileDragging,
                onClick = onUploadFile,
                onDragStateChange = { isFileDragging = it },
                onFilesDropped = { onIntent(AddDocumentIntent.Upload(it)) },
                isUploading = isUploading,
                title = stringResource(Res.string.upload_select_file),
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

            Spacer(modifier = Modifier.height(SectionSpacing))
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
        Spacer(modifier = Modifier.height(SectionSpacing))

        Text(
            text = stringResource(Res.string.upload_uploads_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = modifierText
        )

        Spacer(modifier = Modifier.height(SmallSpacing))

        DocumentUploadList(
            tasks = tasks,
            documents = documents,
            deletionHandles = deletionHandles,
            uploadManager = uploadManager,
            modifier = modifierList
        )
    }
}
