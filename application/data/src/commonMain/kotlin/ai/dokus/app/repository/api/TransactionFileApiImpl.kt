package ai.dokus.app.repository.api

import ai.dokus.foundation.apispec.TransactionFileApi
import ai.dokus.foundation.domain.configuration.ServerEndpoint
import ai.dokus.app.repository.extensions.withCompanyId
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import kotlin.Result

class TransactionFileApiImpl(
    private val client: HttpClient,
) : TransactionFileApi {
    private val basePath = "/api/v1/transactions"

    override suspend fun getTransactionFileUrl(
        transactionId: String,
        companyId: String
    ): Result<String> {
        return runCatching {
            client.get("$basePath/$transactionId/file") {
                withCompanyId(companyId)
            }.body()
        }
    }

    override suspend fun deleteTransactionFile(
        transactionId: String,
        companyId: String
    ): Result<Unit> {
        return runCatching {
            client.delete("$basePath/$transactionId/file") {
                withCompanyId(companyId)
            }
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
            host = endpoint.host
        }
    }
    return TransactionFileApiImpl(
        client = httpClient,
    )
}