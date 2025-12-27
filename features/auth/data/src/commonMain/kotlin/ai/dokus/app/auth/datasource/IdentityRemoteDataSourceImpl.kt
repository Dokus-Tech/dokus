package ai.dokus.app.auth.datasource

import tech.dokus.domain.Email
import tech.dokus.domain.model.auth.LoginRequest
import tech.dokus.domain.model.auth.LoginResponse
import tech.dokus.domain.model.auth.RefreshTokenRequest
import tech.dokus.domain.model.auth.RegisterRequest
import tech.dokus.domain.model.auth.ResetPasswordRequest
import tech.dokus.domain.routes.Identity
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.patch
import io.ktor.client.plugins.resources.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * HTTP implementation of IdentityRemoteDataSource.
 * Uses Ktor HttpClient with type-safe routing to communicate with the identity service.
 */
internal class IdentityRemoteDataSourceImpl(
    private val httpClient: HttpClient,
) : IdentityRemoteDataSource {

    override suspend fun login(request: LoginRequest): Result<LoginResponse> {
        return runCatching {
            httpClient.post(Identity.Login()) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }
    }

    override suspend fun register(request: RegisterRequest): Result<LoginResponse> {
        return runCatching {
            httpClient.post(Identity.Register()) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }
    }

    override suspend fun refreshToken(request: RefreshTokenRequest): Result<LoginResponse> {
        return runCatching {
            httpClient.post(Identity.Refresh()) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }
    }

    override suspend fun requestPasswordReset(email: Email): Result<Unit> {
        return runCatching {
            httpClient.post(Identity.PasswordResets()) {
                contentType(ContentType.Application.Json)
                setBody(mapOf("email" to email))
            }
        }
    }

    override suspend fun resetPassword(resetToken: String, request: ResetPasswordRequest): Result<Unit> {
        return runCatching {
            httpClient.patch(Identity.PasswordResets.ByToken(
                parent = Identity.PasswordResets(),
                token = resetToken
            )) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    override suspend fun verifyEmail(token: String): Result<Unit> {
        return runCatching {
            httpClient.patch(Identity.EmailVerifications.ByToken(
                parent = Identity.EmailVerifications(),
                token = token
            )) {
                contentType(ContentType.Application.Json)
            }
        }
    }
}
