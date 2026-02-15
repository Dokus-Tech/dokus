package tech.dokus.features.cashflow.gateway

import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.DocumentSourceId
import tech.dokus.domain.model.DocumentIntakeResult

/**
 * Gateway for document upload and deletion.
 */
interface DocumentUploadGateway {
    suspend fun uploadDocumentWithProgress(
        fileContent: ByteArray,
        filename: String,
        contentType: String?,
        prefix: String,
        onProgress: (Float) -> Unit
    ): Result<DocumentIntakeResult>

    suspend fun deleteDocument(documentId: DocumentId, sourceId: DocumentSourceId? = null): Result<Unit>
}
