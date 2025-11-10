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
                is DokusException.InvalidCredentials -> HttpStatusCode.Unauthorized
                is DokusException.TokenExpired -> HttpStatusCode.Unauthorized
                is DokusException.TokenInvalid -> HttpStatusCode.Unauthorized
                is DokusException.RefreshTokenExpired -> HttpStatusCode.Unauthorized
                is DokusException.RefreshTokenRevoked -> HttpStatusCode.Unauthorized
                is DokusException.SessionExpired -> HttpStatusCode.Unauthorized
                is DokusException.SessionInvalid -> HttpStatusCode.Unauthorized

                // Account Status
                is DokusException.AccountInactive -> HttpStatusCode.Forbidden
                is DokusException.AccountLocked -> HttpStatusCode.Forbidden
                is DokusException.TooManyLoginAttempts -> HttpStatusCode.TooManyRequests

                // Email Verification
                is DokusException.EmailAlreadyVerified -> HttpStatusCode.Conflict
                is DokusException.EmailNotVerified -> HttpStatusCode.Forbidden
                is DokusException.EmailVerificationTokenExpired -> HttpStatusCode.Gone
                is DokusException.EmailVerificationTokenInvalid -> HttpStatusCode.BadRequest

                // Password Reset
                is DokusException.PasswordResetTokenExpired -> HttpStatusCode.Gone
                is DokusException.PasswordResetTokenInvalid -> HttpStatusCode.BadRequest

                // User Management
                is DokusException.UserAlreadyExists -> HttpStatusCode.Conflict

                // Tenant Management
                is DokusException.TenantCreationFailed -> HttpStatusCode.InternalServerError

                // Connection & Server Errors
                is DokusException.ConnectionError -> HttpStatusCode.ServiceUnavailable
                is DokusException.InternalError -> HttpStatusCode.InternalServerError
                is DokusException.Unknown -> HttpStatusCode.InternalServerError

                // Validation Errors - User Input (400 Bad Request)
                is DokusException.Validation.InvalidEmail -> HttpStatusCode.BadRequest
                is DokusException.Validation.WeakPassword -> HttpStatusCode.BadRequest
                is DokusException.Validation.PasswordDoNotMatch -> HttpStatusCode.BadRequest
                is DokusException.Validation.InvalidFirstName -> HttpStatusCode.BadRequest
                is DokusException.Validation.InvalidLastName -> HttpStatusCode.BadRequest
                is DokusException.Validation.InvalidTaxNumber -> HttpStatusCode.BadRequest
                is DokusException.Validation.InvalidWorkspaceName -> HttpStatusCode.BadRequest

                // Financial Validation Errors (400 Bad Request)
                is DokusException.Validation.InvalidVatNumber -> HttpStatusCode.BadRequest
                is DokusException.Validation.InvalidIban -> HttpStatusCode.BadRequest
                is DokusException.Validation.InvalidBic -> HttpStatusCode.BadRequest
                is DokusException.Validation.InvalidPeppolId -> HttpStatusCode.BadRequest
                is DokusException.Validation.InvalidInvoiceNumber -> HttpStatusCode.BadRequest
                is DokusException.Validation.InvalidMoney -> HttpStatusCode.BadRequest
                is DokusException.Validation.InvalidVatRate -> HttpStatusCode.BadRequest
                is DokusException.Validation.InvalidPercentage -> HttpStatusCode.BadRequest
                is DokusException.Validation.InvalidQuantity -> HttpStatusCode.BadRequest

                // Address Validation Errors (400 Bad Request)
                is DokusException.Validation.InvalidAddress.InvalidStreetName -> HttpStatusCode.BadRequest
                is DokusException.Validation.InvalidAddress.InvalidCity -> HttpStatusCode.BadRequest
                is DokusException.Validation.InvalidAddress.InvalidPostalCode -> HttpStatusCode.BadRequest
                is DokusException.Validation.InvalidAddress.InvalidCountry -> HttpStatusCode.BadRequest
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