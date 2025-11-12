package ai.dokus.app.auth.domain

import ai.dokus.foundation.domain.model.auth.*
import ai.dokus.foundation.domain.model.common.RpcResult
import kotlinx.rpc.annotations.Rpc

/**
 * Remote service for account and authentication operations.
 * Uses KotlinX RPC for client-server communication.
 */
@Rpc
interface AccountRemoteService {

    /**
     * Authenticate user with email and password.
     * Returns JWT tokens on success.
     */
    suspend fun login(request: LoginRequest): RpcResult<LoginResponse>

    /**
     * Register a new user account.
     * Automatically logs in and returns tokens.
     */
    suspend fun register(request: RegisterRequest): RpcResult<LoginResponse>

    /**
     * Refresh an expired access token using refresh token.
     * Returns new token pair.
     */
    suspend fun refreshToken(request: RefreshTokenRequest): RpcResult<LoginResponse>

    /**
     * Logout user and revoke current session.
     */
    suspend fun logout(request: LogoutRequest): RpcResult<Unit>

    /**
     * Request password reset email.
     */
    suspend fun requestPasswordReset(email: String): RpcResult<Unit>

    /**
     * Reset password with token from email.
     */
    suspend fun resetPassword(resetToken: String, request: ResetPasswordRequest): RpcResult<Unit>

    /**
     * Deactivate current user account.
     */
    suspend fun deactivateAccount(request: DeactivateUserRequest): RpcResult<Unit>

    /**
     * Verify email address with token from email.
     */
    suspend fun verifyEmail(token: String): RpcResult<Unit>

    /**
     * Resend email verification email.
     */
    suspend fun resendVerificationEmail(): RpcResult<Unit>
}