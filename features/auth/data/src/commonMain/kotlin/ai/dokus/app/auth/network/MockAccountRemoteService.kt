package ai.dokus.app.auth.network

import ai.dokus.app.auth.domain.AccountRemoteService
import ai.dokus.foundation.domain.model.auth.*
import ai.dokus.foundation.domain.ids.OrganizationId

/**
 * Mock implementation of AccountRemoteService for development.
 * This will be replaced with actual RPC client when backend is ready.
 */
class MockAccountRemoteService : AccountRemoteService {

    override suspend fun login(request: LoginRequest): LoginResponse {
        throw NotImplementedError("Backend not connected")
    }

    override suspend fun register(request: RegisterRequest): LoginResponse {
        throw NotImplementedError("Backend not connected")
    }

    override suspend fun refreshToken(request: RefreshTokenRequest): LoginResponse {
        throw NotImplementedError("Backend not connected")
    }

    override suspend fun selectOrganization(organizationId: OrganizationId): LoginResponse {
        throw NotImplementedError("Backend not connected")
    }

    override suspend fun logout(request: LogoutRequest) {
        // Logout succeeds silently even without backend
    }

    override suspend fun requestPasswordReset(email: String) {
        throw NotImplementedError("Backend not connected")
    }

    override suspend fun resetPassword(resetToken: String, request: ResetPasswordRequest) {
        throw NotImplementedError("Backend not connected")
    }

    override suspend fun deactivateAccount(request: DeactivateUserRequest) {
        throw NotImplementedError("Backend not connected")
    }

    override suspend fun verifyEmail(token: String) {
        throw NotImplementedError("Backend not connected")
    }

    override suspend fun resendVerificationEmail() {
        throw NotImplementedError("Backend not connected")
    }
}
