package ai.dokus.app.onboarding.workspaces.create

import ai.dokus.app.core.viewmodel.BaseViewModel
import ai.dokus.foundation.platform.persistence
import ai.dokus.foundation.domain.exceptions.PredictException
import ai.dokus.foundation.domain.exceptions.asPredictException
import ai.dokus.foundation.domain.model.Address
import ai.dokus.foundation.domain.model.CreateCompanyRequest
import ai.dokus.foundation.domain.usecases.validators.ValidateNewWorkspaceUseCase
import ai.dokus.app.repository.api.UnifiedApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

internal class WorkspaceCreateViewModel :
    BaseViewModel<WorkspaceCreateViewModel.State>(State.Idle), KoinComponent {

    private val validateNewWorkspaceUseCase: ValidateNewWorkspaceUseCase by inject()
    private val api: UnifiedApi by inject()

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