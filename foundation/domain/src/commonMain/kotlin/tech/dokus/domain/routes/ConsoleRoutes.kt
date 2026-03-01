package tech.dokus.domain.routes

import io.ktor.resources.Resource
import kotlinx.serialization.Serializable

/**
 * Type-safe route definitions for Bookkeeper Console API.
 * Base path: /api/v1/console
 *
 * SECURITY: All operations require authentication. Data is scoped to tenants
 * where the authenticated user holds the Accountant role.
 */
@Serializable
@Resource("/api/v1/console")
class Console {
    /**
     * GET /api/v1/console/clients
     * List tenants the current user can access via Bookkeeper Console.
     */
    @Serializable
    @Resource("clients")
    class Clients(val parent: Console = Console())
}
