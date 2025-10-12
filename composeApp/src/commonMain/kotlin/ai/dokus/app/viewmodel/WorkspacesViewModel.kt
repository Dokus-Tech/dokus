package ai.dokus.app.viewmodel

import ai.dokus.app.core.viewmodel.BaseViewModel
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.model.Company
import ai.dokus.foundation.platform.persistence
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

internal class WorkspacesViewModel : BaseViewModel<WorkspacesViewModel.State>(State.Loading),
    KoinComponent {

    private val mutableEffect = MutableSharedFlow<Effect>()
    val effect = mutableEffect.asSharedFlow()

//    private val api: CompanyApi by inject()

    fun fetch() {
        scope.launch {
            mutableState.value = State.Loading
//
//            val workspaces = api.getCompanies().getOrElse {
//                mutableState.value = State.Error(it.asDokusException)
//                return@launch
//            }

//            mutableState.value = State.Loaded(workspaces)
        }
    }

    fun selectWorkspace(workspace: Company) {
        scope.launch {
            persistence.selectedWorkspace = workspace.id
            mutableEffect.emit(Effect.NavigateHome)
        }
    }

    fun createWorkspace() {
        scope.launch {
            mutableEffect.emit(Effect.NavigateCreateWorkspace)
        }
    }

    sealed interface State {
        data object Loading : State
        data class Loaded(val workspaces: List<Company>) : State
        data class Error(val exception: DokusException) : State
    }

    sealed interface Effect {
        data object NavigateHome : Effect
        data object NavigateCreateWorkspace : Effect
    }
}