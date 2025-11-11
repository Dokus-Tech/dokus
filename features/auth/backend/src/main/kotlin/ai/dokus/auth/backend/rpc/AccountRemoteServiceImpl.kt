package ai.dokus.auth.backend.rpc

import ai.dokus.app.auth.domain.AccountRemoteService
import ai.dokus.auth.backend.security.requireAuthenticatedUserId
import ai.dokus.auth.backend.services.AuthService
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.model.auth.*
import org.slf4j.LoggerFactory

/**
 * RPC implementation of AccountRemoteService.
 *
 * This service acts as the backend RPC endpoint that clients call.
 * It delegates business logic to AuthService and wraps responses in Result.
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
    private val authService: AuthService
) : AccountRemoteService {

    private val logger = LoggerFactory.getLogger(AccountRemoteServiceImpl::class.java)

    /**
     * Authenticate user with email and password.
     * Returns JWT tokens on success.
     */
    override suspend fun login(request: LoginRequest): Result<LoginResponse> {
        logger.debug("RPC: login called for email: ${request.email.value}")

        return authService.login(request)
            .onSuccess { response ->
                logger.info("RPC: login successful for email: ${request.email.value}")
            }
            .onFailure { error ->
                logger.error("RPC: login failed for email: ${request.email.value}", error)
            }
    }

    /**
     * Register a new user account.
     * Automatically logs in and returns tokens.
     */
    override suspend fun register(request: RegisterRequest): Result<LoginResponse> {
        logger.debug("RPC: register called for email: ${request.email.value}")

        return authService.register(request)
            .onSuccess { response ->
                logger.info("RPC: registration successful for email: ${request.email.value}")
            }
            .onFailure { error ->
                logger.error("RPC: registration failed for email: ${request.email.value}", error)
            }
    }

    /**
     * Refresh an expired access token using refresh token.
     * Returns new token pair.
     *
     * Status: Not yet implemented
     */
    override suspend fun refreshToken(request: RefreshTokenRequest): Result<LoginResponse> {
        logger.debug("RPC: refreshToken called")

        return authService.refreshToken(request)
            .onFailure { error ->
                logger.error("RPC: refreshToken failed", error)
            }
    }

    /**
     * Logout user and revoke current session.
     */
    override suspend fun logout(request: LogoutRequest): Result<Unit> {
        logger.debug("RPC: logout called")

        return authService.logout(request)
            .onSuccess {
                logger.info("RPC: logout successful")
            }
            .onFailure { error ->
                logger.error("RPC: logout failed", error)
            }
    }

    /**
     * Request password reset email.
     *
     * Always returns success to prevent email enumeration.
     */
    override suspend fun requestPasswordReset(email: String): Result<Unit> {
        logger.debug("RPC: requestPasswordReset called for email")

        return authService.requestPasswordReset(email)
            .onSuccess {
                logger.info("RPC: Password reset email requested successfully")
            }
            .onFailure { error ->
                logger.error("RPC: Password reset request failed", error)
            }
    }

    /**
     * Reset password with token from email.
     *
     * Validates token and updates password.
     */
    override suspend fun resetPassword(resetToken: String, request: ResetPasswordRequest): Result<Unit> {
        logger.debug("RPC: resetPassword called with token")

        return authService.resetPassword(resetToken, request.newPassword)
            .onSuccess {
                logger.info("RPC: Password reset successful")
            }
            .onFailure { error ->
                logger.error("RPC: Password reset failed", error)
            }
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
    override suspend fun deactivateAccount(request: DeactivateUserRequest): Result<Unit> {
        logger.debug("RPC: deactivateAccount called with reason: ${request.reason}")

        return try {
            // Extract userId from JWT authentication context
            val userId = requireAuthenticatedUserId()
            logger.debug("Deactivating account for authenticated user: ${userId.value}")

            authService.deactivateAccount(userId, request.reason)
                .onSuccess {
                    logger.info("RPC: Account deactivated successfully for user: ${userId.value}")
                }
                .onFailure { error ->
                    logger.error("RPC: Account deactivation failed for user: ${userId.value}", error)
                }
        } catch (e: IllegalStateException) {
            // No authentication context available
            logger.error("RPC: deactivateAccount called without authentication")
            Result.failure(DokusException.NotAuthenticated("Authentication required to deactivate account"))
        } catch (e: Exception) {
            logger.error("RPC: Unexpected error in deactivateAccount", e)
            Result.failure(DokusException.InternalError("Failed to deactivate account"))
        }
    }

    /**
     * Verify email address with token from email.
     */
    override suspend fun verifyEmail(token: String): Result<Unit> {
        logger.debug("RPC: verifyEmail called")

        return authService.verifyEmail(token)
            .onSuccess {
                logger.info("RPC: email verification successful")
            }
            .onFailure { error ->
                logger.error("RPC: email verification failed", error)
            }
    }

    /**
     * Resend email verification email.
     * Requires authentication - extracts userId from JWT token in coroutine context.
     */
    override suspend fun resendVerificationEmail(): Result<Unit> {
        logger.debug("RPC: resendVerificationEmail called")

        return try {
            // Extract userId from JWT authentication context
            val userId = requireAuthenticatedUserId()
            logger.debug("Resending verification email for authenticated user: ${userId.value}")

            authService.resendVerificationEmail(userId)
                .onSuccess {
                    logger.info("RPC: Verification email resent successfully for user: ${userId.value}")
                }
                .onFailure { error ->
                    logger.error("RPC: Failed to resend verification email for user: ${userId.value}", error)
                }
        } catch (e: IllegalStateException) {
            // No authentication context available
            logger.error("RPC: resendVerificationEmail called without authentication")
            Result.failure(DokusException.NotAuthenticated("Authentication required to resend verification email"))
        } catch (e: Exception) {
            logger.error("RPC: Unexpected error in resendVerificationEmail", e)
            Result.failure(DokusException.InternalError("Failed to resend verification email"))
        }
    }
}
