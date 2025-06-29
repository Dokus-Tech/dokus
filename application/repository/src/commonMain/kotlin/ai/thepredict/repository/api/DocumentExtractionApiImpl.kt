package ai.thepredict.repository.api

import ai.thepredict.apispec.DocumentExtractionApi
import ai.thepredict.configuration.ServerEndpoint
import ai.thepredict.domain.model.Document
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders

class DocumentExtractionApiImpl(
    private val client: HttpClient,
) : DocumentExtractionApi {
    override suspend fun startDocumentExtraction(documentId: String, companyId: String): Document {
        return client.post("/api/v1/documents/$documentId/extraction") {
            header("X-Company-ID", companyId)
        }.body()
    }

    override suspend fun deleteDocumentExtraction(documentId: String, companyId: String) {
        client.delete("/api/v1/documents/$documentId/extraction") {
            header("X-Company-ID", companyId)
        }
    }

    override suspend fun checkDocumentExtractionExists(documentId: String, companyId: String): Boolean {
        val response = client.head("/api/v1/documents/$documentId/extraction") {
            header("X-Company-ID", companyId)
        }
        return response.status.value in 200..299
    }
}

internal fun DocumentExtractionApi.Companion.create(httpClient: HttpClient, endpoint: ServerEndpoint): DocumentExtractionApi {
    httpClient.config {
        install(DefaultRequest) {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            host = endpoint.externalHost
        }
    }
    return DocumentExtractionApiImpl(
        client = httpClient,
    )
}