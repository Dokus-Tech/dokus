package tech.dokus.features.auth.repository

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import tech.dokus.domain.DisplayName
import tech.dokus.domain.Email
import tech.dokus.domain.Password
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.FirmId
import tech.dokus.domain.ids.SessionId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.Address
import tech.dokus.domain.model.CreateTenantRequest
import tech.dokus.domain.model.Tenant
import tech.dokus.domain.model.TenantSettings
import tech.dokus.domain.model.UpsertTenantAddressRequest
import tech.dokus.domain.model.User
import tech.dokus.domain.model.auth.AccountMeResponse
import tech.dokus.domain.model.auth.AppSurface
import tech.dokus.domain.model.auth.AuthEvent
import tech.dokus.domain.model.auth.ChangePasswordRequest
import tech.dokus.domain.model.auth.ConsoleClientSummary
import tech.dokus.domain.model.auth.CreateFirmRequest
import tech.dokus.domain.model.auth.CreateFirmResponse
import tech.dokus.domain.model.auth.FirmWorkspaceSummary
import tech.dokus.domain.model.auth.LoginRequest
import tech.dokus.domain.model.auth.LoginResponse
import tech.dokus.domain.model.auth.LogoutRequest
import tech.dokus.domain.model.auth.RefreshTokenRequest
import tech.dokus.domain.model.auth.RegisterRequest
import tech.dokus.domain.model.auth.ResetPasswordRequest
import tech.dokus.domain.model.auth.SessionDto
import tech.dokus.domain.model.auth.SurfaceAvailability
import tech.dokus.domain.model.common.PaginatedResponse
import tech.dokus.domain.model.common.Thumbnail
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.features.auth.datasource.AccountRemoteDataSource
import tech.dokus.features.auth.datasource.IdentityRemoteDataSource
import tech.dokus.features.auth.datasource.TenantRemoteDataSource
import tech.dokus.features.auth.manager.AuthManagerMutable
import tech.dokus.features.auth.manager.TokenManagerMutable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthRepositoryTest {

    @Test
    fun logoutSendsAccessAndRefreshTokens() = runTest {
        val tokenManager = FakeTokenManager().apply {
            accessToken = "access-token"
            refreshToken = "refresh-token"
        }
        val authManager = FakeAuthManager()
        val account = FakeAccountRemoteDataSource()
        val identity = FakeIdentityRemoteDataSource()
        val tenant = FakeTenantRemoteDataSource()
        val repository = AuthRepository(tokenManager, authManager, account, identity, tenant)

        repository.logout()

        assertEquals(
            LogoutRequest(sessionToken = "access-token", refreshToken = "refresh-token"),
            account.lastLogoutRequest
        )
        assertTrue(tokenManager.authenticationFailedCalled)
        assertTrue(authManager.logoutCalled)
    }

    @Test
    fun authMethodsDelegateToCorrectDatasources() = runTest {
        val tokenManager = FakeTokenManager()
        val authManager = FakeAuthManager()
        val account = FakeAccountRemoteDataSource()
        val identity = FakeIdentityRemoteDataSource()
        val tenant = FakeTenantRemoteDataSource()
        val repository = AuthRepository(tokenManager, authManager, account, identity, tenant)

        val email = Email("test@dokus.app")
        val resetToken = "reset-token"
        val verifyToken = "verify-token"
        val sessionId = SessionId("00000000-0000-0000-0000-000000000111")
        val oldPassword = Password("OldPass123!")
        val newPassword = Password("NewPass123!")

        assertTrue(repository.requestPasswordReset(email).isSuccess)
        assertEquals(email, identity.lastResetRequestEmail)

        assertTrue(repository.resetPassword(resetToken, newPassword.value).isSuccess)
        assertEquals(resetToken, identity.lastResetPasswordToken)
        assertEquals(ResetPasswordRequest(newPassword.value), identity.lastResetPasswordRequest)

        assertTrue(repository.verifyEmail(verifyToken).isSuccess)
        assertEquals(verifyToken, identity.lastVerifyEmailToken)

        assertTrue(repository.resendVerificationEmail().isSuccess)
        assertTrue(account.resendVerificationCalled)

        assertTrue(repository.changePassword(oldPassword, newPassword).isSuccess)
        assertEquals(
            ChangePasswordRequest(currentPassword = oldPassword, newPassword = newPassword),
            account.lastChangePasswordRequest
        )

        assertTrue(repository.listSessions().isSuccess)
        assertTrue(account.listSessionsCalled)

        assertTrue(repository.revokeSession(sessionId).isSuccess)
        assertEquals(sessionId, account.lastRevokedSessionId)

        assertTrue(repository.revokeOtherSessions().isSuccess)
        assertTrue(account.revokeOtherSessionsCalled)
    }

    @Test
    fun getCurrentUserMapsUserFromAccountMePayload() = runTest {
        val tokenManager = FakeTokenManager()
        val authManager = FakeAuthManager()
        val account = FakeAccountRemoteDataSource().apply {
            accountMeResult = Result.success(
                AccountMeResponse(
                    user = sampleUser(),
                    surface = SurfaceAvailability(
                        canCompanyManager = false,
                        canBookkeeperConsole = true,
                        defaultSurface = AppSurface.BookkeeperConsole
                    )
                )
            )
        }
        val identity = FakeIdentityRemoteDataSource()
        val tenant = FakeTenantRemoteDataSource()
        val repository = AuthRepository(tokenManager, authManager, account, identity, tenant)

        val result = repository.getCurrentUser()

        assertTrue(result.isSuccess)
        assertEquals(sampleUser(), result.getOrThrow())
    }

    @Test
    fun getAccountMeReturnsSurfacePayload() = runTest {
        val tokenManager = FakeTokenManager()
        val authManager = FakeAuthManager()
        val expectedPayload = AccountMeResponse(
            user = sampleUser(),
            surface = SurfaceAvailability(
                canCompanyManager = true,
                canBookkeeperConsole = true,
                defaultSurface = AppSurface.CompanyManager
            )
        )
        val account = FakeAccountRemoteDataSource().apply {
            accountMeResult = Result.success(expectedPayload)
        }
        val identity = FakeIdentityRemoteDataSource()
        val tenant = FakeTenantRemoteDataSource()
        val repository = AuthRepository(tokenManager, authManager, account, identity, tenant)

        val result = repository.getAccountMe()

        assertTrue(result.isSuccess)
        assertEquals(expectedPayload, result.getOrThrow())
    }

    @Test
    fun getAccountMeForwardsFailure() = runTest {
        val tokenManager = FakeTokenManager()
        val authManager = FakeAuthManager()
        val error = RuntimeException("server error")
        val account = FakeAccountRemoteDataSource().apply {
            accountMeResult = Result.failure(error)
        }
        val identity = FakeIdentityRemoteDataSource()
        val tenant = FakeTenantRemoteDataSource()
        val repository = AuthRepository(tokenManager, authManager, account, identity, tenant)

        val result = repository.getAccountMe()

        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }

    @Test
    fun listConsoleClientsDelegatesToAccountDataSource() = runTest {
        val tokenManager = FakeTokenManager()
        val authManager = FakeAuthManager()
        val firmId = FirmId("00000000-0000-0000-0000-000000000111")
        val expectedClients = listOf(
            ConsoleClientSummary(
                tenantId = TenantId("00000000-0000-0000-0000-000000000222"),
                companyName = DisplayName("Invoid BV"),
                vatNumber = VatNumber("BE0792.140.667")
            ),
            ConsoleClientSummary(
                tenantId = TenantId("00000000-0000-0000-0000-000000000333"),
                companyName = DisplayName("PixelForge BV"),
                vatNumber = null
            ),
        )
        val account = FakeAccountRemoteDataSource().apply {
            consoleClientsResult = Result.success(expectedClients)
        }
        val identity = FakeIdentityRemoteDataSource()
        val tenant = FakeTenantRemoteDataSource()
        val repository = AuthRepository(tokenManager, authManager, account, identity, tenant)

        val result = repository.listConsoleClients(firmId)

        assertTrue(account.listConsoleClientsCalled)
        assertEquals(firmId, account.lastConsoleClientsFirmId)
        assertTrue(result.isSuccess)
        assertEquals(expectedClients, result.getOrThrow())
    }

    @Test
    fun listConsoleClientsForwardsFailure() = runTest {
        val tokenManager = FakeTokenManager()
        val authManager = FakeAuthManager()
        val firmId = FirmId("00000000-0000-0000-0000-000000000111")
        val error = RuntimeException("console unavailable")
        val account = FakeAccountRemoteDataSource().apply {
            consoleClientsResult = Result.failure(error)
        }
        val identity = FakeIdentityRemoteDataSource()
        val tenant = FakeTenantRemoteDataSource()
        val repository = AuthRepository(tokenManager, authManager, account, identity, tenant)

        val result = repository.listConsoleClients(firmId)

        assertTrue(account.listConsoleClientsCalled)
        assertEquals(firmId, account.lastConsoleClientsFirmId)
        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }

    @Test
    fun listConsoleClientsRefreshesAndRetriesOnNotAuthorized() = runTest {
        val tokenManager = FakeTokenManager().apply {
            accessToken = "old-access"
            refreshToken = "refresh-token"
            refreshTokenReturn = "new-access"
        }
        val authManager = FakeAuthManager()
        val firmId = FirmId("00000000-0000-0000-0000-000000000111")
        val expectedClients = listOf(
            ConsoleClientSummary(
                tenantId = TenantId("00000000-0000-0000-0000-000000000222"),
                companyName = DisplayName("Invoid BV"),
                vatNumber = VatNumber("BE0792.140.667")
            ),
        )
        val account = FakeAccountRemoteDataSource().apply {
            consoleClientsResultQueue += Result.failure(
                DokusException.NotAuthorized("You do not have access to this firm")
            )
            consoleClientsResultQueue += Result.success(expectedClients)
        }
        val identity = FakeIdentityRemoteDataSource()
        val tenant = FakeTenantRemoteDataSource()
        val repository = AuthRepository(tokenManager, authManager, account, identity, tenant)

        val result = repository.listConsoleClients(firmId)

        assertTrue(result.isSuccess)
        assertEquals(expectedClients, result.getOrThrow())
        assertEquals(2, account.listConsoleClientsCallCount)
        assertEquals(listOf(true), tokenManager.refreshTokenForceCalls)
    }

    @Test
    fun createFirmForcesTokenRefreshAfterSuccess() = runTest {
        val tokenManager = FakeTokenManager().apply {
            accessToken = "old-access"
            refreshToken = "refresh-token"
            refreshTokenReturn = "new-access"
        }
        val authManager = FakeAuthManager()
        val account = FakeAccountRemoteDataSource().apply {
            createFirmResult = Result.success(
                CreateFirmResponse(
                    firm = FirmWorkspaceSummary(
                        id = FirmId("00000000-0000-0000-0000-000000000111"),
                        name = DisplayName("Kantoor Boonen"),
                        vatNumber = VatNumber("BE0777887045"),
                        role = tech.dokus.domain.enums.FirmRole.Owner,
                        clientCount = 0
                    )
                )
            )
        }
        val identity = FakeIdentityRemoteDataSource()
        val tenant = FakeTenantRemoteDataSource()
        val repository = AuthRepository(tokenManager, authManager, account, identity, tenant)

        val result = repository.createFirm(
            CreateFirmRequest(
                name = DisplayName("Kantoor Boonen"),
                vatNumber = VatNumber("BE0777887045"),
            )
        )

        assertTrue(result.isSuccess)
        assertTrue(account.createFirmCalled)
        assertEquals(listOf(true), tokenManager.refreshTokenForceCalls)
    }
}

