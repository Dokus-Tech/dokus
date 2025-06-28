package ai.thepredict.gateway

import ai.thepredict.apispec.service.ContactsRemoteService
import ai.thepredict.configuration.ServerEndpoint
import io.ktor.client.HttpClient
import kotlinx.coroutines.runBlocking
import kotlinx.rpc.krpc.KrpcConfig
import kotlinx.rpc.krpc.client.KrpcClient
import kotlinx.rpc.krpc.ktor.client.installRPC
import kotlinx.rpc.krpc.ktor.client.rpcConfig
import kotlinx.rpc.krpc.rpcClientConfig
import kotlinx.rpc.krpc.serialization.json.json
import kotlinx.rpc.krpc.streamScoped
import kotlinx.rpc.withService

fun main() = runBlocking {
    val config: KrpcConfig.Client =
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

    val client: KrpcClient = KrpcClient {
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

    val contacts = client.withService<ContactsRemoteService>()

    streamScoped {
        contacts.getAll().collect {
            println(it.name)
        }
    }
    println("END")
    ktorClient.close()
}