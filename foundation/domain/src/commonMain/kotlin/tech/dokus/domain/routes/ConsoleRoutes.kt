package tech.dokus.domain.routes

import io.ktor.resources.Resource
import kotlinx.serialization.Serializable

/**
 * Type-safe route definitions for console APIs.
 * Base path: /api/v1/console
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
