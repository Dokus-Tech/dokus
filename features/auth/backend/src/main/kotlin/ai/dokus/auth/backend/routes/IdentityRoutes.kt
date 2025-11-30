package ai.dokus.auth.backend.routes

import ai.dokus.auth.backend.services.AuthService
import ai.dokus.foundation.domain.Email
import ai.dokus.foundation.domain.model.auth.LoginRequest
import ai.dokus.foundation.domain.model.auth.LoginResponse
import ai.dokus.foundation.domain.model.auth.RefreshTokenRequest
import ai.dokus.foundation.domain.model.auth.RegisterRequest
import ai.dokus.foundation.domain.model.auth.ResetPasswordRequest
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

/**
 * Identity routes for unauthenticated operations:
 * - Login
 * - Register
 * - Token refresh
 * - Password reset
 * - Email verification
 */
fun Route.identityRoutes() {
    val authService by inject<AuthService>()

    route("/api/v1/identity") {
        /**
         * POST /api/v1/identity/login
         * Authenticate user with email and password
         */
        post("/login") {
            val request = call.receive<LoginRequest>()
            val response = authService.login(request).getOrThrow()
            call.respond(HttpStatusCode.OK, response)
        }

        /**
         * POST /api/v1/identity/register
         * Register new user account
         */
        post("/register") {
            val request = call.receive<RegisterRequest>()
            val response = authService.register(request).getOrThrow()
            call.respond(HttpStatusCode.Created, response)
        }

        /**
         * POST /api/v1/identity/refresh
         * Refresh access token using refresh token
         */
        post("/refresh") {
            val request = call.receive<RefreshTokenRequest>()
            val response = authService.refreshToken(request).getOrThrow()
            call.respond(HttpStatusCode.OK, response)
        }

        /**
         * POST /api/v1/identity/password-reset/request
         * Request password reset email
         */
        post("/password-reset/request") {
            val email = call.request.queryParameters["email"]
                ?: throw IllegalArgumentException("Email parameter is required")

            authService.requestPasswordReset(email).getOrThrow()
            call.respond(HttpStatusCode.NoContent)
        }

        /**
         * POST /api/v1/identity/password-reset/confirm
         * Confirm password reset with token and new password
         */
        post("/password-reset/confirm") {
            val resetToken = call.request.queryParameters["resetToken"]
                ?: throw IllegalArgumentException("resetToken parameter is required")

            val request = call.receive<ResetPasswordRequest>()
            authService.resetPassword(resetToken, request.newPassword).getOrThrow()
            call.respond(HttpStatusCode.NoContent)
        }

        /**
         * POST /api/v1/identity/verify-email
         * Verify email address with token
         */
        post("/verify-email") {
            val token = call.request.queryParameters["token"]
                ?: throw IllegalArgumentException("token parameter is required")

            authService.verifyEmail(token).getOrThrow()
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
