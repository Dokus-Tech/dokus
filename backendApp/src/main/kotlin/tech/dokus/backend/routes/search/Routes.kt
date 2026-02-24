package tech.dokus.backend.routes.search

import io.ktor.server.application.Application
import io.ktor.server.routing.routing

fun Application.configureSearchRoutes() {
    routing {
        searchRoutes()
    }
}
