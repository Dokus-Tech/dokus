package tech.dokus.features.cashflow.gateway

import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.DocumentSourceId
import tech.dokus.features.cashflow.datasource.CashflowRemoteDataSource

internal class DocumentUploadGatewayImpl(
    private val cashflowRemoteDataSource: CashflowRemoteDataSource
) : DocumentUploadGateway {
    override suspend fun uploadDocumentWithProgress(
        fileContent: ByteArray,
        filename: String,
        contentType: String?,
        prefix: String,
        onProgress: (Float) -> Unit
    ) = cashflowRemoteDataSource.uploadDocumentWithProgress(
        fileContent = fileContent,
        filename = filename,
        contentType = contentType ?: "application/octet-stream",
        prefix = prefix,
        onProgress = onProgress
    )

    override suspend fun deleteDocument(documentId: DocumentId, sourceId: DocumentSourceId?): Result<Unit> {
        return cashflowRemoteDataSource.deleteDocument(documentId, sourceId)
    }
}
