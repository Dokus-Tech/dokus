package ai.dokus.auth.backend.routes

import ai.dokus.foundation.domain.ids.ClientId
import ai.dokus.foundation.domain.ids.VatNumber
import ai.dokus.foundation.domain.model.ClientStats
import ai.dokus.foundation.ktor.security.authenticateJwt
import ai.dokus.foundation.ktor.security.dokusPrincipal
import ai.dokus.foundation.ktor.services.ClientService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Request DTO for creating a client
 */
@Serializable
data class CreateClientRequest(
    val name: String,
    val email: String? = null,
    val phone: String? = null,
    val vatNumber: String? = null,
    val addressLine1: String? = null,
    val addressLine2: String? = null,
    val city: String? = null,
    val postalCode: String? = null,
    val country: String? = null,
    val contactPerson: String? = null,
    val notes: String? = null
)

/**
 * Request DTO for updating a client
 */
@Serializable
data class UpdateClientRequest(
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val vatNumber: String? = null,
    val addressLine1: String? = null,
    val addressLine2: String? = null,
    val city: String? = null,
    val postalCode: String? = null,
    val country: String? = null,
    val contactPerson: String? = null,
    val notes: String? = null
)

/**
 * Client routes for client management operations:
 * - Create client
 * - Get client by ID
 * - List/search clients
 * - Update client
 * - Delete client
 * - Find by Peppol ID
 * - Get client stats
 */
@OptIn(ExperimentalUuidApi::class)
fun Route.clientRoutes() {
    val clientService by inject<ClientService>()

    route("/api/v1/clients") {
        authenticateJwt {
            /**
             * POST /api/v1/clients
             * Create a new client
             */
            post {
                val principal = dokusPrincipal
                val tenantId = principal.requireTenantId()
                val request = call.receive<CreateClientRequest>()

                val vatNumberValue = request.vatNumber?.let { VatNumber(it) }

                val client = clientService.create(
                    tenantId = tenantId,
                    name = request.name,
                    email = request.email,
                    vatNumber = vatNumberValue,
                    addressLine1 = request.addressLine1,
                    addressLine2 = request.addressLine2,
                    city = request.city,
                    postalCode = request.postalCode,
                    country = request.country,
                    contactPerson = request.contactPerson,
                    phone = request.phone,
                    notes = request.notes
                )

                call.respond(HttpStatusCode.Created, client)
            }

            /**
             * GET /api/v1/clients/{id}
             * Get client by ID
             */
            get("/{id}") {
                val principal = dokusPrincipal
                val tenantId = principal.requireTenantId()
                val idParam = call.parameters["id"]
                    ?: throw IllegalArgumentException("Client ID is required")

                val clientId = ClientId(Uuid.parse(idParam))
                val client = clientService.findById(clientId)
                    ?: throw IllegalArgumentException("Client not found: $clientId")

                // Verify tenant isolation
                if (client.tenantId != tenantId) {
                    throw IllegalArgumentException("Client does not belong to tenant: $tenantId")
                }

                call.respond(HttpStatusCode.OK, client)
            }

            /**
             * GET /api/v1/clients
             * List/search clients with optional query parameters
             */
            get {
                val principal = dokusPrincipal
                val tenantId = principal.requireTenantId()
                val search = call.request.queryParameters["search"]
                val activeOnly = call.request.queryParameters["activeOnly"]?.toBoolean() ?: true

                val clients = if (search != null) {
                    clientService.search(tenantId, search, activeOnly)
                } else {
                    clientService.listByTenant(tenantId, activeOnly)
                }

                call.respond(HttpStatusCode.OK, clients)
            }

            /**
             * PUT /api/v1/clients/{id}
             * Update an existing client
             */
            put("/{id}") {
                val principal = dokusPrincipal
                val tenantId = principal.requireTenantId()
                val idParam = call.parameters["id"]
                    ?: throw IllegalArgumentException("Client ID is required")

                val clientId = ClientId(Uuid.parse(idParam))
                val request = call.receive<UpdateClientRequest>()

                // Verify client exists and belongs to tenant
                val existingClient = clientService.findById(clientId)
                    ?: throw IllegalArgumentException("Client not found: $clientId")

                if (existingClient.tenantId != tenantId) {
                    throw IllegalArgumentException("Client does not belong to tenant: $tenantId")
                }

                // Convert String vatNumber to VatNumber value class if provided
                val vatNumberValue = request.vatNumber?.let { VatNumber(it) }

                clientService.update(
                    clientId = clientId,
                    name = request.name,
                    email = request.email,
                    vatNumber = vatNumberValue,
                    addressLine1 = request.addressLine1,
                    addressLine2 = request.addressLine2,
                    city = request.city,
                    postalCode = request.postalCode,
                    country = request.country,
                    contactPerson = request.contactPerson,
                    phone = request.phone,
                    notes = request.notes
                )

                // Return updated client
                val updatedClient = clientService.findById(clientId)
                    ?: throw IllegalStateException("Client disappeared after update: $clientId")

                call.respond(HttpStatusCode.OK, updatedClient)
            }

            /**
             * DELETE /api/v1/clients/{id}
             * Delete (soft delete) a client
             */
            delete("/{id}") {
                val principal = dokusPrincipal
                val tenantId = principal.requireTenantId()
                val idParam = call.parameters["id"]
                    ?: throw IllegalArgumentException("Client ID is required")

                val clientId = ClientId(Uuid.parse(idParam))

                // Verify client exists and belongs to tenant
                val client = clientService.findById(clientId)
                    ?: throw IllegalArgumentException("Client not found: $clientId")

                if (client.tenantId != tenantId) {
                    throw IllegalArgumentException("Client does not belong to tenant: $tenantId")
                }

                clientService.delete(clientId)
                call.respond(HttpStatusCode.NoContent)
            }

            /**
             * GET /api/v1/clients/by-peppol/{peppolId}
             * Find client by Peppol participant ID
             */
            get("/by-peppol/{peppolId}") {
                val principal = dokusPrincipal
                val tenantId = principal.requireTenantId()
                val peppolId = call.parameters["peppolId"]
                    ?: throw IllegalArgumentException("Peppol ID is required")

                // Search through all clients and filter by peppolId
                val allClients = clientService.listByTenant(tenantId)
                val client = allClients.firstOrNull { it.peppolId == peppolId }

                if (client != null) {
                    call.respond(HttpStatusCode.OK, client)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }

            /**
             * GET /api/v1/clients/stats
             * Get client statistics for current tenant
             */
            get("/stats") {
                val principal = dokusPrincipal
                val tenantId = principal.requireTenantId()

                val allClients = clientService.listByTenant(tenantId, activeOnly = false)
                val activeClients = allClients.filter { it.isActive }
                val inactiveClients = allClients.filter { !it.isActive }
                val peppolEnabledClients = allClients.filter { it.peppolEnabled }

                val stats = ClientStats(
                    totalClients = allClients.size.toLong(),
                    activeClients = activeClients.size.toLong(),
                    inactiveClients = inactiveClients.size.toLong(),
                    peppolEnabledClients = peppolEnabledClients.size.toLong()
                )

                call.respond(HttpStatusCode.OK, stats)
            }
        }
    }
}
