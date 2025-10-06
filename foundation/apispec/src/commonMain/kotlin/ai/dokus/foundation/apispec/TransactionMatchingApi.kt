package ai.dokus.foundation.apispec

import ai.dokus.foundation.domain.model.MatchedSchema

interface TransactionMatchingApi {
    companion object;

    // Return Result to handle exceptions properly
    suspend fun getTransactionMatching(
        transactionId: String,
        companyId: String
    ): Result<MatchedSchema>
}
