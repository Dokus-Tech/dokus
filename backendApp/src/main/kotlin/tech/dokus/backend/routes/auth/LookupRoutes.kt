package tech.dokus.backend.routes.auth

import io.ktor.http.HttpStatusCode
import io.ktor.server.resources.get
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import org.koin.ktor.ext.inject
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.model.entity.EntityLookupResponse
import tech.dokus.domain.routes.Lookup
import tech.dokus.foundation.backend.lookup.CbeApiClient
import tech.dokus.foundation.backend.security.authenticateJwt
import tech.dokus.foundation.backend.utils.loggerFor

/**
 * Lookup routes for external data searches (CBE company lookup, etc.)
 */
private val logger = loggerFor("LookupRoutes")

internal fun Route.lookupRoutes() {
    val cbeApiClient by inject<CbeApiClient>()

    authenticateJwt {
        /**
         * GET /api/v1/lookup/company?name={name}
         * Search for companies by name in CBE (Crossroads Bank for Enterprises).
         */
        get<Lookup.Company> { route ->
            val (name, number) = route.name to route.number

            val results = when {
                name.isValid -> cbeApiClient.searchByName(name)
                number.isValid -> cbeApiClient.searchByVat(number)
                else -> throw DokusException.BadRequest("Invalid or missing name or number")
            }.getOrElse {
                logger.error("CBE API lookup failed for '$name'", it)
                throw DokusException.InternalError("Company lookup failed. Please try again.")
            }

            val response = EntityLookupResponse(
                results = results,
                query = "${name}${number}",
                totalCount = results.size
            )

            call.respond<EntityLookupResponse>(HttpStatusCode.OK, response)
        }
    }
}
