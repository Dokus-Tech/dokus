package ai.thepredict.app.onboarding.workspaces.create

import ai.thepredict.app.core.di
import ai.thepredict.app.core.extension.launchStreamScoped
import ai.thepredict.data.Workspace
import ai.thepredict.domain.exceptions.PredictException
import ai.thepredict.domain.exceptions.asPredictException
import ai.thepredict.domain.usecases.CreateNewUserUseCase
import ai.thepredict.domain.usecases.CreateNewWorkspaceUseCase
import ai.thepredict.repository.api.UnifiedApi
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.toList
import org.kodein.di.instance

internal class WorkspaceCreateViewModel : StateScreenModel<WorkspaceCreateViewModel.State>(State.Idle) {

    private val createNewWorkspaceUseCase: CreateNewWorkspaceUseCase by di.instance()
    private val api: UnifiedApi by di.instance { screenModelScope }

    fun create(name: String, legalName: String, taxNumber: String) {
        screenModelScope.launchStreamScoped {
            mutableState.value = State.Loading

            val newWorkspace = createNewWorkspaceUseCase(name, legalName, taxNumber).getOrElse {
                mutableState.value = State.Error(it.asPredictException)
                return@launchStreamScoped
            }

            val workspace = api.createWorkspace(newWorkspace).getOrElse {
                mutableState.value = State.Error(it.asPredictException)
                return@launchStreamScoped
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