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
            if (!name.isValid && !number.isValid) {
                logger.error("CBE API lookup failed for '$name', '$number'")
                throw DokusException.BadRequest("Invalid or missing name or number")
            }

            val nameResults = if (name.isValid) {
                cbeApiClient.searchByName(name)
            } else Result.success(emptyList())
            val numberResults = if (number.isValid) {
                cbeApiClient.searchByVat(number)
            } else Result.success(emptyList())

            if (nameResults.isFailure && numberResults.isFailure) {
                logger.error(
                    "CBE API lookup failed for '$name', '$number'",
                    nameResults.exceptionOrNull() ?: numberResults.exceptionOrNull()
                )
                throw DokusException.InternalError("CBE API lookup failed")
            }

            val results = buildList {
                numberResults.onSuccess { addAll(it) }
                nameResults.onSuccess { addAll(it) }
            }
            val response = EntityLookupResponse(
                results = results,
                query = "${name}${number}",
            )

            call.respond<EntityLookupResponse>(HttpStatusCode.OK, response)
        }
    }
}
