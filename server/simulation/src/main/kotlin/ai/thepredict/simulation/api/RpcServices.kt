package ai.thepredict.simulation.api

import ai.thepredict.common.withUserIdGetter
import kotlinx.rpc.krpc.ktor.server.RPCRoute

fun RPCRoute.registerSimulationRemoteServices() {
    registerService<SimulationRemoteService> { ctx ->
        SimulationRemoteServiceImpl(ctx, withUserIdGetter())
    }
}