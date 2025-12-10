package ai.dokus.cashflow.backend.routes

import ai.dokus.cashflow.backend.service.ClientService
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.model.CreateClientRequest
import ai.dokus.foundation.domain.model.UpdateClientRequest
import ai.dokus.foundation.ktor.security.authenticateJwt
import ai.dokus.foundation.ktor.security.dokusPrincipal
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject

/**
 * Client API Routes
 * Base path: /api/v1/clients
 *
 * Provides endpoints for:
 * - CRUD operations on clients
 * - Peppol-specific client operations
 * - Client statistics
 *
 * All routes require JWT authentication and tenant context.
 */
fun Route.clientRoutes() {
    val clientService by inject<ClientService>()

    route("/api/v1/clients") {
        authenticateJwt {

            // ================================================================
            // CRUD OPERATIONS
            // ================================================================

            /**
             * POST /api/v1/clients
             * Create a new client.
             */
            post {
                val tenantId = dokusPrincipal.requireTenantId()
                val request = call.receive<CreateClientRequest>()

                val client = clientService.createClient(tenantId, request)
                    .getOrElse { throw DokusException.InternalError("Failed to create client: ${it.message}") }

                call.respond(HttpStatusCode.Created, client)
            }

            /**
             * GET /api/v1/clients/{clientId}
             * Get a client by ID.
             */
            get("/{clientId}") {
                val tenantId = dokusPrincipal.requireTenantId()
                val clientId = call.parameters.clientId
                    ?: throw DokusException.BadRequest("Client ID is required")

                val client = clientService.getClient(clientId, tenantId)
                    .getOrElse { throw DokusException.InternalError("Failed to fetch client: ${it.message}") }
                    ?: throw DokusException.NotFound("Client not found")

                call.respond(HttpStatusCode.OK, client)
            }

            /**
             * GET /api/v1/clients
             * List clients with optional filters.
             *
             * Query parameters:
             * - search: Search by name, email, or VAT number
             * - activeOnly: Filter by active status (true/false)
             * - peppolEnabled: Filter by Peppol enabled status (true/false)
             * - limit: Max results (default 50, max 200)
             * - offset: Pagination offset (default 0)
             */
            get {
                val tenantId = dokusPrincipal.requireTenantId()
                val searchQuery = call.parameters["search"]
                val isActive = call.parameters["activeOnly"]?.toBooleanStrictOrNull()
                val peppolEnabled = call.parameters["peppolEnabled"]?.toBooleanStrictOrNull()
                val limit = call.parameters.limit
                val offset = call.parameters.offset

                if (limit < 1 || limit > 200) {
                    throw DokusException.BadRequest("Limit must be between 1 and 200")
                }
                if (offset < 0) {
                    throw DokusException.BadRequest("Offset must be non-negative")
                }

                val clients = clientService.listClients(
                    tenantId = tenantId,
                    isActive = isActive,
                    peppolEnabled = peppolEnabled,
                    searchQuery = searchQuery,
                    limit = limit,
                    offset = offset
                )
                    .getOrElse { throw DokusException.InternalError("Failed to list clients: ${it.message}") }

                call.respond(HttpStatusCode.OK, clients)
            }

            /**
             * PUT /api/v1/clients/{clientId}
             * Update a client.
             */
            put("/{clientId}") {
                val tenantId = dokusPrincipal.requireTenantId()
                val clientId = call.parameters.clientId
                    ?: throw DokusException.BadRequest("Client ID is required")

                val request = call.receive<UpdateClientRequest>()

                val client = clientService.updateClient(clientId, tenantId, request)
                    .getOrElse { throw DokusException.InternalError("Failed to update client: ${it.message}") }

                call.respond(HttpStatusCode.OK, client)
            }

            /**
             * DELETE /api/v1/clients/{clientId}
             * Delete a client.
             */
            delete("/{clientId}") {
                val tenantId = dokusPrincipal.requireTenantId()
                val clientId = call.parameters.clientId
                    ?: throw DokusException.BadRequest("Client ID is required")

                clientService.deleteClient(clientId, tenantId)
                    .getOrElse { throw DokusException.InternalError("Failed to delete client: ${it.message}") }

                call.respond(HttpStatusCode.NoContent)
            }

            // ================================================================
            // STATUS MANAGEMENT
            // ================================================================

            /**
             * POST /api/v1/clients/{clientId}/deactivate
             * Deactivate a client (soft delete).
             */
            post("/{clientId}/deactivate") {
                val tenantId = dokusPrincipal.requireTenantId()
                val clientId = call.parameters.clientId
                    ?: throw DokusException.BadRequest("Client ID is required")

                val success = clientService.deactivateClient(clientId, tenantId)
                    .getOrElse { throw DokusException.InternalError("Failed to deactivate client: ${it.message}") }

                if (!success) {
                    throw DokusException.NotFound("Client not found")
                }

                call.respond(HttpStatusCode.OK, mapOf("message" to "Client deactivated"))
            }

            /**
             * POST /api/v1/clients/{clientId}/reactivate
             * Reactivate a deactivated client.
             */
            post("/{clientId}/reactivate") {
                val tenantId = dokusPrincipal.requireTenantId()
                val clientId = call.parameters.clientId
                    ?: throw DokusException.BadRequest("Client ID is required")

                val success = clientService.reactivateClient(clientId, tenantId)
                    .getOrElse { throw DokusException.InternalError("Failed to reactivate client: ${it.message}") }

                if (!success) {
                    throw DokusException.NotFound("Client not found")
                }

                call.respond(HttpStatusCode.OK, mapOf("message" to "Client reactivated"))
            }

            // ================================================================
            // PEPPOL OPERATIONS
            // ================================================================

            /**
             * PATCH /api/v1/clients/{clientId}/peppol
             * Update a client's Peppol settings.
             */
            patch("/{clientId}/peppol") {
                val tenantId = dokusPrincipal.requireTenantId()
                val clientId = call.parameters.clientId
                    ?: throw DokusException.BadRequest("Client ID is required")

                val request = call.receive<UpdateClientPeppolRequest>()

                val client = clientService.updateClientPeppol(
                    clientId = clientId,
                    tenantId = tenantId,
                    peppolId = request.peppolId,
                    peppolEnabled = request.peppolEnabled
                ).getOrElse { throw DokusException.InternalError("Failed to update client Peppol settings: ${it.message}") }

                call.respond(HttpStatusCode.OK, client)
            }

            /**
             * GET /api/v1/clients/peppol-enabled
             * List all Peppol-enabled clients.
             */
            get("/peppol-enabled") {
                val tenantId = dokusPrincipal.requireTenantId()

                val clients = clientService.listPeppolEnabledClients(tenantId)
                    .getOrElse { throw DokusException.InternalError("Failed to list Peppol-enabled clients: ${it.message}") }

                call.respond(HttpStatusCode.OK, clients)
            }

            // ================================================================
            // STATISTICS
            // ================================================================

            /**
             * GET /api/v1/clients/stats
             * Get client statistics for dashboard.
             */
            get("/stats") {
                val tenantId = dokusPrincipal.requireTenantId()

                val stats = clientService.getClientStats(tenantId)
                    .getOrElse { throw DokusException.InternalError("Failed to get client stats: ${it.message}") }

                call.respond(HttpStatusCode.OK, stats)
            }
        }
    }

}

// ================================================================
// REQUEST DTOs
// ================================================================

@Serializable
data class UpdateClientPeppolRequest(
    val peppolId: String?,
    val peppolEnabled: Boolean
)
