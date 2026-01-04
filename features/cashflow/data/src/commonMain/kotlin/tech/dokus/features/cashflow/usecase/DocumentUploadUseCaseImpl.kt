package tech.dokus.features.cashflow.usecase

import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.DocumentDto
import tech.dokus.features.cashflow.datasource.CashflowRemoteDataSource
import tech.dokus.features.cashflow.usecases.DocumentUploadUseCase

internal class DocumentUploadUseCaseImpl(
    private val cashflowRemoteDataSource: CashflowRemoteDataSource
) : DocumentUploadUseCase {
    override suspend fun uploadDocumentWithProgress(
        fileContent: ByteArray,
        filename: String,
        contentType: String?,
        prefix: String,
        onProgress: (Float) -> Unit
    ): Result<DocumentDto> {
        return cashflowRemoteDataSource.uploadDocumentWithProgress(
            fileContent = fileContent,
            filename = filename,
            contentType = contentType ?: "application/octet-stream",
            prefix = prefix,
            onProgress = onProgress
        )
    }

    override suspend fun deleteDocument(documentId: DocumentId): Result<Unit> {
        return cashflowRemoteDataSource.deleteDocument(documentId)
    }
}
