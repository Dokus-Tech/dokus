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
     * GET/PUT /api/v1/tenants/address
     * GET - Get company address for current tenant
     * PUT - Upsert company address for current tenant
     */
    @Serializable
    @Resource("address")
    class Address(val parent: Tenants = Tenants())

    /**
     * GET /api/v1/tenants/{id}
     * Get tenant by ID
     */
    @Serializable
    @Resource("{id}")
    class Id(val parent: Tenants = Tenants(), val id: String)

    /**
     * POST/GET/DELETE /api/v1/tenants/avatar
     * POST - Upload company avatar (multipart form data)
     * GET - Get current avatar URLs
     * DELETE - Remove company avatar
     */
    @Serializable
    @Resource("avatar")
    class Avatar(val parent: Tenants = Tenants())

    /**
     * GET /api/v1/tenants/invoice-number-preview
     * Preview the next invoice number without consuming it.
     */
    @Serializable
    @Resource("invoice-number-preview")
    class InvoiceNumberPreview(val parent: Tenants = Tenants())
}
