package ai.dokus.app.repository.api

import ai.dokus.foundation.apispec.DocumentFileApi
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

class DocumentFileApiImpl(
    private val client: HttpClient,
) : DocumentFileApi {
    private val basePath = "/api/v1/documents"

    override suspend fun getDocumentFileUrl(documentId: String, companyId: String): Result<String> {
        return runCatching {
            client.get("$basePath/$documentId/file") {
                withCompanyId(companyId)
            }.body()
        }
    }

    override suspend fun deleteDocumentFile(documentId: String, companyId: String): Result<Unit> {
        return runCatching {
            client.delete("$basePath/$documentId/file") {
                withCompanyId(companyId)
            }
        }
    }
}

internal fun DocumentFileApi.Companion.create(
    httpClient: HttpClient,
    endpoint: ServerEndpoint
): DocumentFileApi {
    httpClient.config {
        install(DefaultRequest) {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            host = endpoint.host
        }
    }
    return DocumentFileApiImpl(
        client = httpClient,
    )
}