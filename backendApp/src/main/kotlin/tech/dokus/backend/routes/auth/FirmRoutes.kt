package tech.dokus.backend.routes.auth

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.resources.delete
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory
import tech.dokus.backend.services.auth.FirmInviteTokenService
import tech.dokus.database.repository.auth.FirmRepository
import tech.dokus.database.repository.auth.TenantRepository
import tech.dokus.domain.enums.FirmRole
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.model.auth.CreateFirmRequest
import tech.dokus.domain.model.auth.CreateFirmResponse
import tech.dokus.domain.model.auth.FirmWorkspaceSummary
import tech.dokus.domain.model.auth.GenerateFirmInviteLinkResponse
import tech.dokus.domain.routes.Firms
import tech.dokus.foundation.backend.security.authenticateJwt
import tech.dokus.foundation.backend.security.dokusPrincipal
import kotlin.time.Duration.Companion.days

internal fun Route.firmRoutes() {
    val firmRepository by inject<FirmRepository>()
    val tenantRepository by inject<TenantRepository>()
    val inviteTokenService by inject<FirmInviteTokenService>()
    val logger = LoggerFactory.getLogger("FirmRoutes")

    authenticateJwt {
        post<Firms.Create> {
            val principal = dokusPrincipal
            val request = call.receive<CreateFirmRequest>()

            val prefillTenant = request.prefillTenantId?.let { tenantId ->
                val hasMembership = principal.tenantMemberships.any {
                    it.tenantId == tenantId
                }
                if (!hasMembership) {
                    throw DokusException.NotAuthorized("Tenant prefill not allowed for this user")
                }
                tenantRepository.findById(tenantId)
            }

            val resolvedName = request.name
                ?: prefillTenant?.displayName
                ?: throw DokusException.BadRequest("Firm name is required")
            val resolvedVat = request.vatNumber
                ?: prefillTenant?.vatNumber
                ?: throw DokusException.BadRequest("Firm VAT number is required")

            val createdFirm = firmRepository.createFirm(
                name = resolvedName,
                vatNumber = resolvedVat,
                ownerUserId = principal.userId
            )

            call.respond(
                HttpStatusCode.Created,
                CreateFirmResponse(
                    firm = FirmWorkspaceSummary(
                        id = createdFirm.id,
                        name = createdFirm.name,
                        vatNumber = createdFirm.vatNumber,
                        role = FirmRole.Owner,
                        clientCount = 0
                    )
                )
            )
        }

        post<Firms.ById.InviteLinks> { route ->
            val principal = dokusPrincipal
            val membership = principal.firmMemberships
                .firstOrNull { it.firmId == route.parent.firmId }
                ?: throw DokusException.NotAuthorized("No access to this firm")

            if (membership.role !in setOf(FirmRole.Owner, FirmRole.Admin)) {
                throw DokusException.NotAuthorized("Owner or admin role is required")
            }

            val expiresAt = Clock.System.now() + 7.days
            val token = inviteTokenService.generateToken(
                firmId = route.parent.firmId,
                expiresAt = expiresAt
            )

            call.respond(
                HttpStatusCode.OK,
                GenerateFirmInviteLinkResponse(
                    token = token,
                    expiresAt = expiresAt.toLocalDateTime(TimeZone.UTC)
                )
            )
        }

        delete<Firms.ById.RevokeClientAccess> { route ->
            val principal = dokusPrincipal
            val membership = principal.firmMemberships
                .firstOrNull { it.firmId == route.parent.firmId }
                ?: throw DokusException.NotAuthorized("No access to this firm")

            if (membership.role !in setOf(FirmRole.Owner, FirmRole.Admin)) {
                throw DokusException.NotAuthorized("Owner or admin role is required")
            }

            val revoked = firmRepository.revokeAccess(route.parent.firmId, route.tenantId)
            if (!revoked) {
                throw DokusException.NotFound("Active firm access not found")
            }

            logger.info(
                "Firm access revoked: firmId={}, tenantId={}, revokedBy={}",
                route.parent.firmId, route.tenantId, principal.userId,
            )

            call.respond(HttpStatusCode.NoContent)
        }
    }
}
