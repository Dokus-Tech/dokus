package tech.dokus.backend.routes.contacts

import io.ktor.server.application.Application
import io.ktor.server.routing.routing

/**
 * Registers all Contacts REST API routes.
 *
 * Routes registered:
 * - /api/v1/contacts - Contact CRUD operations
 * - /api/v1/contacts/customers - Customer listing
 * - /api/v1/contacts/vendors - Vendor listing
 * - /api/v1/contacts/{id}/notes - Contact notes management
 * - /api/v1/contacts/{id}/peppol - Peppol settings
 * - /api/v1/contacts/{id}/activity - Activity summary
 * - /api/v1/contacts/{id}/merge-into/{targetId} - Contact merging
 */
fun Application.configureContactsRoutes() {
    routing {
        contactRoutes()
    }
}
