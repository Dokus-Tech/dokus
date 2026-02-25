package tech.dokus.backend.routes.search

import io.ktor.http.HttpStatusCode
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import org.koin.ktor.ext.inject
import tech.dokus.backend.services.search.SearchService
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.model.SearchSignalEventRequest
import tech.dokus.domain.routes.Search
import tech.dokus.foundation.backend.security.authenticateJwt
import tech.dokus.foundation.backend.security.dokusPrincipal

fun Route.searchRoutes() {
    val searchService by inject<SearchService>()

    authenticateJwt {
        get<Search> { route ->
            val principal = dokusPrincipal
            val tenantId = principal.requireTenantId()
            if (route.query.length > 200) {
                throw DokusException.BadRequest("Search query too long")
            }
            if (route.limit < 1 || route.limit > 100) {
                throw DokusException.BadRequest("Limit must be between 1 and 100")
            }
            if (route.suggestionLimit < 1 || route.suggestionLimit > 50) {
                throw DokusException.BadRequest("Suggestion limit must be between 1 and 50")
            }

            val response = searchService.unifiedSearch(
                tenantId = tenantId,
                userId = principal.userId,
                query = route.query,
                scope = route.scope,
                preset = route.preset,
                limit = route.limit,
                suggestionLimit = route.suggestionLimit,
            ).getOrElse {
                throw DokusException.InternalError("Search failed. Please try again later.")
            }

            call.respond(HttpStatusCode.OK, response)
        }

        post<Search.Events> {
            val principal = dokusPrincipal
            val tenantId = principal.requireTenantId()
            val request = call.receive<SearchSignalEventRequest>()

            searchService.recordSignal(
                tenantId = tenantId,
                userId = principal.userId,
                request = request,
            ).getOrElse {
                throw DokusException.InternalError("Failed to record search event.")
            }

            call.respond(HttpStatusCode.NoContent)
        }
    }
}
