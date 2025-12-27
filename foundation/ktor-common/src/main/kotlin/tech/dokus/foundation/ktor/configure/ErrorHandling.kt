package tech.dokus.foundation.ktor.configure

import tech.dokus.domain.exceptions.DokusException
import tech.dokus.foundation.ktor.utils.loggerFor
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import java.net.ConnectException
import java.net.SocketTimeoutException

fun Application.configureErrorHandling() {
    val logger = loggerFor("ErrorHandler")

    install(StatusPages) {
        // Handle rate limiting with Retry-After header
        exception<DokusException.TooManyLoginAttempts> { call, cause ->
            logger.warn("Rate limit exceeded: ${cause.message}")
            call.response.headers.append(HttpHeaders.RetryAfter, cause.retryAfterSeconds.toString())
            call.respond<DokusException>(
                HttpStatusCode.TooManyRequests,
                cause,
            )
        }

        exception<DokusException> { call, cause ->
            logger.warn("DokusException: ${cause.message}")
            call.respond<DokusException>(
                HttpStatusCode.fromValue(cause.httpStatusCode),
                cause,
            )
        }

        exception<IllegalArgumentException> { call, cause ->
            logger.warn("IllegalArgumentException: ${cause.message}")
            call.respond<DokusException>(
                HttpStatusCode.BadRequest,
                DokusException.BadRequest(cause.message ?: "Invalid request"),
            )
        }

        exception<NoSuchElementException> { call, cause ->
            logger.warn("NoSuchElementException: ${cause.message}")
            call.respond<DokusException>(
                HttpStatusCode.NotFound,
                DokusException.NotFound(),
            )
        }

        // Map downstream connection failures to a more appropriate 503 instead of 500
        exception<ConnectException> { call, cause ->
            logger.error("Downstream connection failed: ${cause.message}")
            call.respond<DokusException>(
                HttpStatusCode.ServiceUnavailable,
                DokusException.ConnectionError(
                    cause.message ?: "Downstream service is unavailable"
                ),
            )
        }

        // Map downstream timeouts to 504 Gateway Timeout
        exception<SocketTimeoutException> { call, cause ->
            logger.error("Downstream connection timed out: ${cause.message}")
            call.respond<DokusException>(
                HttpStatusCode.GatewayTimeout,
                DokusException.ConnectionError(cause.message ?: "Downstream service timed out"),
            )
        }

        exception<Throwable> { call, cause ->
            logger.error("Unhandled exception", cause)
            call.respond<DokusException>(
                HttpStatusCode.InternalServerError,
                DokusException.InternalError(cause.message ?: "An unexpected error occurred"),
            )
        }
    }
}
