package ai.thepredict.apispec

import ai.thepredict.domain.model.MatchedSchema

interface TransactionMatchingApi {
    companion object;

    // Return Result to handle exceptions properly
    suspend fun getTransactionMatching(
        transactionId: String,
        companyId: String
    ): Result<MatchedSchema>
}
