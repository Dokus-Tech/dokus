package ai.dokus.app.auth.viewmodel

import ai.dokus.app.auth.repository.AuthRepository
import ai.dokus.app.core.state.DokusState
import ai.dokus.app.core.viewmodel.BaseViewModel
import ai.dokus.foundation.domain.asbtractions.RetryHandler
import ai.dokus.foundation.domain.ids.OrganizationId
import ai.dokus.foundation.domain.model.Organization
import ai.dokus.foundation.domain.rpc.OrganizationRemoteService
import ai.dokus.foundation.platform.Logger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

internal class CompanySelectViewModel(
    private val organizationRemoteService: OrganizationRemoteService,
    private val authRepository: AuthRepository
) : BaseViewModel<DokusState<List<Organization>>>(DokusState.idle()) {

    private val logger = Logger.forClass<CompanySelectViewModel>()
    private val mutableEffect = MutableSharedFlow<Effect>()
    val effect = mutableEffect.asSharedFlow()

    fun loadOrganizations() = scope.launch {
        mutableState.value = DokusState.loading()
        runCatching {
            organizationRemoteService.listMyOrganizations()
        }.onSuccess { organizations ->
            mutableState.value = DokusState.success(organizations)
        }.onFailure { error ->
            logger.e(error) { "Failed to load organizations" }
            mutableState.value = DokusState.error(
                exception = error,
                retryHandler = { loadOrganizations() }
            )
        }
    }

    fun selectOrganization(organizationId: OrganizationId) = scope.launch {
        runCatching {
            authRepository.selectOrganization(organizationId).getOrThrow()
        }.onSuccess {
            mutableEffect.emit(Effect.SelectionCompleted)
        }.onFailure { error ->
            logger.e(error) { "Failed to select organization $organizationId" }
            mutableEffect.emit(Effect.SelectionFailed(error))
        }
    }

    sealed interface Effect {
        data object SelectionCompleted : Effect
        data class SelectionFailed(val error: Throwable) : Effect
    }
}
