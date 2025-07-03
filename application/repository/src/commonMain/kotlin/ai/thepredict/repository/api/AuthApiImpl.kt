package ai.thepredict.repository.api

import ai.thepredict.apispec.AuthApi
import ai.thepredict.configuration.ServerEndpoint
import ai.thepredict.domain.model.LoginRequest
import ai.thepredict.repository.extensions.bodyIfOk
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody

class AuthApiImpl(
    private val client: HttpClient,
) : AuthApi {
    override suspend fun login(request: LoginRequest): Result<String> {
        return runCatching {
            client.post("/api/v1/auth/login") {
                setBody(request)
            }.bodyIfOk<String>().trim('"')
        }
    }
}

internal fun AuthApi.Companion.create(httpClient: HttpClient, endpoint: ServerEndpoint): AuthApi {
    return AuthApiImpl(
        client = httpClient,
    )
}