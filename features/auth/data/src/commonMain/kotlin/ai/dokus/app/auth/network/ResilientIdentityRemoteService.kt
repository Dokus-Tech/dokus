package ai.dokus.app.auth.network

import ai.dokus.app.auth.domain.IdentityRemoteService
import ai.dokus.foundation.network.resilient.ResilientDelegate

class ResilientIdentityRemoteService(
    serviceProvider: () -> IdentityRemoteService
) : IdentityRemoteService {
    private val delegate = ResilientDelegate(serviceProvider)
    private suspend inline fun <R> withRetry(crossinline block: suspend (IdentityRemoteService) -> R): R =
        delegate.withRetry(block)

    override suspend fun login(request: ai.dokus.foundation.domain.model.auth.LoginRequest) =
        withRetry { it.login(request) }

    override suspend fun register(request: ai.dokus.foundation.domain.model.auth.RegisterRequest) =
        withRetry { it.register(request) }

    override suspend fun refreshToken(request: ai.dokus.foundation.domain.model.auth.RefreshTokenRequest) =
        withRetry { it.refreshToken(request) }

    override suspend fun requestPasswordReset(email: String) =
        withRetry { it.requestPasswordReset(email) }

    override suspend fun resetPassword(resetToken: String, request: ai.dokus.foundation.domain.model.auth.ResetPasswordRequest) =
        withRetry { it.resetPassword(resetToken, request) }

    override suspend fun verifyEmail(token: String) =
        withRetry { it.verifyEmail(token) }
}
