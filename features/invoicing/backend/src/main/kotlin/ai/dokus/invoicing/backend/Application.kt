package ai.dokus.invoicing.backend

import ai.dokus.invoicing.backend.config.configureDependencyInjection
import ai.dokus.invoicing.backend.routes.invoiceRoutes
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.slf4j.event.Level
import org.slf4j.LoggerFactory

fun main() {
    embeddedServer(
        Netty,
        port = System.getenv("PORT")?.toIntOrNull() ?: 9092,
        host = "0.0.0.0",
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    val logger = LoggerFactory.getLogger("ai.dokus.invoicing.backend.Application")

    // Content Negotiation
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }

    // Call Logging
    install(CallLogging) {
        level = Level.INFO
    }

    // Status Pages
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            // Log the full exception with stack trace
            logger.error("Request failed: ${cause.message}", cause)
            cause.printStackTrace()  // Print to stderr for immediate visibility
            call.respond(
                io.ktor.http.HttpStatusCode.InternalServerError,
                mapOf("error" to (cause.message ?: "Unknown error"))
            )
        }
    }

    // Dependency Injection
    configureDependencyInjection()

    // Routes
    routing {
        // Health check
        get("/health") {
            call.respond(mapOf("status" to "healthy", "service" to "invoicing"))
        }

        // Metrics
        get("/metrics") {
            call.respond(mapOf("service" to "invoicing", "status" to "ok"))
        }

        // Invoice routes
        invoiceRoutes()
    }
}
