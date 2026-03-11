package tech.dokus.backend.services.documents

import tech.dokus.database.repository.cashflow.DocumentRepository
import tech.dokus.database.repository.cashflow.DocumentSourceRepository
import tech.dokus.database.repository.cashflow.selectPreferredSource
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.features.ai.services.DocumentFetcher
import tech.dokus.features.ai.services.DocumentFetcher.FetchedDocumentData
import tech.dokus.foundation.backend.storage.DocumentStorageService

class StorageDocumentFetcher(
    private val documentRepository: DocumentRepository,
    private val sourceRepository: DocumentSourceRepository,
    private val storageService: DocumentStorageService
) : DocumentFetcher {
    override suspend fun invoke(tenantId: TenantId, documentId: DocumentId): Result<FetchedDocumentData> {
        return runCatching {
            require(documentRepository.exists(tenantId, documentId)) {
                "Document $documentId not found for tenant $tenantId"
            }
            val sources = sourceRepository.listByDocument(tenantId, documentId)
            val source = selectPreferredSource(sources)
                ?: error("No source available for document $documentId")
            val bytes = storageService.downloadDocument(source.storageKey)
            FetchedDocumentData(bytes = bytes, mimeType = source.contentType)
        }
    }
}
