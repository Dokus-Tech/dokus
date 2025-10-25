package ai.dokus.app.viewmodel

import ai.dokus.app.core.viewmodel.BaseViewModel
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.model.Address
import ai.dokus.foundation.domain.model.CreateCompanyRequest
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

internal class WorkspaceCreateViewModel :
    BaseViewModel<WorkspaceCreateViewModel.State>(State.Idle) {
    private val mutableEffect = MutableSharedFlow<Effect>()
    val effect = mutableEffect.asSharedFlow()

    fun create(
        name: String,
        taxNumber: String,
        address: Address,
    ) {
        scope.launch {
            mutableState.value = State.Loading

            val request = CreateCompanyRequest(
                name = name,
                taxId = taxNumber,
                address = address,
            )
//            runCatching { validateNewWorkspaceUseCase(request) }.getOrElse {
//                mutableState.value = State.Error(it.asDokusException)
//                return@launch
//            }

//            val company = api.createCompany(request).getOrElse {
//                mutableState.value = State.Error(it.asDokusException)
//                return@launch
//            }
//
//            persistence.selectedWorkspace = company.id
//            mutableEffect.emit(Effect.NavigateHome)
        }
    }

    sealed interface State {
        data object Idle : State

        data object Loading : State

        data class Error(val exception: DokusException) : State
    }

    sealed interface Effect {
        data object NavigateHome : Effect
    }
}