package ai.thepredict.apispec

import ai.thepredict.domain.model.Transaction

interface TransactionExtractionApi {
    suspend fun startTransactionExtraction(transactionId: String, companyId: String): Transaction
    suspend fun deleteTransactionExtraction(transactionId: String, companyId: String)
    suspend fun checkTransactionExtractionExists(transactionId: String, companyId: String): Boolean
}