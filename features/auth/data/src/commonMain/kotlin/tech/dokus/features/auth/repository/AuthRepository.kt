package tech.dokus.features.auth.repository

import kotlinx.coroutines.flow.StateFlow
import tech.dokus.domain.DisplayName
import tech.dokus.domain.Email
import tech.dokus.domain.LegalName
import tech.dokus.domain.Name
import tech.dokus.domain.enums.Language
import tech.dokus.domain.enums.SubscriptionTier
import tech.dokus.domain.enums.TenantType
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.CreateTenantRequest
import tech.dokus.domain.model.Tenant
import tech.dokus.domain.model.UpsertTenantAddressRequest
import tech.dokus.domain.model.User
import tech.dokus.domain.model.auth.DeactivateUserRequest
import tech.dokus.domain.model.auth.LoginRequest
import tech.dokus.domain.model.auth.LoginResponse
import tech.dokus.domain.model.auth.LogoutRequest
import tech.dokus.domain.model.auth.RefreshTokenRequest
import tech.dokus.domain.model.auth.RegisterRequest
import tech.dokus.domain.model.auth.ResetPasswordRequest
import tech.dokus.domain.model.auth.UpdateProfileRequest
import tech.dokus.features.auth.datasource.AccountRemoteDataSource
import tech.dokus.features.auth.datasource.IdentityRemoteDataSource
import tech.dokus.features.auth.datasource.TenantRemoteDataSource
import tech.dokus.features.auth.gateway.AuthGateway
import tech.dokus.features.auth.manager.AuthManagerMutable
import tech.dokus.features.auth.manager.TokenManagerMutable
import tech.dokus.foundation.platform.Logger

// Number of characters to show in email preview for logging (privacy)
private const val EmailPreviewLength = 3

/**
 * Repository for authentication operations.
 * Coordinates between TokenManager, AuthManager, and HTTP DataSources.
 *
 * This remains as a gateway because token lifecycle and multi-source orchestration
 * are centralized here and reused across auth use cases.
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
) : AuthGateway {
    private val logger = Logger.forClass<AuthRepository>()

    override val isAuthenticated: StateFlow<Boolean> = tokenManager.isAuthenticated

    init {
        // Set up token refresh callback
        tokenManager.onTokenRefreshNeeded = { refreshToken, tenantId ->
            refreshTokenInternal(refreshToken, tenantId)
        }
    }

    /**
     * Initialize auth repository - load stored tokens.
     */
    override suspend fun initialize() {
        logger.d { "Initializing auth repository" }
        tokenManager.initialize()
    }

    /**
     * Login with email and password.
     */
    override suspend fun login(request: LoginRequest): Result<Unit> {
        logger.d { "Login attempt for email: ${request.email.value.take(EmailPreviewLength)}***" }

        val response = identityDataSource.login(request).getOrElse { error ->
            logger.e(error) { "Login failed" }
            return Result.failure(error)
        }

        logger.i { "Login successful" }
        tokenManager.saveTokens(response)
        authManager.onLoginSuccess()
        return Result.success(Unit)
    }

    /**
     * Register a new user account.
     */
    override suspend fun register(request: RegisterRequest): Result<Unit> {
        logger.d { "Registration attempt for email: ${request.email.value.take(EmailPreviewLength)}***" }

        val response = identityDataSource.register(request).getOrElse { error ->
            logger.e(error) { "Registration failed" }
            return Result.failure(error)
        }

        logger.i { "Registration successful, auto-logging in" }
        tokenManager.saveTokens(response)
        authManager.onLoginSuccess()
        return Result.success(Unit)
    }

    /**
     * Select a tenant and refresh scoped tokens.
     */
    override suspend fun selectTenant(tenantId: TenantId): Result<Unit> {
        logger.d { "Selecting tenant: $tenantId" }

        val response = accountDataSource.selectTenant(tenantId).getOrElse { error ->
            logger.e(error) { "Tenant selection failed" }
            return Result.failure(error)
        }

        tokenManager.saveTokens(response)
        authManager.onLoginSuccess()
        return Result.success(Unit)
    }

    /**
     * Create a tenant with address and scope tokens to it.
     */
    @Suppress("LongParameterList") // All tenant creation parameters are required
    override suspend fun createTenant(
        type: TenantType,
        legalName: LegalName,
        displayName: DisplayName,
        plan: SubscriptionTier,
        language: Language,
        vatNumber: VatNumber,
        address: UpsertTenantAddressRequest,
    ): Result<Tenant> {
        logger.d { "Creating tenant: ${legalName.value}" }
        val request = CreateTenantRequest(
            type = type,
            legalName = legalName,
            displayName = displayName,
            subscription = plan,
            language = language,
            vatNumber = vatNumber,
            address = address,
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
    override suspend fun hasFreelancerTenant(): Result<Boolean> {
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
    override suspend fun getCurrentUser(): Result<User> {
        return accountDataSource.getCurrentUser()
            .onFailure { error ->
                logger.e(error) { "Failed to get current user" }
            }
    }

    /**
     * Update user profile (first name, last name).
     */
    override suspend fun updateProfile(firstName: Name?, lastName: Name?): Result<User> {
        logger.d { "Updating user profile" }
        val request = UpdateProfileRequest(
            firstName = firstName,
            lastName = lastName
        )
        return accountDataSource.updateProfile(request)
            .onSuccess {
                logger.i { "Profile updated successfully" }
            }
            .onFailure { error ->
                logger.e(error) { "Profile update failed" }
            }
    }

    /**
     * Logout current user.
     */
    override suspend fun logout() {
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
    override suspend fun requestPasswordReset(email: Email): Result<Unit> {
        logger.d { "Password reset requested for: ${email.value.take(EmailPreviewLength)}***" }
        return identityDataSource.requestPasswordReset(email)
            .onFailure { error ->
                logger.e(error) { "Password reset request failed" }
            }
    }

    /**
     * Reset password with a token.
     */
    override suspend fun resetPassword(resetToken: String, newPassword: String): Result<Unit> {
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
     *
     * Error handling:
     * - Returns LoginResponse on success
     * - Returns null for authentication failures (401, expired/invalid tokens)
     * - Throws for network errors (ConnectException, SocketTimeoutException, etc.)
     *
     * This allows TokenManager to distinguish between:
     * - Auth failures (should logout) → null return
     * - Network failures (should NOT logout) → exception thrown
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
            .getOrElse { e ->
                // Check if it's a network error - throw it so TokenManager doesn't logout
                if (tech.dokus.foundation.app.network.isNetworkException(e)) {
                    logger.w(e) { "Token refresh failed due to network error" }
                    throw e
                }
                // For auth errors (401, etc.), return null to signal auth failure
                logger.e(e) { "Token refresh failed due to auth error" }
                null
            }
    }
}
