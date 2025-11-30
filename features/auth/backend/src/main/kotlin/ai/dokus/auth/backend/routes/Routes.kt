package ai.dokus.auth.backend.routes

import io.ktor.server.application.*
import io.ktor.server.routing.*

/**
 * Registers all REST API routes for the Auth service.
 *
 * Route structure:
 * - /api/v1/identity - Unauthenticated identity operations (login, register, etc.)
 * - /api/v1/account - Authenticated account operations (me, logout, etc.)
 * - /api/v1/tenants - Tenant management operations
 *
 * Usage in Application.kt:
 * ```kotlin
 * routing {
 *     configureRoutes()
 * }
 * ```
 */
fun Application.configureRoutes() {
    routing {
        // Unauthenticated routes
        identityRoutes()

        // Authenticated routes
        accountRoutes()
        tenantRoutes()
    }
}
