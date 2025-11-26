package ai.dokus.app.media.screens

import ai.dokus.app.media.viewmodel.MediaViewModel
import ai.dokus.foundation.design.components.common.PTopAppBar
import ai.dokus.foundation.domain.enums.MediaStatus
import ai.dokus.foundation.domain.model.MediaDto
import ai.dokus.foundation.domain.model.MediaUploadRequest
import com.mohamedrejeb.calf.core.LocalPlatformContext
import com.mohamedrejeb.calf.io.getName
import com.mohamedrejeb.calf.io.readByteArray
import com.mohamedrejeb.calf.picker.FilePickerFileType
import com.mohamedrejeb.calf.picker.FilePickerSelectionMode
import com.mohamedrejeb.calf.picker.rememberFilePickerLauncher
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

@Composable
internal fun MediaScreen(
    viewModel: MediaViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    val platformContext = LocalPlatformContext.current

    val filePickerLauncher = rememberFilePickerLauncher(
        type = FilePickerFileType.Document,
        selectionMode = FilePickerSelectionMode.Single
    ) { files ->
        val file = files.firstOrNull() ?: return@rememberFilePickerLauncher
        scope.launch {
            val bytes = file.readByteArray(platformContext)
            val filename = file.getName(platformContext) ?: "upload.bin"
            val mimeType = mimeTypeFromName(filename)

            viewModel.upload(
                MediaUploadRequest(
                    fileContent = bytes,
                    filename = filename,
                    contentType = mimeType
                )
            )
        }
    }

    Scaffold(
        topBar = { PTopAppBar(title = "Media Inbox") }
    ) { contentPadding ->
        when (val current = state) {
            is MediaViewModel.State.Loading -> LoadingContent(contentPadding)
            is MediaViewModel.State.Error -> ErrorContent(current, contentPadding) { viewModel.refresh() }
            is MediaViewModel.State.Content -> Content(
                state = current,
                contentPadding = contentPadding,
                onUploadClick = { filePickerLauncher.launch() },
                onRetry = { viewModel.refresh() }
            )
        }
    }
}

@Composable
private fun LoadingContent(contentPadding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorContent(
    error: MediaViewModel.State.Error,
    contentPadding: PaddingValues,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = error.exception.message ?: "Something went wrong",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@Composable
private fun Content(
    state: MediaViewModel.State.Content,
    contentPadding: PaddingValues,
    onUploadClick: () -> Unit,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Upload receipts, invoices, or documents",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "We will extract details and keep them queued for AI processing.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(onClick = onUploadClick, enabled = !state.isUploading) {
                Text(if (state.isUploading) "Uploading..." else "Upload")
            }
        }

        if (state.errorMessage != null) {
            Text(
                text = state.errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (state.media.isEmpty()) {
            EmptyState(onRetry)
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.media) { media ->
                    MediaCard(media)
                }
            }
        }
    }
}

@Composable
private fun EmptyState(onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "No media queued",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Upload a document to start processing or refresh the queue.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onRetry, modifier = Modifier.align(Alignment.End)) {
                Text("Refresh")
            }
        }
    }
}

@Composable
private fun MediaCard(media: MediaDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = media.filename,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${humanReadableSize(media.sizeBytes)} • ${media.mimeType}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusPill(media.status)
            }

            media.processingSummary?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            media.extraction?.let { extraction ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Extracted",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    extraction.summary?.let {
                        Text(text = it, style = MaterialTheme.typography.bodyMedium)
                    }

                    extraction.invoice?.let { invoice ->
                        Text(
                            text = "Type: Invoice",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        invoice.invoiceNumber?.let {
                            Text("Invoice #$it", style = MaterialTheme.typography.bodySmall)
                        }
                        invoice.total?.let {
                            Text("Total: ${it.value}", style = MaterialTheme.typography.bodySmall)
                        }
                        invoice.clientName?.let {
                            Text("Client: $it", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    extraction.expense?.let { expense ->
                        Text(
                            text = "Type: Expense",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        expense.merchant?.let {
                            Text("Merchant: $it", style = MaterialTheme.typography.bodySmall)
                        }
                        expense.amount?.let {
                            Text("Amount: ${it.value}", style = MaterialTheme.typography.bodySmall)
                        }
                        expense.category?.let {
                            Text("Category: ${it.name}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            media.attachedEntityType?.let { entityType ->
                val id = media.attachedEntityId ?: ""
                Text(
                    text = "Attached to $entityType: $id",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun mimeTypeFromName(filename: String): String {
    val lower = filename.lowercase()
    return when {
        lower.endsWith(".pdf") -> "application/pdf"
        lower.endsWith(".png") -> "image/png"
        lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> "image/jpeg"
        lower.endsWith(".webp") -> "image/webp"
        lower.endsWith(".gif") -> "image/gif"
        lower.endsWith(".csv") -> "text/csv"
        lower.endsWith(".txt") -> "text/plain"
        lower.endsWith(".doc") || lower.endsWith(".docx") ->
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        lower.endsWith(".xls") || lower.endsWith(".xlsx") ->
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        else -> "application/octet-stream"
    }
}

private fun humanReadableSize(sizeBytes: Long): String {
    if (sizeBytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (ln(sizeBytes.toDouble()) / ln(1024.0)).toInt()
    val value = sizeBytes / 1024.0.pow(digitGroups.toDouble())
    val rounded = (value * 10).roundToInt() / 10.0
    return "$rounded ${units[digitGroups]}"
}

@Composable
private fun StatusPill(status: MediaStatus) {
    val (label, color) = when (status) {
        MediaStatus.Pending -> "Pending" to MaterialTheme.colorScheme.tertiary
        MediaStatus.Processing -> "Processing" to MaterialTheme.colorScheme.secondary
        MediaStatus.Processed -> "Processed" to MaterialTheme.colorScheme.primary
        MediaStatus.Failed -> "Failed" to MaterialTheme.colorScheme.error
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelMedium
        )
    }
}