private class FakeTokenManager : TokenManagerMutable {
    override val isAuthenticated = MutableStateFlow(true)
    override var onTokenRefreshNeeded: (suspend (refreshToken: String, tenantId: TenantId?) -> LoginResponse?)? = null

    var accessToken: String? = null
    var refreshToken: String? = null
    var selectedTenantId: TenantId? = null
    var authenticationFailedCalled: Boolean = false
    var refreshTokenReturn: String? = null
    val refreshTokenForceCalls: MutableList<Boolean> = mutableListOf()

    override suspend fun initialize() = Unit

    override suspend fun saveTokens(loginResponse: LoginResponse) {
        accessToken = loginResponse.accessToken
        refreshToken = loginResponse.refreshToken
    }

    override suspend fun getValidAccessToken(): String? = accessToken

    override suspend fun getRefreshToken(): String? = refreshToken

    override suspend fun getSelectedTenantId(): TenantId? = selectedTenantId

    override suspend fun refreshToken(force: Boolean): String? {
        refreshTokenForceCalls += force
        return refreshTokenReturn ?: accessToken
    }

    override suspend fun onAuthenticationFailed() {
        authenticationFailedCalled = true
    }
}

private class FakeAuthManager : AuthManagerMutable {
    private val events = MutableSharedFlow<AuthEvent>(replay = 1)
    override val authenticationEvents: SharedFlow<AuthEvent> = events

