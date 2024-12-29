package ai.thepredict.app.onboarding.workspaces

import ai.thepredict.app.core.di
import ai.thepredict.app.core.extension.launchStreamScoped
import ai.thepredict.domain.Workspace
import ai.thepredict.repository.api.UnifiedApi
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.toList
import org.kodein.di.instance

class WorkspaceSelectionViewModel : StateScreenModel<WorkspaceSelectionViewModel.State>(State.Loading) {

    private val api: UnifiedApi by di.instance { screenModelScope }

    fun fetch() {
        screenModelScope.launchStreamScoped {
            mutableState.value = State.Loaded(api.myWorkspaces().toList())
        }
    }

    sealed interface State {
        data object Loading : State

        data class Loaded(val workspaces: List<Workspace>) : State

        data class Error(val throwable: Throwable) : State
    }
}