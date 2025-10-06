package ai.dokus.foundation.apispec

import ai.dokus.foundation.domain.model.MatchedSchema
import ai.dokus.foundation.domain.model.SimpleMatchDocumentsResult

interface MatchingApi {
    companion object {}

    // Return Result to handle exceptions properly
    suspend fun getDocumentMatching(documentId: String, companyId: String): Result<MatchedSchema>
    suspend fun getAllMatching(companyId: String): Result<SimpleMatchDocumentsResult>
}