    var logoutCalled = false

    override suspend fun onAuthenticationFailed() {
        events.emit(AuthEvent.ForceLogout)
    }

    override suspend fun onUserLogout() {
        logoutCalled = true
        events.emit(AuthEvent.UserLogout)
    }

    override suspend fun onLoginSuccess() {
        events.emit(AuthEvent.LoginSuccess)
    }
}

private class FakeAccountRemoteDataSource : AccountRemoteDataSource {
    var lastLogoutRequest: LogoutRequest? = null
    var resendVerificationCalled: Boolean = false
    var listSessionsCalled: Boolean = false
    var revokeOtherSessionsCalled: Boolean = false
    var lastRevokedSessionId: SessionId? = null
    var lastChangePasswordRequest: ChangePasswordRequest? = null
    var listConsoleClientsCalled: Boolean = false
    var listConsoleClientsCallCount: Int = 0
    var lastConsoleClientsFirmId: FirmId? = null
    var createFirmCalled: Boolean = false
    var lastCreateFirmRequest: CreateFirmRequest? = null
    var accountMeResult: Result<AccountMeResponse> = Result.failure(IllegalStateException("not needed"))
    var consoleClientsResult: Result<List<ConsoleClientSummary>> =
        Result.failure(IllegalStateException("not needed"))
    val consoleClientsResultQueue: MutableList<Result<List<ConsoleClientSummary>>> = mutableListOf()
    var createFirmResult: Result<CreateFirmResponse> =
        Result.failure(IllegalStateException("not needed"))

