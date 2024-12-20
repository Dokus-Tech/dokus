package ai.thepredict.common

import ai.thepredict.configuration.ServerEndpoint
import ai.thepredict.configuration.info
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

inline fun Route.addInfoResponse(endpoint: ServerEndpoint) {
    get("/info") {
        call.respondText(endpoint.info)
    }
}