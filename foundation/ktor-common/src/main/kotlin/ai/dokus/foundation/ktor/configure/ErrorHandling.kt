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
            val statusCode = when (cause) {
                // Authentication & Authorization
                is DokusException.NotAuthenticated -> HttpStatusCode.Unauthorized
                is DokusException.NotAuthorized -> HttpStatusCode.Forbidden

                // User Management
                is DokusException.UserAlreadyExists -> HttpStatusCode.Conflict

                // Connection & Server Errors
                is DokusException.ConnectionError -> HttpStatusCode.ServiceUnavailable
                is DokusException.InternalError -> HttpStatusCode.InternalServerError
                is DokusException.Unknown -> HttpStatusCode.InternalServerError

                // Validation Errors - User Input (400 Bad Request)
                is DokusException.InvalidEmail -> HttpStatusCode.BadRequest
                is DokusException.WeakPassword -> HttpStatusCode.BadRequest
                is DokusException.PasswordDoNotMatch -> HttpStatusCode.BadRequest
                is DokusException.InvalidFirstName -> HttpStatusCode.BadRequest
                is DokusException.InvalidLastName -> HttpStatusCode.BadRequest
                is DokusException.InvalidTaxNumber -> HttpStatusCode.BadRequest
                is DokusException.InvalidWorkspaceName -> HttpStatusCode.BadRequest

                // Financial Validation Errors (400 Bad Request)
                is DokusException.InvalidVatNumber -> HttpStatusCode.BadRequest
                is DokusException.InvalidIban -> HttpStatusCode.BadRequest
                is DokusException.InvalidBic -> HttpStatusCode.BadRequest
                is DokusException.InvalidPeppolId -> HttpStatusCode.BadRequest
                is DokusException.InvalidInvoiceNumber -> HttpStatusCode.BadRequest
                is DokusException.InvalidMoney -> HttpStatusCode.BadRequest
                is DokusException.InvalidVatRate -> HttpStatusCode.BadRequest
                is DokusException.InvalidPercentage -> HttpStatusCode.BadRequest
                is DokusException.InvalidQuantity -> HttpStatusCode.BadRequest

                // Address Validation Errors (400 Bad Request)
                is DokusException.InvalidAddress.InvalidStreetName -> HttpStatusCode.BadRequest
                is DokusException.InvalidAddress.InvalidCity -> HttpStatusCode.BadRequest
                is DokusException.InvalidAddress.InvalidPostalCode -> HttpStatusCode.BadRequest
                is DokusException.InvalidAddress.InvalidCountry -> HttpStatusCode.BadRequest
            }

            val errorName = cause::class.simpleName ?: "DokusException"

            if (statusCode.value >= 500) {
                logger.error("DokusException [$errorName]: ${cause.message}", cause)
            } else {
                logger.warn("DokusException [$errorName]: ${cause.message}")
            }

            call.respond(
                statusCode,
                ErrorResponse(
                    error = errorName,
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