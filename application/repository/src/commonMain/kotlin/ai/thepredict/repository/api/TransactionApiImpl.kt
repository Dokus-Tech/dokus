package ai.thepredict.repository.api

import ai.thepredict.apispec.TransactionApi
import ai.thepredict.domain.configuration.ServerEndpoint
import ai.thepredict.domain.model.Transaction
import ai.thepredict.domain.model.PaginatedResponse
import ai.thepredict.domain.model.TransactionUploadResponse
import ai.thepredict.repository.extensions.withAmountRange
import ai.thepredict.repository.extensions.withCompanyId
import ai.thepredict.repository.extensions.withDateRange
import ai.thepredict.repository.extensions.withIds
import ai.thepredict.repository.extensions.withPagination
import ai.thepredict.repository.extensions.withSupplierId
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.request.delete
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders

class TransactionApiImpl(
    private val client: HttpClient,
) : TransactionApi {
    private val basePath = "/api/v1/transactions"

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
    ): Result<PaginatedResponse<Transaction>> {
        return runCatching {
            client.get(basePath) {
                withCompanyId(companyId)
                withSupplierId(supplierId)
                withDateRange(dateFrom, dateTo)
                withAmountRange(amountMin, amountMax)
                withIds(ids)
                withPagination(page = page, size = size)
            }.body()
        }
    }

    override suspend fun uploadTransactionFile(
        companyId: String,
        fileBytes: ByteArray
    ): Result<TransactionUploadResponse> {
        return runCatching {
            client.submitFormWithBinaryData(
                url = "$basePath/upload",
                formData = formData {
                    append("file", fileBytes, Headers.build {
                        append(HttpHeaders.ContentDisposition, "filename=\"transactions.csv\"")
                    })
                }
            ) {
                withCompanyId(companyId)
            }.body()
        }
    }

    override suspend fun getTransaction(
        transactionId: String,
        companyId: String
    ): Result<Transaction> {
        return runCatching {
            client.get("$basePath/$transactionId") {
                withCompanyId(companyId)
            }.body()
        }
    }

    override suspend fun deleteTransaction(transactionId: String, companyId: String): Result<Unit> {
        return runCatching {
            client.delete("$basePath/$transactionId") {
                withCompanyId(companyId)
            }
        }
    }

    override suspend fun checkTransactionExists(
        transactionId: String,
        companyId: String
    ): Result<Boolean> {
        return runCatching {
            val response = client.head("$basePath/$transactionId") {
                withCompanyId(companyId)
            }
            response.status.value in 200..299
        }
    }
}

internal fun TransactionApi.Companion.create(
    httpClient: HttpClient,
    endpoint: ServerEndpoint
): TransactionApi {
    httpClient.config {
        install(DefaultRequest) {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            host = endpoint.host
        }
    }
    return TransactionApiImpl(
        client = httpClient,
    )
}