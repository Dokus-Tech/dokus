package ai.dokus.app.core.viewmodel

import ai.dokus.app.core.state.DokusState
import ai.dokus.foundation.domain.exceptions.asDokusException
import ai.dokus.foundation.domain.model.common.HealthStatus
import ai.dokus.foundation.domain.usecases.GetCombinedHealthStatusUseCase
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class HealthStatusViewModel(
    getCombinedHealthStatus: GetCombinedHealthStatusUseCase
) : ViewModel() {
    val state = getCombinedHealthStatus
        .execute()
        .map { healthStatuses ->
            DokusState.success(healthStatuses)
        }
        .catch {
            DokusState.error<List<HealthStatus>>(it.asDokusException) {}
        }
}