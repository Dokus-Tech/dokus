package ai.dokus.app.auth.domain

import ai.dokus.foundation.domain.Email
import ai.dokus.foundation.domain.model.auth.LoginRequest
import ai.dokus.foundation.domain.model.auth.LoginResponse
import ai.dokus.foundation.domain.model.auth.RefreshTokenRequest
import ai.dokus.foundation.domain.model.auth.RegisterRequest
import ai.dokus.foundation.domain.model.auth.ResetPasswordRequest
import kotlinx.rpc.annotations.Rpc

@Rpc
interface IdentityRemoteService {
    suspend fun login(request: LoginRequest): LoginResponse
    suspend fun register(request: RegisterRequest): LoginResponse
    suspend fun refreshToken(request: RefreshTokenRequest): LoginResponse
    suspend fun requestPasswordReset(email: Email)
    suspend fun resetPassword(resetToken: String, request: ResetPasswordRequest)
    suspend fun verifyEmail(token: String)
}
