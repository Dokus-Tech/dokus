package ai.dokus.app.auth.datasource

import ai.dokus.foundation.domain.ids.TenantId
import tech.dokus.domain.model.User
import ai.dokus.foundation.domain.model.auth.DeactivateUserRequest
import ai.dokus.foundation.domain.model.auth.LoginResponse
import ai.dokus.foundation.domain.model.auth.LogoutRequest
import ai.dokus.foundation.domain.model.auth.UpdateProfileRequest

/**
 * Remote data source for account management operations.
 * All methods require authentication and return Result to handle errors gracefully.
 */
interface AccountRemoteDataSource {
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
}
