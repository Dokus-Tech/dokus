package tech.dokus.features.cashflow.presentation.documents.model

import androidx.compose.runtime.Immutable
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.DocumentDto
import tech.dokus.domain.model.DocumentListItemDto
import tech.dokus.features.cashflow.presentation.cashflow.model.DocumentUploadTask
import tech.dokus.features.cashflow.presentation.cashflow.model.UploadStatus
import tech.dokus.features.cashflow.presentation.documents.mvi.DocumentFilter

@Immutable
internal data class DocumentsLocalUploadRow(
    val taskId: String,
    val fileName: String,
    val status: Status,
    val documentId: DocumentId? = null,
) {
    @Immutable
    enum class Status {
        Uploading,
        PreparingDocument,
        ReadingDocument,
        Failed,
    }
}

internal fun buildDocumentsLocalUploadRows(
    filter: DocumentFilter,
    uploadTasks: List<DocumentUploadTask>,
    uploadedDocuments: Map<String, DocumentDto>,
    remoteDocuments: List<DocumentListItemDto>,
    knownRemoteDocumentIds: Set<DocumentId> = emptySet(),
): List<DocumentsLocalUploadRow> {
    if (uploadTasks.isEmpty()) return emptyList()

    val remoteIds = remoteDocuments.mapTo(mutableSetOf()) { it.documentId }

    return uploadTasks.asReversed().mapNotNull { task ->
        val documentId = task.documentId ?: uploadedDocuments[task.id]?.id
        val isRemotePresent = documentId != null && remoteIds.contains(documentId)

        val localStatus = when (task.status) {
            UploadStatus.UPLOADING -> DocumentsLocalUploadRow.Status.Uploading
            UploadStatus.PENDING -> DocumentsLocalUploadRow.Status.PreparingDocument
            UploadStatus.COMPLETED -> DocumentsLocalUploadRow.Status.ReadingDocument

            UploadStatus.FAILED -> DocumentsLocalUploadRow.Status.Failed
        }

        if (task.status == UploadStatus.COMPLETED && isRemotePresent) return@mapNotNull null
        if (!shouldShowLocalRow(
                filter = filter,
                isRemotePresent = isRemotePresent,
                documentId = documentId,
                knownRemoteDocumentIds = knownRemoteDocumentIds
            )
        ) {
            return@mapNotNull null
        }

        DocumentsLocalUploadRow(
            taskId = task.id,
            fileName = task.fileName,
            status = localStatus,
            documentId = documentId
        )
    }
}

private fun shouldShowLocalRow(
    filter: DocumentFilter,
    isRemotePresent: Boolean,
    documentId: DocumentId?,
    knownRemoteDocumentIds: Set<DocumentId>,
): Boolean {
    return when (filter) {
        DocumentFilter.All -> true
        // Hide rows once we know their replacement exists remotely, even if not in this filtered page.
        DocumentFilter.NeedsAttention -> !isRemotePresent &&
            (documentId == null || !knownRemoteDocumentIds.contains(documentId))
        DocumentFilter.Confirmed -> false
    }
}
