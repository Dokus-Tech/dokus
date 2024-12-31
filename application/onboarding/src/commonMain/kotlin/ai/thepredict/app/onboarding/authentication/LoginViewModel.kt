package ai.thepredict.app.onboarding.authentication

import ai.thepredict.app.core.di
import ai.thepredict.app.core.extension.launchStreamScoped
import ai.thepredict.domain.Contact
import ai.thepredict.repository.api.UnifiedApi
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.toList
import org.kodein.di.instance

internal class LoginViewModel : StateScreenModel<LoginViewModel.State>(State.Loading) {

    private val api: UnifiedApi by di.instance { screenModelScope }

    fun fetch() {
        screenModelScope.launchStreamScoped {
            api.myWorkspaces()
            mutableState.value = State.Loaded(api.getAll().getOrNull()?.toList() ?: emptyList())
        }
    }

    sealed interface State {
        data object Loading : State

        data class Loaded(val contacts: List<Contact>) : State

        data class Error(val throwable: Throwable) : State
    }
}