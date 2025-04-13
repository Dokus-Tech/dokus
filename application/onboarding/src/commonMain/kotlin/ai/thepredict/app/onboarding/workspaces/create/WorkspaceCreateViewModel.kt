package ai.thepredict.app.onboarding.workspaces.create

import ai.thepredict.app.core.di
import ai.thepredict.domain.exceptions.PredictException
import ai.thepredict.domain.exceptions.asPredictException
import ai.thepredict.domain.model.Workspace
import ai.thepredict.domain.usecases.CreateNewWorkspaceUseCase
import ai.thepredict.repository.api.UnifiedApi
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.launch
import org.kodein.di.instance

internal class WorkspaceCreateViewModel : StateScreenModel<WorkspaceCreateViewModel.State>(State.Idle) {

    private val createNewWorkspaceUseCase: CreateNewWorkspaceUseCase by di.instance()
    private val api: UnifiedApi by di.instance { screenModelScope }

    fun create(name: String, legalName: String, taxNumber: String) {
        screenModelScope.launch {
            mutableState.value = State.Loading

            val newWorkspace = createNewWorkspaceUseCase(name, legalName, taxNumber).getOrElse {
                mutableState.value = State.Error(it.asPredictException)
                return@launch
            }

            val workspace = api.createWorkspace(newWorkspace).getOrElse {
                mutableState.value = State.Error(it.asPredictException)
                return@launch
            }

            mutableState.value = State.Loaded(workspace)
        }
    }

    sealed interface State {
        data object Idle : State

        data object Loading : State

        data class Loaded(val workspace: Workspace) : State

        data class Error(val exception: PredictException) : State
    }
}