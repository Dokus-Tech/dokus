package ai.thepredict.repository.api

import ai.thepredict.apispec.DocumentApi
import ai.thepredict.configuration.ServerEndpoint
import ai.thepredict.domain.model.Document
import ai.thepredict.domain.model.DocumentType
import ai.thepredict.domain.model.PaginatedResponse
import ai.thepredict.domain.model.DocumentUploadResponse
import ai.thepredict.repository.extensions.withCompanyId
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.request.delete
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders

class DocumentApiImpl(
    private val client: HttpClient,
) : DocumentApi {
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
    ): PaginatedResponse<Document> {
        return client.get("/api/v1/documents") {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            withCompanyId(companyId)
            documentType?.let { parameter("document_type", it.name) }
            supplierId?.let { parameter("supplier_id", it) }
            dateFrom?.let { parameter("date_from", it) }
            dateTo?.let { parameter("date_to", it) }
            amountMin?.let { parameter("amount_min", it) }
            amountMax?.let { parameter("amount_max", it) }
            ids?.let { parameter("ids", it.joinToString(",")) }
            parameter("page", page)
            parameter("size", size)
        }.body()
    }

    override suspend fun uploadDocumentFile(
        companyId: String,
        fileBytes: ByteArray
    ): DocumentUploadResponse {
        return client.submitFormWithBinaryData(
            url = "/api/v1/documents/upload",
            formData = formData {
                append("file", fileBytes, Headers.build {
                    append(HttpHeaders.ContentDisposition, "filename=document")
                })
            }
        ) {
            withCompanyId(companyId)
        }.body()
    }

    override suspend fun getDocument(documentId: String, companyId: String): Document {
        return client.get("/api/v1/documents/$documentId") {
            withCompanyId(companyId)
        }.body()
    }

    override suspend fun deleteDocument(documentId: String, companyId: String) {
        client.delete("/api/v1/documents/$documentId") {
            withCompanyId(companyId)
        }
    }

    override suspend fun checkDocumentExists(documentId: String, companyId: String): Boolean {
        val response = client.head("/api/v1/documents/$documentId") {
            withCompanyId(companyId)
        }
        return response.status.value in 200..299
    }
}

internal fun DocumentApi.Companion.create(
    httpClient: HttpClient,
    endpoint: ServerEndpoint
): DocumentApi {
    httpClient.config {
        install(DefaultRequest) {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            host = endpoint.externalHost
        }
    }
    return DocumentApiImpl(
        client = httpClient,
    )
}