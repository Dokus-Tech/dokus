package tech.dokus.features.cashflow.gateway

import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.DocumentDto

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
    ): Result<DocumentDto>

    suspend fun deleteDocument(documentId: DocumentId): Result<Unit>
}
