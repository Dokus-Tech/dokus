package tech.dokus.foundation.app.network

import ai.dokus.foundation.domain.asbtractions.TokenManager
import ai.dokus.foundation.domain.config.DynamicDokusEndpointProvider
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

internal expect fun createDokusHttpClient(block: HttpClientConfig<*>.() -> Unit): HttpClient

/**
 * Creates a basic HTTP client using dynamic endpoint configuration.
 *
 * This variant supports self-hosted servers by using [DynamicDokusEndpointProvider]
 * to get the current server configuration at runtime.
 *
 * @param endpointProvider Provider for dynamic endpoint configuration
 * @param onAuthenticationFailed Callback invoked on 401 responses
 * @param block Additional client configuration
 */
fun createDynamicBaseHttpClient(
    endpointProvider: DynamicDokusEndpointProvider,
    block: HttpClientConfig<*>.() -> Unit = {},
) = createDokusHttpClient {
    expectSuccess = false
    withJsonContentNegotiation()
    withResources()
    withDynamicDokusEndpoint(endpointProvider)
    withLogging()
    withResponseValidation()
    block()
}

/**
 * Creates an authenticated HTTP client using dynamic endpoint configuration.
 *
 * This variant supports self-hosted servers by using [DynamicDokusEndpointProvider]
 * to get the current server configuration at runtime.
 *
 * @param endpointProvider Provider for dynamic endpoint configuration
 * @param tokenManager Manages access and refresh tokens
 * @param onAuthenticationFailed Callback invoked on 401 responses
 */
fun createDynamicAuthenticatedHttpClient(
    endpointProvider: DynamicDokusEndpointProvider,
    tokenManager: TokenManager,
    onAuthenticationFailed: suspend () -> Unit = {}
) = createDynamicBaseHttpClient(endpointProvider) {
    withDynamicBearerAuth(tokenManager)
    withUnauthorizedRefreshRetry(
        tokenManager = tokenManager,
        onAuthenticationFailed = onAuthenticationFailed,
        maxRetries = 1
    )
}
