package ai.thepredict.repository.api

import ai.thepredict.apispec.TransactionApi
import ai.thepredict.configuration.ServerEndpoint
import ai.thepredict.domain.model.Transaction
import ai.thepredict.domain.model.PaginatedResponse
import ai.thepredict.domain.model.TransactionUploadResponse
import ai.thepredict.repository.extensions.withCompanyId
import ai.thepredict.repository.extensions.withPagination
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.head
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
        supplierId: String?,
        dateFrom: String?,
        dateTo: String?,
        amountMin: Double?,
        amountMax: Double?,
        ids: List<String>?,
        page: Int,
        size: Int
    ): PaginatedResponse<Transaction> {
        return client.get("/api/v1/transactions") {
            withCompanyId(companyId)
            supplierId?.let { parameter("supplier_id", it) }
            dateFrom?.let { parameter("date_from", it) }
            dateTo?.let { parameter("date_to", it) }
            amountMin?.let { parameter("amount_min", it) }
            amountMax?.let { parameter("amount_max", it) }
            ids?.let { parameter("ids", it.joinToString(",")) }
            withPagination(page = page, size = size)
        }.body()
    }

    override suspend fun uploadTransactionFile(
        companyId: String,
        fileBytes: ByteArray
    ): TransactionUploadResponse {
        return client.submitFormWithBinaryData(
            url = "/api/v1/transactions/upload",
            formData = formData {
                append("file", fileBytes, Headers.build {
                    append(HttpHeaders.ContentDisposition, "filename=\"transactions.csv\"")
                })
            }
        ) {
            withCompanyId(companyId)
        }.body()
    }

    override suspend fun getTransaction(transactionId: String, companyId: String): Transaction {
        return client.get("/api/v1/transactions/$transactionId") {
            withCompanyId(companyId)
        }.body()
    }

    override suspend fun deleteTransaction(transactionId: String, companyId: String) {
        client.delete("/api/v1/transactions/$transactionId") {
            withCompanyId(companyId)
        }
    }

    override suspend fun checkTransactionExists(transactionId: String, companyId: String): Boolean {
        val response = client.head("/api/v1/transactions/$transactionId") {
            withCompanyId(companyId)
        }
        return response.status.value in 200..299
    }
}

internal fun TransactionApi.Companion.create(
    httpClient: HttpClient,
    endpoint: ServerEndpoint
): TransactionApi {
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