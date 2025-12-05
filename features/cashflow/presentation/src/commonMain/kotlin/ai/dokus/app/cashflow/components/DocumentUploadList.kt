package ai.dokus.app.cashflow.components

import ai.dokus.app.cashflow.manager.DocumentUploadManager
import ai.dokus.app.cashflow.model.DocumentDeletionHandle
import ai.dokus.app.cashflow.model.DocumentUploadTask
import ai.dokus.foundation.domain.model.DocumentDto
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A list of document upload items.
 *
 * Uses a regular Column instead of LazyColumn to avoid nesting scrollable containers.
 * This is acceptable since upload lists are typically small (a few items at a time).
 *
 * Displays all upload tasks with their current state (pending, uploading, failed, uploaded, deleting).
 * Each item is independently managed via [DocumentUploadItem].
 *
 * @param tasks List of upload tasks to display
 * @param documents Map of task ID to uploaded document (for completed uploads)
 * @param deletionHandles Map of task ID to deletion handle (for items being deleted)
 * @param uploadManager The upload manager for handling actions
 * @param modifier Modifier for the list
 * @param contentPadding Padding around the list content
 */
@Composable
fun DocumentUploadList(
    tasks: List<DocumentUploadTask>,
    documents: Map<String, DocumentDto>,
    deletionHandles: Map<String, DocumentDeletionHandle>,
    uploadManager: DocumentUploadManager,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    if (tasks.isEmpty()) {
        EmptyUploadList(modifier = modifier.padding(contentPadding))
    } else {
        Column(
            modifier = modifier
                .padding(contentPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tasks.forEach { task ->
                DocumentUploadItem(
                    taskId = task.id,
                    task = task,
                    document = documents[task.id],
                    deletionHandle = deletionHandles[task.id],
                    uploadManager = uploadManager,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun EmptyUploadList(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No documents uploading",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