    override suspend fun getAccountMe(): Result<AccountMeResponse> = accountMeResult

    override suspend fun createFirm(request: CreateFirmRequest): Result<CreateFirmResponse> {
        createFirmCalled = true
        lastCreateFirmRequest = request
        return createFirmResult
    }

    override suspend fun listConsoleClients(firmId: FirmId): Result<List<ConsoleClientSummary>> {
        listConsoleClientsCalled = true
        listConsoleClientsCallCount += 1
        lastConsoleClientsFirmId = firmId
        return if (consoleClientsResultQueue.isNotEmpty()) {
            consoleClientsResultQueue.removeAt(0)
        } else {
            consoleClientsResult
        }
    }

    override suspend fun listConsoleClientDocuments(
        firmId: FirmId,
        tenantId: TenantId,
        page: Int,
        limit: Int
    ): Result<PaginatedResponse<DocumentRecordDto>> =
        Result.failure(IllegalStateException("not needed"))

    override suspend fun getConsoleClientDocument(
        firmId: FirmId,
        tenantId: TenantId,
        documentId: String
    ): Result<DocumentRecordDto> =
        Result.failure(IllegalStateException("not needed"))

    override suspend fun selectTenant(tenantId: TenantId): Result<LoginResponse> =
        Result.failure(IllegalStateException("not needed"))

    override suspend fun logout(request: LogoutRequest): Result<Unit> {
        lastLogoutRequest = request
        return Result.success(Unit)
    }

    override suspend fun updateProfile(request: tech.dokus.domain.model.auth.UpdateProfileRequest): Result<User> =
        Result.failure(IllegalStateException("not needed"))

    override suspend fun uploadUserAvatar(
        userId: UserId,
        imageBytes: ByteArray,
        filename: String,
        contentType: String,
        onProgress: (Float) -> Unit
    ): Result<Thumbnail> = Result.failure(IllegalStateException("not needed"))

    override suspend fun deleteUserAvatar(userId: UserId): Result<Unit> =
        Result.failure(IllegalStateException("not needed"))

