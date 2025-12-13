package ai.dokus.foundation.domain.routes

import io.ktor.resources.*
import kotlinx.serialization.Serializable

/**
 * Type-safe route definitions for Tenant API.
 * Base path: /api/v1/tenants
 */
@Serializable
@Resource("/api/v1/tenants")
class Tenants {
    /**
     * GET /api/v1/tenants/settings - Get tenant settings
     * PUT /api/v1/tenants/settings - Update tenant settings
     */
    @Serializable
    @Resource("settings")
    class Settings(val parent: Tenants = Tenants())

    /**
     * GET /api/v1/tenants/next-invoice-number - Get next invoice number
     */
    @Serializable
    @Resource("next-invoice-number")
    class NextInvoiceNumber(val parent: Tenants = Tenants())

    /**
     * GET /api/v1/tenants/has-freelancer - Check if tenant has freelancer profile
     */
    @Serializable
    @Resource("has-freelancer")
    class HasFreelancer(val parent: Tenants = Tenants())

    /**
     * GET /api/v1/tenants/{id} - Get tenant by ID
     */
    @Serializable
    @Resource("{id}")
    class Id(val parent: Tenants = Tenants(), val id: String)
}
