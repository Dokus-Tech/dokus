package tech.dokus.features.banking.usecases

import kotlinx.datetime.LocalDate
import tech.dokus.domain.enums.BankTransactionSource
import tech.dokus.domain.enums.BankTransactionStatus
import tech.dokus.domain.ids.BankTransactionId
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.model.BankAccountSummary
import tech.dokus.domain.model.BankTransactionDto
import tech.dokus.domain.model.BankTransactionSummary
import tech.dokus.domain.model.LinkTransactionRequest
import tech.dokus.domain.model.common.PaginatedResponse

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

interface LinkTransactionUseCase {
    suspend operator fun invoke(
        transactionId: BankTransactionId,
        cashflowEntryId: CashflowEntryId
    ): Result<BankTransactionDto>
}

interface IgnoreTransactionUseCase {
    suspend operator fun invoke(
        transactionId: BankTransactionId
    ): Result<BankTransactionDto>
}

interface ConfirmTransactionUseCase {
    suspend operator fun invoke(
        transactionId: BankTransactionId
    ): Result<BankTransactionDto>
}
