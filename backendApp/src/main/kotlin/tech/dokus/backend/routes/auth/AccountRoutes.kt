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
import tech.dokus.backend.services.auth.SurfaceResolver
import tech.dokus.backend.services.avatar.projectAvatarThumbnail
import tech.dokus.backend.services.avatar.projectUserAvatar
import tech.dokus.database.repository.auth.FirmRepository
import tech.dokus.database.repository.auth.TenantRepository
import tech.dokus.database.repository.auth.UserRepository
import tech.dokus.domain.DeviceType
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.model.auth.AccountMeResponse
import tech.dokus.domain.model.auth.ChangePasswordRequest
import tech.dokus.domain.model.auth.DeactivateUserRequest
import tech.dokus.domain.model.auth.FirmWorkspaceSummary
import tech.dokus.domain.model.auth.LogoutRequest
import tech.dokus.domain.model.auth.SelectTenantRequest
import tech.dokus.domain.model.auth.TenantWorkspaceSummary
import tech.dokus.domain.model.auth.UpdateProfileRequest
import tech.dokus.domain.routes.Account
import tech.dokus.foundation.backend.security.authenticateJwt
import tech.dokus.foundation.backend.security.dokusPrincipal
import tech.dokus.foundation.backend.storage.AvatarStorageService
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
    val tenantRepository by inject<TenantRepository>()
    val firmRepository by inject<FirmRepository>()
    val avatarStorageService by inject<AvatarStorageService>()

    authenticateJwt {
        /**
         * GET /api/v1/account/me
         * Get current authenticated user
         */
        get<Account.Me> {
            val principal = dokusPrincipal
            val user = userRepository.findById(principal.userId)
                ?: throw DokusException.NotAuthenticated("User not found")
            val projectedUser = userRepository.projectUserAvatar(user, avatarStorageService)
            val tenantMemberships = userRepository.getUserTenants(principal.userId)
                .filter { it.isActive }
            val firmsMemberships = firmRepository.listUserMemberships(principal.userId)
                .filter { it.isActive }
            val surface = SurfaceResolver.resolve(
                tenantMemberships = tenantMemberships,
                firmMemberships = firmsMemberships
            )

            val tenantsById = tenantRepository.findByIds(tenantMemberships.map { it.tenantId })
                .associateBy { it.id }
            val tenantSummaries = buildList {
                for (membership in tenantMemberships) {
                    val tenant = tenantsById[membership.tenantId] ?: continue
                    add(
                        TenantWorkspaceSummary(
                            id = tenant.id,
                            name = tenant.displayName,
                            vatNumber = tenant.vatNumber,
                            role = membership.role,
                            type = tenant.type,
                            avatar = avatarStorageService.projectAvatarThumbnail(
                                tenantRepository.getAvatarStorageKey(tenant.id)
                            )
                        )
                    )
                }
            }

            val firmsById = firmRepository.listFirmsByIds(firmsMemberships.map { it.firmId })
                .associateBy { it.id }
            val clientCountByFirmId = firmRepository.countActiveClientsByFirmIds(
                firmsMemberships.map { it.firmId }
            )
            val firmSummaries = firmsMemberships.mapNotNull { membership ->
                val firm = firmsById[membership.firmId] ?: return@mapNotNull null
                FirmWorkspaceSummary(
                    id = firm.id,
                    name = firm.name,
                    vatNumber = firm.vatNumber,
                    role = membership.role,
                    clientCount = clientCountByFirmId[membership.firmId] ?: 0
                )
            }

            call.respond(
                HttpStatusCode.OK,
                AccountMeResponse(
                    user = projectedUser,
                    surface = surface,
                    tenants = tenantSummaries,
                    firms = firmSummaries
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
