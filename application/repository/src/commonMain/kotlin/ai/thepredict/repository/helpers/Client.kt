package ai.thepredict.repository.helpers

import ai.thepredict.configuration.ServerEndpoint
import ai.thepredict.domain.api.OperationResult
import ai.thepredict.repository.httpClient
import kotlinx.rpc.RemoteService
import kotlinx.rpc.krpc.ktor.client.KtorRPCClient
import kotlinx.rpc.krpc.ktor.client.installRPC
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.ktor.client.rpcConfig
import kotlinx.rpc.krpc.serialization.json.json
import kotlinx.rpc.withService
import kotlin.coroutines.CoroutineContext

internal interface ServiceProvider<ServiceType> {
    val coroutineContext: CoroutineContext
    val service: ServiceType?

    companion object {
        internal fun <ServiceType : RemoteService> create(
            coroutineContext: CoroutineContext,
            endpoint: ServerEndpoint,
        ): ServiceProvider<ServiceType> {
            return ServiceProviderImpl(coroutineContext, endpoint)
        }
    }
}

internal class ServiceProviderImpl<ServiceType : RemoteService>(
    override val coroutineContext: CoroutineContext,
    private val endpoint: ServerEndpoint,
) : ServiceProvider<ServiceType> {

    override var service: ServiceType? = null

    suspend inline fun <reified Service : RemoteService> createService(): Result<ServiceType> =
        runCatching {
            val client = createClient(endpoint)
            @Suppress("UNCHECKED_CAST")
            return@runCatching client.withService<Service>() as ServiceType
        }
}

internal suspend inline fun <reified ServiceType : RemoteService, reified ReturnType> ServiceProvider<ServiceType>.withService(
    onException: ReturnType,
    crossinline func: suspend ServiceType.() -> ReturnType,
): ReturnType {
    if (this !is ServiceProviderImpl) throw RuntimeException("Unknown service provider")

    val currentService = service
    if (currentService == null) {
        val newService = createService<ServiceType>().getOrNull()
        if (newService == null) {
            // TODO: Log it
            return onException
        }
        service = newService
        return func(newService)
    }
    return func(currentService)
}

internal suspend inline fun <reified ServiceType : RemoteService> ServiceProvider<ServiceType>.withServiceOrFailure(
    crossinline func: suspend ServiceType.() -> OperationResult,
): OperationResult {
    return withService(onException = OperationResult.Failure, func = func)
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