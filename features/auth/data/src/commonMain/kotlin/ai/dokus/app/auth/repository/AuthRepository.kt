package ai.dokus.app.auth.repository

import ai.dokus.app.auth.datasource.AccountRemoteDataSource
import ai.dokus.app.auth.datasource.IdentityRemoteDataSource
import ai.dokus.app.auth.datasource.TenantRemoteDataSource
import ai.dokus.app.auth.manager.AuthManagerMutable
import ai.dokus.app.auth.manager.TokenManagerMutable
import ai.dokus.foundation.domain.DisplayName
import ai.dokus.foundation.domain.Email
import ai.dokus.foundation.domain.LegalName
import ai.dokus.foundation.domain.enums.Language
import ai.dokus.foundation.domain.enums.TenantPlan
import ai.dokus.foundation.domain.enums.TenantType
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.ids.VatNumber
import ai.dokus.foundation.domain.model.CreateTenantRequest
import ai.dokus.foundation.domain.model.Tenant
import ai.dokus.foundation.domain.model.User
import ai.dokus.foundation.domain.model.auth.DeactivateUserRequest
import ai.dokus.foundation.domain.model.auth.LoginRequest
import ai.dokus.foundation.domain.model.auth.LoginResponse
import ai.dokus.foundation.domain.model.auth.LogoutRequest
import ai.dokus.foundation.domain.model.auth.RefreshTokenRequest
import ai.dokus.foundation.domain.model.auth.RegisterRequest
import ai.dokus.foundation.domain.model.auth.ResetPasswordRequest
import ai.dokus.foundation.platform.Logger
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository for authentication operations.
 * Coordinates between TokenManager, AuthManager, and HTTP DataSources.
 *
 * Error Handling:
 * - HTTP DataSource methods return Result<T>
 * - Repository propagates results to callers
 */
class AuthRepository(
    private val tokenManager: TokenManagerMutable,
    private val authManager: AuthManagerMutable,
    private val accountDataSource: AccountRemoteDataSource,
    private val identityDataSource: IdentityRemoteDataSource,
    private val tenantDataSource: TenantRemoteDataSource
) {
    private val logger = Logger.forClass<AuthRepository>()

    val isAuthenticated: StateFlow<Boolean> = tokenManager.isAuthenticated

    init {
        // Set up token refresh callback
        tokenManager.onTokenRefreshNeeded = { refreshToken, tenantId ->
            refreshTokenInternal(refreshToken, tenantId)
        }
    }

    /**
     * Initialize auth repository - load stored tokens.
     */
    suspend fun initialize() {
        logger.d { "Initializing auth repository" }
        tokenManager.initialize()
    }

    /**
     * Login with email and password.
     */
    suspend fun login(request: LoginRequest): Result<Unit> {
        logger.d { "Login attempt for email: ${request.email.value.take(3)}***" }

        return identityDataSource.login(request)
            .onSuccess { response ->
                logger.i { "Login successful" }
                tokenManager.saveTokens(response)
                authManager.onLoginSuccess()
            }
            .onFailure { error ->
                logger.e(error) { "Login failed" }
            }
            .map { }
    }

    /**
     * Register a new user account.
     */
    suspend fun register(request: RegisterRequest): Result<Unit> {
        logger.d { "Registration attempt for email: ${request.email.value.take(3)}***" }

        return identityDataSource.register(request)
            .onSuccess { response ->
                logger.i { "Registration successful, auto-logging in" }
                tokenManager.saveTokens(response)
                authManager.onLoginSuccess()
            }
            .onFailure { error ->
                logger.e(error) { "Registration failed" }
            }
            .map { }
    }

    /**
     * Select a tenant and refresh scoped tokens.
     */
    suspend fun selectTenant(tenantId: TenantId): Result<Unit> {
        logger.d { "Selecting tenant: $tenantId" }

        return accountDataSource.selectTenant(tenantId)
            .onSuccess { response ->
                tokenManager.saveTokens(response)
                authManager.onLoginSuccess()
            }
            .onFailure { error ->
                logger.e(error) { "Tenant selection failed" }
            }
            .map { }
    }

    /**
     * Create a tenant and scope tokens to it.
     */
    suspend fun createTenant(
        type: TenantType,
        legalName: LegalName,
        displayName: DisplayName,
        plan: TenantPlan,
        language: Language,
        vatNumber: VatNumber
    ): Result<Tenant> {
        logger.d { "Creating tenant: ${legalName.value}" }
        val request = CreateTenantRequest(
            type = type,
            legalName = legalName,
            displayName = displayName,
            plan = plan,
            language = language,
            vatNumber = vatNumber
        )
        return tenantDataSource.createTenant(request)
            .onSuccess { tenant ->
                selectTenant(tenant.id).getOrThrow()
            }
            .onFailure { error ->
                logger.e(error) { "Tenant creation failed" }
            }
    }

    /**
     * Check if the current user already has a freelancer tenant.
     * Implemented by listing all tenants and filtering for freelancer type.
     */
    suspend fun hasFreelancerTenant(): Result<Boolean> {
        return tenantDataSource.listMyTenants()
            .map { tenants ->
                tenants.any { it.type == TenantType.Freelancer }
            }
            .onFailure { error ->
                logger.e(error) { "Failed to check freelancer tenant status" }
            }
    }

    /**
     * Get current user info.
     */
    suspend fun getCurrentUser(): Result<User> {
        return accountDataSource.getCurrentUser()
            .onFailure { error ->
                logger.e(error) { "Failed to get current user" }
            }
    }

    /**
     * Logout current user.
     */
    suspend fun logout() {
        logger.d { "Logging out user" }

        val token = tokenManager.getValidAccessToken()
        if (token != null) {
            val request = LogoutRequest(sessionToken = token)
            accountDataSource.logout(request)
                .onFailure { e ->
                    logger.w(e) { "Logout API call failed, clearing local tokens anyway" }
                }
        }

        tokenManager.onAuthenticationFailed()
        authManager.onUserLogout()
    }

    /**
     * Request password reset email.
     */
    suspend fun requestPasswordReset(email: Email): Result<Unit> {
        logger.d { "Password reset requested for: ${email.value.take(3)}***" }
        return identityDataSource.requestPasswordReset(email)
            .onFailure { error ->
                logger.e(error) { "Password reset request failed" }
            }
    }

    /**
     * Reset password with a token.
     */
    suspend fun resetPassword(resetToken: String, newPassword: String): Result<Unit> {
        logger.d { "Resetting password with token" }
        val request = ResetPasswordRequest(newPassword = newPassword)
        return identityDataSource.resetPassword(resetToken, request)
            .onFailure { error ->
                logger.e(error) { "Password reset failed" }
            }
    }

    /**
     * Deactivate current user account.
     */
    suspend fun deactivateAccount(reason: String? = null): Result<Unit> {
        logger.d { "Deactivating account" }
        val request = DeactivateUserRequest(reason = reason ?: "User requested")
        return accountDataSource.deactivateAccount(request)
            .onFailure { error ->
                logger.e(error) { "Account deactivation failed" }
            }
    }

    /**
     * Internal token refresh implementation.
     */
    private suspend fun refreshTokenInternal(
        refreshToken: String,
        tenantId: TenantId?
    ): LoginResponse? {
        logger.d { "Refreshing access token" }

        val request = RefreshTokenRequest(
            refreshToken = refreshToken,
            tenantId = tenantId
        )
        return identityDataSource.refreshToken(request)
            .onSuccess {
                logger.i { "Token refreshed successfully" }
            }
            .onFailure { e ->
                logger.e(e) { "Token refresh error" }
            }
            .getOrNull()
    }
}
