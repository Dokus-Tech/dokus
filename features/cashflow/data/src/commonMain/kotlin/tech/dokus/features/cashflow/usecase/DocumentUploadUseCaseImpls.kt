package tech.dokus.features.cashflow.usecase

import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.DocumentSourceId
import tech.dokus.domain.model.DocumentIntakeResult
import tech.dokus.features.cashflow.gateway.DocumentUploadGateway
import tech.dokus.features.cashflow.usecases.DeleteDocumentUseCase
import tech.dokus.features.cashflow.usecases.UploadDocumentUseCase

internal class UploadDocumentUseCaseImpl(
    private val documentUploadGateway: DocumentUploadGateway
) : UploadDocumentUseCase {
    override suspend fun invoke(
        fileContent: ByteArray,
        filename: String,
        contentType: String?,
        prefix: String,
        onProgress: (Float) -> Unit
    ): Result<DocumentIntakeResult> {
        return documentUploadGateway.uploadDocumentWithProgress(
            fileContent = fileContent,
            filename = filename,
            contentType = contentType,
            prefix = prefix,
            onProgress = onProgress
        )
    }
}

internal class DeleteDocumentUseCaseImpl(
    private val documentUploadGateway: DocumentUploadGateway
) : DeleteDocumentUseCase {
    override suspend fun invoke(documentId: DocumentId, sourceId: DocumentSourceId?): Result<Unit> {
        return documentUploadGateway.deleteDocument(documentId, sourceId)
    }
}
