package ai.dokus.foundation.network

import ai.dokus.foundation.domain.config.DokusEndpoint
import ai.dokus.foundation.domain.config.DynamicEndpoint
import ai.dokus.foundation.domain.exceptions.DokusException
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.resources.Resources
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

fun HttpClientConfig<*>.withJsonContentNegotiation() {
    install(ContentNegotiation) {
        json(Json {
            encodeDefaults = true
            isLenient = true
            coerceInputValues = true
            ignoreUnknownKeys = true
        })
    }
}

/**
 * Configures the HTTP client to use the gateway endpoint with path prefix.
 * All requests go through the Traefik gateway, with the path prefix prepended.
 *
 * @deprecated Use [withDynamicDokusEndpoint] for dynamic server selection support.
 */
fun HttpClientConfig<*>.withDokusEndpoint(endpoint: DokusEndpoint) {
    defaultRequest {
        host = endpoint.gatewayHost
        port = endpoint.gatewayPort
        url {
            protocol = when (endpoint.gatewayProtocol) {
                "https" -> URLProtocol.HTTPS
                else -> URLProtocol.HTTP
            }
            // Path prefix is applied in routes via type-safe resources
        }
    }
}

/**
 * Configures the HTTP client to use a dynamic endpoint for self-hosted server support.
 *
 * Unlike [withDokusEndpoint] which uses compile-time BuildKonfig values,
 * this function uses runtime values from the user's selected server configuration.
 *
 * @param endpoint The dynamic endpoint configuration from [DynamicDokusEndpointProvider]
 */
fun HttpClientConfig<*>.withDynamicDokusEndpoint(endpoint: DynamicEndpoint) {
    defaultRequest {
        host = endpoint.host
        this.port = endpoint.port
        url {
            protocol = when (endpoint.protocol) {
                "https" -> URLProtocol.HTTPS
                else -> URLProtocol.HTTP
            }
            // Path prefix is applied in routes via type-safe resources
        }
    }
}

fun HttpClientConfig<*>.withResponseValidation(onUnauthorized: suspend () -> Unit = {}) {
    HttpResponseValidator {
        validateResponse { response: HttpResponse ->
            if (response.status.isSuccess()) return@validateResponse
            runCatching { response.body<DokusException>() }.fold(
                onSuccess = { exception ->
                    // For rate limit exceptions, try to extract Retry-After header
                    if (exception is DokusException.TooManyLoginAttempts) {
                        val retryAfter = parseRetryAfterHeader(response)
                        throw DokusException.TooManyLoginAttempts(
                            retryAfterSeconds = retryAfter ?: exception.retryAfterSeconds
                        )
                    }
                    throw exception
                },
                onFailure = {
                    when (response.status) {
                        HttpStatusCode.Unauthorized -> {
                            onUnauthorized()
                            throw DokusException.NotAuthenticated()
                        }

                        HttpStatusCode.Forbidden -> {
                            throw DokusException.NotAuthorized()
                        }

                        HttpStatusCode.NotFound -> throw DokusException.NotFound()

                        HttpStatusCode.TooManyRequests -> {
                            val retryAfter = parseRetryAfterHeader(response)
                            throw DokusException.TooManyLoginAttempts(
                                retryAfterSeconds = retryAfter ?: 60
                            )
                        }

                        else -> throw DokusException.Unknown(it)
                    }
                }
            )
        }
    }
}

/**
 * Parse the Retry-After header from an HTTP response.
 * Supports both seconds format and HTTP-date format (falls back to default).
 *
 * @return Number of seconds to wait, or null if header is missing/invalid
 */
private fun parseRetryAfterHeader(response: HttpResponse): Int? {
    val retryAfter = response.headers[HttpHeaders.RetryAfter] ?: return null
    return retryAfter.toIntOrNull()
}

fun HttpClientConfig<*>.withLogging(logLevel: LogLevel = LogLevel.ALL) {
    install(Logging) {
        logger = Logger.SIMPLE
        level = logLevel
    }
}

fun HttpClientConfig<*>.withResources() {
    install(Resources)
}
