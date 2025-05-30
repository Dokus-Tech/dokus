package ai.thepredict.prediction.api

import ai.thepredict.apispec.service.PredictionRemoteService
import ai.thepredict.common.UserIdGetter
import kotlin.coroutines.CoroutineContext

internal class PredictionRemoteServiceImpl(
    override val coroutineContext: CoroutineContext,
    private val userIdGetter: UserIdGetter,
) : PredictionRemoteService