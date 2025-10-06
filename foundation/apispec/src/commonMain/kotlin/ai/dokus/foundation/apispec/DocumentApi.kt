package ai.dokus.foundation.apispec

import ai.dokus.foundation.domain.model.Document
import ai.dokus.foundation.domain.model.DocumentType
import ai.dokus.foundation.domain.model.DocumentUploadResponse
import ai.dokus.foundation.domain.model.PaginatedResponse

interface DocumentApi {
    companion object {}

    // Return Result to handle exceptions properly
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
    ): Result<PaginatedResponse<Document>>

    suspend fun uploadDocumentFile(
        companyId: String,
        fileBytes: ByteArray
    ): Result<DocumentUploadResponse>

    suspend fun getDocument(documentId: String, companyId: String): Result<Document>
    suspend fun deleteDocument(documentId: String, companyId: String): Result<Unit>
    suspend fun checkDocumentExists(documentId: String, companyId: String): Result<Boolean>
}
