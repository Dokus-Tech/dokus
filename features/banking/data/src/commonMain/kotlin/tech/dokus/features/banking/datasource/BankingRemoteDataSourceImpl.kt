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
import tech.dokus.domain.ids.BankTransactionId
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.model.BankAccountSummary
import tech.dokus.domain.model.BankConnectionDto
import tech.dokus.domain.model.BankTransactionDto
import tech.dokus.domain.model.BankTransactionSummary
import tech.dokus.domain.model.LinkTransactionRequest
import tech.dokus.domain.model.common.PaginatedResponse
import tech.dokus.domain.routes.Banking

internal class BankingRemoteDataSourceImpl(
    private val httpClient: HttpClient,
    private val endpointProvider: DynamicDokusEndpointProvider,
) : BankingRemoteDataSource {

    override suspend fun listConnections(): Result<List<BankConnectionDto>> = runCatching {
        httpClient.get(Banking.Accounts()).body()
    }

    override suspend fun getAccountSummary(): Result<BankAccountSummary> = runCatching {
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

    override suspend fun getTransactionSummary(): Result<BankTransactionSummary> = runCatching {
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
        transactionId: BankTransactionId
    ): Result<BankTransactionDto> = runCatching {
        val txRoute = Banking.Transactions()
        val idRoute = Banking.Transactions.Id(parent = txRoute, id = transactionId.toString())
        httpClient.resourcePost(Banking.Transactions.Id.Ignore(parent = idRoute)).body()
    }

    override suspend fun confirmTransaction(
        transactionId: BankTransactionId
    ): Result<BankTransactionDto> = runCatching {
        val txRoute = Banking.Transactions()
        val idRoute = Banking.Transactions.Id(parent = txRoute, id = transactionId.toString())
        httpClient.resourcePost(Banking.Transactions.Id.Confirm(parent = idRoute)).body()
    }
}
