package tech.dokus.backend.routes.auth

import ai.dokus.foundation.database.repository.auth.AddressRepository
import ai.dokus.foundation.database.repository.auth.TenantRepository
import ai.dokus.foundation.database.repository.auth.UserRepository
import ai.dokus.foundation.database.services.InvoiceNumberGenerator
import ai.dokus.foundation.domain.enums.UserRole
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.ids.TenantId
import tech.dokus.domain.model.CreateTenantRequest
import tech.dokus.domain.model.InvoiceNumberPreviewResponse
import tech.dokus.domain.model.TenantSettings
import tech.dokus.domain.model.UpsertTenantAddressRequest
import tech.dokus.domain.routes.Tenants
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.resources.put
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import org.koin.ktor.ext.inject
import tech.dokus.foundation.ktor.security.authenticateJwt
import tech.dokus.foundation.ktor.security.dokusPrincipal
import tech.dokus.foundation.ktor.storage.AvatarStorageService
import tech.dokus.foundation.ktor.utils.loggerFor
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
    val tenantRepository by inject<TenantRepository>()
    val addressRepository by inject<AddressRepository>()
    val userRepository by inject<UserRepository>()
    val avatarStorageService by inject<AvatarStorageService>()
    val invoiceNumberGenerator by inject<InvoiceNumberGenerator>()

    authenticateJwt {
        /**
         * GET /api/v1/tenants
         * List all tenants the authenticated user belongs to.
         * Includes avatar URLs (small size) for each tenant that has an avatar.
         */
        get<Tenants> {
            val principal = dokusPrincipal

            val tenants = buildList {
                for (membership in userRepository.getUserTenants(principal.userId)) {
                    if (!membership.isActive) continue
                    val tenant = tenantRepository.findById(membership.tenantId) ?: continue

                    // Try to get avatar for this tenant
                    val avatar = try {
                        val storageKey = tenantRepository.getAvatarStorageKey(tenant.id)
                        if (storageKey != null) {
                            avatarStorageService.getAvatarUrls(storageKey)
                        } else null
                    } catch (e: Exception) {
                        logger.warn("Failed to get avatar for tenant ${tenant.id}", e)
                        null
                    }

                    add(tenant.copy(avatar = avatar))
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
                address = request.address,
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
         * GET /api/v1/tenants/invoice-number-preview
         * Preview the next invoice number without consuming it.
         */
        get<Tenants.InvoiceNumberPreview> {
            val tenantId = dokusPrincipal.requireTenantId()
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
            val tenantId = dokusPrincipal.requireTenantId()
            val address = addressRepository.getCompanyAddress(tenantId)
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
            val tenantId = dokusPrincipal.requireTenantId()
            val request = call.receive<UpsertTenantAddressRequest>()
            val address = addressRepository.upsertCompanyAddress(tenantId, request)
            call.respond(HttpStatusCode.OK, address)
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
         * Get tenant by ID (includes avatar)
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

            // Include avatar if available
            val avatar = try {
                val storageKey = tenantRepository.getAvatarStorageKey(tenantId)
                if (storageKey != null) {
                    avatarStorageService.getAvatarUrls(storageKey)
                } else null
            } catch (e: Exception) {
                logger.warn("Failed to get avatar for tenant $tenantId", e)
                null
            }

            call.respond(HttpStatusCode.OK, tenant.copy(avatar = avatar))
        }
    }
}
