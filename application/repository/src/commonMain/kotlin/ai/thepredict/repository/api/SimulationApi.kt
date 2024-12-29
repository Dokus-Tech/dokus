package ai.thepredict.repository.api

import ai.thepredict.configuration.ServerEndpoint
import ai.thepredict.repository.helpers.ServiceProvider
import ai.thepredict.simulation.api.SimulationRemoteService
import kotlinx.rpc.withService
import kotlin.coroutines.CoroutineContext

interface SimulationApi {

    companion object : ApiCompanion<SimulationApi, ServerEndpoint.Simulation> {
        override fun create(
            coroutineContext: CoroutineContext,
            endpoint: ServerEndpoint.Simulation,
        ): SimulationApi {
            return SimulationApiImpl(coroutineContext, endpoint)
        }

        override fun create(
            coroutineContext: CoroutineContext,
            endpoint: ServerEndpoint.Gateway,
        ): SimulationApi {
            return SimulationApiImpl(coroutineContext, endpoint)
        }
    }
}

private class SimulationApiImpl(
    coroutineContext: CoroutineContext,
    endpoint: ServerEndpoint,
) : SimulationApi {

    private val serviceProvider = ServiceProvider<SimulationRemoteService>(
        coroutineContext = coroutineContext,
        endpoint = endpoint,
        createService = { withService() }
    )
}