package ai.thepredict.repository.api

import ai.thepredict.apispec.DocumentExtractionApi
import ai.thepredict.domain.configuration.ServerEndpoint
import ai.thepredict.domain.model.Document
import ai.thepredict.repository.extensions.withCompanyId
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.request.delete
import io.ktor.client.request.head
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders

class DocumentExtractionApiImpl(
    private val client: HttpClient,
) : DocumentExtractionApi {
    private val basePath = "/api/v1/documents"

    override suspend fun startDocumentExtraction(
        documentId: String,
        companyId: String
    ): Result<Document> {
        return runCatching {
            client.post("$basePath/$documentId/extraction") {
                withCompanyId(companyId)
            }.body()
        }
    }

    override suspend fun deleteDocumentExtraction(
        documentId: String,
        companyId: String
    ): Result<Unit> {
        return runCatching {
            client.delete("$basePath/$documentId/extraction") {
                withCompanyId(companyId)
            }
        }
    }

    override suspend fun checkDocumentExtractionExists(
        documentId: String,
        companyId: String
    ): Result<Boolean> {
        return runCatching {
            val response = client.head("$basePath/$documentId/extraction") {
                withCompanyId(companyId)
            }
            response.status.value in 200..299
        }
    }
}

internal fun DocumentExtractionApi.Companion.create(
    httpClient: HttpClient,
    endpoint: ServerEndpoint
): DocumentExtractionApi {
    httpClient.config {
        install(DefaultRequest) {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            host = endpoint.host
        }
    }
    return DocumentExtractionApiImpl(
        client = httpClient,
    )
}