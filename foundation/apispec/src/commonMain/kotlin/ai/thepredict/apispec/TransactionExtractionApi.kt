package ai.thepredict.apispec

import ai.thepredict.domain.model.Transaction
import kotlin.Result

interface TransactionExtractionApi {
    companion object {}

    suspend fun startTransactionExtraction(
        transactionId: String,
        companyId: String
    ): Result<Transaction>

    suspend fun deleteTransactionExtraction(transactionId: String, companyId: String): Result<Unit>
    suspend fun checkTransactionExtractionExists(
        transactionId: String,
        companyId: String
    ): Result<Boolean>
}
