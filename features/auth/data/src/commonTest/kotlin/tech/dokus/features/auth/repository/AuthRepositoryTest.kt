package tech.dokus.features.auth.repository

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.test.runTest
import tech.dokus.domain.Email
import tech.dokus.domain.Password
import tech.dokus.domain.ids.SessionId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.Address
import tech.dokus.domain.model.AvatarUploadResponse
import tech.dokus.domain.model.CreateTenantRequest
import tech.dokus.domain.model.Tenant
import tech.dokus.domain.model.TenantSettings
import tech.dokus.domain.model.UpsertTenantAddressRequest
import tech.dokus.domain.model.User
import tech.dokus.domain.model.auth.AuthEvent
import tech.dokus.domain.model.auth.ChangePasswordRequest
import tech.dokus.domain.model.auth.LoginRequest
import tech.dokus.domain.model.auth.LoginResponse
import tech.dokus.domain.model.auth.LogoutRequest
import tech.dokus.domain.model.auth.RefreshTokenRequest
import tech.dokus.domain.model.auth.RegisterRequest
import tech.dokus.domain.model.auth.ResetPasswordRequest
import tech.dokus.domain.model.auth.SessionDto
import tech.dokus.domain.model.common.Thumbnail
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
}

private class FakeTokenManager : TokenManagerMutable {
    override val isAuthenticated = MutableStateFlow(true)
    override var onTokenRefreshNeeded: (suspend (refreshToken: String, tenantId: TenantId?) -> LoginResponse?)? = null

    var accessToken: String? = null
    var refreshToken: String? = null
    var selectedTenantId: TenantId? = null
    var authenticationFailedCalled: Boolean = false

    override suspend fun initialize() = Unit

    override suspend fun saveTokens(loginResponse: LoginResponse) {
        accessToken = loginResponse.accessToken
        refreshToken = loginResponse.refreshToken
    }

    override suspend fun getValidAccessToken(): String? = accessToken

    override suspend fun getRefreshToken(): String? = refreshToken

    override suspend fun getSelectedTenantId(): TenantId? = selectedTenantId

    override suspend fun refreshToken(force: Boolean): String? = accessToken

    override suspend fun onAuthenticationFailed() {
        authenticationFailedCalled = true
    }

    override suspend fun getCurrentClaims() = null
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

    override suspend fun getCurrentUser(): Result<User> = Result.failure(IllegalStateException("not needed"))

    override suspend fun selectTenant(tenantId: TenantId): Result<LoginResponse> =
        Result.failure(IllegalStateException("not needed"))

    override suspend fun logout(request: LogoutRequest): Result<Unit> {
        lastLogoutRequest = request
        return Result.success(Unit)
    }

    override suspend fun updateProfile(request: tech.dokus.domain.model.auth.UpdateProfileRequest): Result<User> =
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
        imageBytes: ByteArray,
        filename: String,
        contentType: String,
        onProgress: (Float) -> Unit
    ): Result<AvatarUploadResponse> = Result.failure(IllegalStateException("not needed"))

    override suspend fun getAvatar(): Result<Thumbnail?> =
        Result.failure(IllegalStateException("not needed"))

    override suspend fun deleteAvatar(): Result<Unit> =
        Result.failure(IllegalStateException("not needed"))

    override suspend fun getInvoiceNumberPreview(): Result<String> =
        Result.failure(IllegalStateException("not needed"))
}
