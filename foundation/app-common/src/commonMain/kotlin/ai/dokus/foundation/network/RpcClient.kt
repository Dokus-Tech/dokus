package ai.dokus.foundation.network

import ai.dokus.foundation.domain.asbtractions.TokenManager
import ai.dokus.foundation.domain.config.DokusEndpoint
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.http.encodedPath
import kotlinx.rpc.annotations.Rpc
import kotlinx.rpc.krpc.ktor.client.KtorRpcClient
import kotlinx.rpc.krpc.ktor.client.installKrpc
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.ktor.client.rpcConfig
import kotlinx.rpc.krpc.serialization.json.json
import kotlinx.rpc.withService

/**
 * Creates an RPC client for the given endpoint (unauthenticated).
 *
 * @param endpoint Backend service endpoint configuration
 * @param waitForServices Whether to wait for services to be available (default: true)
 * @return KtorRpcClient instance or null if connection fails
 */
fun createRpcClient(endpoint: DokusEndpoint, waitForServices: Boolean = true): KtorRpcClient? {
    return runCatching {
        createDokusHttpClient {
            withLogging()
            install(WebSockets)
            installKrpc {
                if (!waitForServices) {
                    connector {
                        dontWait()
                    }
                }
            }
        }.rpc {
            url {
                host = endpoint.host
                port = endpoint.port
                encodedPath = "rpc"
            }

            rpcConfig {
                serialization {
                    json()
                }
            }
        }
    }.getOrNull()
}

/**
 * Creates an authenticated RPC client with JWT bearer token support.
 *
 * IMPORTANT: Use this for all authenticated RPC calls to backend services.
 * Automatically adds JWT tokens to RPC requests.
 *
 * @param endpoint Backend service endpoint configuration
 * @param tokenManager Manages access and refresh tokens
 * @param onAuthenticationFailed Callback invoked on 401 responses
 * @param waitForServices Whether to wait for services to be available (default: true)
 * @return KtorRpcClient instance or null if connection fails
 */
fun createAuthenticatedRpcClient(
    endpoint: DokusEndpoint,
    tokenManager: TokenManager,
    onAuthenticationFailed: suspend () -> Unit = {},
    waitForServices: Boolean = true
): KtorRpcClient? {
    return runCatching {
        createDokusHttpClient {
            withJsonContentNegotiation()
            withDokusEndpoint(endpoint)
            withLogging()
            withResponseValidation {
                onAuthenticationFailed()
            }
            install(Auth) {
                bearer {
                    loadTokens {
                        val accessToken = tokenManager.getValidAccessToken()
                        val refreshToken = tokenManager.refreshToken()
                        BearerTokens(
                            accessToken = accessToken.orEmpty(),
                            refreshToken = refreshToken.orEmpty()
                        )
                    }
                    refreshTokens {
                        val accessToken = tokenManager.getValidAccessToken()
                        val refreshToken = tokenManager.refreshToken()
                        BearerTokens(
                            accessToken = accessToken.orEmpty(),
                            refreshToken = refreshToken.orEmpty()
                        )
                    }
                }
            }
            install(WebSockets)
            installKrpc {
                if (!waitForServices) {
                    connector {
                        dontWait()
                    }
                }
            }
        }.rpc {
            url {
                host = endpoint.host
                port = endpoint.port
                encodedPath = "rpc"
            }

            rpcConfig {
                serialization {
                    json()
                }
            }
        }
    }.getOrNull()
}

/**
 * Extension function to safely retrieve an RPC service.
 *
 * @return Service instance or null if retrieval fails
 */
inline fun <@Rpc reified T : Any> KtorRpcClient.service(): T? {
    return runCatching { withService<T>() }.getOrNull()
}

/**
 * Infix operator for fallback pattern: `rpcClient?.service<T>() or StubImpl`
 * Returns stub implementation if service is null
 */
inline infix fun <@Rpc reified T : Any> T?.or(other: T): T {
    return this ?: other
}
