package ai.dokus.app.auth.datasource

import ai.dokus.foundation.domain.Email
import ai.dokus.foundation.domain.model.auth.LoginRequest
import ai.dokus.foundation.domain.model.auth.LoginResponse
import ai.dokus.foundation.domain.model.auth.RefreshTokenRequest
import ai.dokus.foundation.domain.model.auth.RegisterRequest
import ai.dokus.foundation.domain.model.auth.ResetPasswordRequest

/**
 * Remote data source for identity operations (login, register, password reset).
 * All methods return Result to handle errors gracefully.
 */
interface IdentityRemoteDataSource {
    /**
     * Authenticate user with email and password.
     * @param request Login credentials
     * @return Result containing LoginResponse with access and refresh tokens
     */
    suspend fun login(request: LoginRequest): Result<LoginResponse>

    /**
     * Register a new user account.
     * @param request Registration details (email, password, name)
     * @return Result containing LoginResponse with access and refresh tokens
     */
    suspend fun register(request: RegisterRequest): Result<LoginResponse>

    /**
     * Refresh access token using refresh token.
     * @param request Refresh token details
     * @return Result containing new LoginResponse with fresh tokens
     */
    suspend fun refreshToken(request: RefreshTokenRequest): Result<LoginResponse>

    /**
     * Request a password reset email.
     * @param email User's email address
     * @return Result indicating success or failure
     */
    suspend fun requestPasswordReset(email: Email): Result<Unit>

    /**
     * Reset password using reset token.
     * @param resetToken Token from reset email
     * @param request New password
     * @return Result indicating success or failure
     */
    suspend fun resetPassword(resetToken: String, request: ResetPasswordRequest): Result<Unit>

    /**
     * Verify email address using verification token.
     * @param token Verification token from email
     * @return Result indicating success or failure
     */
    suspend fun verifyEmail(token: String): Result<Unit>
}
