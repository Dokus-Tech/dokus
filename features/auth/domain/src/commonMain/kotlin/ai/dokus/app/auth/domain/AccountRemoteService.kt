package ai.dokus.app.auth.domain

import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.model.auth.DeactivateUserRequest
import ai.dokus.foundation.domain.model.auth.LoginResponse
import ai.dokus.foundation.domain.model.auth.LogoutRequest
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
     * Select a tenant and issue tokens scoped to it.
     * @throws Exception if selection fails or user is not a member
     */
    suspend fun selectTenant(tenantId: TenantId): LoginResponse

    /**
     * Logout user and revoke the current session.
     * @throws Exception if logout fails
     */
    suspend fun logout(request: LogoutRequest)

    /**
     * Deactivate current user account.
     * @throws Exception if deactivation fails
     */
    suspend fun deactivateAccount(request: DeactivateUserRequest)

    /**
     * Resend email verification email.
     * @throws Exception if resend fails
     */
    suspend fun resendVerificationEmail()
}
