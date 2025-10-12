package ai.dokus.foundation.ktor.configure

import ai.dokus.foundation.domain.exceptions.DokusException
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
        exception<DokusException> { call, cause ->
            val statusCode = HttpStatusCode.InternalServerError
//            val statusCode = when (cause) {
//                is DokusException.InvalidCredentials -> HttpStatusCode.Unauthorized
//                is DokusException.UserNotFound -> HttpStatusCode.NotFound
//                is DokusException.UserLocked -> HttpStatusCode.Forbidden
//                is DokusException.AccountNotVerified -> HttpStatusCode.Forbidden
//                is DokusException.PasswordExpired -> HttpStatusCode.Forbidden
//                is DokusException.InvalidToken -> HttpStatusCode.Unauthorized
//                is DokusException.TokenExpired -> HttpStatusCode.Unauthorized
//                is DokusException.TooManyRequests -> HttpStatusCode.TooManyRequests
//                is DokusException.EntityAlreadyExists -> HttpStatusCode.Conflict
//                is DokusException.ValidationError -> HttpStatusCode.BadRequest
//                is DokusException.PermissionDenied -> HttpStatusCode.Forbidden
//                is DokusException.SystemError -> HttpStatusCode.InternalServerError
//                else -> HttpStatusCode.InternalServerError
//            }

            logger.warn("DokusException: ${cause.message}")
            call.respond(
                statusCode,
                ErrorResponse(
                    error = cause.cause?.javaClass?.simpleName ?: "DokusException",
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