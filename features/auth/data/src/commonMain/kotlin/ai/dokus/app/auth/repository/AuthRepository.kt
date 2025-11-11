package ai.dokus.app.auth.repository

import ai.dokus.app.auth.domain.AccountRemoteService
import ai.dokus.app.auth.manager.AuthManagerMutable
import ai.dokus.app.auth.manager.TokenManagerMutable
import ai.dokus.foundation.domain.model.auth.*
import ai.dokus.foundation.platform.Logger
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository for authentication operations.
 * Coordinates between TokenManager, AuthManager, and AccountRemoteService.
 */
class AuthRepository(
    private val tokenManager: TokenManagerMutable,
    private val authManager: AuthManagerMutable,
    private val accountService: AccountRemoteService
) {
    private val logger = Logger.forClass<AuthRepository>()

    val isAuthenticated: StateFlow<Boolean> = tokenManager.isAuthenticated

    init {
        // Set up token refresh callback
        tokenManager.onTokenRefreshNeeded = { refreshToken ->
            refreshTokenInternal(refreshToken)
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

        return try {
            val result = accountService.login(request)

            result.fold(
                onSuccess = { response ->
                    logger.i { "Login successful" }
                    tokenManager.saveTokens(response)
                    authManager.onLoginSuccess()
                    Result.success(Unit)
                },
                onFailure = { error ->
                    logger.e(error) { "Login failed" }
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            logger.e(e) { "Login error" }
            Result.failure(e)
        }
    }

    /**
     * Register a new user account.
     */
    suspend fun register(request: RegisterRequest): Result<Unit> {
        logger.d { "Registration attempt for email: ${request.email.value.take(3)}***" }

        return try {
            val result = accountService.register(request)

            result.fold(
                onSuccess = { response ->
                    logger.i { "Registration successful, auto-logging in" }
                    tokenManager.saveTokens(response)
                    authManager.onLoginSuccess()
                    Result.success(Unit)
                },
                onFailure = { error ->
                    logger.e(error) { "Registration failed" }
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            logger.e(e) { "Registration error" }
            Result.failure(e)
        }
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
    suspend fun requestPasswordReset(email: String): Result<Unit> {
        logger.d { "Password reset requested for: ${email.take(3)}***" }

        return try {
            accountService.requestPasswordReset(email)
        } catch (e: Exception) {
            logger.e(e) { "Password reset request failed" }
            Result.failure(e)
        }
    }

    /**
     * Reset password with token.
     */
    suspend fun resetPassword(resetToken: String, newPassword: String): Result<Unit> {
        logger.d { "Resetting password with token" }

        return try {
            val request = ResetPasswordRequest(newPassword = newPassword)
            accountService.resetPassword(resetToken, request)
        } catch (e: Exception) {
            logger.e(e) { "Password reset failed" }
            Result.failure(e)
        }
    }

    /**
     * Deactivate current user account.
     */
    suspend fun deactivateAccount(reason: String? = null): Result<Unit> {
        logger.d { "Deactivating account" }

        return try {
            val request = DeactivateUserRequest(reason = reason ?: "User requested")
            accountService.deactivateAccount(request)
        } catch (e: Exception) {
            logger.e(e) { "Account deactivation failed" }
            Result.failure(e)
        }
    }

    /**
     * Internal token refresh implementation.
     */
    private suspend fun refreshTokenInternal(refreshToken: String): LoginResponse? {
        logger.d { "Refreshing access token" }

        return try {
            val request = RefreshTokenRequest(refreshToken = refreshToken)
            val result = accountService.refreshToken(request)

            result.getOrNull().also {
                if (it != null) {
                    logger.i { "Token refreshed successfully" }
                } else {
                    logger.w { "Token refresh failed" }
                }
            }
        } catch (e: Exception) {
            logger.e(e) { "Token refresh error" }
            null
        }
    }
}