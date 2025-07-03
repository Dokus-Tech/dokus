package ai.thepredict.app.onboarding.workspaces.overview

import ai.thepredict.apispec.CompanyApi
import ai.thepredict.app.core.di
import ai.thepredict.domain.exceptions.PredictException
import ai.thepredict.domain.exceptions.asPredictException
import ai.thepredict.domain.model.Company
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.kodein.di.instance

internal class WorkspacesViewModel : StateScreenModel<WorkspacesViewModel.State>(State.Loading) {

    private val mutableEffect = MutableSharedFlow<Effect>()
    val effect = mutableEffect.asSharedFlow()

    private val api: CompanyApi by di.instance()

    fun fetch() {
        screenModelScope.launch {
            mutableState.value = State.Loading

            val workspaces = api.getCompanies().getOrElse {
                mutableState.value = State.Error(it.asPredictException)
                return@launch
            }

            mutableState.value = State.Loaded(workspaces)
        }
    }

    fun createWorkspace() {
        screenModelScope.launch {
            mutableEffect.emit(Effect.NavigateCreateWorkspace)
        }
    }

    fun continueToHome() {
        screenModelScope.launch {
//            val workspaces = api.myWorkspaces().getOrNull()?.toList()
//            if (workspaces.isNullOrEmpty()) return@launch
//
//            persistence.selectedWorkspaceId = workspaces.first().id
//
//            mutableEffect.emit(Effect.NavigateHome)
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