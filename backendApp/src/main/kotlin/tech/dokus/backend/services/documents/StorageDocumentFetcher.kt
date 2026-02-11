package tech.dokus.backend.services.documents

import tech.dokus.database.repository.cashflow.DocumentRepository
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.features.ai.services.DocumentFetcher
import tech.dokus.features.ai.services.DocumentFetcher.FetchedDocumentData
import tech.dokus.foundation.backend.storage.DocumentStorageService

class StorageDocumentFetcher(
    private val documentRepository: DocumentRepository,
    private val storageService: DocumentStorageService
) : DocumentFetcher {
    override suspend fun invoke(tenantId: TenantId, documentId: DocumentId): Result<FetchedDocumentData> {
        return runCatching {
            val doc = documentRepository.getById(tenantId, documentId)
            requireNotNull(doc) { "Document $documentId not found for tenant $tenantId" }
            val bytes = storageService.downloadDocument(doc.storageKey)
            FetchedDocumentData(bytes = bytes, mimeType = doc.contentType)
        }
    }
}
