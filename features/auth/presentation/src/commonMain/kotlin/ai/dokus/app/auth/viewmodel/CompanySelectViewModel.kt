package ai.dokus.app.auth.viewmodel

import ai.dokus.app.auth.domain.TenantRemoteService
import ai.dokus.app.auth.usecases.SelectTenantUseCase
import ai.dokus.app.core.state.DokusState
import ai.dokus.app.core.viewmodel.BaseViewModel
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.model.Tenant
import ai.dokus.foundation.platform.Logger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

internal class CompanySelectViewModel(
    private val tenantRemoteService: TenantRemoteService,
    private val selectTenantUseCase: SelectTenantUseCase
) : BaseViewModel<DokusState<List<Tenant>>>(DokusState.idle()) {

    private val logger = Logger.forClass<CompanySelectViewModel>()
    private val mutableEffect = MutableSharedFlow<Effect>()
    val effect = mutableEffect.asSharedFlow()

    fun loadTenants() {
        scope.launch {
            mutableState.value = DokusState.loading()
            runCatching {
                tenantRemoteService.listMyTenants()
            }.onSuccess { tenants ->
                mutableState.value = DokusState.success(tenants)
            }.onFailure { error ->
                logger.e(error) { "Failed to load tenants" }
                mutableState.value = DokusState.error(
                    exception = error,
                    retryHandler = { loadTenants() }
                )
            }
        }
    }

    fun selectTenant(tenantId: TenantId) {
        scope.launch {
            runCatching {
                selectTenantUseCase(tenantId).getOrThrow()
            }.onSuccess {
                mutableEffect.emit(Effect.CompanySelected)
            }.onFailure { error ->
                logger.e(error) { "Failed to select tenant $tenantId" }
                mutableEffect.emit(Effect.SelectionFailed(error))
            }
        }
    }

    sealed interface Effect {
        data object CompanySelected : Effect
        data class SelectionFailed(val error: Throwable) : Effect
    }
}
