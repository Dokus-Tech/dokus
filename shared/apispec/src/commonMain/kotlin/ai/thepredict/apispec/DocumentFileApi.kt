package ai.thepredict.apispec

interface DocumentFileApi {
    companion object {}

    // Return Result to handle exceptions properly
    suspend fun getDocumentFileUrl(documentId: String, companyId: String): Result<String>
    suspend fun deleteDocumentFile(documentId: String, companyId: String): Result<Unit>
}
