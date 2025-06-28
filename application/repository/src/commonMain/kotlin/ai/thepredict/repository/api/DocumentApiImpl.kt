package ai.thepredict.repository.api

import ai.thepredict.apispec.DocumentApi
import ai.thepredict.configuration.ServerEndpoint
import ai.thepredict.domain.model.Document
import ai.thepredict.domain.model.DocumentType
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.request.delete
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
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
        offset: Int,
        limit: Int
    ): List<Document> {
        return client.get("/companies/$companyId/documents") {
            parameter("offset", offset)
            parameter("limit", limit)
            documentType?.let { parameter("type", it.name) }
        }.body()
    }

    override suspend fun uploadDocumentFile(companyId: String, fileBytes: ByteArray): Document {
        return client.submitFormWithBinaryData(
            url = "/companies/$companyId/documents/upload",
            formData = formData {
                append("file", fileBytes, Headers.build {
                    append(HttpHeaders.ContentDisposition, "filename=document")
                })
            }
        ).body()
    }

    override suspend fun getDocument(documentId: String, companyId: String): Document {
        return client.get("/companies/$companyId/documents/$documentId").body()
    }

    override suspend fun deleteDocument(documentId: String, companyId: String) {
        client.delete("/companies/$companyId/documents/$documentId")
    }

    override suspend fun checkDocumentExists(documentId: String, companyId: String): Boolean {
        return client.get("/companies/$companyId/documents/$documentId/exists").body()
    }
}

internal fun DocumentApi.Companion.create(httpClient: HttpClient, endpoint: ServerEndpoint): DocumentApi {
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