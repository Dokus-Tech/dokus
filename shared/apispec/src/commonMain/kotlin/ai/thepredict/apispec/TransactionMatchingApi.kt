package ai.thepredict.apispec

import ai.thepredict.domain.model.MatchedSchema

interface TransactionMatchingApi {
    companion object;

    suspend fun getTransactionMatching(transactionId: String, companyId: String): MatchedSchema
}