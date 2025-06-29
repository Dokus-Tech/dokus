package ai.thepredict.apispec

import ai.thepredict.domain.model.Transaction
import ai.thepredict.domain.model.PaginatedResponse
import ai.thepredict.domain.model.TransactionUploadResponse

interface TransactionApi {
    companion object {}

    suspend fun listTransactions(
        companyId: String,
        supplierId: String? = null,
        dateFrom: String? = null,
        dateTo: String? = null,
        amountMin: Double? = null,
        amountMax: Double? = null,
        ids: List<String>? = null,
        page: Int = 1,
        size: Int = 100
    ): PaginatedResponse<Transaction>

    suspend fun uploadTransactionFile(companyId: String, fileBytes: ByteArray): TransactionUploadResponse
    suspend fun getTransaction(transactionId: String, companyId: String): Transaction
    suspend fun deleteTransaction(transactionId: String, companyId: String)
    suspend fun checkTransactionExists(transactionId: String, companyId: String): Boolean
}