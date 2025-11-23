@file:OptIn(ExperimentalUuidApi::class)

package ai.dokus.auth.backend.rpc

import ai.dokus.app.auth.domain.IdentityRemoteService
import ai.dokus.auth.backend.services.AuthService
import ai.dokus.foundation.domain.Email
import ai.dokus.foundation.domain.model.auth.LoginRequest
import ai.dokus.foundation.domain.model.auth.LoginResponse
import ai.dokus.foundation.domain.model.auth.RefreshTokenRequest
import ai.dokus.foundation.domain.model.auth.RegisterRequest
import ai.dokus.foundation.domain.model.auth.ResetPasswordRequest
import org.slf4j.LoggerFactory
import kotlin.uuid.ExperimentalUuidApi

class IdentityRemoteServiceImpl(
    private val authService: AuthService
) : IdentityRemoteService {

    private val logger = LoggerFactory.getLogger(IdentityRemoteServiceImpl::class.java)

    override suspend fun login(request: LoginRequest): LoginResponse {
        logger.debug("RPC: login called for email: ${request.email.value}")
        return authService.login(request)
            .onSuccess { logger.info("RPC: login successful for email: ${request.email.value}") }
            .onFailure { error -> logger.error("RPC: login failed for email: ${request.email.value}", error) }
            .getOrThrow()
    }

    override suspend fun register(request: RegisterRequest): LoginResponse {
        logger.debug("RPC: register called for email: ${request.email.value}")
        return authService.register(request)
            .onSuccess { logger.info("RPC: registration successful for email: ${request.email.value}") }
            .onFailure { error -> logger.error("RPC: registration failed for email: ${request.email.value}", error) }
            .getOrThrow()
    }

    override suspend fun refreshToken(request: RefreshTokenRequest): LoginResponse {
        logger.debug("RPC: refreshToken called")
        return authService.refreshToken(request)
            .onFailure { error -> logger.error("RPC: refreshToken failed", error) }
            .getOrThrow()
    }

    override suspend fun requestPasswordReset(email: Email) {
        logger.debug("RPC: requestPasswordReset called for email")
        authService.requestPasswordReset(email.value)
            .onSuccess { logger.info("RPC: Password reset email requested successfully") }
            .onFailure { error -> logger.error("RPC: Password reset request failed", error) }
            .getOrThrow()
    }

    override suspend fun resetPassword(resetToken: String, request: ResetPasswordRequest) {
        logger.debug("RPC: resetPassword called with token")
        authService.resetPassword(resetToken, request.newPassword)
            .onSuccess { logger.info("RPC: Password reset successful") }
            .onFailure { error -> logger.error("RPC: Password reset failed", error) }
            .getOrThrow()
    }

    override suspend fun verifyEmail(token: String) {
        logger.debug("RPC: verifyEmail called")
        authService.verifyEmail(token)
            .onSuccess { logger.info("RPC: email verification successful") }
            .onFailure { error -> logger.error("RPC: email verification failed", error) }
            .getOrThrow()
    }
}
