package ai.thepredict.apispec

import ai.thepredict.domain.model.Document
import ai.thepredict.domain.model.DocumentType

interface DocumentApi {
    companion object {}

    suspend fun listDocuments(
        companyId: String,
        documentType: DocumentType? = null,
        offset: Int = 0,
        limit: Int = 10
    ): List<Document>

    suspend fun uploadDocumentFile(companyId: String, fileBytes: ByteArray): Document

    suspend fun getDocument(documentId: String, companyId: String): Document
    suspend fun deleteDocument(documentId: String, companyId: String)
    suspend fun checkDocumentExists(documentId: String, companyId: String): Boolean
}