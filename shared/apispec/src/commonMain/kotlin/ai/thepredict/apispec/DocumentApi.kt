package ai.thepredict.apispec

import ai.thepredict.domain.model.Document
import ai.thepredict.domain.model.DocumentType
import ai.thepredict.domain.model.DocumentUploadResponse
import ai.thepredict.domain.model.PaginatedResponse

interface DocumentApi {
    companion object {}

    suspend fun listDocuments(
        companyId: String,
        documentType: DocumentType? = null,
        supplierId: String? = null,
        dateFrom: String? = null,
        dateTo: String? = null,
        amountMin: Double? = null,
        amountMax: Double? = null,
        ids: List<String>? = null,
        page: Int = 1,
        size: Int = 10
    ): PaginatedResponse<Document>

    suspend fun uploadDocumentFile(companyId: String, fileBytes: ByteArray): DocumentUploadResponse

    suspend fun getDocument(documentId: String, companyId: String): Document
    suspend fun deleteDocument(documentId: String, companyId: String)
    suspend fun checkDocumentExists(documentId: String, companyId: String): Boolean
}