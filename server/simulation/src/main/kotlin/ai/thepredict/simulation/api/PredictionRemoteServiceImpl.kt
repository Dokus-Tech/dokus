package ai.thepredict.simulation.api

import ai.thepredict.apispec.service.SimulationRemoteService
import ai.thepredict.common.UserIdGetter
import kotlin.coroutines.CoroutineContext

internal class SimulationRemoteServiceImpl(
    override val coroutineContext: CoroutineContext,
    private val userIdGetter: UserIdGetter,
) : SimulationRemoteService