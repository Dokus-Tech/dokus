package tech.dokus.backend.routes.auth

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.resources.post
import io.ktor.server.resources.put
import io.ktor.server.response.*
import io.ktor.server.routing.Route
import org.koin.ktor.ext.inject
import tech.dokus.backend.security.requireTenantId
import tech.dokus.backend.services.admin.TenantManagementService
import tech.dokus.backend.services.business.BusinessProfileService
import tech.dokus.backend.services.business.EnrichmentTrigger
import tech.dokus.database.services.InvoiceNumberGenerator
import tech.dokus.domain.enums.UserRole
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.CreateTenantRequest
import tech.dokus.domain.model.InvoiceNumberPreviewResponse
import tech.dokus.domain.model.PinBusinessProfileFieldsRequest
import tech.dokus.domain.model.Tenant
import tech.dokus.domain.model.TenantSettings
import tech.dokus.domain.model.UpdateBusinessProfileRequest
import tech.dokus.domain.model.UpsertTenantAddressRequest
import tech.dokus.domain.model.common.Thumbnail
import tech.dokus.domain.routes.Tenants
import tech.dokus.foundation.backend.security.authenticateJwt
import tech.dokus.foundation.backend.security.dokusPrincipal
import tech.dokus.foundation.backend.utils.loggerFor
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Tenant routes using Ktor Type-Safe Routing for tenant management operations:
 * - Get tenant by ID
 * - Get/update tenant settings
 */
private val logger = loggerFor("TenantRoutes")

