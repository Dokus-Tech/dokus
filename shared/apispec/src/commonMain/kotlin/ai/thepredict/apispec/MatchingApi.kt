package ai.thepredict.apispec

import ai.thepredict.domain.model.MatchedSchema
import ai.thepredict.domain.model.SimpleMatchDocumentsResult

interface MatchingApi {
    companion object {}

    // Return Result to handle exceptions properly
    suspend fun getDocumentMatching(documentId: String, companyId: String): Result<MatchedSchema>
    suspend fun getAllMatching(companyId: String): Result<SimpleMatchDocumentsResult>
}
