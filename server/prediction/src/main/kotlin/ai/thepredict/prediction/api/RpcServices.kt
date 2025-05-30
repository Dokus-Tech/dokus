package ai.thepredict.prediction.api

import ai.thepredict.apispec.service.PredictionRemoteService
import ai.thepredict.common.withUserIdGetter
import kotlinx.rpc.krpc.ktor.server.KrpcRoute

fun KrpcRoute.registerPredictionRemoteServices() {
    registerService<PredictionRemoteService> { ctx ->
        PredictionRemoteServiceImpl(ctx, withUserIdGetter())
    }
}