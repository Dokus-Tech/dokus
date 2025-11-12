package ai.dokus.app.auth.network

import ai.dokus.app.auth.domain.AccountRemoteService
import ai.dokus.foundation.domain.model.auth.*
import ai.dokus.foundation.domain.model.common.RpcResult

/**
 * Mock implementation of AccountRemoteService for development.
 * This will be replaced with actual RPC client when backend is ready.
 */
class MockAccountRemoteService : AccountRemoteService {

    override suspend fun login(request: LoginRequest): RpcResult<LoginResponse> {
        // TODO: Replace with actual RPC call
        return RpcResult.failure("Backend not connected", "NOT_IMPLEMENTED")
    }

    override suspend fun register(request: RegisterRequest): RpcResult<LoginResponse> {
        // TODO: Replace with actual RPC call
        return RpcResult.failure("Backend not connected", "NOT_IMPLEMENTED")
    }

    override suspend fun refreshToken(request: RefreshTokenRequest): RpcResult<LoginResponse> {
        // TODO: Replace with actual RPC call
        return RpcResult.failure("Backend not connected", "NOT_IMPLEMENTED")
    }

    override suspend fun logout(request: LogoutRequest): RpcResult<Unit> {
        // TODO: Replace with actual RPC call
        return RpcResult.success(Unit)
    }

    override suspend fun requestPasswordReset(email: String): RpcResult<Unit> {
        // TODO: Replace with actual RPC call
        return RpcResult.failure("Backend not connected", "NOT_IMPLEMENTED")
    }

    override suspend fun resetPassword(resetToken: String, request: ResetPasswordRequest): RpcResult<Unit> {
        // TODO: Replace with actual RPC call
        return RpcResult.failure("Backend not connected", "NOT_IMPLEMENTED")
    }

    override suspend fun deactivateAccount(request: DeactivateUserRequest): RpcResult<Unit> {
        // TODO: Replace with actual RPC call
        return RpcResult.failure("Backend not connected", "NOT_IMPLEMENTED")
    }

    override suspend fun verifyEmail(token: String): RpcResult<Unit> {
        // TODO: Replace with actual RPC call
        return RpcResult.failure("Backend not connected", "NOT_IMPLEMENTED")
    }

    override suspend fun resendVerificationEmail(): RpcResult<Unit> {
        // TODO: Replace with actual RPC call
        return RpcResult.failure("Backend not connected", "NOT_IMPLEMENTED")
    }
}
