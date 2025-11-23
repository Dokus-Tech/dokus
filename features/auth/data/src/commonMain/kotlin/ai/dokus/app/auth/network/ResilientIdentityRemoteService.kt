package ai.dokus.app.auth.network

import ai.dokus.app.auth.domain.IdentityRemoteService
import ai.dokus.foundation.domain.Email
import ai.dokus.foundation.domain.model.auth.LoginRequest
import ai.dokus.foundation.domain.model.auth.LoginResponse
import ai.dokus.foundation.domain.model.auth.RefreshTokenRequest
import ai.dokus.foundation.domain.model.auth.RegisterRequest
import ai.dokus.foundation.domain.model.auth.ResetPasswordRequest
import ai.dokus.foundation.network.resilient.RemoteServiceDelegate
import ai.dokus.foundation.network.resilient.invoke

class ResilientIdentityRemoteService(
    private val delegate: RemoteServiceDelegate<IdentityRemoteService>,
) : IdentityRemoteService {

    override suspend fun login(request: LoginRequest): LoginResponse {
        return delegate { it.login(request) }
    }

    override suspend fun register(request: RegisterRequest): LoginResponse {
        return delegate { it.register(request) }
    }

    override suspend fun refreshToken(request: RefreshTokenRequest): LoginResponse {
        return delegate { it.refreshToken(request) }
    }

    override suspend fun requestPasswordReset(email: Email) {
        return delegate { it.requestPasswordReset(email) }
    }

    override suspend fun resetPassword(
        resetToken: String,
        request: ResetPasswordRequest
    ) {
        return delegate { it.resetPassword(resetToken, request) }
    }

    override suspend fun verifyEmail(token: String) {
        return delegate { it.verifyEmail(token) }
    }
}
