package ai.dokus.foundation.ktor.configure

import ai.dokus.foundation.domain.exceptions.DokusException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

@Serializable
data class ErrorResponse(
    val error: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

fun Application.configureErrorHandling() {
    val logger = LoggerFactory.getLogger("ErrorHandler")

    install(StatusPages) {
        exception<DokusException> { call, cause ->
            val errorName = cause::class.simpleName ?: "DokusException"

            if (cause.httpStatusCode >= 500) {
                logger.error("DokusException [$errorName]: ${cause.message}", cause)
            } else {
                logger.warn("DokusException [$errorName]: ${cause.message}")
            }

            call.respond<DokusException>(HttpStatusCode.fromValue(cause.httpStatusCode), cause)
        }

        exception<IllegalArgumentException> { call, cause ->
            logger.warn("IllegalArgumentException: ${cause.message}")
            call.respond(
                HttpStatusCode.BadRequest,
                DokusException.Validation.Other,
            )
        }

        exception<NoSuchElementException> { call, cause ->
            logger.warn("NoSuchElementException: ${cause.message}")
            call.respond(
                HttpStatusCode.NotFound,
                ErrorResponse(
                    error = "NotFound",
                    message = "Resource not found"
                )
            )
        }

        exception<Throwable> { call, cause ->
            logger.error("Unhandled exception", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(
                    error = "InternalServerError",
                    message = "An unexpected error occurred"
                )
            )
        }
    }
}