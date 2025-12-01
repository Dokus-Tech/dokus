package ai.dokus.app.auth.viewmodel

import ai.dokus.app.core.state.DokusState
import ai.dokus.app.core.state.emit
import ai.dokus.app.core.state.emitLoading
import ai.dokus.app.core.viewmodel.BaseViewModel
import ai.dokus.foundation.domain.Email
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

internal class ForgotPasswordViewModel :
    BaseViewModel<DokusState<Unit>>(DokusState.idle()), KoinComponent {

    fun submit(emailValue: Email) {
        scope.launch {
            mutableState.emitLoading()

            runCatching { emailValue.validOrThrows }.getOrElse {
                mutableState.emit(it) { submit(emailValue) }
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
