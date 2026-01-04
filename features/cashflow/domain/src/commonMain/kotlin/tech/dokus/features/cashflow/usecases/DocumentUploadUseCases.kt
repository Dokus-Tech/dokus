package tech.dokus.features.cashflow.usecases

import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.DocumentDto

/**
 * Use case for uploading a document with progress callbacks.
 */
interface UploadDocumentUseCase {
    suspend operator fun invoke(
        fileContent: ByteArray,
        filename: String,
        contentType: String?,
        prefix: String,
        onProgress: (Float) -> Unit
    ): Result<DocumentDto>
}

/**
 * Use case for deleting a document.
 */
interface DeleteDocumentUseCase {
    suspend operator fun invoke(documentId: DocumentId): Result<Unit>
}
