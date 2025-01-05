package ai.thepredict.app.onboarding.workspaces.overview

import ai.thepredict.app.core.di
import ai.thepredict.app.core.extension.launchStreamScoped
import ai.thepredict.app.platform.persistence
import ai.thepredict.data.Workspace
import ai.thepredict.domain.exceptions.PredictException
import ai.thepredict.domain.exceptions.asPredictException
import ai.thepredict.repository.api.UnifiedApi
import ai.thepredict.repository.extensions.selectedWorkspaceId
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.rpc.krpc.streamScoped
import org.kodein.di.instance

internal class WorkspacesViewModel : StateScreenModel<WorkspacesViewModel.State>(State.Loading) {

    private val mutableEffect = MutableStateFlow<Effect>(Effect.None)
    val effect = mutableEffect.asStateFlow()

    private val api: UnifiedApi by di.instance { screenModelScope }

    fun fetch() {
        screenModelScope.launchStreamScoped {
            mutableState.value = State.Loading

            val workspaces = api.myWorkspaces().getOrElse {
                mutableState.value = State.Error(it.asPredictException)
                return@launchStreamScoped
            }

            val workspacesList = workspaces.toList()
            mutableState.value = State.Loaded(workspacesList)
        }
    }

    fun createWorkspace() {
        screenModelScope.launch {
            mutableEffect.emit(Effect.NavigateCreateWorkspace)
        }
    }

    fun continueToHome() {
        screenModelScope.launchStreamScoped {
            val workspaces = api.myWorkspaces().getOrNull()?.toList()
            if (workspaces.isNullOrEmpty()) return@launchStreamScoped

            persistence.selectedWorkspaceId = workspaces.first().id

            mutableEffect.emit(Effect.NavigateHome)
        }
    }

    sealed interface State {
        data object Loading : State
        data class Loaded(val workspaces: List<Workspace>) : State
        data class Error(val exception: PredictException) : State
    }

    sealed interface Effect {
        data object None : Effect
        data object NavigateHome : Effect
        data object NavigateCreateWorkspace : Effect
    }
}