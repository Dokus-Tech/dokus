package tech.dokus.backend.routes.auth

import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.model.EntityLookupResponse
import tech.dokus.domain.routes.Lookup
import tech.dokus.foundation.ktor.lookup.CbeApiClient
import tech.dokus.foundation.ktor.security.authenticateJwt
import tech.dokus.foundation.ktor.utils.loggerFor
import io.ktor.http.HttpStatusCode
import io.ktor.server.resources.get
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import org.koin.ktor.ext.inject

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
            val name = route.name.trim()

            if (name.length < 3) {
                throw DokusException.BadRequest("Company name must be at least 3 characters")
            }

            val results = cbeApiClient.searchByName(name).getOrElse { e ->
                logger.error("CBE API lookup failed for '$name'", e)
                throw DokusException.InternalError("Company lookup failed. Please try again.")
            }

            val response = EntityLookupResponse(
                results = results,
                query = name,
                totalCount = results.size
            )

            call.respond(HttpStatusCode.OK, response)
        }
    }
}
