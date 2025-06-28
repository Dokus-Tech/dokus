package ai.thepredict.apispec

import ai.thepredict.domain.model.Document

interface DocumentExtractionApi {
    companion object {}

    suspend fun startDocumentExtraction(documentId: String, companyId: String): Document
    suspend fun deleteDocumentExtraction(documentId: String, companyId: String)
    suspend fun checkDocumentExtractionExists(documentId: String, companyId: String): Boolean
}