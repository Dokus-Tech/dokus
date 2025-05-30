package ai.thepredict.simulation.api

import ai.thepredict.apispec.service.SimulationRemoteService
import ai.thepredict.common.withUserIdGetter
import kotlinx.rpc.krpc.ktor.server.KrpcRoute

fun KrpcRoute.registerSimulationRemoteServices() {
    registerService<SimulationRemoteService> { ctx ->
        SimulationRemoteServiceImpl(ctx, withUserIdGetter())
    }
}