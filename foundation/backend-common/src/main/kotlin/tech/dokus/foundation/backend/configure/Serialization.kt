package tech.dokus.foundation.backend.configure

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import tech.dokus.domain.utils.json

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(json)
    }
}
