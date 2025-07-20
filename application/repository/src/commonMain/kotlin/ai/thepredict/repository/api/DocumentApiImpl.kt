package ai.thepredict.repository.api

import ai.thepredict.apispec.DocumentApi
import ai.thepredict.configuration.ServerEndpoint
import ai.thepredict.domain.model.Document
import ai.thepredict.domain.model.DocumentType
import ai.thepredict.domain.model.DocumentUploadResponse
import ai.thepredict.domain.model.PaginatedResponse
import ai.thepredict.repository.extensions.withAmountRange
import ai.thepredict.repository.extensions.withCompanyId
import ai.thepredict.repository.extensions.withDateRange
import ai.thepredict.repository.extensions.withDocumentType
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

class DocumentApiImpl(
    private val client: HttpClient,
) : DocumentApi {
    private val basePath = "/api/v1/documents"

    override suspend fun listDocuments(
        companyId: String,
        documentType: DocumentType?,
        supplierId: String?,
        dateFrom: String?,
        dateTo: String?,
        amountMin: Double?,
        amountMax: Double?,
        ids: List<String>?,
        page: Int,
        size: Int
    ): Result<PaginatedResponse<Document>> {
        return runCatching {
            client.get(basePath) {
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                withCompanyId(companyId)
                withDocumentType(documentType)
                withSupplierId(supplierId)
                withDateRange(dateFrom, dateTo)
                withAmountRange(amountMin, amountMax)
                withIds(ids)
                withPagination(page = page, size = size)
            }.body()
        }
    }

    override suspend fun uploadDocumentFile(
        companyId: String,
        fileBytes: ByteArray
    ): Result<DocumentUploadResponse> {
        return runCatching {
            client.submitFormWithBinaryData(
                url = "$basePath/upload",
                formData = formData {
                    append("file", fileBytes, Headers.build {
                        append(HttpHeaders.ContentDisposition, "filename=document")
                    })
                }
            ) {
                withCompanyId(companyId)
            }.body()
        }
    }

    override suspend fun getDocument(documentId: String, companyId: String): Result<Document> {
        return runCatching {
            client.get("$basePath/$documentId") {
                withCompanyId(companyId)
            }.body()
        }
    }

    override suspend fun deleteDocument(documentId: String, companyId: String): Result<Unit> {
        return runCatching {
            client.delete("$basePath/$documentId") {
                withCompanyId(companyId)
            }
        }
    }

    override suspend fun checkDocumentExists(
        documentId: String,
        companyId: String
    ): Result<Boolean> {
        return runCatching {
            val response = client.head("$basePath/$documentId") {
                withCompanyId(companyId)
            }
            response.status.value in 200..299
        }
    }
}

internal fun DocumentApi.Companion.create(
    httpClient: HttpClient,
    endpoint: ServerEndpoint
): DocumentApi {
    httpClient.config {
        install(DefaultRequest) {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            host = endpoint.host
        }
    }
    return DocumentApiImpl(
        client = httpClient,
    )
}