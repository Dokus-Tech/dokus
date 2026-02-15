package tech.dokus.features.cashflow.usecases

import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.DocumentSourceId
import tech.dokus.domain.model.DocumentIntakeResult

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
    ): Result<DocumentIntakeResult>
}

/**
 * Use case for deleting a document.
 */
interface DeleteDocumentUseCase {
    suspend operator fun invoke(documentId: DocumentId, sourceId: DocumentSourceId? = null): Result<Unit>
}
