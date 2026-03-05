package tech.dokus.features.auth.datasource

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.delete
import io.ktor.client.plugins.resources.get
import io.ktor.client.plugins.resources.patch
import io.ktor.client.plugins.resources.post
import io.ktor.client.plugins.resources.put
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import tech.dokus.domain.ids.SessionId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.FirmId
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.model.User
import tech.dokus.domain.model.common.PaginatedResponse
import tech.dokus.domain.model.auth.AccountMeResponse
import tech.dokus.domain.model.auth.ChangePasswordRequest
import tech.dokus.domain.model.auth.ConsoleClientSummary
import tech.dokus.domain.model.auth.CreateFirmRequest
import tech.dokus.domain.model.auth.CreateFirmResponse
import tech.dokus.domain.model.auth.DeactivateUserRequest
import tech.dokus.domain.model.auth.LoginResponse
import tech.dokus.domain.model.auth.LogoutRequest
import tech.dokus.domain.model.auth.SelectTenantRequest
import tech.dokus.domain.model.auth.SessionDto
import tech.dokus.domain.model.auth.UpdateProfileRequest
import tech.dokus.domain.routes.Account
import tech.dokus.domain.routes.Console
import tech.dokus.domain.routes.Firms

/**
 * HTTP implementation of AccountRemoteDataSource.
 * Uses authenticated Ktor HttpClient with type-safe routing to communicate with the account service.
 */
internal class AccountRemoteDataSourceImpl(
    private val httpClient: HttpClient,
) : AccountRemoteDataSource {

    private companion object {
        const val FirmHeaderName = "X-Firm-Id"
    }

    override suspend fun getAccountMe(): Result<AccountMeResponse> {
        return runCatching {
            httpClient.get(Account.Me()).body()
        }
    }

    override suspend fun createFirm(request: CreateFirmRequest): Result<CreateFirmResponse> {
        return runCatching {
            httpClient.post(Firms.Create()) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }
    }

    override suspend fun listConsoleClients(firmId: FirmId): Result<List<ConsoleClientSummary>> {
        return runCatching {
            httpClient.get(Console.Clients()) {
                header(FirmHeaderName, firmId.toString())
            }.body()
        }
    }

    override suspend fun listConsoleClientDocuments(
        firmId: FirmId,
        tenantId: TenantId,
        page: Int,
        limit: Int
    ): Result<PaginatedResponse<DocumentRecordDto>> {
        return runCatching {
            httpClient.get(
                Console.Client.Documents(
                    parent = Console.Client(tenantId = tenantId),
                    page = page,
                    limit = limit
                )
            ) {
                header(FirmHeaderName, firmId.toString())
            }.body()
        }
    }

    override suspend fun getConsoleClientDocument(
        firmId: FirmId,
        tenantId: TenantId,
        documentId: String
    ): Result<DocumentRecordDto> {
        return runCatching {
            httpClient.get(
                Console.Client.Document(
                    parent = Console.Client(tenantId = tenantId),
                    documentId = documentId
                )
            ) {
                header(FirmHeaderName, firmId.toString())
            }.body()
        }
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
