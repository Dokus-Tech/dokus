package ai.dokus.app.viewmodel

import ai.dokus.app.auth.usecases.GetCurrentTenantUseCase
import ai.dokus.app.core.state.DokusState
import ai.dokus.foundation.domain.model.Tenant
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class DashboardViewModel(
    private val getCurrentTenantUseCase: GetCurrentTenantUseCase
) : ViewModel() {
    private val mutableCurrentTenantState = MutableStateFlow<DokusState<Tenant?>>(DokusState.idle())
    val currentTenantState = mutableCurrentTenantState.asStateFlow()

    fun refreshTenant() {
        viewModelScope.launch {
            mutableCurrentTenantState.value = DokusState.loading()

            val nextState = getCurrentTenantUseCase().fold(
                onSuccess = { tenant ->
                    DokusState.success(tenant)
                },
                onFailure = {
                    DokusState.error(it) { refreshTenant() }
                }
            )

            mutableCurrentTenantState.value = nextState
        }
    }
}
