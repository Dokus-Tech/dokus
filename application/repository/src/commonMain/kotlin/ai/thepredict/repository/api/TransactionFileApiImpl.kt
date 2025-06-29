package ai.thepredict.repository.api

import ai.thepredict.apispec.TransactionFileApi
import ai.thepredict.configuration.ServerEndpoint
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders

class TransactionFileApiImpl(
    private val client: HttpClient,
) : TransactionFileApi {
    override suspend fun getTransactionFileUrl(transactionId: String, companyId: String): String {
        return client.get("/api/v1/transactions/$transactionId/file/url") {
            header("X-Company-ID", companyId)
        }.body()
    }

    override suspend fun deleteTransactionFile(transactionId: String, companyId: String) {
        client.delete("/api/v1/transactions/$transactionId/file") {
            header("X-Company-ID", companyId)
        }
    }
}

internal fun TransactionFileApi.Companion.create(httpClient: HttpClient, endpoint: ServerEndpoint): TransactionFileApi {
    httpClient.config {
        install(DefaultRequest) {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            host = endpoint.externalHost
        }
    }
    return TransactionFileApiImpl(
        client = httpClient,
    )
}