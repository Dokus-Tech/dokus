package ai.dokus.foundation.domain.routes

import io.ktor.resources.*
import kotlinx.serialization.Serializable

/**
 * Type-safe route definitions for Tenant API.
 * Base path: /api/v1/tenants
 *
 * SECURITY: Operations are scoped to tenants the authenticated user has access to.
 */
@Serializable
@Resource("/api/v1/tenants")
class Tenants {
    /**
     * GET/PUT /api/v1/tenants/settings
     * GET - Get tenant settings for current tenant
     * PUT - Update tenant settings
     */
    @Serializable
    @Resource("settings")
    class Settings(val parent: Tenants = Tenants())

    /**
     * GET /api/v1/tenants/{id}
     * Get tenant by ID
     */
    @Serializable
    @Resource("{id}")
    class Id(val parent: Tenants = Tenants(), val id: String)
}
