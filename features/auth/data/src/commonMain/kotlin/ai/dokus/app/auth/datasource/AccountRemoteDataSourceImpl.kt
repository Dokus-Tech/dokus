package ai.dokus.app.auth.datasource

import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.model.User
import ai.dokus.foundation.domain.model.auth.DeactivateUserRequest
import ai.dokus.foundation.domain.model.auth.LoginResponse
import ai.dokus.foundation.domain.model.auth.LogoutRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * HTTP implementation of AccountRemoteDataSource.
 * Uses authenticated Ktor HttpClient to communicate with the account service.
 */
internal class AccountRemoteDataSourceImpl(
    private val httpClient: HttpClient,
) : AccountRemoteDataSource {

    override suspend fun getCurrentUser(): Result<User> {
        return runCatching {
            httpClient.get("/api/v1/account/me").body()
        }
    }

    override suspend fun selectTenant(tenantId: TenantId): Result<LoginResponse> {
        return runCatching {
            httpClient.post("/api/v1/account/select-tenant") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("tenantId" to tenantId))
            }.body()
        }
    }

    override suspend fun logout(request: LogoutRequest): Result<Unit> {
        return runCatching {
            httpClient.post("/api/v1/account/logout") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    override suspend fun deactivateAccount(request: DeactivateUserRequest): Result<Unit> {
        return runCatching {
            httpClient.post("/api/v1/account/deactivate") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    override suspend fun resendVerificationEmail(): Result<Unit> {
        return runCatching {
            httpClient.post("/api/v1/account/resend-verification") {
                contentType(ContentType.Application.Json)
            }
        }
    }
}
