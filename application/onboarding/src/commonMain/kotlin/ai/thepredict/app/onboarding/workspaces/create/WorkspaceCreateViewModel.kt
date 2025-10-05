package ai.thepredict.app.onboarding.workspaces.create

import ai.thepredict.app.core.di
import ai.thepredict.app.core.viewmodel.BaseViewModel
import ai.thepredict.app.platform.persistence
import ai.thepredict.domain.exceptions.PredictException
import ai.thepredict.domain.exceptions.asPredictException
import ai.thepredict.domain.model.Address
import ai.thepredict.domain.model.CreateCompanyRequest
import ai.thepredict.domain.usecases.validators.ValidateNewWorkspaceUseCase
import ai.thepredict.repository.api.UnifiedApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.kodein.di.instance

internal class WorkspaceCreateViewModel :
    BaseViewModel<WorkspaceCreateViewModel.State>(State.Idle) {

    private val validateNewWorkspaceUseCase: ValidateNewWorkspaceUseCase by di.instance()
    private val api: UnifiedApi by di.instance { scope }

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
            runCatching { validateNewWorkspaceUseCase(request) }.getOrElse {
                mutableState.value = State.Error(it.asPredictException)
                return@launch
            }

            val company = api.createCompany(request).getOrElse {
                mutableState.value = State.Error(it.asPredictException)
                return@launch
            }

            persistence.selectedWorkspace = company.id
            mutableEffect.emit(Effect.NavigateHome)
        }
    }

    sealed interface State {
        data object Idle : State

        data object Loading : State

        data class Error(val exception: PredictException) : State
    }

    sealed interface Effect {
        data object NavigateHome : Effect
    }
}