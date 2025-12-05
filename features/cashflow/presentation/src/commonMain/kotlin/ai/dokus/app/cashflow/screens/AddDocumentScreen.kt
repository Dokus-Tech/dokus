package ai.dokus.app.cashflow.screens

import ai.dokus.app.cashflow.components.AppDownloadQrDialog
import ai.dokus.app.cashflow.components.DocumentUploadList
import ai.dokus.app.cashflow.components.DocumentUploadZone
import ai.dokus.app.cashflow.components.DroppedFile
import ai.dokus.app.cashflow.components.UploadIcon
import ai.dokus.app.cashflow.viewmodel.AddDocumentViewModel
import ai.dokus.foundation.design.components.common.PTopAppBar
import ai.dokus.foundation.design.local.LocalScreenSize
import ai.dokus.foundation.navigation.local.LocalNavController
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.calf.core.LocalPlatformContext
import com.mohamedrejeb.calf.io.getName
import com.mohamedrejeb.calf.io.readByteArray
import com.mohamedrejeb.calf.picker.FilePickerFileType
import com.mohamedrejeb.calf.picker.FilePickerSelectionMode
import com.mohamedrejeb.calf.picker.rememberFilePickerLauncher
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

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
    viewModel: AddDocumentViewModel = koinViewModel()
) {
    val navController = LocalNavController.current
    val state by viewModel.state.collectAsState()
    val uploadTasks by viewModel.uploadTasks.collectAsState()
    val uploadedDocuments by viewModel.uploadedDocuments.collectAsState()
    val deletionHandles by viewModel.deletionHandles.collectAsState()

    val platformContext = LocalPlatformContext.current
    val isLarge = LocalScreenSize.current.isLarge

    var isQrDialogOpen by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val filePickerLauncher = rememberFilePickerLauncher(
        type = FilePickerFileType.Document,
        selectionMode = FilePickerSelectionMode.Multiple
    ) { files ->
        scope.launch {
            val dropped = files.mapNotNull { file ->
                val bytes = runCatching { file.readByteArray(platformContext) }.getOrNull()
                val name = file.getName(platformContext) ?: return@mapNotNull null
                bytes?.let { DroppedFile(name = name, bytes = it, mimeType = null) }
            }
            viewModel.uploadFiles(dropped)
        }
    }

    Scaffold { contentPadding ->
        Box(
            modifier = Modifier.padding(
                start = contentPadding.calculateStartPadding(LocalLayoutDirection.current),
                end = contentPadding.calculateEndPadding(LocalLayoutDirection.current),
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding()
            )
        )

        if (isLarge) {
            // Desktop layout - simplified (sidebar handles most functionality now)
            DesktopLayout(
                onUploadFile = { filePickerLauncher.launch() },
                isUploading = state.isUploading,
                uploadTasks = uploadTasks,
                uploadedDocuments = uploadedDocuments,
                deletionHandles = deletionHandles,
                viewModel = viewModel,
                onShowQrCode = { isQrDialogOpen = true }
            )
        } else {
            // Mobile layout with upload zones and upload list
            MobileLayout(
                onUploadFile = { filePickerLauncher.launch() },
                onUploadCamera = { /* TODO: Implement camera upload */ },
                isUploading = state.isUploading,
                uploadTasks = uploadTasks,
                uploadedDocuments = uploadedDocuments,
                deletionHandles = deletionHandles,
                viewModel = viewModel,
                onShowQrCode = { isQrDialogOpen = true }
            )
        }

        // QR code dialog
        AppDownloadQrDialog(
            isVisible = isQrDialogOpen,
            onDismiss = { isQrDialogOpen = false }
        )
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
    uploadTasks: List<ai.dokus.app.cashflow.model.DocumentUploadTask>,
    uploadedDocuments: Map<String, ai.dokus.foundation.domain.model.DocumentDto>,
    deletionHandles: Map<String, ai.dokus.app.cashflow.model.DocumentDeletionHandle>,
    viewModel: AddDocumentViewModel,
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
                onFilesDropped = { viewModel.uploadFiles(it) },
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

            if (uploadTasks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Uploads",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(0.5f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                DocumentUploadList(
                    tasks = uploadTasks,
                    documents = uploadedDocuments,
                    deletionHandles = deletionHandles,
                    uploadManager = viewModel.provideUploadManager(),
                    modifier = Modifier.fillMaxWidth(0.5f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
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
    uploadTasks: List<ai.dokus.app.cashflow.model.DocumentUploadTask>,
    uploadedDocuments: Map<String, ai.dokus.foundation.domain.model.DocumentDto>,
    deletionHandles: Map<String, ai.dokus.app.cashflow.model.DocumentDeletionHandle>,
    viewModel: AddDocumentViewModel,
    onShowQrCode: () -> Unit
) {
    Scaffold(
        topBar = {
            PTopAppBar(
                title = "Add a new document",
            )
        },
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
                onUploadClick = onUploadCamera,
                isUploading = isUploading,
                title = "Upload with camera",
                icon = UploadIcon.Camera,
                modifier = Modifier.fillMaxWidth()
            )

            // File upload zone
            DocumentUploadZone(
                onUploadClick = onUploadFile,
                isUploading = isUploading,
                title = "Select file or\ndrag it here",
                icon = UploadIcon.Document,
                modifier = Modifier.fillMaxWidth()
            )

            // "Don't have the application?" link
            TextButton(
                onClick = onShowQrCode,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = "Don't have the application? Click here",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Help text
            Text(
                text = "To import an image or scan a document for your invoice, make sure the file is clear and in a compatible format. Scan/upload your file, and the software will extract the relevant information to fill in the invoice fields.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Upload list section
            if (uploadTasks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Uploads",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                DocumentUploadList(
                    tasks = uploadTasks,
                    documents = uploadedDocuments,
                    deletionHandles = deletionHandles,
                    uploadManager = viewModel.provideUploadManager(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
