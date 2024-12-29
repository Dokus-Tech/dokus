package ai.thepredict.identity

import ai.thepredict.configuration.ServerEndpoint
import ai.thepredict.identity.api.IdentityRemoteService
import io.ktor.client.HttpClient
import kotlinx.coroutines.runBlocking
import kotlinx.rpc.krpc.RPCConfig
import kotlinx.rpc.krpc.ktor.client.KtorRPCClient
import kotlinx.rpc.krpc.ktor.client.installRPC
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.ktor.client.rpcConfig
import kotlinx.rpc.krpc.rpcClientConfig
import kotlinx.rpc.krpc.serialization.json.json
import kotlinx.rpc.krpc.streamScoped
import kotlinx.rpc.withService

fun main() = runBlocking {
    val config: RPCConfig.Client =
        rpcClientConfig { // same for RPCConfig.Server with rpcServerConfig
            waitForServices = true // default parameter
            serialization {
                json()
            }
        }

    val ktorClient = HttpClient {
        installRPC {
            waitForServices = false // default parameter
            serialization {
                json()
            }
        }
    }

    val client: KtorRPCClient = ktorClient.rpc {
        url {
            host = "predict.local"
            port = ServerEndpoint.Gateway().externalPort
        }

        rpcConfig {
            serialization {
                json()
            }
        }
    }

    val identity = client.withService<IdentityRemoteService>()

    streamScoped {
        identity.myWorkspaces().collect {
            println(it.name)
        }
    }
    println("END")
    ktorClient.close()
}