package ai.thepredict.app.onboarding.workspaces.overview

import ai.thepredict.app.core.di
import ai.thepredict.app.core.extension.launchStreamScoped
import ai.thepredict.domain.Workspace
import ai.thepredict.domain.exceptions.PredictException
import ai.thepredict.repository.api.UnifiedApi
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.toList
import org.kodein.di.instance

internal class WorkspacesViewModel : StateScreenModel<WorkspacesViewModel.State>(State.Loading) {

    private val api: UnifiedApi by di.instance { screenModelScope }

    fun fetch() {
        screenModelScope.launchStreamScoped {
            mutableState.value =
                State.Loaded(api.myWorkspaces().getOrNull()?.toList() ?: emptyList())
        }
    }

    sealed interface State {
        data object Loading : State

        data class Loaded(val workspaces: List<Workspace>) : State

        data class Error(val exception: PredictException) : State
    }
}