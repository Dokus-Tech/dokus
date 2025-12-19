package ai.dokus.app.auth.viewmodel

import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.app.state.emit
import tech.dokus.foundation.app.state.emitLoading
import tech.dokus.foundation.app.viewmodel.BaseViewModel
import ai.dokus.foundation.domain.Password
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

internal class NewPasswordViewModel : BaseViewModel<DokusState<Unit>>(DokusState.idle()), KoinComponent {

    fun submit(password: Password, passwordConfirmation: Password) {
        scope.launch {
            mutableState.emitLoading()

            runCatching { password.validOrThrows }.getOrElse {
                mutableState.emit(it) { submit(password, passwordConfirmation) }
                return@launch
            }
//
//        persistence.user = api.authenticate(emailValue, passwordValue).getOrElse {
//            mutableState.value = State.Error(it.asPredictException)
//            return@launch
//        }

            mutableState.emit(Unit)
        }
    }
}
