package ai.thepredict.apispec

interface DocumentFileApi {
    companion object {}

    suspend fun getDocumentFileUrl(documentId: String, companyId: String): String
    suspend fun deleteDocumentFile(documentId: String, companyId: String)
}