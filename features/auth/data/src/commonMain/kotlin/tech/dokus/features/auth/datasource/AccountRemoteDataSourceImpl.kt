package tech.dokus.features.auth.datasource

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.delete
import io.ktor.client.plugins.resources.get
import io.ktor.client.plugins.resources.patch
import io.ktor.client.plugins.resources.post
import io.ktor.client.plugins.resources.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import tech.dokus.domain.ids.SessionId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.User
import tech.dokus.domain.model.auth.AccountMeResponse
import tech.dokus.domain.model.auth.ChangePasswordRequest
import tech.dokus.domain.model.auth.DeactivateUserRequest
import tech.dokus.domain.model.auth.LoginResponse
import tech.dokus.domain.model.auth.LogoutRequest
import tech.dokus.domain.model.auth.SelectTenantRequest
import tech.dokus.domain.model.auth.SessionDto
import tech.dokus.domain.model.auth.UpdateProfileRequest
import tech.dokus.domain.routes.Account

/**
 * HTTP implementation of AccountRemoteDataSource.
 * Uses authenticated Ktor HttpClient with type-safe routing to communicate with the account service.
 */
internal class AccountRemoteDataSourceImpl(
    private val httpClient: HttpClient,
) : AccountRemoteDataSource {

    override suspend fun getAccountMe(): Result<AccountMeResponse> {
        return runCatching {
            httpClient.get(Account.Me()).body()
        }
    }

    override suspend fun getCurrentUser(): Result<User> {
        return getAccountMe().map { it.user }
    }

    override suspend fun selectTenant(tenantId: TenantId): Result<LoginResponse> {
        return runCatching {
            httpClient.put(Account.ActiveTenant()) {
                contentType(ContentType.Application.Json)
                setBody(SelectTenantRequest(tenantId = tenantId))
            }.body()
        }
    }

    override suspend fun logout(request: LogoutRequest): Result<Unit> {
        return runCatching {
            httpClient.post(Account.Logout()) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    override suspend fun updateProfile(request: UpdateProfileRequest): Result<User> {
        return runCatching {
            httpClient.patch(Account.Profile()) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }
    }

    override suspend fun deactivateAccount(request: DeactivateUserRequest): Result<Unit> {
        return runCatching {
            httpClient.post(Account.Deactivate()) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    override suspend fun resendVerificationEmail(): Result<Unit> {
        return runCatching {
            httpClient.post(Account.EmailVerifications()) {
                contentType(ContentType.Application.Json)
            }
        }
    }

    override suspend fun changePassword(request: ChangePasswordRequest): Result<Unit> {
        return runCatching {
            httpClient.post(Account.Password()) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    override suspend fun listSessions(): Result<List<SessionDto>> {
        return runCatching {
            httpClient.get(Account.Sessions()).body()
        }
    }

    override suspend fun revokeSession(sessionId: SessionId): Result<Unit> {
        return runCatching {
            httpClient.delete(
                Account.Sessions.ById(
                    parent = Account.Sessions(),
                    sessionId = sessionId
                )
            )
        }
    }

    override suspend fun revokeOtherSessions(): Result<Unit> {
        return runCatching {
            httpClient.post(Account.Sessions.RevokeOthers()) {
                contentType(ContentType.Application.Json)
            }
        }
    }
}
