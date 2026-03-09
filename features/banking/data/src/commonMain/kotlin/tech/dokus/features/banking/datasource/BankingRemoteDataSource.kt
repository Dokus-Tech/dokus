package tech.dokus.features.banking.datasource

import kotlinx.datetime.LocalDate
import tech.dokus.domain.enums.BankTransactionSource
import tech.dokus.domain.enums.BankTransactionStatus
import tech.dokus.domain.ids.BankTransactionId
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.model.BalanceHistoryResponse
import tech.dokus.domain.model.BankAccountSummary
import tech.dokus.domain.model.BankConnectionDto
import tech.dokus.domain.model.BankTransactionDto
import tech.dokus.domain.model.BankTransactionSummary
import tech.dokus.domain.model.common.PaginatedResponse

interface BankingRemoteDataSource {

    suspend fun listConnections(): Result<List<BankConnectionDto>>

    suspend fun getAccountSummary(): Result<BankAccountSummary>

    suspend fun listTransactions(
        status: BankTransactionStatus? = null,
        source: BankTransactionSource? = null,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null,
        limit: Int = 50,
        offset: Int = 0
    ): Result<PaginatedResponse<BankTransactionDto>>

    suspend fun getTransactionSummary(): Result<BankTransactionSummary>

    suspend fun getTransaction(transactionId: BankTransactionId): Result<BankTransactionDto>

    suspend fun linkTransaction(
        transactionId: BankTransactionId,
        cashflowEntryId: CashflowEntryId
    ): Result<BankTransactionDto>

    suspend fun ignoreTransaction(transactionId: BankTransactionId): Result<BankTransactionDto>

    suspend fun confirmTransaction(transactionId: BankTransactionId): Result<BankTransactionDto>

    suspend fun getBalanceHistory(days: Int = 30): Result<BalanceHistoryResponse>
}
