package ai.dokus.app.cashflow.usecase

import ai.dokus.app.cashflow.components.BusinessHealthData
import tech.dokus.foundation.app.state.DokusState
import ai.dokus.foundation.domain.exceptions.DokusException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Use case for loading business health metrics.
 *
 * TODO: Replace placeholder implementation with actual API call
 * when the business health endpoint becomes available.
 */
class LoadBusinessHealthUseCase {

    /**
     * Load business health data.
     *
     * @return Flow emitting loading state then success/error with health data.
     */
    operator fun invoke(): Flow<DokusState<BusinessHealthData>> = flow {
        emit(DokusState.loading())
        try {
            // TODO: Replace with actual API call when endpoint is available
            val data = BusinessHealthData.empty
            emit(DokusState.success(data))
        } catch (e: Exception) {
            emit(DokusState.error(DokusException.Unknown(e)) { /* No-op: placeholder implementation */ })
        }
    }
}
