package tech.dokus.features.auth.datasource

import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.SessionId
import tech.dokus.domain.model.User
import tech.dokus.domain.model.auth.AccountMeResponse
import tech.dokus.domain.model.auth.ChangePasswordRequest
import tech.dokus.domain.model.auth.DeactivateUserRequest
import tech.dokus.domain.model.auth.LoginResponse
import tech.dokus.domain.model.auth.LogoutRequest
import tech.dokus.domain.model.auth.SessionDto
import tech.dokus.domain.model.auth.UpdateProfileRequest

/**
 * Remote data source for account management operations.
 * All methods require authentication and return Result to handle errors gracefully.
 */
interface AccountRemoteDataSource {
    /**
     * Get the currently authenticated user and surface availability.
     * @return Result containing the session/bootstrap payload
     */
    suspend fun getAccountMe(): Result<AccountMeResponse>

    /**
     * Get the currently authenticated user.
     * @return Result containing the User object
     */
    suspend fun getCurrentUser(): Result<User>

    /**
     * Select/switch to a specific tenant.
     * @param tenantId The tenant to switch to
     * @return Result containing LoginResponse with tenant-scoped tokens
     */
    suspend fun selectTenant(tenantId: TenantId): Result<LoginResponse>

    /**
     * Logout the current user session.
     * @param request Logout details (session and refresh tokens)
     * @return Result indicating success or failure
     */
    suspend fun logout(request: LogoutRequest): Result<Unit>

    /**
     * Update the current user's profile (first name, last name).
     * @param request Profile update data
     * @return Result containing the updated User object
     */
    suspend fun updateProfile(request: UpdateProfileRequest): Result<User>

    /**
     * Deactivate the current user account.
     * @param request Deactivation reason
     * @return Result indicating success or failure
     */
    suspend fun deactivateAccount(request: DeactivateUserRequest): Result<Unit>

    /**
     * Resend email verification email to the current user.
     * @return Result indicating success or failure
     */
    suspend fun resendVerificationEmail(): Result<Unit>

    /**
     * Change the current user's password.
     */
    suspend fun changePassword(request: ChangePasswordRequest): Result<Unit>

    /**
     * List active sessions for the current user.
     */
    suspend fun listSessions(): Result<List<SessionDto>>

    /**
     * Revoke a specific session by id.
     */
    suspend fun revokeSession(sessionId: SessionId): Result<Unit>

    /**
     * Revoke all sessions except the current one.
     */
    suspend fun revokeOtherSessions(): Result<Unit>
}
