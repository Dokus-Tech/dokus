package tech.dokus.foundation.app.network

import ai.dokus.foundation.domain.asbtractions.TokenManager
import ai.dokus.foundation.domain.config.DynamicDokusEndpointProvider
import ai.dokus.foundation.domain.exceptions.DokusException
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.HttpClientCall
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.Sender
import io.ktor.client.plugins.plugin
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.plugins.resources.Resources
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.AttributeKey
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

fun HttpClientConfig<*>.withResponseValidation() {
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
                        HttpStatusCode.Unauthorized -> throw DokusException.NotAuthenticated()

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

/**
 * Retries once on 401 by attempting a token refresh.
 *
 * This is intentionally separate from [withResponseValidation] so we only force logout
 * after the refresh+retry attempt fails.
 */
fun HttpClientConfig<*>.withUnauthorizedRefreshRetry(
    tokenManager: TokenManager,
    onAuthenticationFailed: suspend () -> Unit = {},
    maxRetries: Int = 1,
) {
    require(maxRetries >= 0) { "maxRetries must be >= 0" }

    install(UnauthorizedRefreshRetryPlugin) {
        this.tokenManager = tokenManager
        this.onAuthenticationFailed = onAuthenticationFailed
        this.maxRetries = maxRetries
    }
}

private class UnauthorizedRefreshRetryConfig {
    lateinit var tokenManager: TokenManager
    var onAuthenticationFailed: suspend () -> Unit = {}
    var maxRetries: Int = 1
}

private val UnauthorizedRefreshRetryPlugin = createClientPlugin(
    name = "UnauthorizedRefreshRetry",
    createConfiguration = ::UnauthorizedRefreshRetryConfig
) {
    val tokenManager = pluginConfig.tokenManager
    val onAuthenticationFailed = pluginConfig.onAuthenticationFailed
    val maxRetries = pluginConfig.maxRetries

    client.plugin(HttpSend).intercept { request ->
        executeWithUnauthorizedRefreshRetry(
            request = request,
            tokenManager = tokenManager,
            maxRetries = maxRetries,
            onAuthenticationFailed = onAuthenticationFailed
        )
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

private val UnauthorizedRefreshRetryAttemptKey = AttributeKey<Int>("UnauthorizedRefreshRetryAttempt")

private suspend fun Sender.executeWithUnauthorizedRefreshRetry(
    request: HttpRequestBuilder,
    tokenManager: TokenManager,
    maxRetries: Int,
    onAuthenticationFailed: suspend () -> Unit,
): HttpClientCall {
    var attempts = if (request.attributes.contains(UnauthorizedRefreshRetryAttemptKey)) {
        request.attributes[UnauthorizedRefreshRetryAttemptKey]
    } else {
        0
    }

    while (true) {
        try {
            return execute(request)
        } catch (cause: Throwable) {
            val dokusException = cause as? DokusException
            val isUnauthorized = dokusException?.httpStatusCode == HttpStatusCode.Unauthorized.value
            if (!isUnauthorized) throw cause

            if (attempts >= maxRetries) {
                onAuthenticationFailed()
                throw cause
            }

            attempts += 1
            request.attributes.put(UnauthorizedRefreshRetryAttemptKey, attempts)

            val tokenUsedForFailedRequest = extractBearerToken(request.headers[HttpHeaders.Authorization])
            val latestValidToken = tokenManager.getValidAccessToken()

            if (latestValidToken.isNullOrBlank()) {
                onAuthenticationFailed()
                throw cause
            }

            val tokenForRetry = if (latestValidToken != tokenUsedForFailedRequest) {
                latestValidToken
            } else {
                tokenManager.refreshToken(force = true)
            }

            if (tokenForRetry.isNullOrBlank()) {
                onAuthenticationFailed()
                throw cause
            }

            request.headers.remove(HttpHeaders.Authorization)
            request.headers.append(HttpHeaders.Authorization, "Bearer $tokenForRetry")
        }
    }
}

private fun extractBearerToken(authorizationHeader: String?): String? {
    val headerValue = authorizationHeader?.trim().orEmpty()
    if (headerValue.isBlank()) return null

    val spaceIndex = headerValue.indexOf(' ')
    if (spaceIndex <= 0) return null

    val scheme = headerValue.substring(0, spaceIndex)
    if (!scheme.equals("Bearer", ignoreCase = true)) return null

    val token = headerValue.substring(spaceIndex + 1).trim()
    return token.takeIf { it.isNotBlank() }
}

/**
 * Installs connection monitoring that tracks successful/failed requests.
 *
 * When requests succeed, [ServerConnectionMonitor.reportSuccess] is called.
 * When requests fail with network errors, [ServerConnectionMonitor.reportNetworkError] is called.
 *
 * This allows reactive connection state tracking without health endpoint polling.
 */
fun HttpClientConfig<*>.withConnectionMonitoring(monitor: ServerConnectionMonitor) {
    install(ConnectionMonitorPlugin) {
        this.monitor = monitor
    }
}

private class ConnectionMonitorConfig {
    lateinit var monitor: ServerConnectionMonitor
}

private val ConnectionMonitorPlugin = createClientPlugin(
    name = "ConnectionMonitor",
    createConfiguration = ::ConnectionMonitorConfig
) {
    val monitor = pluginConfig.monitor

    client.plugin(HttpSend).intercept { request ->
        try {
            val call = execute(request)
            // Request succeeded - we're connected
            monitor.reportSuccess()
            call
        } catch (cause: Throwable) {
            // Check if it's a network error
            monitor.reportNetworkError(cause)
            throw cause
        }
    }
}