    override suspend fun deactivateAccount(request: tech.dokus.domain.model.auth.DeactivateUserRequest): Result<Unit> =
        Result.failure(IllegalStateException("not needed"))

    override suspend fun resendVerificationEmail(): Result<Unit> {
        resendVerificationCalled = true
        return Result.success(Unit)
    }

    override suspend fun changePassword(request: ChangePasswordRequest): Result<Unit> {
        lastChangePasswordRequest = request
        return Result.success(Unit)
    }

    override suspend fun listSessions(): Result<List<SessionDto>> {
        listSessionsCalled = true
        return Result.success(emptyList())
    }

    override suspend fun revokeSession(sessionId: SessionId): Result<Unit> {
        lastRevokedSessionId = sessionId
        return Result.success(Unit)
    }

    override suspend fun revokeOtherSessions(): Result<Unit> {
        revokeOtherSessionsCalled = true
        return Result.success(Unit)
    }
}

private class FakeIdentityRemoteDataSource : IdentityRemoteDataSource {
    var lastResetRequestEmail: Email? = null
    var lastResetPasswordToken: String? = null
    var lastResetPasswordRequest: ResetPasswordRequest? = null
    var lastVerifyEmailToken: String? = null

    override suspend fun login(request: LoginRequest): Result<LoginResponse> =
        Result.failure(IllegalStateException("not needed"))

    override suspend fun register(request: RegisterRequest): Result<LoginResponse> =
        Result.failure(IllegalStateException("not needed"))

    override suspend fun refreshToken(request: RefreshTokenRequest): Result<LoginResponse> =
        Result.failure(IllegalStateException("not needed"))

    override suspend fun requestPasswordReset(email: Email): Result<Unit> {
        lastResetRequestEmail = email
        return Result.success(Unit)
    }

    override suspend fun resetPassword(resetToken: String, request: ResetPasswordRequest): Result<Unit> {
        lastResetPasswordToken = resetToken
        lastResetPasswordRequest = request
        return Result.success(Unit)
    }

    override suspend fun verifyEmail(token: String): Result<Unit> {
        lastVerifyEmailToken = token
        return Result.success(Unit)
    }
}

private class FakeTenantRemoteDataSource : TenantRemoteDataSource {
    override suspend fun listMyTenants(): Result<List<Tenant>> = Result.success(emptyList())
    override suspend fun createTenant(request: CreateTenantRequest): Result<Tenant> =
        Result.failure(IllegalStateException("not needed"))

    override suspend fun getTenant(id: TenantId): Result<Tenant> =
        Result.failure(IllegalStateException("not needed"))

    override suspend fun getTenantSettings(): Result<TenantSettings> =
        Result.failure(IllegalStateException("not needed"))

    override suspend fun updateTenantSettings(settings: TenantSettings): Result<Unit> =
        Result.failure(IllegalStateException("not needed"))

    override suspend fun getTenantAddress(): Result<Address?> =
        Result.failure(IllegalStateException("not needed"))

    override suspend fun upsertTenantAddress(request: UpsertTenantAddressRequest): Result<Address> =
        Result.failure(IllegalStateException("not needed"))

    override suspend fun uploadAvatar(
        tenantId: TenantId,
        imageBytes: ByteArray,
        filename: String,
        contentType: String,
        onProgress: (Float) -> Unit
    ): Result<Thumbnail> = Result.failure(IllegalStateException("not needed"))

    override suspend fun getAvatar(tenantId: TenantId): Result<Thumbnail?> =
        Result.failure(IllegalStateException("not needed"))

    override suspend fun deleteAvatar(tenantId: TenantId): Result<Unit> =
        Result.failure(IllegalStateException("not needed"))

    override suspend fun getInvoiceNumberPreview(): Result<String> =
        Result.failure(IllegalStateException("not needed"))
}

private fun sampleUser(): User = User(
    id = UserId("00000000-0000-0000-0000-000000000111"),
    email = Email("test@dokus.app"),
    firstName = null,
    lastName = null,
    emailVerified = true,
    isActive = true,
    lastLoginAt = null,
    createdAt = LocalDateTime(2026, 1, 1, 12, 0),
    updatedAt = LocalDateTime(2026, 1, 1, 12, 0)
)
