package ai.thepredict.apispec

interface DocumentFileApi {
    suspend fun getDocumentFileUrl(documentId: String, companyId: String): String
    suspend fun deleteDocumentFile(documentId: String, companyId: String)
}