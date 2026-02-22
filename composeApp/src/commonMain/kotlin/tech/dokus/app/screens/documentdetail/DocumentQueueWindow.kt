package tech.dokus.app.screens.documentdetail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.domain.ids.DocumentId
import tech.dokus.foundation.app.shell.DocQueueItem
import tech.dokus.foundation.app.shell.DocQueueStatus
import tech.dokus.foundation.aura.components.common.KeyboardNavigationHint
import tech.dokus.foundation.aura.components.queue.DocQueueHeader
import tech.dokus.foundation.aura.components.queue.DocQueueItemRow
import tech.dokus.foundation.aura.components.status.StatusDotType
import tech.dokus.foundation.aura.style.statusConfirmed
import tech.dokus.foundation.aura.style.statusError
import tech.dokus.foundation.aura.style.statusWarning
import tech.dokus.foundation.aura.style.textMuted
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

/**
 * Document queue window — left 220dp glass panel in document detail mode.
 * Shows back button, position counter, and scrollable document list.
 */
@Composable
internal fun DocumentQueueWindow(
    documents: List<DocQueueItem>,
    selectedDocumentId: DocumentId,
    onSelectDocument: (DocumentId) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedIndex = documents.indexOfFirst { it.id == selectedDocumentId }
    val positionText = if (selectedIndex >= 0) "${selectedIndex + 1}/${documents.size}" else ""

    Column(modifier = modifier.fillMaxHeight()) {
        DocQueueHeader(
            positionText = positionText,
            onExit = onExit,
        )

        HorizontalDivider(color = Color.Black.copy(alpha = 0.06f))

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            itemsIndexed(
                items = documents,
                key = { _, item -> item.id.toString() }
            ) { _, item ->
                DocQueueItemRow(
                    vendorName = item.vendorName,
                    date = item.date,
                    amount = item.amount,
                    statusDotType = item.status.dotType(),
                    statusTextColor = item.status.color(),
                    statusDetail = item.statusDetail,
                    isSelected = item.id == selectedDocumentId,
                    onClick = { onSelectDocument(item.id) },
                )
            }
        }

        HorizontalDivider(color = Color.Black.copy(alpha = 0.06f))
        KeyboardNavigationHint()
    }
}

@Composable
private fun DocQueueStatus.color() = when (this) {
    DocQueueStatus.Paid -> MaterialTheme.colorScheme.statusConfirmed
    DocQueueStatus.Overdue -> MaterialTheme.colorScheme.statusError
    DocQueueStatus.Review -> MaterialTheme.colorScheme.statusWarning
    DocQueueStatus.Unpaid -> MaterialTheme.colorScheme.textMuted
    DocQueueStatus.Processing -> MaterialTheme.colorScheme.statusWarning
}

private fun DocQueueStatus.dotType(): StatusDotType = when (this) {
    DocQueueStatus.Paid -> StatusDotType.Confirmed
    DocQueueStatus.Overdue -> StatusDotType.Error
    DocQueueStatus.Review -> StatusDotType.Warning
    DocQueueStatus.Unpaid -> StatusDotType.Empty
    DocQueueStatus.Processing -> StatusDotType.Warning
}

// =============================================================================
// Previews
// =============================================================================

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
@Preview
@Composable
private fun DocumentQueueWindowPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    val docId1 = DocumentId(kotlin.uuid.Uuid.random())
    val docId2 = DocumentId(kotlin.uuid.Uuid.random())
    val docId3 = DocumentId(kotlin.uuid.Uuid.random())
    val sampleDocuments = listOf(
        DocQueueItem(
            id = docId1,
            vendorName = "Acme Corp",
            amount = "1,250.00",
            date = "Feb 15",
            status = DocQueueStatus.Review,
            statusDetail = "Review",
        ),
        DocQueueItem(
            id = docId2,
            vendorName = "Tech Solutions",
            amount = "890.50",
            date = "Feb 14",
            status = DocQueueStatus.Paid,
            statusDetail = "Paid",
        ),
        DocQueueItem(
            id = docId3,
            vendorName = "Cloud Services Ltd",
            amount = "3,200.00",
            date = "Feb 13",
            status = DocQueueStatus.Overdue,
            statusDetail = "13d",
        ),
    )
    TestWrapper(parameters) {
        DocumentQueueWindow(
            documents = sampleDocuments,
            selectedDocumentId = docId1,
            onSelectDocument = {},
            onExit = {},
        )
    }
}
