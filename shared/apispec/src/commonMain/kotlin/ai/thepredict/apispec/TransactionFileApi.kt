package ai.thepredict.apispec

interface TransactionFileApi {
    companion object {}

    suspend fun getTransactionFileUrl(transactionId: String, companyId: String): String
    suspend fun deleteTransactionFile(transactionId: String, companyId: String)
}