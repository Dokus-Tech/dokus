package ai.thepredict.repository.api

import ai.thepredict.configuration.ServerEndpoint
import ai.thepredict.prediction.api.PredictionRemoteService
import ai.thepredict.repository.helpers.ServiceProvider
import kotlinx.rpc.withService
import kotlin.coroutines.CoroutineContext

interface PredictionApi {

    companion object : ApiCompanion<PredictionApi, ServerEndpoint.Prediction> {
        override fun create(
            coroutineContext: CoroutineContext,
            endpoint: ServerEndpoint.Prediction,
        ): PredictionApi {
            return PredictionApiImpl(coroutineContext, endpoint)
        }

        override fun create(
            coroutineContext: CoroutineContext,
            endpoint: ServerEndpoint.Gateway,
        ): PredictionApi {
            return PredictionApiImpl(coroutineContext, endpoint)
        }
    }
}

private class PredictionApiImpl(
    coroutineContext: CoroutineContext,
    endpoint: ServerEndpoint,
) : PredictionApi {

    private val serviceProvider by lazy {
        ServiceProvider<PredictionRemoteService>(
            coroutineContext,
            endpoint
        ) {
            withService()
        }
    }
}