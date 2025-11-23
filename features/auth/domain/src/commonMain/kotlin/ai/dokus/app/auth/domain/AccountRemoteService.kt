package ai.dokus.app.auth.domain

import ai.dokus.foundation.domain.model.auth.DeactivateUserRequest
import ai.dokus.foundation.domain.model.auth.LoginRequest
import ai.dokus.foundation.domain.model.auth.LoginResponse
import ai.dokus.foundation.domain.model.auth.LogoutRequest
import ai.dokus.foundation.domain.model.auth.RefreshTokenRequest
import ai.dokus.foundation.domain.model.auth.RegisterRequest
import ai.dokus.foundation.domain.model.auth.ResetPasswordRequest
import ai.dokus.foundation.domain.ids.OrganizationId
import kotlinx.rpc.annotations.Rpc

/**
 * Remote service for account and authentication operations.
 * Uses KotlinX RPC for client-server communication.
 *
 * Error Handling:
 * - Methods return plain types on success
 * - Throws exceptions on failure (serialized by RPC framework)
 */
@Rpc
interface AccountRemoteService {

    /**
     * Authenticate user with email and password.
     * Returns JWT tokens on success.
     * @throws Exception if authentication fails
     */
    suspend fun login(request: LoginRequest): LoginResponse

    /**
     * Register a new user account.
     * Automatically logs in and returns tokens.
     * @throws Exception if registration fails
     */
    suspend fun register(request: RegisterRequest): LoginResponse

    /**
     * Refresh an expired access token using refresh token.
     * Returns new token pair.
     * @throws Exception if token refresh fails
     */
    suspend fun refreshToken(request: RefreshTokenRequest): LoginResponse

    /**
     * Select an organization and issue tokens scoped to it.
     * @throws Exception if selection fails or user is not a member
     */
    suspend fun selectOrganization(organizationId: OrganizationId): LoginResponse

    /**
     * Logout user and revoke current session.
     * @throws Exception if logout fails
     */
    suspend fun logout(request: LogoutRequest)

    /**
     * Request password reset email.
     * @throws Exception if request fails
     */
    suspend fun requestPasswordReset(email: String)

    /**
     * Reset password with token from email.
     * @throws Exception if password reset fails
     */
    suspend fun resetPassword(resetToken: String, request: ResetPasswordRequest)

    /**
     * Deactivate current user account.
     * @throws Exception if deactivation fails
     */
    suspend fun deactivateAccount(request: DeactivateUserRequest)

    /**
     * Verify email address with token from email.
     * @throws Exception if verification fails
     */
    suspend fun verifyEmail(token: String)

    /**
     * Resend email verification email.
     * @throws Exception if resend fails
     */
    suspend fun resendVerificationEmail()
}
