package ai.thepredict.repository.api

import ai.thepredict.configuration.ServerEndpoint
import io.ktor.client.HttpClient
import kotlinx.rpc.krpc.ktor.client.KtorRPCClient
import kotlinx.rpc.krpc.ktor.client.installRPC
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.ktor.client.rpcConfig
import kotlinx.rpc.krpc.serialization.json.json

internal suspend fun createClient(endpoint: ServerEndpoint): KtorRPCClient {
    val ktorClient = HttpClient {
        installRPC {
            waitForServices = false // default parameter
            serialization {
                json()
            }
        }
    }

    return ktorClient.rpc {
        url {
            host = endpoint.externalHost
            port = endpoint.externalPort
        }

        rpcConfig {
            serialization {
                json()
            }
        }
    }
}