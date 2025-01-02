package ai.thepredict.repository.helpers

import ai.thepredict.app.platform.persistence
import ai.thepredict.configuration.ServerEndpoint
import ai.thepredict.data.AuthCredentials
import ai.thepredict.domain.api.OperationResult
import ai.thepredict.domain.exceptions.asPredictException
import ai.thepredict.repository.extensions.authCredentials
import ai.thepredict.repository.httpClient
import io.ktor.client.request.basicAuth
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
        retryAttempt: Boolean = false,
        func: suspend ServiceType.(credentials: AuthCredentials?) -> ReturnType,
    ): Result<ReturnType> = withContext(coroutineContext) {
        val currentService = service
        val result: Result<ReturnType> = if (currentService == null) {
            val newServiceResult = createClientAndService()
            val newService = newServiceResult.getOrNull()
            if (newService == null) {
                // TODO: Log it
                return@withContext Result.failure(newServiceResult.asPredictException)
            }
            service = newService

            runCatching { func(newService, persistence.authCredentials) }
        } else {
            runCatching { func(currentService, persistence.authCredentials) }
        }

        if (result.exceptionOrNull() != null && !retryAttempt) { // TODO: Handle connection exception
            service = null
            delay(1.seconds)
            return@withContext withService(true, func)
        }
        return@withContext result
    }

    suspend fun withServiceOrFailure(
        retryAttempt: Boolean = false,
        func: suspend ServiceType.(credentials: AuthCredentials?) -> OperationResult,
    ): OperationResult = withContext(coroutineContext) {
        val currentService = service
        val result = if (currentService == null) {
            val newService = createClientAndService().getOrNull()
            if (newService == null) {
                // TODO: Log it
                return@withContext OperationResult.Failure
            }
            service = newService

            runCatching { func(newService, persistence.authCredentials) }
        } else {
            runCatching { func(currentService, persistence.authCredentials) }
        }

        if (result.exceptionOrNull() != null && !retryAttempt) { // TODO: Handle connection exception
            service = null
            delay(1.seconds)
            return@withContext withServiceOrFailure(true, func)
        }
        return@withContext if (result.isFailure) {
            OperationResult.Failure
        } else OperationResult.Success
    }
}

private suspend inline fun createClient(endpoint: ServerEndpoint): KtorRPCClient {
    val ktorClient = httpClient {
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
            basicAuth(persistence.email.orEmpty(), persistence.password.orEmpty())
            serialization {
                json()
            }
        }
    }
}