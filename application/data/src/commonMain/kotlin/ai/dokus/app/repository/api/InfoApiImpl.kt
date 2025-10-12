package ai.dokus.app.repository.api

import ai.dokus.foundation.apispec.InfoApi
import ai.dokus.foundation.domain.configuration.ServerEndpoint
import ai.dokus.foundation.domain.model.InfoSchema
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders

class InfoApiImpl(
    private val client: HttpClient,
) : InfoApi {
    private val basePath = "/api/v1"

    override suspend fun getApiInfo(): Result<InfoSchema> {
        return runCatching {
            client.get("$basePath/info").body()
        }
    }
}

internal fun InfoApi.Companion.create(httpClient: HttpClient, endpoint: ServerEndpoint): InfoApi {
    httpClient.config {
        install(DefaultRequest) {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            host = endpoint.host
        }
    }
    return InfoApiImpl(
        client = httpClient,
    )
}