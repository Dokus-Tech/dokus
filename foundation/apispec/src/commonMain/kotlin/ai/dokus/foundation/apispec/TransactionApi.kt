package ai.dokus.foundation.apispec

import ai.dokus.foundation.domain.model.Transaction
import ai.dokus.foundation.domain.model.PaginatedResponse
import ai.dokus.foundation.domain.model.TransactionUploadResponse

interface TransactionApi {
    companion object {}

    // Return Result to handle exceptions properly
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
    ): Result<PaginatedResponse<Transaction>>

    suspend fun uploadTransactionFile(
        companyId: String,
        fileBytes: ByteArray
    ): Result<TransactionUploadResponse>

    suspend fun getTransaction(transactionId: String, companyId: String): Result<Transaction>
    suspend fun deleteTransaction(transactionId: String, companyId: String): Result<Unit>
    suspend fun checkTransactionExists(transactionId: String, companyId: String): Result<Boolean>
}
