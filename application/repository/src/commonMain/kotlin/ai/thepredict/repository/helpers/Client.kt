package ai.thepredict.repository.helpers

import ai.thepredict.configuration.ServerEndpoint
import ai.thepredict.domain.api.OperationResult
import ai.thepredict.repository.httpClient
import kotlinx.coroutines.withContext
import kotlinx.rpc.RPCClient
import kotlinx.rpc.RemoteService
import kotlinx.rpc.krpc.ktor.client.KtorRPCClient
import kotlinx.rpc.krpc.ktor.client.installRPC
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.ktor.client.rpcConfig
import kotlinx.rpc.krpc.serialization.json.json
import kotlin.coroutines.CoroutineContext

internal class ServiceProvider<ServiceType : RemoteService>(
    val coroutineContext: CoroutineContext,
    private val endpoint: ServerEndpoint,
    private val createService: suspend RPCClient.() -> ServiceType,
) {
    private var service: ServiceType? = null

    private suspend fun createClientAndService(): Result<ServiceType> {
        return withContext(coroutineContext) {
            runCatching {
                val client = createClient(endpoint)
                return@runCatching createService(client)
            }
        }
    }

    suspend inline fun <reified ReturnType> withService(
        onException: ReturnType,
        crossinline func: suspend ServiceType.() -> ReturnType,
    ): ReturnType {
        val currentService = service
        if (currentService == null) {
            val newService = withContext(coroutineContext) {
                createClientAndService().getOrNull()
            }
            if (newService == null) {
                // TODO: Log it
                return onException
            }
            service = newService
            return func(newService)
        }
        return func(currentService)
    }

    suspend fun withServiceOrFailure(
        func: suspend ServiceType.() -> OperationResult,
    ): OperationResult {
        return withService<OperationResult>(
            onException = OperationResult.Failure,
            func = func
        )
    }
}

suspend inline fun createClient(endpoint: ServerEndpoint): KtorRPCClient {
    val ktorClient = httpClient {
        installRPC {
            waitForServices = true // default parameter
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