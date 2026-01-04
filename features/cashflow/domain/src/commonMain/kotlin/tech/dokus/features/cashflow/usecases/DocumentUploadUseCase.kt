package tech.dokus.features.cashflow.usecases

import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.DocumentDto

/**
 * Use case for uploading and deleting documents.
 */
interface DocumentUploadUseCase {
    suspend fun uploadDocumentWithProgress(
        fileContent: ByteArray,
        filename: String,
        contentType: String?,
        prefix: String,
        onProgress: (Float) -> Unit
    ): Result<DocumentDto>

    suspend fun deleteDocument(documentId: DocumentId): Result<Unit>
}
