package ai.dokus.app.cashflow.screens

import ai.dokus.app.cashflow.components.DocumentUploadZone
import ai.dokus.app.cashflow.components.DroppedFile
import ai.dokus.app.cashflow.components.InvoiceDetailsForm
import ai.dokus.app.cashflow.components.UploadIcon
import ai.dokus.app.cashflow.components.documentDropTarget
import ai.dokus.app.cashflow.viewmodel.AddDocumentViewModel
import ai.dokus.foundation.design.components.common.Breakpoints
import ai.dokus.foundation.design.components.common.PTopAppBar
import ai.dokus.foundation.navigation.local.LocalNavController
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.calf.core.LocalPlatformContext
import com.mohamedrejeb.calf.io.getName
import com.mohamedrejeb.calf.io.readByteArray
import com.mohamedrejeb.calf.picker.FilePickerFileType
import com.mohamedrejeb.calf.picker.FilePickerSelectionMode
import com.mohamedrejeb.calf.picker.rememberFilePickerLauncher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

/**
 * Add a document screen for uploading and processing documents/invoices.
 * Displays different layouts for mobile (upload zones) and desktop (form + upload).
 */
@Composable
internal fun AddDocumentScreen(
    viewModel: AddDocumentViewModel = koinViewModel()
) {
    val navController = LocalNavController.current
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    val platformContext = LocalPlatformContext.current

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

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isLargeScreen = maxWidth >= Breakpoints.LARGE.dp

        if (isLargeScreen) {
            // Desktop layout with top bar
            DesktopLayout(
                onAddNewDocument = { filePickerLauncher.launch() },
                onUploadFile = { filePickerLauncher.launch() },
                isUploading = state is AddDocumentViewModel.State.Uploading,
                viewModel = viewModel,
                scope = scope
            )
        } else {
            // Mobile layout with simple top bar
            MobileLayout(
                onUploadFile = { filePickerLauncher.launch() },
                onUploadCamera = { /* TODO: Implement camera upload */ },
                isUploading = state is AddDocumentViewModel.State.Uploading,
            )
        }
    }
}

/**
 * Desktop layout with side-by-side upload zone and details form.
 */
@Composable
private fun DesktopLayout(
    onAddNewDocument: () -> Unit,
    onUploadFile: () -> Unit,
    isUploading: Boolean,
    viewModel: AddDocumentViewModel,
    scope: CoroutineScope
) {
    Scaffold(
        topBar = { PTopAppBar("Add a new document") },
        containerColor = MaterialTheme.colorScheme.background
    ) { contentPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(32.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Left side: Upload zone
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            ) {
                Text(
                    text = "New invoice",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "To import an image or scan a document for your invoice, make sure the file is clear and in a compatible format. Scan/upload your file, and the software will extract the relevant information to fill in the invoice fields. Just double-check the data for accuracy before finalizing.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                DocumentUploadZone(
                    onUploadClick = onUploadFile,
                    isUploading = isUploading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .documentDropTarget(scope) { viewModel.uploadFiles(it) }
                )

                // Show "Don't have the application?" link if needed
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Don't have the application? Click here",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Right side: Details form
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                InvoiceDetailsForm()
            }
        }
    }
}

/**
 * Mobile layout with stacked upload zones.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun MobileLayout(
    onUploadFile: () -> Unit,
    onUploadCamera: () -> Unit,
    isUploading: Boolean,
) {
    Scaffold(
        topBar = {
            PTopAppBar(
                title = "Upload document",
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
            Text(
                text = "Upload new document",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

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

            // Help text
            Text(
                text = "To import an image or scan a document for your invoice, make sure the file is clear and in a compatible format. Scan/upload your file, and the software will extract the relevant information to fill in the invoice fields. Just double-check the data for accuracy before finalizing.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
