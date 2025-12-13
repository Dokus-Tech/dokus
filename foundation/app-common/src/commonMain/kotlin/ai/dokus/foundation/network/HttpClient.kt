package ai.dokus.foundation.network

import ai.dokus.foundation.domain.asbtractions.TokenManager
import ai.dokus.foundation.domain.config.DokusEndpoint
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer

internal expect fun createDokusHttpClient(block: HttpClientConfig<*>.() -> Unit): HttpClient

/**
 * Creates a basic HTTP client with JSON serialization, logging, and response validation.
 *
 * @param dokusEndpoint Backend service endpoint configuration
 * @param onAuthenticationFailed Callback invoked on 401 responses
 * @param block Additional client configuration
 */
fun createBaseHttpClient(
    dokusEndpoint: DokusEndpoint,
    onAuthenticationFailed: suspend () -> Unit = {},
    block: HttpClientConfig<*>.() -> Unit = {},
) = createDokusHttpClient {
    // Let HttpResponseValidator handle non-2xx responses instead of throwing at the engine level.
    expectSuccess = false
    withJsonContentNegotiation()
    withResources()
    withDokusEndpoint(dokusEndpoint)
    withLogging()
    withResponseValidation {
        onAuthenticationFailed()
    }
    block()
}

/**
 * Creates an authenticated HTTP client with JWT bearer token support.
 *
 * Automatically adds JWT tokens to requests and handles token refresh.
 * Triggers logout callback on authentication failure (401).
 *
 * @param dokusEndpoint Backend service endpoint configuration
 * @param tokenManager Manages access and refresh tokens
 * @param onAuthenticationFailed Callback invoked on 401 responses
 */
fun createAuthenticatedHttpClient(
    dokusEndpoint: DokusEndpoint,
    tokenManager: TokenManager,
    onAuthenticationFailed: suspend () -> Unit = {}
) = createBaseHttpClient(dokusEndpoint, onAuthenticationFailed) {
    install(Auth) {
        bearer {
            loadTokens {
                // Only attach the current valid access token. Do NOT trigger a refresh here.
                val accessToken = tokenManager.getValidAccessToken()
                accessToken?.let { BearerTokens(accessToken = it, refreshToken = "") }
            }
            refreshTokens {
                // Attempt to refresh the token only when the server requests it (e.g., 401).
                val newAccessToken = tokenManager.refreshToken()
                if (newAccessToken.isNullOrEmpty()) {
                    onAuthenticationFailed()
                    null
                } else {
                    BearerTokens(accessToken = newAccessToken, refreshToken = "")
                }
            }
        }
    }
}
