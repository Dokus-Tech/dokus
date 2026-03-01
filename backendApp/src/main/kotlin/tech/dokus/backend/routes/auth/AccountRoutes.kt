package tech.dokus.backend.routes.auth

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.resources.delete
import io.ktor.server.resources.get
import io.ktor.server.resources.patch
import io.ktor.server.resources.post
import io.ktor.server.resources.put
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import org.koin.ktor.ext.inject
import tech.dokus.backend.services.auth.AuthService
import tech.dokus.backend.services.auth.SessionContext
import tech.dokus.database.repository.auth.UserRepository
import tech.dokus.domain.DeviceType
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.model.auth.ChangePasswordRequest
import tech.dokus.domain.model.auth.DeactivateUserRequest
import tech.dokus.domain.model.auth.LogoutRequest
import tech.dokus.domain.model.auth.SelectTenantRequest
import tech.dokus.domain.model.auth.UpdateProfileRequest
import tech.dokus.backend.services.auth.SurfaceResolver
import tech.dokus.domain.model.auth.AccountMeResponse
import tech.dokus.domain.routes.Account
import tech.dokus.foundation.backend.security.authenticateJwt
import tech.dokus.foundation.backend.security.dokusPrincipal
import tech.dokus.foundation.backend.utils.extractClientIpAddress

/**
 * Account routes using Ktor Type-Safe Routing for authenticated user operations:
 * - Get current user
 * - Set active tenant
 * - Logout
 * - Update account (including deactivation)
 * - Request email verification
 */
internal fun Route.accountRoutes() {
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
            val memberships = userRepository.getUserTenants(principal.userId)
            val surface = SurfaceResolver.resolve(memberships)

            call.respond(
                HttpStatusCode.OK,
                AccountMeResponse(
                    user = user,
                    surface = surface
                )
            )
        }

        /**
         * PATCH /api/v1/account/profile
         * Update user profile (first name, last name)
         */
        patch<Account.Profile> {
            val principal = dokusPrincipal
            val request = call.receive<UpdateProfileRequest>()
            val user = authService.updateProfile(principal.userId, request).getOrThrow()
            call.respond(HttpStatusCode.OK, user)
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
         * PUT /api/v1/account/active-tenant
         * Set active tenant and get new scoped tokens
         */
        put<Account.ActiveTenant> {
            val principal = dokusPrincipal
            val request = call.receive<SelectTenantRequest>()
            val userAgent = call.request.header(HttpHeaders.UserAgent)
            val response = authService.selectOrganization(
                userId = principal.userId,
                tenantId = request.tenantId,
                sessionContext = SessionContext(
                    deviceType = DeviceType.fromAgent(userAgent),
                    ipAddress = call.extractClientIpAddress(),
                    userAgent = userAgent
                )
            )
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
         * POST /api/v1/account/email-verifications
         * Resend verification email for the authenticated user.
         */
        post<Account.EmailVerifications> {
            val principal = dokusPrincipal
            authService.resendVerificationEmail(principal.userId).getOrThrow()
            call.respond(HttpStatusCode.NoContent)
        }

        /**
         * POST /api/v1/account/password
         * Change current user's password.
         */
        post<Account.Password> {
            val principal = dokusPrincipal
            val request = call.receive<ChangePasswordRequest>()
            authService.changePassword(
                userId = principal.userId,
                currentPassword = request.currentPassword,
                newPassword = request.newPassword,
                currentSessionJti = principal.sessionJti
            ).getOrThrow()
            call.respond(HttpStatusCode.NoContent)
        }

        /**
         * GET /api/v1/account/sessions
         * List active sessions for the current user.
         */
        get<Account.Sessions> {
            val principal = dokusPrincipal
            val sessions = authService.listSessions(
                userId = principal.userId,
                currentSessionJti = principal.sessionJti
            ).getOrThrow()
            call.respond(HttpStatusCode.OK, sessions)
        }

        /**
         * DELETE /api/v1/account/sessions/{sessionId}
         * Revoke a single session.
         */
        delete<Account.Sessions.ById> { route ->
            val principal = dokusPrincipal
            authService.revokeSession(
                userId = principal.userId,
                sessionId = route.sessionId
            ).getOrThrow()
            call.respond(HttpStatusCode.NoContent)
        }

        /**
         * POST /api/v1/account/sessions/revoke-others
         * Revoke all sessions except the current session.
         */
        post<Account.Sessions.RevokeOthers> {
            val principal = dokusPrincipal
            authService.revokeOtherSessions(
                userId = principal.userId,
                currentSessionJti = principal.sessionJti
            ).getOrThrow()
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
