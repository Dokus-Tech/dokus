package ai.thepredict.repository.api

import ai.thepredict.apispec.TransactionFileApi
import ai.thepredict.configuration.ServerEndpoint
import ai.thepredict.repository.extensions.withCompanyId
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
    private val basePath = "/api/v1/transactions"

    override suspend fun getTransactionFileUrl(transactionId: String, companyId: String): String {
        return client.get("$basePath/$transactionId/file/url") {
            withCompanyId(companyId)
        }.body()
    }

    override suspend fun deleteTransactionFile(transactionId: String, companyId: String) {
        client.delete("$basePath/$transactionId/file") {
            withCompanyId(companyId)
        }
    }
}

internal fun TransactionFileApi.Companion.create(
    httpClient: HttpClient,
    endpoint: ServerEndpoint
): TransactionFileApi {
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