package tech.dokus.foundation.app.network

import ai.dokus.foundation.domain.asbtractions.TokenManager
import ai.dokus.foundation.domain.config.DynamicDokusEndpointProvider
import ai.dokus.foundation.domain.exceptions.DokusException
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.plugins.resources.Resources
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
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
 * Configures the HTTP client to use a dynamic endpoint for self-hosted server support.
 *
 * Unlike [withDokusEndpoint] which uses compile-time BuildKonfig values,
 * this function uses runtime values from the user's selected server configuration.
 *
 * @param endpointProvider Provider of the current endpoint configuration
 */
fun HttpClientConfig<*>.withDynamicDokusEndpoint(endpointProvider: DynamicDokusEndpointProvider) {
    defaultRequest {
        val endpoint = endpointProvider.currentEndpointSnapshot()

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
                    if (response.status == HttpStatusCode.Unauthorized) {
                        onUnauthorized()
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

/**
 * Adds an `Authorization: Bearer ...` header per-request using the latest token.
 *
 * This avoids in-memory token caching issues (e.g., after tenant selection the access token
 * changes and must be used immediately for subsequent requests).
 */
fun HttpClientConfig<*>.withDynamicBearerAuth(tokenManager: TokenManager) {
    install(DynamicBearerAuthPlugin) {
        this.tokenManager = tokenManager
    }
}

private class DynamicBearerAuthConfig {
    lateinit var tokenManager: TokenManager
}

private val DynamicBearerAuthPlugin = createClientPlugin(
    name = "DynamicBearerAuth",
    createConfiguration = ::DynamicBearerAuthConfig
) {
    val tokenManager = pluginConfig.tokenManager

    onRequest { request, _ ->
        if (request.headers[HttpHeaders.Authorization] != null) return@onRequest

        val token = runCatching { tokenManager.getValidAccessToken() }.getOrNull()
        if (!token.isNullOrBlank()) {
            request.headers.append(HttpHeaders.Authorization, "Bearer $token")
        }
    }
}