@OptIn(ExperimentalUuidApi::class)
internal fun Route.tenantRoutes() {
    val tenantManagementService by inject<TenantManagementService>()
    val invoiceNumberGenerator by inject<InvoiceNumberGenerator>()
    val businessProfileService by inject<BusinessProfileService>()

    authenticateJwt {
        /**
         * GET /api/v1/tenants
         * List all tenants the authenticated user belongs to.
         * Includes avatar URLs (small size) for each tenant that has an avatar.
         */
        get<Tenants> {
            val principal = dokusPrincipal

            val tenants = buildList {
                for (membership in tenantManagementService.getUserTenants(principal.userId)) {
                    if (!membership.isActive) continue
                    val rawTenant = tenantManagementService.findTenantById(membership.tenantId) ?: continue
                    val tenant = runCatching { businessProfileService.projectTenant(rawTenant) }
                        .getOrElse { error ->
                            logger.warn("Failed to project tenant business profile for {}", rawTenant.id, error)
                            rawTenant
                        }

                    // Try to get avatar for this tenant
                    val avatar = try {
                        val storageKey = tenantManagementService.getTenantAvatarStorageKey(tenant.id)
                        if (storageKey != null) {
                            businessProfileService.buildTenantAvatarThumbnail(tenant.id)
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        logger.warn("Failed to get avatar for tenant ${tenant.id}", e)
                        null
                    }

                    add(projectTenantForMembership(tenant, membership.role, avatar))
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

            val tenantId = tenantManagementService.createTenant(
                type = request.type,
                legalName = request.legalName,
                displayName = request.displayName,
                subscription = request.subscription,
                language = request.language,
                vatNumber = request.vatNumber,
                address = request.address,
            )

            tenantManagementService.addUserToTenant(
                userId = principal.userId,
                tenantId = tenantId,
                role = UserRole.Owner,
            )

            val rawTenant = tenantManagementService.findTenantById(tenantId)
                ?: throw DokusException.InternalError("Failed to load created tenant")
            val tenant = runCatching { businessProfileService.projectTenant(rawTenant) }
                .getOrElse { rawTenant }

            businessProfileService.enqueueTenant(
                tenantId = tenantId,
                trigger = EnrichmentTrigger.TenantCreated
            ).onFailure { error ->
                logger.warn("Failed to enqueue business profile enrichment for tenant {}", tenantId, error)
            }

            call.respond(
                HttpStatusCode.Created,
                tenant.copy(role = UserRole.Owner)
            )
        }

        /**
         * GET /api/v1/tenants/settings
         * Get tenant settings for current tenant
         */
        get<Tenants.Settings> {
            val tenantId = requireTenantId()
            val settings = tenantManagementService.getSettings(tenantId)
            call.respond(HttpStatusCode.OK, settings)
        }

        /**
         * GET /api/v1/tenants/invoice-number-preview
         * Preview the next invoice number without consuming it.
         */
        get<Tenants.InvoiceNumberPreview> {
            val tenantId = requireTenantId()
            val preview = invoiceNumberGenerator.previewNextInvoiceNumber(tenantId).getOrElse {
                throw DokusException.InternalError("Failed to generate invoice number preview")
            }
            call.respond(HttpStatusCode.OK, InvoiceNumberPreviewResponse(invoiceNumber = preview))
        }

        /**
         * GET /api/v1/tenants/address
         * Get company address for current tenant.
         */
        get<Tenants.Address> {
            val tenantId = requireTenantId()
            val address = tenantManagementService.getCompanyAddress(tenantId)
            if (address == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("message" to "Company address not configured")
                )
            } else {
                call.respond(HttpStatusCode.OK, address)
            }
        }

        /**
         * PUT /api/v1/tenants/address
         * Upsert company address for current tenant.
         */
        put<Tenants.Address> {
            val tenantId = requireTenantId()
            val request = call.receive<UpsertTenantAddressRequest>()
            val address = tenantManagementService.upsertCompanyAddress(tenantId, request)
            businessProfileService.enqueueTenant(
                tenantId = tenantId,
                trigger = EnrichmentTrigger.TenantAddressUpdated
            ).onFailure { error ->
                logger.warn(
                    "Failed to enqueue business profile enrichment after tenant address update {}",
                    tenantId,
                    error
                )
            }
            call.respond(HttpStatusCode.OK, address)
        }

        /**
         * PUT /api/v1/tenants/settings
         * Update tenant settings
         */
        put<Tenants.Settings> {
            val tenantId = requireTenantId()

            val settings = call.receive<TenantSettings>()
            if (settings.tenantId != tenantId) {
                throw DokusException.NotAuthorized("Cannot update settings for another tenant")
            }

            tenantManagementService.updateSettings(settings.copy(tenantId = tenantId))
            call.respond(HttpStatusCode.NoContent)
        }

        /**
         * GET /api/v1/tenants/{id}
         * Get tenant by ID (includes avatar)
         */
        get<Tenants.Id> { route ->
            val principal = dokusPrincipal

            val tenantId = try {
                TenantId(Uuid.parse(route.id))
            } catch (_: IllegalArgumentException) {
                throw DokusException.BadRequest("Invalid tenant id")
            }

            val membership = tenantManagementService.getMembership(principal.userId, tenantId)
            if (membership == null || !membership.isActive) {
                throw DokusException.NotFound("Tenant not found")
            }

            val tenant = tenantManagementService.findTenantById(tenantId)
                ?: throw DokusException.NotFound("Tenant not found")
            val projectedTenant = runCatching { businessProfileService.projectTenant(tenant) }
                .getOrElse { error ->
                    logger.warn("Failed to project tenant business profile for {}", tenantId, error)
                    tenant
                }

            // Include avatar if available
            val avatar = try {
                val storageKey = tenantManagementService.getTenantAvatarStorageKey(tenantId)
                if (storageKey != null) {
                    businessProfileService.buildTenantAvatarThumbnail(tenantId)
                } else {
                    null
                }
            } catch (e: Exception) {
                logger.warn("Failed to get avatar for tenant $tenantId", e)
                null
            }

            call.respond(HttpStatusCode.OK, projectTenantForMembership(projectedTenant, membership.role, avatar))
        }

        /**
         * PUT /api/v1/tenants/business-profile
         * Backend-only endpoint: update tenant business profile values and pin edited fields.
         */
        put<Tenants.BusinessProfile> {
            val tenantId = requireTenantId()
            val request = call.receive<UpdateBusinessProfileRequest>()
            val response = businessProfileService.updateTenantProfile(tenantId, request)
            call.respond(HttpStatusCode.OK, response)
        }

        /**
         * PUT /api/v1/tenants/business-profile/pins
         * Backend-only endpoint: update tenant business profile pin flags.
         */
        put<Tenants.BusinessProfilePins> {
            val tenantId = requireTenantId()
            val request = call.receive<PinBusinessProfileFieldsRequest>()
            val response = businessProfileService.updateTenantPins(tenantId, request)
            call.respond(HttpStatusCode.OK, response)
        }
    }
}

internal fun projectTenantForMembership(
    tenant: Tenant,
    role: UserRole,
    avatar: Thumbnail?
): Tenant = tenant.copy(
    role = role,
    avatar = avatar
)
