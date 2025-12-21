package tech.dokus.app.viewmodel

import ai.dokus.app.auth.datasource.TenantRemoteDataSource
import ai.dokus.app.auth.usecases.GetCurrentTenantUseCase
import ai.dokus.foundation.domain.model.CompanyAvatar
import ai.dokus.foundation.domain.model.Tenant
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    savedStateHandle: SavedStateHandle,
    private val getCurrentTenantUseCase: GetCurrentTenantUseCase,
    private val tenantDataSource: TenantRemoteDataSource
) : ViewModel() {

    private val _currentTenant = MutableStateFlow<Tenant?>(null)
    val currentTenant: StateFlow<Tenant?> = _currentTenant.asStateFlow()

    private val _currentAvatar = MutableStateFlow<CompanyAvatar?>(null)
    val currentAvatar: StateFlow<CompanyAvatar?> = _currentAvatar.asStateFlow()

    init {
        loadTenantInfo()
    }

    private fun loadTenantInfo() {
        viewModelScope.launch {
            getCurrentTenantUseCase().onSuccess { tenant ->
                _currentTenant.value = tenant
            }
            tenantDataSource.getAvatar().onSuccess { avatar ->
                _currentAvatar.value = avatar
            }
        }
    }

    /**
     * Refresh tenant and avatar info (e.g., after avatar upload).
     */
    fun refreshTenantInfo() {
        loadTenantInfo()
    }
}