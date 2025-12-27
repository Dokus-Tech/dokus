package tech.dokus.foundation.app.network

import tech.dokus.domain.asbtractions.TokenManager
import tech.dokus.domain.config.DynamicDokusEndpointProvider
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
 * @param connectionMonitor Optional monitor for tracking connection state based on request results
 * @param block Additional client configuration
 */
fun createDynamicBaseHttpClient(
    endpointProvider: DynamicDokusEndpointProvider,
    connectionMonitor: ServerConnectionMonitor? = null,
    block: HttpClientConfig<*>.() -> Unit = {},
) = createDokusHttpClient {
    expectSuccess = false
    withJsonContentNegotiation()
    withResources()
    withDynamicDokusEndpoint(endpointProvider)
    withLogging()
    withResponseValidation()
    connectionMonitor?.let { withConnectionMonitoring(it) }
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
 * @param connectionMonitor Optional monitor for tracking connection state based on request results
 * @param onAuthenticationFailed Callback invoked on 401 responses
 */
fun createDynamicAuthenticatedHttpClient(
    endpointProvider: DynamicDokusEndpointProvider,
    tokenManager: TokenManager,
    connectionMonitor: ServerConnectionMonitor? = null,
    onAuthenticationFailed: suspend () -> Unit = {}
) = createDynamicBaseHttpClient(endpointProvider, connectionMonitor) {
    withDynamicBearerAuth(tokenManager)
    withUnauthorizedRefreshRetry(
        tokenManager = tokenManager,
        onAuthenticationFailed = onAuthenticationFailed,
        maxRetries = 1
    )
}
