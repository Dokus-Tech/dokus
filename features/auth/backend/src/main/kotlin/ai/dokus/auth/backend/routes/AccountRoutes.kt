package ai.dokus.auth.backend.routes

import ai.dokus.foundation.database.repository.auth.UserRepository
import ai.dokus.auth.backend.services.AuthService
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.model.auth.DeactivateUserRequest
import ai.dokus.foundation.domain.model.auth.LogoutRequest
import ai.dokus.foundation.domain.model.auth.SelectTenantRequest
import ai.dokus.foundation.domain.routes.Account
import ai.dokus.foundation.ktor.security.authenticateJwt
import ai.dokus.foundation.ktor.security.dokusPrincipal
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import org.koin.ktor.ext.inject

/**
 * Account routes using Ktor Type-Safe Routing for authenticated user operations:
 * - Get current user
 * - Select tenant
 * - Logout
 * - Deactivate account
 * - Resend verification email
 */
fun Route.accountRoutes() {
    val authService by inject<AuthService>()
    val userRepository by inject<UserRepository>()

    authenticateJwt {
        /**
         * GET /api/v1/account/me
         * Get current authenticated user
         */
        get<Account.Me> {
            val principal = dokusPrincipal
            val user = userRepository.findById(principal.userId)
                ?: throw DokusException.NotAuthenticated("User not found")

            call.respond(HttpStatusCode.OK, user)
        }

        /**
         * POST /api/v1/account/select-tenant
         * Select a tenant and get new scoped tokens
         */
        post<Account.SelectTenant> {
            val principal = dokusPrincipal
            val request = call.receive<SelectTenantRequest>()
            val response = authService.selectOrganization(principal.userId, request.tenantId)
                .getOrThrow()

            call.respond(HttpStatusCode.OK, response)
        }

        /**
         * POST /api/v1/account/logout
         * Logout user and revoke tokens
         */
        post<Account.Logout> {
            val request = call.receive<LogoutRequest>()
            authService.logout(request).getOrThrow()
            call.respond(HttpStatusCode.NoContent)
        }

        /**
         * POST /api/v1/account/deactivate
         * Deactivate user account
         */
        post<Account.Deactivate> {
            val principal = dokusPrincipal
            val request = call.receive<DeactivateUserRequest>()
            authService.deactivateAccount(principal.userId, request.reason).getOrThrow()
            call.respond(HttpStatusCode.NoContent)
        }

        /**
         * POST /api/v1/account/resend-verification
         * Resend email verification email
         */
        post<Account.ResendVerification> {
            val principal = dokusPrincipal
            authService.resendVerificationEmail(principal.userId).getOrThrow()
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
