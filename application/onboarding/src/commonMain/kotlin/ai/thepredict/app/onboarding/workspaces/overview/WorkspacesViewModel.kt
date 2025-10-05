package ai.thepredict.app.onboarding.workspaces.overview

import ai.thepredict.apispec.CompanyApi
import ai.thepredict.app.core.viewmodel.BaseViewModel
import ai.thepredict.app.platform.persistence
import ai.thepredict.domain.exceptions.PredictException
import ai.thepredict.domain.exceptions.asPredictException
import ai.thepredict.domain.model.Company
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

internal class WorkspacesViewModel : BaseViewModel<WorkspacesViewModel.State>(State.Loading), KoinComponent {

    private val mutableEffect = MutableSharedFlow<Effect>()
    val effect = mutableEffect.asSharedFlow()

    private val api: CompanyApi by inject()

    fun fetch() {
        scope.launch {
            mutableState.value = State.Loading

            val workspaces = api.getCompanies().getOrElse {
                mutableState.value = State.Error(it.asPredictException)
                return@launch
            }

            mutableState.value = State.Loaded(workspaces)
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
        data class Error(val exception: PredictException) : State
    }

    sealed interface Effect {
        data object NavigateHome : Effect
        data object NavigateCreateWorkspace : Effect
    }
}