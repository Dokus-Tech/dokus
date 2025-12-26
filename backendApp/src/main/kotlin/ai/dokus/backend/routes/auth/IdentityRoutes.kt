package ai.dokus.backend.routes.auth

import ai.dokus.backend.services.auth.AuthService
import ai.dokus.foundation.domain.Email
import ai.dokus.foundation.domain.model.auth.LoginRequest
import ai.dokus.foundation.domain.model.auth.RefreshTokenRequest
import ai.dokus.foundation.domain.model.auth.RegisterRequest
import ai.dokus.foundation.domain.model.auth.ResetPasswordRequest
import ai.dokus.foundation.domain.routes.Identity
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.resources.patch
import io.ktor.server.resources.post
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

/**
 * Identity routes using Ktor Type-Safe Routing for unauthenticated operations:
 * - Login
 * - Register
 * - Token refresh
 * - Password reset
 * - Email verification
 */
fun Route.identityRoutes() {
    val authService by inject<AuthService>()

    /**
     * POST /api/v1/identity/login
     * Authenticate user with email and password
     */
    post<Identity.Login> {
        val request = call.receive<LoginRequest>()
        val response = authService.login(request).getOrThrow()
        call.respond(HttpStatusCode.OK, response)
    }

    /**
     * POST /api/v1/identity/register
     * Register new user account
     */
    post<Identity.Register> {
        val request = call.receive<RegisterRequest>()
        val response = authService.register(request).getOrThrow()
        call.respond(HttpStatusCode.Created, response)
    }

    /**
     * POST /api/v1/identity/refresh
     * Refresh access token using refresh token
     */
    post<Identity.Refresh> {
        val request = call.receive<RefreshTokenRequest>()
        val response = authService.refreshToken(request).getOrThrow()
        call.respond(HttpStatusCode.OK, response)
    }

    /**
     * POST /api/v1/identity/password-resets
     * Request password reset email (creates a password reset request)
     */
    post<Identity.PasswordResets> {
        val payload = call.receive<PasswordResetRequest>()
        val email = payload.email.value

        authService.requestPasswordReset(email).getOrThrow()
        call.respond(HttpStatusCode.NoContent)
    }

    /**
     * PATCH /api/v1/identity/password-resets/{token}
     * Complete password reset with token and new password
     */
    patch<Identity.PasswordResets.ByToken> { route ->
        val request = call.receive<ResetPasswordRequest>()
        authService.resetPassword(route.token, request.newPassword).getOrThrow()
        call.respond(HttpStatusCode.NoContent)
    }

    /**
     * PATCH /api/v1/identity/email-verifications/{token}
     * Verify email address with token
     */
    patch<Identity.EmailVerifications.ByToken> { route ->
        authService.verifyEmail(route.token).getOrThrow()
        call.respond(HttpStatusCode.NoContent)
    }
}

@kotlinx.serialization.Serializable
private data class PasswordResetRequest(
    val email: Email
)
