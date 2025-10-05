package ai.thepredict.apispec

interface TransactionFileApi {
    companion object {}

    // Return Result to handle exceptions properly
    suspend fun getTransactionFileUrl(transactionId: String, companyId: String): Result<String>
    suspend fun deleteTransactionFile(transactionId: String, companyId: String): Result<Unit>
}
