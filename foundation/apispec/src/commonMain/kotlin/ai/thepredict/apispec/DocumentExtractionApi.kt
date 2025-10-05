package ai.thepredict.apispec

import ai.thepredict.domain.model.Document
import kotlin.Result

interface DocumentExtractionApi {
    companion object {}

    // Return Result to handle exceptions properly
    suspend fun startDocumentExtraction(documentId: String, companyId: String): Result<Document>
    suspend fun deleteDocumentExtraction(documentId: String, companyId: String): Result<Unit>
    suspend fun checkDocumentExtractionExists(
        documentId: String,
        companyId: String
    ): Result<Boolean>
}
