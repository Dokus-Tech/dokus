package ai.thepredict.apispec

import ai.thepredict.domain.model.MatchedSchema
import ai.thepredict.domain.model.SimpleMatchDocumentsResult

interface MatchingApi {
    companion object {}

    suspend fun getDocumentMatching(documentId: String, companyId: String): MatchedSchema
    suspend fun getAllMatching(companyId: String): SimpleMatchDocumentsResult
}