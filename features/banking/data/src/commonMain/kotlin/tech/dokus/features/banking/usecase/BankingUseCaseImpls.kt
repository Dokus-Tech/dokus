package tech.dokus.features.banking.usecase

import kotlinx.datetime.LocalDate
import tech.dokus.domain.enums.BankTransactionSource
import tech.dokus.domain.enums.BankTransactionStatus
import tech.dokus.domain.enums.IgnoredReason
import tech.dokus.domain.ids.BankTransactionId
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.model.BalanceHistoryResponse
import tech.dokus.domain.model.BankAccountDto
import tech.dokus.domain.model.BankAccountSummaryDto
import tech.dokus.domain.model.BankTransactionDto
import tech.dokus.domain.model.BankTransactionSummaryDto
import tech.dokus.domain.model.common.PaginatedResponse
import tech.dokus.features.banking.datasource.BankingRemoteDataSource
import tech.dokus.features.banking.usecases.ConfirmTransactionUseCase
import tech.dokus.features.banking.usecases.CreateExpenseFromTransactionUseCase
import tech.dokus.features.banking.usecases.GetAccountSummaryUseCase
import tech.dokus.features.banking.usecases.GetBalanceHistoryUseCase
import tech.dokus.features.banking.usecases.GetBankTransactionUseCase
import tech.dokus.features.banking.usecases.GetTransactionSummaryUseCase
import tech.dokus.features.banking.usecases.IgnoreTransactionUseCase
import tech.dokus.features.banking.usecases.LinkTransactionUseCase
import tech.dokus.features.banking.usecases.ListBankAccountsUseCase
import tech.dokus.features.banking.usecases.ListBankTransactionsUseCase
import tech.dokus.features.banking.usecases.MarkTransferTransactionUseCase
import tech.dokus.features.banking.usecases.UndoTransferTransactionUseCase
import tech.dokus.domain.model.MarkTransferMode
import tech.dokus.domain.model.MarkTransferRequest
import tech.dokus.domain.ids.BankAccountId
import kotlin.time.Duration

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
    override suspend fun invoke(): Result<BankTransactionSummaryDto> {
        return dataSource.getTransactionSummary()
    }
}

internal class GetAccountSummaryUseCaseImpl(
    private val dataSource: BankingRemoteDataSource
) : GetAccountSummaryUseCase {
    override suspend fun invoke(): Result<BankAccountSummaryDto> {
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
    override suspend fun invoke(transactionId: BankTransactionId, reason: IgnoredReason): Result<BankTransactionDto> {
        return dataSource.ignoreTransaction(transactionId, reason)
    }
}

internal class ConfirmTransactionUseCaseImpl(
    private val dataSource: BankingRemoteDataSource
) : ConfirmTransactionUseCase {
    override suspend fun invoke(transactionId: BankTransactionId): Result<BankTransactionDto> {
        return dataSource.confirmTransaction(transactionId)
    }
}

internal class CreateExpenseFromTransactionUseCaseImpl(
    private val dataSource: BankingRemoteDataSource,
) : CreateExpenseFromTransactionUseCase {
    override suspend fun invoke(transactionId: BankTransactionId): Result<BankTransactionDto> {
        return dataSource.createExpenseFromTransaction(transactionId)
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
    override suspend fun invoke(duration: Duration): Result<BalanceHistoryResponse> {
        return dataSource.getBalanceHistory(duration.inWholeDays.toInt())
    }
}

internal class MarkTransferTransactionUseCaseImpl(
    private val dataSource: BankingRemoteDataSource,
) : MarkTransferTransactionUseCase {
    override suspend fun invoke(
        transactionId: BankTransactionId,
        mode: MarkTransferMode,
        counterpartTransactionId: BankTransactionId?,
        destinationAccountId: BankAccountId?,
    ): Result<BankTransactionDto> {
        return dataSource.markTransfer(
            transactionId,
            MarkTransferRequest(
                mode = mode,
                counterpartTransactionId = counterpartTransactionId,
                destinationAccountId = destinationAccountId,
            )
        )
    }
}

internal class UndoTransferTransactionUseCaseImpl(
    private val dataSource: BankingRemoteDataSource,
) : UndoTransferTransactionUseCase {
    override suspend fun invoke(transactionId: BankTransactionId): Result<BankTransactionDto> {
        return dataSource.undoTransfer(transactionId)
    }
}
