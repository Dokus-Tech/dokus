package tech.dokus.backend.routes.auth

import io.ktor.server.application.Application
import io.ktor.server.routing.routing

/**
 * Registers all REST API routes for the Auth service.
 */
fun Application.configureAuthRoutes() {
    routing {
        // Unauthenticated routes
        identityRoutes()

        // Authenticated routes
        accountRoutes()
        tenantRoutes()
        avatarRoutes()
        teamRoutes()
        notificationRoutes()
        lookupRoutes()
    }
}
