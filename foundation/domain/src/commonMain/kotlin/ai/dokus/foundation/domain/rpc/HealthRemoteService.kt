package ai.dokus.foundation.domain.rpc

import ai.dokus.foundation.domain.model.common.HealthStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.rpc.annotations.Rpc

@Rpc
interface HealthRemoteService {
    fun getHealthFlow(): Flow<HealthStatus>

    companion object {
        val stub: HealthRemoteService = object : HealthRemoteService {
            override fun getHealthFlow(): Flow<HealthStatus> = emptyFlow()
        }
    }
}