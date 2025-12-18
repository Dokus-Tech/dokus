package ai.dokus.auth.backend.routes

import ai.dokus.foundation.database.repository.auth.TenantRepository
import ai.dokus.foundation.database.repository.auth.UserRepository
import ai.dokus.foundation.domain.enums.UserRole
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.model.TenantSettings
import ai.dokus.foundation.domain.model.CreateTenantRequest
import ai.dokus.foundation.domain.routes.Tenants
import ai.dokus.foundation.ktor.security.authenticateJwt
import ai.dokus.foundation.ktor.security.dokusPrincipal
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.resources.put
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import org.koin.ktor.ext.inject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Tenant routes using Ktor Type-Safe Routing for tenant management operations:
 * - Get tenant by ID
 * - Get/update tenant settings
 */
@OptIn(ExperimentalUuidApi::class)
fun Route.tenantRoutes() {
    val tenantRepository by inject<TenantRepository>()
    val userRepository by inject<UserRepository>()

    authenticateJwt {
        /**
         * GET /api/v1/tenants
         * List all tenants the authenticated user belongs to.
         */
        get<Tenants> {
            val principal = dokusPrincipal

            val tenants = buildList {
                for (membership in userRepository.getUserTenants(principal.userId)) {
                    if (!membership.isActive) continue
                    val tenant = tenantRepository.findById(membership.tenantId) ?: continue
                    add(tenant)
                }
            }

            call.respond(HttpStatusCode.OK, tenants)
        }

        /**
         * POST /api/v1/tenants
         * Create a new tenant and add the authenticated user as Owner.
         */
        post<Tenants> {
            val principal = dokusPrincipal
            val request = call.receive<CreateTenantRequest>()

            val tenantId = tenantRepository.create(
                type = request.type,
                legalName = request.legalName,
                displayName = request.displayName,
                plan = request.plan,
                language = request.language,
                vatNumber = request.vatNumber,
            )

            userRepository.addToTenant(
                userId = principal.userId,
                tenantId = tenantId,
                role = UserRole.Owner,
            )

            val tenant = tenantRepository.findById(tenantId)
                ?: throw DokusException.InternalError("Failed to load created tenant")

            call.respond(HttpStatusCode.Created, tenant)
        }

        /**
         * GET /api/v1/tenants/settings
         * Get tenant settings for current tenant
         */
        get<Tenants.Settings> {
            val principal = dokusPrincipal
            val tenantId = principal.requireTenantId()
            val settings = tenantRepository.getSettings(tenantId)
            call.respond(HttpStatusCode.OK, settings)
        }

        /**
         * PUT /api/v1/tenants/settings
         * Update tenant settings
         */
        put<Tenants.Settings> {
            val principal = dokusPrincipal
            val tenantId = principal.requireTenantId()

            val settings = call.receive<TenantSettings>()
            if (settings.tenantId != tenantId) {
                throw DokusException.NotAuthorized("Cannot update settings for another tenant")
            }

            tenantRepository.updateSettings(settings.copy(tenantId = tenantId))
            call.respond(HttpStatusCode.NoContent)
        }

        /**
         * GET /api/v1/tenants/{id}
         * Get tenant by ID
         */
        get<Tenants.Id> { route ->
            val principal = dokusPrincipal

            val tenantId = try {
                TenantId(Uuid.parse(route.id))
            } catch (_: IllegalArgumentException) {
                throw DokusException.BadRequest("Invalid tenant id")
            }

            val membership = userRepository.getMembership(principal.userId, tenantId)
            if (membership == null || !membership.isActive) {
                throw DokusException.NotFound("Tenant not found")
            }

            val tenant = tenantRepository.findById(tenantId)
                ?: throw DokusException.NotFound("Tenant not found")

            call.respond(HttpStatusCode.OK, tenant)
        }
    }
}
