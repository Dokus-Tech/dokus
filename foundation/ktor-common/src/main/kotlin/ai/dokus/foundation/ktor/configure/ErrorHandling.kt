package be.police.pulse.foundation.ktor.configure

import be.police.pulse.domain.exceptions.PulseException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
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
        exception<PulseException> { call, cause ->
            val statusCode = when (cause) {
                is PulseException.InvalidCredentials -> HttpStatusCode.Unauthorized
                is PulseException.UserNotFound -> HttpStatusCode.NotFound
                is PulseException.UserLocked -> HttpStatusCode.Forbidden
                is PulseException.AccountNotVerified -> HttpStatusCode.Forbidden
                is PulseException.PasswordExpired -> HttpStatusCode.Forbidden
                is PulseException.InvalidToken -> HttpStatusCode.Unauthorized
                is PulseException.TokenExpired -> HttpStatusCode.Unauthorized
                is PulseException.TooManyRequests -> HttpStatusCode.TooManyRequests
                is PulseException.EntityAlreadyExists -> HttpStatusCode.Conflict
                is PulseException.ValidationError -> HttpStatusCode.BadRequest
                is PulseException.PermissionDenied -> HttpStatusCode.Forbidden
                is PulseException.SystemError -> HttpStatusCode.InternalServerError
                else -> HttpStatusCode.InternalServerError
            }

            logger.warn("PulseException: ${cause.message}")
            call.respond(
                statusCode,
                ErrorResponse(
                    error = cause.errorCode,
                    message = cause.message ?: "An error occurred"
                )
            )
        }

        exception<IllegalArgumentException> { call, cause ->
            logger.warn("IllegalArgumentException: ${cause.message}")
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(
                    error = "BadRequest",
                    message = cause.message ?: "Invalid request"
                )
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