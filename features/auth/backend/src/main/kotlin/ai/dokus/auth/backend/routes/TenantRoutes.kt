package ai.dokus.auth.backend.routes

import ai.dokus.foundation.database.repository.auth.TenantRepository
import ai.dokus.foundation.database.repository.auth.UserRepository
import ai.dokus.foundation.domain.enums.TenantType
import ai.dokus.foundation.domain.enums.UserRole
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.model.CreateTenantRequest
import ai.dokus.foundation.domain.model.TenantSettings
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
 * - List user's tenants
 * - Create tenant
 * - Get tenant by ID
 * - Get/update tenant settings
 * - Get next invoice number
 * - Check for freelancer tenant
 */
@OptIn(ExperimentalUuidApi::class)
fun Route.tenantRoutes() {
    val tenantRepository by inject<TenantRepository>()
    val userRepository by inject<UserRepository>()

    authenticateJwt {
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
            val settings = call.receive<TenantSettings>()
            tenantRepository.updateSettings(settings)
            call.respond(HttpStatusCode.NoContent)
        }

        /**
         * GET /api/v1/tenants/next-invoice-number
         * Get next invoice number for current tenant
         */
        get<Tenants.NextInvoiceNumber> {
            val principal = dokusPrincipal
            val tenantId = principal.requireTenantId()
            val invoiceNumber = tenantRepository.getNextInvoiceNumber(tenantId)
            call.respond(HttpStatusCode.OK, mapOf("invoiceNumber" to invoiceNumber))
        }

        /**
         * GET /api/v1/tenants/has-freelancer
         * Check if user has a freelancer tenant
         */
        get<Tenants.HasFreelancer> {
            val principal = dokusPrincipal
            val memberships = userRepository.getUserTenants(principal.userId)
                .filter { it.isActive }

            val hasFreelancer = memberships.any { membership ->
                tenantRepository.findById(membership.tenantId)?.type == TenantType.Freelancer
            }

            call.respond(HttpStatusCode.OK, mapOf("hasFreelancer" to hasFreelancer))
        }

        /**
         * GET /api/v1/tenants/{id}
         * Get tenant by ID
         */
        get<Tenants.Id> { route ->
            val tenantId = TenantId(Uuid.parse(route.id))
            val tenant = tenantRepository.findById(tenantId)
                ?: throw IllegalArgumentException("Tenant not found: $tenantId")

            call.respond(HttpStatusCode.OK, tenant)
        }
    }
}
