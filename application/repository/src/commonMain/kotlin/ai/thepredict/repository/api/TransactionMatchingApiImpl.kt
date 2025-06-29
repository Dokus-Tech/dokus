package ai.thepredict.repository.api

import ai.thepredict.apispec.TransactionMatchingApi
import ai.thepredict.configuration.ServerEndpoint
import ai.thepredict.domain.model.MatchedSchema
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
    override suspend fun getTransactionMatching(transactionId: String, companyId: String): MatchedSchema {
        return client.get("/api/v1/transactions/$transactionId/matching") {
            header("X-Company-ID", companyId)
        }.body()
    }
}

internal fun TransactionMatchingApi.Companion.create(httpClient: HttpClient, endpoint: ServerEndpoint): TransactionMatchingApi {
    httpClient.config {
        install(DefaultRequest) {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            host = endpoint.externalHost
        }
    }
    return TransactionMatchingApiImpl(
        client = httpClient,
    )
}