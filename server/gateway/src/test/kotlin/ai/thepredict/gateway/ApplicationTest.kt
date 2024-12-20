package ai.thepredict.gateway

import ai.thepredict.configuration.ServerEndpoints
import ai.thepredict.shared.api.ContactsApi
import io.ktor.client.HttpClient
import io.ktor.http.encodedPath
import io.ktor.server.config.configLoaders
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
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
    val config: RPCConfig.Client = rpcClientConfig { // same for RPCConfig.Server with rpcServerConfig
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
            host = "localhost"
            port = ServerEndpoints.Gateway.internalPort
        }

        rpcConfig {
            serialization {
                json()
            }
        }
    }

    val contacts: ContactsApi = client.withService<ContactsApi>()

    streamScoped {
        contacts.my().collect {
            println(it.name)
        }
    }
    println("END")
    ktorClient.close()
}