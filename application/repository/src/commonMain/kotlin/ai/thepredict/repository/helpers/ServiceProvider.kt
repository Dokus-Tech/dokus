package ai.thepredict.repository.helpers

import ai.thepredict.configuration.ServerEndpoint
import ai.thepredict.domain.api.OperationResult
import ai.thepredict.repository.httpClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.rpc.RPCClient
import kotlinx.rpc.RemoteService
import kotlinx.rpc.krpc.ktor.client.KtorRPCClient
import kotlinx.rpc.krpc.ktor.client.installRPC
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.ktor.client.rpcConfig
import kotlinx.rpc.krpc.serialization.json.json
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds

internal class ServiceProvider<ServiceType : RemoteService>(
    private val coroutineContext: CoroutineContext,
    private val endpoint: ServerEndpoint,
    private val createService: suspend RPCClient.() -> ServiceType,
) {
    private var service: ServiceType? = null

    private suspend fun createClientAndService(): Result<ServiceType> = runCatching {
        val client = createClient(endpoint)
        return@runCatching createService(client)
    }

    suspend fun <ReturnType> withService(
        onException: ReturnType,
        retryAttempt: Boolean = false,
        func: suspend ServiceType.() -> ReturnType,
    ): ReturnType = withContext(coroutineContext) {
        val currentService = service
        val result: Result<ReturnType> = if (currentService == null) {
            val newService = createClientAndService().getOrNull()
            if (newService == null) {
                // TODO: Log it
                return@withContext onException
            }
            service = newService

            runCatching { func(newService) }
        } else {
            runCatching { func(currentService) }
        }

        if (result.exceptionOrNull() != null && !retryAttempt) { // TODO: Handle connection exception
            service = null
            delay(1.seconds)
            return@withContext withService(onException, true, func)
        }
        return@withContext result.getOrNull() ?: onException
    }

    suspend fun withServiceOrFailure(
        func: suspend ServiceType.() -> OperationResult,
    ): OperationResult = withService(
        onException = OperationResult.Failure,
        func = func
    )
}

private suspend inline fun createClient(endpoint: ServerEndpoint): KtorRPCClient {
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