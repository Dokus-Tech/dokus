package tech.dokus.features.banking.datasource

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.client.plugins.resources.get
import io.ktor.client.plugins.resources.post as resourcePost
import kotlinx.datetime.LocalDate
import tech.dokus.domain.config.DynamicDokusEndpointProvider
import tech.dokus.domain.enums.BankTransactionSource
import tech.dokus.domain.enums.BankTransactionStatus
import tech.dokus.domain.enums.IgnoredReason
import tech.dokus.domain.ids.BankTransactionId
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.model.BalanceHistoryResponse
import tech.dokus.domain.model.BankAccountSummaryDto
import tech.dokus.domain.model.BankAccountDto
import tech.dokus.domain.model.BankTransactionDto
import tech.dokus.domain.model.BankTransactionSummaryDto
import tech.dokus.domain.model.IgnoreTransactionRequest
import tech.dokus.domain.model.LinkTransactionRequest
import tech.dokus.domain.model.MarkTransferRequest
import tech.dokus.domain.model.common.PaginatedResponse
import tech.dokus.domain.routes.Banking

internal class BankingRemoteDataSourceImpl(
    private val httpClient: HttpClient,
    private val endpointProvider: DynamicDokusEndpointProvider,
) : BankingRemoteDataSource {

    override suspend fun listAccounts(): Result<List<BankAccountDto>> = runCatching {
        httpClient.get(Banking.Accounts()).body()
    }

    override suspend fun getAccountSummary(): Result<BankAccountSummaryDto> = runCatching {
        httpClient.get(Banking.AccountsSummary()).body()
    }

    override suspend fun listTransactions(
        status: BankTransactionStatus?,
        source: BankTransactionSource?,
        fromDate: LocalDate?,
        toDate: LocalDate?,
        limit: Int,
        offset: Int
    ): Result<PaginatedResponse<BankTransactionDto>> = runCatching {
        httpClient.get(
            Banking.Transactions(
                status = status,
                source = source,
                fromDate = fromDate,
                toDate = toDate,
                limit = limit,
                offset = offset
            )
        ).body()
    }

    override suspend fun getTransactionSummary(): Result<BankTransactionSummaryDto> = runCatching {
        httpClient.get(Banking.Transactions.Summary()).body()
    }

    override suspend fun getTransaction(
        transactionId: BankTransactionId
    ): Result<BankTransactionDto> = runCatching {
        val txRoute = Banking.Transactions()
        httpClient.get(Banking.Transactions.Id(parent = txRoute, id = transactionId.toString())).body()
    }

    override suspend fun linkTransaction(
        transactionId: BankTransactionId,
        cashflowEntryId: CashflowEntryId
    ): Result<BankTransactionDto> = runCatching {
        val txRoute = Banking.Transactions()
        val idRoute = Banking.Transactions.Id(parent = txRoute, id = transactionId.toString())
        httpClient.resourcePost(Banking.Transactions.Id.Link(parent = idRoute)) {
            contentType(ContentType.Application.Json)
            setBody(LinkTransactionRequest(cashflowEntryId = cashflowEntryId))
        }.body()
    }

    override suspend fun ignoreTransaction(
        transactionId: BankTransactionId,
        reason: IgnoredReason,
    ): Result<BankTransactionDto> = runCatching {
        val txRoute = Banking.Transactions()
        val idRoute = Banking.Transactions.Id(parent = txRoute, id = transactionId.toString())
        httpClient.resourcePost(Banking.Transactions.Id.Ignore(parent = idRoute)) {
            contentType(ContentType.Application.Json)
            setBody(IgnoreTransactionRequest(reason = reason))
        }.body()
    }

    override suspend fun confirmTransaction(
        transactionId: BankTransactionId
    ): Result<BankTransactionDto> = runCatching {
        val txRoute = Banking.Transactions()
        val idRoute = Banking.Transactions.Id(parent = txRoute, id = transactionId.toString())
        httpClient.resourcePost(Banking.Transactions.Id.Confirm(parent = idRoute)).body()
    }

    override suspend fun createExpenseFromTransaction(
        transactionId: BankTransactionId,
    ): Result<BankTransactionDto> = runCatching {
        val txRoute = Banking.Transactions()
        val idRoute = Banking.Transactions.Id(parent = txRoute, id = transactionId.toString())
        httpClient.resourcePost(Banking.Transactions.Id.CreateExpense(parent = idRoute)).body()
    }

    override suspend fun getBalanceHistory(days: Int): Result<BalanceHistoryResponse> = runCatching {
        httpClient.get(Banking.AccountsBalanceHistory(days = days)).body()
    }

    override suspend fun markTransfer(
        transactionId: BankTransactionId,
        request: MarkTransferRequest,
    ): Result<BankTransactionDto> = runCatching {
        val txRoute = Banking.Transactions()
        val idRoute = Banking.Transactions.Id(parent = txRoute, id = transactionId.toString())
        httpClient.resourcePost(Banking.Transactions.Id.MarkTransfer(parent = idRoute)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    override suspend fun undoTransfer(
        transactionId: BankTransactionId,
    ): Result<BankTransactionDto> = runCatching {
        val txRoute = Banking.Transactions()
        val idRoute = Banking.Transactions.Id(parent = txRoute, id = transactionId.toString())
        httpClient.resourcePost(Banking.Transactions.Id.UndoTransfer(parent = idRoute)).body()
    }
}
