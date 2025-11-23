package ai.dokus.foundation.network

import ai.dokus.foundation.domain.asbtractions.TokenManager
import ai.dokus.foundation.domain.config.DokusEndpoint
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
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
fun createRpcClient(endpoint: DokusEndpoint, waitForServices: Boolean = true): KtorRpcClient {
    return createDokusHttpClient {
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
): KtorRpcClient {
    return createDokusHttpClient {
        withJsonContentNegotiation()
        withDokusEndpoint(endpoint)
        withLogging()
        withResponseValidation {
            onAuthenticationFailed()
        }
        install(Auth) {
            bearer {
                // Always send Authorization header preemptively, including on WebSocket handshake
                // Without this, the WS upgrade request may not carry the token, and Ktor won't
                // attach a principal, leading to missing auth context on the server.
                sendWithoutRequest { true }
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
        // WebSockets are required for KRPC transport
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
}

/**
 * Extension function to safely retrieve an RPC service.
 *
 * @return Service instance or null if retrieval fails
 */
inline fun <@Rpc reified T : Any> KtorRpcClient.service(): T {
    return withService<T>()
}

/**
 * Infix operator for a fallback pattern: `rpcClient?.service<T>() or StubImpl`
 * Returns stub implementation if service is null
 */
inline infix fun <@Rpc reified T : Any> T?.or(other: T): T {
    return this ?: other
}
