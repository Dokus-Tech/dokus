package ai.thepredict.app.onboarding.login

import ai.thepredict.app.core.di
import ai.thepredict.app.core.extension.launchStreamScoped
import ai.thepredict.domain.Contact
import ai.thepredict.repository.api.UnifiedApi
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.rpc.krpc.streamScoped
import org.kodein.di.instance

class LoginViewModel : StateScreenModel<LoginViewModel.State>(State.Loading) {

    private val api: UnifiedApi by di.instance()

    fun fetch() {
        screenModelScope.launchStreamScoped {
            mutableState.value = State.Loaded(api.getAll().toList())
        }
    }

    sealed interface State {
        data object Loading : State

        data class Loaded(val contacts: List<Contact>) : State

        data class Error(val throwable: Throwable) : State
    }
}