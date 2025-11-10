package ai.dokus.app.auth.network

import ai.dokus.app.auth.domain.AccountRemoteService
import ai.dokus.foundation.domain.model.auth.*

/**
 * Mock implementation of AccountRemoteService for development.
 * This will be replaced with actual RPC client when backend is ready.
 */
class MockAccountRemoteService : AccountRemoteService {

    override suspend fun login(request: LoginRequest): Result<LoginResponse> {
        // TODO: Replace with actual RPC call
        return Result.failure(NotImplementedError("Backend not connected"))
    }

    override suspend fun register(request: RegisterRequest): Result<LoginResponse> {
        // TODO: Replace with actual RPC call
        return Result.failure(NotImplementedError("Backend not connected"))
    }

    override suspend fun refreshToken(request: RefreshTokenRequest): Result<LoginResponse> {
        // TODO: Replace with actual RPC call
        return Result.failure(NotImplementedError("Backend not connected"))
    }

    override suspend fun logout(request: LogoutRequest): Result<Unit> {
        // TODO: Replace with actual RPC call
        return Result.success(Unit)
    }

    override suspend fun requestPasswordReset(email: String): Result<Unit> {
        // TODO: Replace with actual RPC call
        return Result.failure(NotImplementedError("Backend not connected"))
    }

    override suspend fun resetPassword(resetToken: String, request: ResetPasswordRequest): Result<Unit> {
        // TODO: Replace with actual RPC call
        return Result.failure(NotImplementedError("Backend not connected"))
    }

    override suspend fun deactivateAccount(request: DeactivateUserRequest): Result<Unit> {
        // TODO: Replace with actual RPC call
        return Result.failure(NotImplementedError("Backend not connected"))
    }
}
