package ai.thepredict.repository.api

import ai.thepredict.apispec.TransactionExtractionApi
import ai.thepredict.domain.configuration.ServerEndpoint
import ai.thepredict.domain.model.Transaction
import ai.thepredict.repository.extensions.withCompanyId
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.request.delete
import io.ktor.client.request.head
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders

class TransactionExtractionApiImpl(
    private val client: HttpClient,
) : TransactionExtractionApi {
    private val basePath = "/api/v1/transactions"

    override suspend fun startTransactionExtraction(
        transactionId: String,
        companyId: String
    ): Result<Transaction> {
        return runCatching {
            client.post("$basePath/$transactionId/extraction") {
                withCompanyId(companyId)
            }.body()
        }
    }

    override suspend fun deleteTransactionExtraction(
        transactionId: String,
        companyId: String
    ): Result<Unit> {
        return runCatching {
            client.delete("$basePath/$transactionId/extraction") {
                withCompanyId(companyId)
            }
        }
    }

    override suspend fun checkTransactionExtractionExists(
        transactionId: String,
        companyId: String
    ): Result<Boolean> {
        return runCatching {
            val response = client.head("$basePath/$transactionId/extraction") {
                withCompanyId(companyId)
            }
            response.status.value in 200..299
        }
    }
}

internal fun TransactionExtractionApi.Companion.create(
    httpClient: HttpClient,
    endpoint: ServerEndpoint
): TransactionExtractionApi {
    httpClient.config {
        install(DefaultRequest) {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            host = endpoint.host
        }
    }
    return TransactionExtractionApiImpl(
        client = httpClient,
    )
}