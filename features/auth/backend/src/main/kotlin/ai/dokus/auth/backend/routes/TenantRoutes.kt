package ai.dokus.auth.backend.routes

import ai.dokus.auth.backend.database.repository.TenantRepository
import ai.dokus.auth.backend.database.repository.UserRepository
import ai.dokus.foundation.domain.enums.TenantType
import ai.dokus.foundation.domain.enums.UserRole
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.model.CreateTenantRequest
import ai.dokus.foundation.domain.model.TenantSettings
import ai.dokus.foundation.ktor.security.authenticateJwt
import ai.dokus.foundation.ktor.security.dokusPrincipal
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Tenant routes for tenant management operations:
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

    route("/api/v1/tenants") {
        authenticateJwt {
            /**
             * GET /api/v1/tenants
             * List all tenants for the authenticated user
             */
            get {
                val principal = dokusPrincipal
                val memberships = userRepository.getUserTenants(principal.userId)
                    .filter { it.isActive }

                val tenants = memberships.mapNotNull { membership ->
                    tenantRepository.findById(membership.tenantId)
                }

                call.respond(HttpStatusCode.OK, tenants)
            }

            /**
             * POST /api/v1/tenants
             * Create a new tenant
             */
            post {
                val principal = dokusPrincipal
                val request = call.receive<CreateTenantRequest>()

                // Validate: user can only have one Freelancer tenant
                if (request.type == TenantType.Freelancer) {
                    val existingTenants = userRepository.getUserTenants(principal.userId)
                        .filter { it.isActive }
                        .mapNotNull { tenantRepository.findById(it.tenantId) }

                    if (existingTenants.any { it.type == TenantType.Freelancer }) {
                        throw IllegalArgumentException("User can only have one Freelancer workspace")
                    }
                }

                // Create the tenant
                val tenantId = tenantRepository.create(
                    type = request.type,
                    legalName = request.legalName,
                    displayName = request.displayName,
                    plan = request.plan,
                    language = request.language,
                    vatNumber = request.vatNumber
                )

                // Add the creating user as Owner of the new tenant
                userRepository.addToTenant(principal.userId, tenantId, UserRole.Owner)

                val tenant = tenantRepository.findById(tenantId)
                    ?: throw IllegalArgumentException("Tenant not found: $tenantId")

                call.respond(HttpStatusCode.Created, tenant)
            }

            /**
             * GET /api/v1/tenants/{id}
             * Get tenant by ID
             */
            get("/{id}") {
                val idParam = call.parameters["id"]
                    ?: throw IllegalArgumentException("Tenant ID is required")

                val tenantId = TenantId(Uuid.parse(idParam))
                val tenant = tenantRepository.findById(tenantId)
                    ?: throw IllegalArgumentException("Tenant not found: $tenantId")

                call.respond(HttpStatusCode.OK, tenant)
            }

            /**
             * GET /api/v1/tenants/settings
             * Get tenant settings for current tenant
             */
            get("/settings") {
                val principal = dokusPrincipal
                val tenantId = principal.requireTenantId()
                val settings = tenantRepository.getSettings(tenantId)
                call.respond(HttpStatusCode.OK, settings)
            }

            /**
             * PUT /api/v1/tenants/settings
             * Update tenant settings
             */
            put("/settings") {
                val settings = call.receive<TenantSettings>()
                tenantRepository.updateSettings(settings)
                call.respond(HttpStatusCode.NoContent)
            }

            /**
             * GET /api/v1/tenants/next-invoice-number
             * Get next invoice number for current tenant
             */
            get("/next-invoice-number") {
                val principal = dokusPrincipal
                val tenantId = principal.requireTenantId()
                val invoiceNumber = tenantRepository.getNextInvoiceNumber(tenantId)
                call.respond(HttpStatusCode.OK, mapOf("invoiceNumber" to invoiceNumber))
            }

            /**
             * GET /api/v1/tenants/has-freelancer
             * Check if user has a freelancer tenant
             */
            get("/has-freelancer") {
                val principal = dokusPrincipal
                val memberships = userRepository.getUserTenants(principal.userId)
                    .filter { it.isActive }

                val hasFreelancer = memberships.any { membership ->
                    tenantRepository.findById(membership.tenantId)?.type == TenantType.Freelancer
                }

                call.respond(HttpStatusCode.OK, mapOf("hasFreelancer" to hasFreelancer))
            }
        }
    }
}
