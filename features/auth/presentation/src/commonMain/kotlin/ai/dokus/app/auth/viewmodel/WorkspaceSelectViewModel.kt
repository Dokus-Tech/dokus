package ai.dokus.app.auth.viewmodel

import ai.dokus.app.auth.datasource.TenantRemoteDataSource
import ai.dokus.app.auth.usecases.SelectTenantUseCase
import ai.dokus.app.core.state.DokusState
import ai.dokus.app.core.state.emit
import ai.dokus.app.core.state.emitLoading
import ai.dokus.app.core.viewmodel.BaseViewModel
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.model.Tenant
import ai.dokus.foundation.platform.Logger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

internal class WorkspaceSelectViewModel(
    private val tenantDataSource: TenantRemoteDataSource,
    private val selectTenantUseCase: SelectTenantUseCase
) : BaseViewModel<DokusState<List<Tenant>>>(DokusState.idle()) {

    private val logger = Logger.forClass<WorkspaceSelectViewModel>()
    private val mutableEffect = MutableSharedFlow<Effect>()
    val effect = mutableEffect.asSharedFlow()

    fun loadTenants() {
        scope.launch {
            mutableState.emitLoading()
            tenantDataSource.listMyTenants()
                .onSuccess { tenants ->
                    mutableState.emit(tenants)
                }
                .onFailure { error ->
                    logger.e(error) { "Failed to load tenants" }
                    mutableState.emit(error) { loadTenants() }
                }
        }
    }

    fun selectTenant(tenantId: TenantId) {
        scope.launch {
            selectTenantUseCase(tenantId)
                .onSuccess {
                    mutableEffect.emit(Effect.WorkspaceSelected)
                }.onFailure { error ->
                    logger.e(error) { "Failed to select tenant $tenantId" }
                    mutableEffect.emit(Effect.SelectionFailed(error))
                }
        }
    }

    sealed interface Effect {
        data object WorkspaceSelected : Effect
        data class SelectionFailed(val error: Throwable) : Effect
    }
}
