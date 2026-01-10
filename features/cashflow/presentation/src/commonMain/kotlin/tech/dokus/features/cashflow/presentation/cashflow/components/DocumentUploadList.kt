package tech.dokus.features.cashflow.presentation.cashflow.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.upload_no_documents
import tech.dokus.domain.model.DocumentDto
import tech.dokus.features.cashflow.presentation.cashflow.model.DocumentDeletionHandle
import tech.dokus.features.cashflow.presentation.cashflow.model.DocumentUploadTask
import tech.dokus.features.cashflow.presentation.cashflow.model.manager.DocumentUploadManager

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
 * @param documents Map of task ID to an uploaded document (for completed uploads)
 * @param deletionHandles Map of task ID to a deletion handle (for items being deleted)
 * @param uploadManager The upload manager for handling actions
 * @param modifier Modifier for the list
 * @param contentPadding Padding around the list content
 * @param scrollable Whether the list should be scrollable. Set to false when inside a scrollable parent.
 */
@Composable
fun DocumentUploadList(
    tasks: List<DocumentUploadTask>,
    documents: Map<String, DocumentDto>,
    deletionHandles: Map<String, DocumentDeletionHandle>,
    uploadManager: DocumentUploadManager,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    scrollable: Boolean = false,
    showEmptyState: Boolean = true
) {
    if (tasks.isEmpty() && showEmptyState) {
        EmptyUploadList(modifier = modifier.padding(contentPadding))
    } else if (tasks.isNotEmpty()) {
        val scrollModifier = if (scrollable) {
            Modifier.verticalScroll(rememberScrollState())
        } else {
            Modifier
        }
        Column(
            modifier = modifier
                .padding(contentPadding)
                .then(scrollModifier),
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
    Text(
        text = stringResource(Res.string.upload_no_documents),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    )
}
