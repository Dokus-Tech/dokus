package ai.thepredict.repository.api

import ai.thepredict.apispec.TransactionMatchingApi
import ai.thepredict.configuration.ServerEndpoint
import ai.thepredict.domain.model.MatchedSchema
import ai.thepredict.repository.extensions.withCompanyId
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders

class TransactionMatchingApiImpl(
    private val client: HttpClient,
) : TransactionMatchingApi {
    private val basePath = "/api/v1/transactions"

    override suspend fun getTransactionMatching(
        transactionId: String,
        companyId: String
    ): Result<MatchedSchema> {
        return runCatching {
            client.get("$basePath/$transactionId/matching") {
                withCompanyId(companyId)
            }.body()
        }
    }
}

internal fun TransactionMatchingApi.Companion.create(
    httpClient: HttpClient,
    endpoint: ServerEndpoint
): TransactionMatchingApi {
    httpClient.config {
        install(DefaultRequest) {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            host = endpoint.host
        }
    }
    return TransactionMatchingApiImpl(
        client = httpClient,
    )
}