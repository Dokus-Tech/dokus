package ai.dokus.foundation.network

import ai.dokus.foundation.domain.config.DokusEndpoint
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
 * Creates an RPC client for the given endpoint.
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
