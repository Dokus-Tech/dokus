package ai.thepredict.repository.api

import ai.thepredict.apispec.DocumentFileApi
import ai.thepredict.configuration.ServerEndpoint
import ai.thepredict.repository.extensions.withCompanyId
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
    override suspend fun getDocumentFileUrl(documentId: String, companyId: String): String {
        return client.get("/api/v1/documents/$documentId/file/url") {
            withCompanyId(companyId)
        }.body()
    }

    override suspend fun deleteDocumentFile(documentId: String, companyId: String) {
        client.delete("/api/v1/documents/$documentId/file") {
            withCompanyId(companyId)
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
            host = endpoint.externalHost
        }
    }
    return DocumentFileApiImpl(
        client = httpClient,
    )
}