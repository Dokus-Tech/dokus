package ai.thepredict.apispec

import ai.thepredict.domain.model.Transaction

interface TransactionApi {
    suspend fun listTransactions(
        companyId: String,
        offset: Int = 0,
        limit: Int = 100
    ): List<Transaction>

    suspend fun uploadTransactionFile(companyId: String, fileBytes: ByteArray): List<Transaction>
    suspend fun getTransaction(transactionId: String, companyId: String): Transaction
    suspend fun deleteTransaction(transactionId: String, companyId: String)
    suspend fun checkTransactionExists(transactionId: String, companyId: String): Boolean
}