package ai.dokus.app.auth.repository

import ai.dokus.app.auth.domain.AccountRemoteService
import ai.dokus.app.auth.domain.IdentityRemoteService
import ai.dokus.app.auth.domain.OrganizationRemoteService
import ai.dokus.app.auth.manager.AuthManagerMutable
import ai.dokus.app.auth.manager.TokenManagerMutable
import ai.dokus.foundation.domain.Email
import ai.dokus.foundation.domain.LegalName
import ai.dokus.foundation.domain.enums.Country
import ai.dokus.foundation.domain.enums.Language
import ai.dokus.foundation.domain.enums.OrganizationPlan
import ai.dokus.foundation.domain.ids.OrganizationId
import ai.dokus.foundation.domain.ids.VatNumber
import ai.dokus.foundation.domain.model.Organization
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
 * Coordinates between TokenManager, AuthManager, and AccountRemoteService.
 *
 * Error Handling:
 * - RPC methods throw exceptions on failure
 * - Repository catches exceptions and wraps them in Result<T> for internal use
 */
class AuthRepository(
    private val tokenManager: TokenManagerMutable,
    private val authManager: AuthManagerMutable,
    private val accountService: AccountRemoteService,
    private val identityService: IdentityRemoteService,
    private val organizationRemoteService: OrganizationRemoteService
) {
    private val logger = Logger.forClass<AuthRepository>()

    val isAuthenticated: StateFlow<Boolean> = tokenManager.isAuthenticated

    init {
        // Set up token refresh callback
        tokenManager.onTokenRefreshNeeded = { refreshToken, organizationId ->
            refreshTokenInternal(refreshToken, organizationId)
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
    suspend fun login(request: LoginRequest): Result<Unit> = runCatching {
        logger.d { "Login attempt for email: ${request.email.value.take(3)}***" }

        val response = identityService.login(request)
        logger.i { "Login successful" }
        tokenManager.saveTokens(response)
        authManager.onLoginSuccess()
    }.onFailure { error ->
        logger.e(error) { "Login failed" }
    }

    /**
     * Register a new user account.
     */
    suspend fun register(request: RegisterRequest): Result<Unit> = runCatching {
        logger.d { "Registration attempt for email: ${request.email.value.take(3)}***" }

        val response = identityService.register(request)
        logger.i { "Registration successful, auto-logging in" }
        tokenManager.saveTokens(response)
        authManager.onLoginSuccess()
    }.onFailure { error ->
        logger.e(error) { "Registration failed" }
    }

    /**
     * Select an organization and refresh scoped tokens.
     */
    suspend fun selectOrganization(organizationId: OrganizationId): Result<Unit> = runCatching {
        logger.d { "Selecting organization: $organizationId" }

        val response = accountService.selectOrganization(organizationId)
        tokenManager.saveTokens(response)
        authManager.onLoginSuccess()
    }.onFailure { error ->
        logger.e(error) { "Organization selection failed" }
    }

    /**
     * Create an organization and scope tokens to it.
     */
    suspend fun createOrganization(
        legalName: LegalName,
        email: Email,
        plan: OrganizationPlan,
        country: Country,
        language: Language,
        vatNumber: VatNumber
    ): Result<Organization> = runCatching {
        logger.d { "Creating organization: ${legalName.value}" }
        val organization = organizationRemoteService.createOrganization(
            legalName = legalName,
            email = email,
            plan = plan,
            country = country,
            language = language,
            vatNumber = vatNumber
        )
        selectOrganization(organization.id).getOrThrow()
        organization
    }.onFailure { error ->
        logger.e(error) { "Organization creation failed" }
    }

    /**
     * Logout current user.
     */
    suspend fun logout() {
        logger.d { "Logging out user" }

        try {
            val token = tokenManager.getValidAccessToken()
            if (token != null) {
                val request = LogoutRequest(sessionToken = token)
                accountService.logout(request)
            }
        } catch (e: Exception) {
            logger.w(e) { "Logout API call failed, clearing local tokens anyway" }
        }

        tokenManager.onAuthenticationFailed()
        authManager.onUserLogout()
    }

    /**
     * Request password reset email.
     */
    suspend fun requestPasswordReset(email: String): Result<Unit> = runCatching {
        logger.d { "Password reset requested for: ${email.take(3)}***" }
        identityService.requestPasswordReset(email)
    }.onFailure { error ->
        logger.e(error) { "Password reset request failed" }
    }

    /**
     * Reset password with token.
     */
    suspend fun resetPassword(resetToken: String, newPassword: String): Result<Unit> = runCatching {
        logger.d { "Resetting password with token" }
        val request = ResetPasswordRequest(newPassword = newPassword)
        identityService.resetPassword(resetToken, request)
    }.onFailure { error ->
        logger.e(error) { "Password reset failed" }
    }

    /**
     * Deactivate current user account.
     */
    suspend fun deactivateAccount(reason: String? = null): Result<Unit> = runCatching {
        logger.d { "Deactivating account" }
        val request = DeactivateUserRequest(reason = reason ?: "User requested")
        accountService.deactivateAccount(request)
    }.onFailure { error ->
        logger.e(error) { "Account deactivation failed" }
    }

    /**
     * Internal token refresh implementation.
     */
    private suspend fun refreshTokenInternal(
        refreshToken: String,
        organizationId: OrganizationId?
    ): LoginResponse? {
        logger.d { "Refreshing access token" }

        return try {
            val request = RefreshTokenRequest(
                refreshToken = refreshToken,
                organizationId = organizationId
            )
            identityService.refreshToken(request).also {
                logger.i { "Token refreshed successfully" }
            }
        } catch (e: Exception) {
            logger.e(e) { "Token refresh error" }
            null
        }
    }
}
