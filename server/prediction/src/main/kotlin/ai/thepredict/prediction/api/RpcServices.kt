package ai.thepredict.prediction.api

import ai.thepredict.common.withUserIdGetter
import kotlinx.rpc.krpc.ktor.server.RPCRoute

fun RPCRoute.registerPredictionRemoteServices() {
    registerService<PredictionRemoteService> { ctx ->
        PredictionRemoteServiceImpl(ctx, withUserIdGetter())
    }
}