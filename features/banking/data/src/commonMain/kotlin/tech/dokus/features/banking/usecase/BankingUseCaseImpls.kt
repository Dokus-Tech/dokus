package tech.dokus.features.banking.usecase

import kotlinx.datetime.LocalDate
import tech.dokus.domain.enums.BankTransactionSource
import tech.dokus.domain.enums.BankTransactionStatus
import tech.dokus.domain.ids.BankTransactionId
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.model.BalanceHistoryResponse
import tech.dokus.domain.model.BankAccountSummary
import tech.dokus.domain.model.BankAccountDto
import tech.dokus.domain.model.BankTransactionDto
import tech.dokus.domain.model.BankTransactionSummary
import tech.dokus.domain.model.common.PaginatedResponse
import tech.dokus.features.banking.datasource.BankingRemoteDataSource
import tech.dokus.features.banking.usecases.ConfirmTransactionUseCase
import tech.dokus.features.banking.usecases.GetAccountSummaryUseCase
import tech.dokus.features.banking.usecases.GetBalanceHistoryUseCase
import tech.dokus.features.banking.usecases.GetBankTransactionUseCase
import tech.dokus.features.banking.usecases.GetTransactionSummaryUseCase
import tech.dokus.features.banking.usecases.IgnoreTransactionUseCase
import tech.dokus.features.banking.usecases.LinkTransactionUseCase
import tech.dokus.features.banking.usecases.ListBankAccountsUseCase
import tech.dokus.features.banking.usecases.ListBankTransactionsUseCase

internal class ListBankTransactionsUseCaseImpl(
    private val dataSource: BankingRemoteDataSource
) : ListBankTransactionsUseCase {
    override suspend fun invoke(
        page: Int,
        pageSize: Int,
        status: BankTransactionStatus?,
        source: BankTransactionSource?,
        fromDate: LocalDate?,
        toDate: LocalDate?
    ): Result<PaginatedResponse<BankTransactionDto>> {
        return dataSource.listTransactions(
            status = status,
            source = source,
            fromDate = fromDate,
            toDate = toDate,
            limit = pageSize,
            offset = page * pageSize
        )
    }
}

internal class GetBankTransactionUseCaseImpl(
    private val dataSource: BankingRemoteDataSource
) : GetBankTransactionUseCase {
    override suspend fun invoke(transactionId: BankTransactionId): Result<BankTransactionDto> {
        return dataSource.getTransaction(transactionId)
    }
}

internal class GetTransactionSummaryUseCaseImpl(
    private val dataSource: BankingRemoteDataSource
) : GetTransactionSummaryUseCase {
    override suspend fun invoke(): Result<BankTransactionSummary> {
        return dataSource.getTransactionSummary()
    }
}

internal class GetAccountSummaryUseCaseImpl(
    private val dataSource: BankingRemoteDataSource
) : GetAccountSummaryUseCase {
    override suspend fun invoke(): Result<BankAccountSummary> {
        return dataSource.getAccountSummary()
    }
}

internal class LinkTransactionUseCaseImpl(
    private val dataSource: BankingRemoteDataSource
) : LinkTransactionUseCase {
    override suspend fun invoke(
        transactionId: BankTransactionId,
        cashflowEntryId: CashflowEntryId
    ): Result<BankTransactionDto> {
        return dataSource.linkTransaction(transactionId, cashflowEntryId)
    }
}

internal class IgnoreTransactionUseCaseImpl(
    private val dataSource: BankingRemoteDataSource
) : IgnoreTransactionUseCase {
    override suspend fun invoke(transactionId: BankTransactionId): Result<BankTransactionDto> {
        return dataSource.ignoreTransaction(transactionId)
    }
}

internal class ConfirmTransactionUseCaseImpl(
    private val dataSource: BankingRemoteDataSource
) : ConfirmTransactionUseCase {
    override suspend fun invoke(transactionId: BankTransactionId): Result<BankTransactionDto> {
        return dataSource.confirmTransaction(transactionId)
    }
}

internal class ListBankAccountsUseCaseImpl(
    private val dataSource: BankingRemoteDataSource,
) : ListBankAccountsUseCase {
    override suspend fun invoke(): Result<List<BankAccountDto>> {
        return dataSource.listAccounts()
    }
}

internal class GetBalanceHistoryUseCaseImpl(
    private val dataSource: BankingRemoteDataSource,
) : GetBalanceHistoryUseCase {
    override suspend fun invoke(days: Int): Result<BalanceHistoryResponse> {
        return dataSource.getBalanceHistory(days)
    }
}
