package ai.dokus.auth.backend.rpc

import ai.dokus.app.auth.domain.AccountRemoteService
import ai.dokus.auth.backend.services.AuthService
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
 * - resendVerificationEmail: Partially implemented (needs auth context)
 * - requestPasswordReset: Fully implemented
 * - resetPassword: Fully implemented
 * - deactivateAccount: Not yet implemented (returns error)
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
     *
     * Status: Not yet implemented
     */
    override suspend fun deactivateAccount(request: DeactivateUserRequest): Result<Unit> {
        logger.debug("RPC: deactivateAccount called with reason: ${request.reason}")

        return Result.failure<Unit>(
            NotImplementedError("Account deactivation functionality not yet implemented")
        ).also {
            logger.warn("RPC: deactivateAccount not implemented, reason: ${request.reason}")
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
     *
     * TODO: Extract userId from JWT token in request context
     */
    override suspend fun resendVerificationEmail(): Result<Unit> {
        logger.debug("RPC: resendVerificationEmail called")

        // TODO: Get userId from JWT token in request context
        // For now, this will fail and needs to be implemented when authentication context is available
        return Result.failure<Unit>(
            NotImplementedError("Resend verification email requires authenticated user context")
        ).also {
            logger.warn("RPC: resendVerificationEmail not fully implemented - needs user context")
        }
    }
}
