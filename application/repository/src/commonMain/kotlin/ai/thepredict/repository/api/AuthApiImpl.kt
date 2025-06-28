package ai.thepredict.repository.api

import ai.thepredict.apispec.AuthApi
import ai.thepredict.configuration.ServerEndpoint
import ai.thepredict.domain.model.LoginRequest
import io.ktor.client.*
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

class AuthApiImpl(
    private val client: HttpClient,
) : AuthApi {
    override suspend fun login(request: LoginRequest): String {
        return client.post("/auth/login") {
            setBody(request)
        }.bodyAsText()
    }
}

internal fun AuthApi.Companion.create(httpClient: HttpClient, endpoint: ServerEndpoint): AuthApi {
    httpClient.config {
        install(DefaultRequest) {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            host = endpoint.externalHost
        }
    }
    return AuthApiImpl(
        client = httpClient,
    )
}