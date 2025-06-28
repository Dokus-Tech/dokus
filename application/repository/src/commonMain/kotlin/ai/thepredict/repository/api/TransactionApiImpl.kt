package ai.thepredict.repository.api

import ai.thepredict.apispec.TransactionApi
import ai.thepredict.configuration.ServerEndpoint
import ai.thepredict.domain.model.Transaction
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders

class TransactionApiImpl(
    private val client: HttpClient,
) : TransactionApi {
    override suspend fun listTransactions(
        companyId: String,
        offset: Int,
        limit: Int
    ): List<Transaction> {
        return client.get("/companies/$companyId/transactions") {
            parameter("offset", offset)
            parameter("limit", limit)
        }.body()
    }

    override suspend fun uploadTransactionFile(companyId: String, fileBytes: ByteArray): List<Transaction> {
        return client.post("/companies/$companyId/transactions/upload") {
            setBody(
                MultiPartFormDataContent(
                formData {
                    append("file", fileBytes, Headers.build {
                        append(HttpHeaders.ContentDisposition, "filename=\"transactions.csv\"")
                    })
                }
            ))
        }.body()
    }

    override suspend fun getTransaction(transactionId: String, companyId: String): Transaction {
        return client.get("/companies/$companyId/transactions/$transactionId").body()
    }

    override suspend fun deleteTransaction(transactionId: String, companyId: String) {
        client.delete("/companies/$companyId/transactions/$transactionId")
    }

    override suspend fun checkTransactionExists(transactionId: String, companyId: String): Boolean {
        return client.get("/companies/$companyId/transactions/$transactionId/exists").body()
    }
}

internal fun TransactionApi.Companion.create(httpClient: HttpClient, endpoint: ServerEndpoint): TransactionApi {
    httpClient.config {
        install(DefaultRequest) {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            host = endpoint.externalHost
        }
    }
    return TransactionApiImpl(
        client = httpClient,
    )
}