package tech.dokus.features.cashflow.presentation.documents.model

import androidx.compose.runtime.Immutable
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.DocumentDto
import tech.dokus.domain.model.DocumentRecordDto
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
        Preparing,
        Failed,
    }
}

internal fun buildDocumentsLocalUploadRows(
    filter: DocumentFilter,
    uploadTasks: List<DocumentUploadTask>,
    uploadedDocuments: Map<String, DocumentDto>,
    remoteDocuments: List<DocumentRecordDto>,
    knownNonAttentionDocumentIds: Set<DocumentId> = emptySet(),
): List<DocumentsLocalUploadRow> {
    if (uploadTasks.isEmpty()) return emptyList()

    val remoteIds = remoteDocuments.mapTo(mutableSetOf()) { it.document.id }

    return uploadTasks.asReversed().mapNotNull { task ->
        val documentId = task.documentId ?: uploadedDocuments[task.id]?.id
        val isRemotePresent = documentId != null && remoteIds.contains(documentId)

        val localStatus = when (task.status) {
            UploadStatus.PENDING,
            UploadStatus.UPLOADING,
            UploadStatus.COMPLETED -> DocumentsLocalUploadRow.Status.Preparing

            UploadStatus.FAILED -> DocumentsLocalUploadRow.Status.Failed
        }

        if (task.status == UploadStatus.COMPLETED && isRemotePresent) return@mapNotNull null
        if (!shouldShowLocalRow(
                filter = filter,
                isRemotePresent = isRemotePresent,
                documentId = documentId,
                knownNonAttentionDocumentIds = knownNonAttentionDocumentIds
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
    knownNonAttentionDocumentIds: Set<DocumentId>,
): Boolean {
    return when (filter) {
        DocumentFilter.All -> true
        // Hide rows known to be non-attention even if current filtered page does not include them.
        DocumentFilter.NeedsAttention -> !isRemotePresent &&
            (documentId == null || !knownNonAttentionDocumentIds.contains(documentId))
        DocumentFilter.Confirmed -> false
    }
}
