package tech.dokus.app.viewmodel

import ai.dokus.app.auth.usecases.GetCurrentTenantUseCase
import tech.dokus.foundation.app.state.DokusState
import ai.dokus.foundation.domain.model.Tenant
import ai.dokus.foundation.platform.Logger
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the main settings screen.
 * Manages the current workspace/tenant state for the workspace picker.
 */
class SettingsViewModel(
    private val getCurrentTenantUseCase: GetCurrentTenantUseCase
) : ViewModel() {

    private val logger = Logger.forClass<SettingsViewModel>()

    private val _currentTenantState = MutableStateFlow<DokusState<Tenant?>>(DokusState.idle())
    val currentTenantState: StateFlow<DokusState<Tenant?>> = _currentTenantState.asStateFlow()

    /**
     * Load the current tenant/workspace.
     * Called when the settings screen is displayed.
     */
    fun loadCurrentTenant() {
        viewModelScope.launch {
            logger.d { "Loading current tenant" }
            _currentTenantState.value = DokusState.loading()

            getCurrentTenantUseCase().fold(
                onSuccess = { tenant ->
                    logger.i { "Current tenant loaded: ${tenant?.displayName?.value}" }
                    _currentTenantState.value = DokusState.success(tenant)
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to load current tenant" }
                    _currentTenantState.value = DokusState.error(error) { loadCurrentTenant() }
                }
            )
        }
    }

    /**
     * Refresh the current tenant after returning from workspace selection.
     */
    fun refresh() {
        loadCurrentTenant()
    }
}
