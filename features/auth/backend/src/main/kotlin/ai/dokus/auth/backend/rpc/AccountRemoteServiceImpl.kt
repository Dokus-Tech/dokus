package ai.dokus.auth.backend.rpc

import ai.dokus.app.auth.domain.AccountRemoteService
import ai.dokus.auth.backend.services.AuthService
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.model.auth.DeactivateUserRequest
import ai.dokus.foundation.domain.model.auth.LoginRequest
import ai.dokus.foundation.domain.model.auth.LoginResponse
import ai.dokus.foundation.domain.model.auth.LogoutRequest
import ai.dokus.foundation.domain.model.auth.RefreshTokenRequest
import ai.dokus.foundation.domain.model.auth.RegisterRequest
import ai.dokus.foundation.domain.model.auth.ResetPasswordRequest
import ai.dokus.foundation.ktor.security.AuthInfoProvider
import ai.dokus.foundation.ktor.security.requireAuthenticatedUserId
import org.slf4j.LoggerFactory

/**
 * RPC implementation of AccountRemoteService.
 *
 * This service acts as the backend RPC endpoint that clients call.
 * It delegates business logic to AuthService.
 *
 * Error Handling:
 * - Returns plain types on success
 * - Throws exceptions on failure (automatically serialized by RPC framework)
 *
 * Implementation status:
 * - login: Fully implemented
 * - register: Fully implemented
 * - logout: Fully implemented
 * - refreshToken: Fully implemented
 * - verifyEmail: Fully implemented
 * - resendVerificationEmail: Fully implemented
 * - requestPasswordReset: Fully implemented
 * - resetPassword: Fully implemented
 * - deactivateAccount: Fully implemented
 */
class AccountRemoteServiceImpl(
    private val authService: AuthService,
    private val authInfoProvider: AuthInfoProvider
) : AccountRemoteService {

    private val logger = LoggerFactory.getLogger(AccountRemoteServiceImpl::class.java)

    /**
     * Authenticate user with email and password.
     * Returns JWT tokens on success.
     */
    override suspend fun login(request: LoginRequest): LoginResponse {
        logger.debug("RPC: login called for email: ${request.email.value}")

        return authService.login(request)
            .onSuccess { logger.info("RPC: login successful for email: ${request.email.value}") }
            .onFailure { error ->
                logger.error(
                    "RPC: login failed for email: ${request.email.value}",
                    error
                )
            }
            .getOrThrow()
    }

    /**
     * Register a new user account.
     * Automatically logs in and returns tokens.
     */
    override suspend fun register(request: RegisterRequest): LoginResponse {
        logger.debug("RPC: register called for email: ${request.email.value}")

        return authService.register(request)
            .onSuccess { logger.info("RPC: registration successful for email: ${request.email.value}") }
            .onFailure { error ->
                logger.error(
                    "RPC: registration failed for email: ${request.email.value}",
                    error
                )
            }
            .getOrThrow()
    }

    /**
     * Refresh an expired access token using refresh token.
     * Returns new token pair.
     */
    override suspend fun refreshToken(request: RefreshTokenRequest): LoginResponse {
        logger.debug("RPC: refreshToken called")

        return authService.refreshToken(request)
            .onFailure { error -> logger.error("RPC: refreshToken failed", error) }
            .getOrThrow()
    }

    /**
     * Logout user and revoke current session.
     */
    override suspend fun logout(request: LogoutRequest) {
        return authInfoProvider.withAuthInfo {
            logger.debug("RPC: logout called")

            authService.logout(request)
                .onSuccess { logger.info("RPC: logout successful") }
                .onFailure { error -> logger.error("RPC: logout failed", error) }
                .getOrThrow()
        }
    }

    /**
     * Request password reset email.
     *
     * Always returns success to prevent email enumeration.
     */
    override suspend fun requestPasswordReset(email: String) {
        logger.debug("RPC: requestPasswordReset called for email")

        authService.requestPasswordReset(email)
            .onSuccess { logger.info("RPC: Password reset email requested successfully") }
            .onFailure { error -> logger.error("RPC: Password reset request failed", error) }
            .getOrThrow()
    }

    /**
     * Reset password with token from email.
     *
     * Validates token and updates password.
     */
    override suspend fun resetPassword(resetToken: String, request: ResetPasswordRequest) {
        logger.debug("RPC: resetPassword called with token")

        authService.resetPassword(resetToken, request.newPassword)
            .onSuccess { logger.info("RPC: Password reset successful") }
            .onFailure { error -> logger.error("RPC: Password reset failed", error) }
            .getOrThrow()
    }

    /**
     * Deactivate current user account.
     * Requires authentication - extracts userId from JWT token in coroutine context.
     *
     * Security considerations:
     * - Only the authenticated user can deactivate their own account
     * - The userId is extracted from the JWT token, not from the request
     * - All deactivation attempts are logged for audit purposes
     * - All refresh tokens are revoked to terminate all sessions
     */
    override suspend fun deactivateAccount(request: DeactivateUserRequest) {
        logger.debug("RPC: deactivateAccount called with reason: ${request.reason}")

        try {
            // Extract userId from JWT authentication context
            val userId = requireAuthenticatedUserId()
            logger.debug("Deactivating account for authenticated user: ${userId.value}")

            authService.deactivateAccount(userId, request.reason)
                .onSuccess { logger.info("RPC: Account deactivated successfully for user: ${userId.value}") }
                .onFailure { error ->
                    logger.error(
                        "RPC: Account deactivation failed for user: ${userId.value}",
                        error
                    )
                }
                .getOrThrow()
        } catch (e: IllegalStateException) {
            // No authentication context available
            logger.error("RPC: deactivateAccount called without authentication")
            throw DokusException.NotAuthenticated("Authentication required to deactivate account")
        } catch (e: Exception) {
            logger.error("RPC: Unexpected error in deactivateAccount", e)
            throw e
        }
    }

    /**
     * Verify email address with token from email.
     */
    override suspend fun verifyEmail(token: String) {
        logger.debug("RPC: verifyEmail called")

        authService.verifyEmail(token)
            .onSuccess { logger.info("RPC: email verification successful") }
            .onFailure { error -> logger.error("RPC: email verification failed", error) }
            .getOrThrow()
    }

    /**
     * Resend email verification email.
     * Requires authentication - extracts userId from JWT token in coroutine context.
     */
    override suspend fun resendVerificationEmail() {
        logger.debug("RPC: resendVerificationEmail called")

        try {
            // Extract userId from JWT authentication context
            val userId = requireAuthenticatedUserId()
            logger.debug("Resending verification email for authenticated user: ${userId.value}")

            authService.resendVerificationEmail(userId)
                .onSuccess { logger.info("RPC: Verification email resent successfully for user: ${userId.value}") }
                .onFailure { error ->
                    logger.error(
                        "RPC: Failed to resend verification email for user: ${userId.value}",
                        error
                    )
                }
                .getOrThrow()
        } catch (e: IllegalStateException) {
            // No authentication context available
            logger.error("RPC: resendVerificationEmail called without authentication")
            throw DokusException.NotAuthenticated("Authentication required to resend verification email")
        } catch (e: Exception) {
            logger.error("RPC: Unexpected error in resendVerificationEmail", e)
            throw e
        }
    }
}
