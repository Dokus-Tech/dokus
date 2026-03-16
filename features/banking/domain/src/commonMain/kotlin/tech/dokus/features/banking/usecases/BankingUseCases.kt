package tech.dokus.features.banking.usecases

import kotlinx.datetime.LocalDate
import tech.dokus.domain.enums.BankTransactionSource
import tech.dokus.domain.enums.BankTransactionStatus
import tech.dokus.domain.enums.IgnoredReason
import tech.dokus.domain.ids.BankTransactionId
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.model.BalanceHistoryResponse
import tech.dokus.domain.model.BankAccountDto
import tech.dokus.domain.model.BankAccountSummary
import tech.dokus.domain.model.BankTransactionDto
import tech.dokus.domain.model.BankTransactionSummary
import tech.dokus.domain.model.common.PaginatedResponse
import kotlin.time.Duration

interface ListBankTransactionsUseCase {
    suspend operator fun invoke(
        page: Int,
        pageSize: Int,
        status: BankTransactionStatus? = null,
        source: BankTransactionSource? = null,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null
    ): Result<PaginatedResponse<BankTransactionDto>>
}

interface GetBankTransactionUseCase {
    suspend operator fun invoke(
        transactionId: BankTransactionId
    ): Result<BankTransactionDto>
}

interface GetTransactionSummaryUseCase {
    suspend operator fun invoke(): Result<BankTransactionSummary>
}

interface GetAccountSummaryUseCase {
    suspend operator fun invoke(): Result<BankAccountSummary>
}

interface ListBankAccountsUseCase {
    suspend operator fun invoke(): Result<List<BankAccountDto>>
}

interface GetBalanceHistoryUseCase {
    suspend operator fun invoke(duration: Duration): Result<BalanceHistoryResponse>
}

interface LinkTransactionUseCase {
    suspend operator fun invoke(
        transactionId: BankTransactionId,
        cashflowEntryId: CashflowEntryId
    ): Result<BankTransactionDto>
}

interface IgnoreTransactionUseCase {
    suspend operator fun invoke(
        transactionId: BankTransactionId,
        reason: IgnoredReason,
    ): Result<BankTransactionDto>
}

interface ConfirmTransactionUseCase {
    suspend operator fun invoke(
        transactionId: BankTransactionId
    ): Result<BankTransactionDto>
}

interface CreateExpenseFromTransactionUseCase {
    suspend operator fun invoke(
        transactionId: BankTransactionId,
    ): Result<BankTransactionDto>
}

interface MarkTransferTransactionUseCase {
    suspend operator fun invoke(
        transactionId: BankTransactionId,
        mode: tech.dokus.domain.model.MarkTransferMode,
        counterpartTransactionId: BankTransactionId? = null,
        destinationAccountId: tech.dokus.domain.ids.BankAccountId? = null,
    ): Result<BankTransactionDto>
}

interface UndoTransferTransactionUseCase {
    suspend operator fun invoke(
        transactionId: BankTransactionId,
    ): Result<BankTransactionDto>
}
