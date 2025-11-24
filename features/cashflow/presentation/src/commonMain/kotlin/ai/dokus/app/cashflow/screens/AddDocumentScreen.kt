package ai.dokus.app.cashflow.screens

import ai.dokus.app.cashflow.components.DroppedFile
import ai.dokus.app.cashflow.components.documentDropTarget
import ai.dokus.app.cashflow.viewmodel.AddDocumentViewModel
import ai.dokus.foundation.design.components.PButton
import ai.dokus.foundation.design.components.PButtonVariant
import ai.dokus.foundation.design.components.common.PTopAppBar
import ai.dokus.foundation.navigation.local.LocalNavController
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

    Scaffold(
        topBar = {
            PTopAppBar(
                title = "Upload document",
                canNavigateBack = true,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            val background = MaterialTheme.colorScheme.surfaceVariant
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(background)
                    .documentDropTarget(scope) { viewModel.uploadFiles(it) }
                    .padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Drag & drop files here",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "PDF, images, spreadsheets. You can also browse to choose files.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                PButton(
                    text = "Browse files",
                    variant = PButtonVariant.Default,
                    onClick = { filePickerLauncher.launch() },
                    isLoading = state is AddDocumentViewModel.State.Uploading
                )

                when (val current = state) {
                    AddDocumentViewModel.State.Idle -> Unit
                    AddDocumentViewModel.State.Uploading -> {
                        Text(
                            text = "Uploading...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    is AddDocumentViewModel.State.Error -> {
                        Text(
                            text = current.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        PButton(
                            text = "Try again",
                            variant = PButtonVariant.Outline,
                            onClick = { viewModel.reset() }
                        )
                    }

                    is AddDocumentViewModel.State.Success -> {
                        Text(
                            text = "Uploaded ${current.uploadedCount} file(s).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF2E7D32)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        PButton(
                            text = "Back to cashflow",
                            variant = PButtonVariant.Outline,
                            onClick = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
